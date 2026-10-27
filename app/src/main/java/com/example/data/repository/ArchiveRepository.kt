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
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupInspection(
    val siteUrl: String,
    val incomingPosts: List<ParsedPost>,
    val existingSite: SiteEntity?,
    val existingPostsCount: Int
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
     * Inspects backup input stream, extracts siteurl and counts incoming posts,
     * checking against existing database records for duplicate prevention.
     */
    suspend fun inspectBackupStream(
        inputStream: InputStream,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): BackupInspection = withContext(Dispatchers.IO) {
        val scan = WordPressDumpParser.parseDump(inputStream, onProgress)
        val existingSite = dao.getSiteByUrl(scan.siteUrl)
        val existingCount = if (existingSite != null) dao.getPostsCountForSite(existingSite.id) else 0

        BackupInspection(
            siteUrl = scan.siteUrl,
            incomingPosts = scan.posts,
            existingSite = existingSite,
            existingPostsCount = existingCount
        )
    }

    /**
     * Inspects demo data for testing
     */
    suspend fun inspectDemoData(): BackupInspection = withContext(Dispatchers.IO) {
        val scan = DemoDataProvider.getDemoBackupData()
        val existingSite = dao.getSiteByUrl(scan.siteUrl)
        val existingCount = if (existingSite != null) dao.getPostsCountForSite(existingSite.id) else 0

        BackupInspection(
            siteUrl = scan.siteUrl,
            incomingPosts = scan.posts,
            existingSite = existingSite,
            existingPostsCount = existingCount
        )
    }

    /**
     * Executes the actual import in batch transactions
     */
    suspend fun executeImport(
        siteUrl: String,
        posts: List<ParsedPost>,
        overwrite: Boolean,
        onProgress: (insertedCount: Int, totalCount: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        database.withTransaction {
            val existing = dao.getSiteByUrl(siteUrl)
            val siteId = if (existing != null) {
                if (overwrite) {
                    dao.deletePostsBySiteId(existing.id)
                    dao.updateSite(
                        existing.copy(
                            totalPosts = posts.size,
                            importedAt = System.currentTimeMillis()
                        )
                    )
                }
                existing.id
            } else {
                val newSite = SiteEntity(
                    siteUrl = siteUrl,
                    totalPosts = posts.size,
                    importedAt = System.currentTimeMillis()
                )
                dao.insertSite(newSite).toInt()
            }

            // Batch insert posts (chunk size 100)
            val chunkSize = 100
            var insertedTotal = 0

            posts.chunked(chunkSize).forEach { chunk ->
                val entities = chunk.map { p ->
                    PostEntity(
                        siteId = siteId,
                        uniqueKey = "$siteUrl-${p.originalId}",
                        originalId = p.originalId,
                        postTitle = p.postTitle,
                        postContent = p.postContent,
                        postDate = p.postDate,
                        isBookmarked = 0,
                        bookmarkedAt = null
                    )
                }
                dao.insertPosts(entities)
                insertedTotal += entities.size
                onProgress(insertedTotal, posts.size)
            }

            // Ensure site total_posts is accurate
            val actualCount = dao.getPostsCountForSite(siteId)
            dao.updateSiteTotalPosts(siteId, actualCount)
            actualCount
        }
    }

    /**
     * Exports the active Room SQLite database file to the user's Downloads folder
     */
    suspend fun exportDatabaseToDownloads(context: Context): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Checkpoint database to flush WAL
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
