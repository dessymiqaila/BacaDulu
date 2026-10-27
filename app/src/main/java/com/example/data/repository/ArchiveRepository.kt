package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.withTransaction
import com.example.data.dao.BacaDuluDao
import com.example.data.database.BacaDuluDatabase
import com.example.data.demo.DemoDataProvider
import com.example.data.model.PostEntity
import com.example.data.model.PostWithSite
import com.example.data.model.SiteEntity
import com.example.data.parser.ParsedPost
import com.example.data.parser.WordPressDumpParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupInspection(
    val siteUrl: String,
    val existingSite: SiteEntity?,
    val existingPostsCount: Int,
    val fileUri: Uri? = null,
    val isDemo: Boolean = false
)

class ArchiveRepository(
    private val database: BacaDuluDatabase,
    private val dao: BacaDuluDao
) {
    val allSites: Flow<List<SiteEntity>> = dao.getAllSitesFlow()
    val bookmarkedPosts: Flow<List<PostWithSite>> = dao.getBookmarkedPostsFlow()
    val readingHistory: Flow<List<PostWithSite>> = dao.getReadingHistoryFlow()
    val totalPostsCount: Flow<Int> = dao.getTotalPostsCount()
    val totalSitesCount: Flow<Int> = dao.getTotalSitesCount()

    suspend fun getRandom15Posts(): List<PostWithSite> = withContext(Dispatchers.IO) {
        dao.getRandom15Posts()
    }

    fun searchPosts(query: String): Flow<List<PostWithSite>> {
        return dao.searchPostsAlphabetical(query.trim())
    }

    fun getPostFlow(postId: Int): Flow<PostWithSite?> {
        return dao.getPostWithSiteByIdFlow(postId)
    }

    suspend fun getPost(postId: Int): PostWithSite? = withContext(Dispatchers.IO) {
        dao.getPostWithSiteById(postId)
    }

    suspend fun getPreviousPostId(siteId: Int, currentId: Int): Int? = withContext(Dispatchers.IO) {
        dao.getPreviousPostId(siteId, currentId)
    }

    suspend fun getNextPostId(siteId: Int, currentId: Int): Int? = withContext(Dispatchers.IO) {
        dao.getNextPostId(siteId, currentId)
    }

    suspend fun toggleBookmark(postId: Int, currentlyBookmarked: Boolean) = withContext(Dispatchers.IO) {
        val newStatus = if (currentlyBookmarked) 0 else 1
        val timestamp = if (newStatus == 1) System.currentTimeMillis() else null
        dao.setBookmark(postId, newStatus, timestamp)
    }

    suspend fun recordPostRead(postId: Int) = withContext(Dispatchers.IO) {
        dao.updateLastRead(postId, System.currentTimeMillis())
    }

    suspend fun clearReadingHistory() = withContext(Dispatchers.IO) {
        dao.clearReadingHistory()
    }

    suspend fun deleteSite(siteId: Int) = withContext(Dispatchers.IO) {
        database.withTransaction {
            dao.deletePostsBySiteId(siteId)
            dao.deleteSiteById(siteId)
        }
    }

    /**
     * Inspects backup file header lightly without loading heavy post content into memory.
     * Prevents OOM during inspection of large files with 5000+ posts.
     */
    suspend fun inspectBackupFile(
        context: Context,
        uri: Uri,
        fileName: String
    ): BackupInspection = withContext(Dispatchers.IO) {
        val detectedSiteUrl = context.contentResolver.openInputStream(uri)?.use { stream ->
            WordPressDumpParser.quickScanSiteUrl(stream, fileName.substringBeforeLast('.'))
        } ?: WordPressDumpParser.cleanSiteUrl(fileName.substringBeforeLast('.'))

        val existingSite = dao.getSiteByUrl(detectedSiteUrl)
        val existingCount = if (existingSite != null) dao.getPostsCountForSite(existingSite.id) else 0

        BackupInspection(
            siteUrl = detectedSiteUrl,
            existingSite = existingSite,
            existingPostsCount = existingCount,
            fileUri = uri,
            isDemo = false
        )
    }

    /**
     * Inspects demo data for testing
     */
    suspend fun inspectDemoData(): BackupInspection = withContext(Dispatchers.IO) {
        val demoUrl = DemoDataProvider.DEMO_SITE_URL
        val existingSite = dao.getSiteByUrl(demoUrl)
        val existingCount = if (existingSite != null) dao.getPostsCountForSite(existingSite.id) else 0

        BackupInspection(
            siteUrl = demoUrl,
            existingSite = existingSite,
            existingPostsCount = existingCount,
            fileUri = null,
            isDemo = true
        )
    }

    /**
     * Streams and saves posts in memory-efficient batches directly to SQLite.
     * Completely prevents OOM and transaction crash on 5000+ novel chapter dumps.
     */
    suspend fun streamImportBackup(
        context: Context,
        uri: Uri,
        siteUrlHint: String,
        overwrite: Boolean,
        onStatus: (String) -> Unit,
        onProgress: (Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var targetSiteId = -1
        var finalSiteUrl = siteUrlHint

        val totalSaved = WordPressDumpParser.streamParseAndSave(
            context = context,
            uri = uri,
            fallbackName = siteUrlHint,
            onSiteIdentified = { identifiedUrl ->
                finalSiteUrl = identifiedUrl
                // Prepare or fetch site record
                val existing = dao.getSiteByUrl(finalSiteUrl)
                if (existing != null) {
                    if (overwrite) {
                        dao.deletePostsBySiteId(existing.id)
                        dao.updateSite(existing.copy(importedAt = System.currentTimeMillis()))
                    }
                    targetSiteId = existing.id
                } else {
                    val newSite = SiteEntity(
                        siteUrl = finalSiteUrl,
                        totalPosts = 0,
                        importedAt = System.currentTimeMillis()
                    )
                    targetSiteId = dao.insertSite(newSite).toInt()
                }
            },
            onBatchReady = { batch, totalSoFar ->
                if (targetSiteId != -1 && batch.isNotEmpty()) {
                    saveBatchToDb(targetSiteId, finalSiteUrl, batch)
                    onProgress(totalSoFar)
                }
            },
            onStatus = onStatus
        )

        if (targetSiteId != -1) {
            val count = dao.getPostsCountForSite(targetSiteId)
            dao.updateSiteTotalPosts(targetSiteId, count)
        }

        totalSaved
    }

    /**
     * Imports demo data in safe batches
     */
    suspend fun importDemoData(
        overwrite: Boolean,
        onProgress: (Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val demo = DemoDataProvider.getDemoBackupData()
        val existing = dao.getSiteByUrl(demo.siteUrl)
        val siteId = if (existing != null) {
            if (overwrite) {
                dao.deletePostsBySiteId(existing.id)
                dao.updateSite(existing.copy(importedAt = System.currentTimeMillis()))
            }
            existing.id
        } else {
            val newSite = SiteEntity(
                siteUrl = demo.siteUrl,
                totalPosts = demo.posts.size,
                importedAt = System.currentTimeMillis()
            )
            dao.insertSite(newSite).toInt()
        }

        saveBatchToDb(siteId, demo.siteUrl, demo.posts)
        onProgress(demo.posts.size)
        val count = dao.getPostsCountForSite(siteId)
        dao.updateSiteTotalPosts(siteId, count)
        count
    }

    private suspend fun saveBatchToDb(
        siteId: Int,
        siteUrl: String,
        posts: List<ParsedPost>
    ) {
        val entities = posts.map { p ->
            PostEntity(
                siteId = siteId,
                uniqueKey = "$siteUrl-${p.originalId}",
                originalId = p.originalId,
                postTitle = p.postTitle,
                postContent = p.postContent,
                postDate = p.postDate,
                isBookmarked = 0,
                bookmarkedAt = null,
                lastReadAt = null
            )
        }
        try {
            dao.insertPosts(entities)
        } catch (e: Exception) {
            // Fallback: insert individually if any single row has encoding conflict
            for (item in entities) {
                try {
                    dao.insertPosts(listOf(item))
                } catch (_: Exception) {}
            }
        }
    }

    /**
     * Exports the active Room SQLite database file to the user's Downloads folder
     */
    suspend fun exportDatabaseToDownloads(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            val dbFile = context.getDatabasePath(BacaDuluDatabase.DATABASE_NAME)
            if (!dbFile.exists()) {
                return@withContext Result.failure(Exception("File database belum dibuat."))
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val exportFileName = "bacadulu_backup_$timestamp.db"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, exportFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.sqlite3")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/BacaDulu")
                }

                val uri: Uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext Result.failure(Exception("Gagal mengalokasikan file di Downloads."))

                context.contentResolver.openOutputStream(uri).use { output ->
                    FileInputStream(dbFile).use { input ->
                        input.copyTo(output ?: throw Exception("Output stream null"))
                    }
                }
                Result.success("Tersimpan di Download/BacaDulu/$exportFileName")
            } else {
                @Suppress("DEPRECATION")
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadDir, "BacaDulu")
                if (!targetDir.exists()) targetDir.mkdirs()
                val targetFile = File(targetDir, exportFileName)

                FileInputStream(dbFile).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Result.success("Tersimpan di ${targetFile.absolutePath}")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
