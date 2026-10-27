package com.example.data.parser

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

data class ParsedPost(
    val originalId: Long,
    val postTitle: String,
    val postContent: String,
    val postDate: String
)

object WordPressDumpParser {

    private const val BATCH_SIZE = 25

    fun cleanSiteUrl(rawUrl: String): String {
        var cleaned = rawUrl.trim()
        if (cleaned.startsWith("https://", ignoreCase = true)) {
            cleaned = cleaned.substring(8)
        } else if (cleaned.startsWith("http://", ignoreCase = true)) {
            cleaned = cleaned.substring(7)
        }
        cleaned = cleaned.trimEnd('/')
        return cleaned.ifBlank { "arsip-wordpress.local" }
    }

    /**
     * Determines the format by inspecting magic bytes:
     * - GZIP (0x1f, 0x8b)
     * - SQLite 3 ("SQLite format 3")
     * - Plain SQL text
     */
    enum class FileFormat {
        GZIP_SQL,
        RAW_SQLITE,
        PLAIN_SQL
    }

    fun detectFormat(inputStream: BufferedInputStream): FileFormat {
        inputStream.mark(32)
        val header = ByteArray(16)
        val read = inputStream.read(header)
        inputStream.reset()

        if (read >= 2 && (header[0].toInt() and 0xFF) == 0x1F && (header[1].toInt() and 0xFF) == 0x8B) {
            return FileFormat.GZIP_SQL
        }

        if (read >= 15) {
            val prefix = String(header, 0, 15, Charsets.US_ASCII)
            if (prefix.startsWith("SQLite format 3")) {
                return FileFormat.RAW_SQLITE
            }
        }

        return FileFormat.PLAIN_SQL
    }

    /**
     * Quick metadata inspection: finds siteurl and estimates if site already exists
     * Reads only the first portion of the stream (or options table) to avoid full parsing.
     */
    fun quickScanSiteUrl(rawStream: InputStream, fallbackName: String = "arsip-novel.local"): String {
        val buffered = rawStream.buffered(64 * 1024)
        val format = detectFormat(buffered)

        if (format == FileFormat.RAW_SQLITE) {
            return cleanSiteUrl(fallbackName)
        }

        val decoded = if (format == FileFormat.GZIP_SQL) GZIPInputStream(buffered) else buffered
        val reader = BufferedReader(InputStreamReader(decoded, Charsets.UTF_8), 32 * 1024)

        val siteUrlRegex = Regex("""['"]siteurl['"]\s*,\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
        val homeRegex = Regex("""['"]home['"]\s*,\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)

        var linesRead = 0
        var line: String?

        // Check first 3000 lines (options is always at the top of WordPress dumps)
        while (reader.readLine().also { line = it } != null && linesRead < 3000) {
            linesRead++
            val l = line ?: break
            if (l.contains("options", ignoreCase = true) || l.contains("siteurl", ignoreCase = true)) {
                val match = siteUrlRegex.find(l) ?: homeRegex.find(l)
                if (match != null) {
                    return cleanSiteUrl(match.groupValues[1])
                }
            }
        }

        return cleanSiteUrl(fallbackName)
    }

    /**
     * Streams and parses the database backup directly, emitting posts in small batches
     * so that memory is immediately freed and OOM is completely prevented for 5000+ posts.
     */
    suspend fun streamParseAndSave(
        context: Context,
        uri: Uri,
        fallbackName: String,
        onSiteIdentified: suspend (siteUrl: String) -> Unit,
        onBatchReady: suspend (posts: List<ParsedPost>, totalSoFar: Int) -> Unit,
        onStatus: (String) -> Unit
    ): Int {
        val rawStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Tidak dapat membuka file dari penyimpanan")

        val buffered = BufferedInputStream(rawStream, 64 * 1024)
        val format = detectFormat(buffered)

        return when (format) {
            FileFormat.RAW_SQLITE -> {
                parseRawSqliteFile(context, uri, fallbackName, onSiteIdentified, onBatchReady, onStatus)
            }
            FileFormat.GZIP_SQL, FileFormat.PLAIN_SQL -> {
                val decoded = if (format == FileFormat.GZIP_SQL) GZIPInputStream(buffered) else buffered
                parseSqlStream(decoded, fallbackName, onSiteIdentified, onBatchReady, onStatus)
            }
        }
    }

    private suspend fun parseSqlStream(
        decodedStream: InputStream,
        fallbackName: String,
        onSiteIdentified: suspend (siteUrl: String) -> Unit,
        onBatchReady: suspend (posts: List<ParsedPost>, totalSoFar: Int) -> Unit,
        onStatus: (String) -> Unit
    ): Int {
        val reader = BufferedReader(InputStreamReader(decodedStream, Charsets.UTF_8), 64 * 1024)

        var detectedSiteUrl: String? = null
        val siteUrlRegex = Regex("""['"]siteurl['"]\s*,\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
        val homeRegex = Regex("""['"]home['"]\s*,\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)

        val postsTableRegex = Regex("""INSERT\s+INTO\s+[`"']?(\w*posts)[`"']?\s*(?:\(([^)]+)\))?\s*VALUES""", RegexOption.IGNORE_CASE)

        var totalSaved = 0
        val currentBatch = ArrayList<ParsedPost>(BATCH_SIZE)

        var insideInsertPosts = false
        var postColumnMap: Map<String, Int>? = null

        // Character level scanner to avoid giant string buffer
        var line: String?
        val tupleBuffer = StringBuilder(32 * 1024)
        var inTuple = false
        var inString = false
        var escapeNext = false

        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: break
            val trimmed = currentLine.trim()

            if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                continue
            }

            // Check for siteurl if not yet detected
            if (detectedSiteUrl == null && (trimmed.contains("options", ignoreCase = true) || trimmed.contains("siteurl", ignoreCase = true))) {
                val siteMatch = siteUrlRegex.find(trimmed) ?: homeRegex.find(trimmed)
                if (siteMatch != null) {
                    val rawUrl = siteMatch.groupValues[1]
                    detectedSiteUrl = cleanSiteUrl(rawUrl)
                    onSiteIdentified(detectedSiteUrl)
                    onStatus("Website terdeteksi: $detectedSiteUrl")
                }
            }

            // Check for start of posts INSERT
            val match = postsTableRegex.find(trimmed)
            if (match != null) {
                if (detectedSiteUrl == null) {
                    detectedSiteUrl = cleanSiteUrl(fallbackName)
                    onSiteIdentified(detectedSiteUrl)
                }

                insideInsertPosts = true
                val columnsPart = match.groupValues.getOrNull(2)
                postColumnMap = parseColumnNames(columnsPart)

                val valuesIdx = trimmed.indexOf("VALUES", ignoreCase = true)
                val remaining = if (valuesIdx != -1) trimmed.substring(valuesIdx + 6) else ""

                scanLineForTuples(remaining, tupleBuffer, inTuple, inString, escapeNext, postColumnMap) { post ->
                    currentBatch.add(post)
                }
                inTuple = false
                inString = false
                escapeNext = false
            } else if (insideInsertPosts) {
                // Process lines within INSERT statement
                val len = trimmed.length
                var i = 0
                while (i < len) {
                    val c = trimmed[i]

                    if (escapeNext) {
                        if (inTuple) tupleBuffer.append(c)
                        escapeNext = false
                        i++
                        continue
                    }

                    if (c == '\\') {
                        if (inTuple) tupleBuffer.append(c)
                        escapeNext = true
                        i++
                        continue
                    }

                    if (c == '\'') {
                        if (inString && i + 1 < len && trimmed[i + 1] == '\'') {
                            if (inTuple) tupleBuffer.append("''")
                            i += 2
                            continue
                        }
                        inString = !inString
                        if (inTuple) tupleBuffer.append(c)
                        i++
                        continue
                    }

                    if (!inString) {
                        if (c == '(' && !inTuple) {
                            inTuple = true
                            tupleBuffer.setLength(0)
                            i++
                            continue
                        } else if (c == ')' && inTuple) {
                            inTuple = false
                            val post = parseSingleTuple(tupleBuffer.toString(), postColumnMap)
                            if (post != null) {
                                currentBatch.add(post)
                            }
                            tupleBuffer.setLength(0)
                            i++
                            continue
                        } else if (c == ';') {
                            insideInsertPosts = false
                            inTuple = false
                            break
                        }
                    }

                    if (inTuple) {
                        tupleBuffer.append(c)
                    }
                    i++
                }
            }

            // Flush batch to database as soon as it reaches BATCH_SIZE
            if (currentBatch.size >= BATCH_SIZE) {
                totalSaved += currentBatch.size
                onBatchReady(ArrayList(currentBatch), totalSaved)
                currentBatch.clear()
                if (totalSaved % 100 == 0) {
                    onStatus("Menyimpan pos novel... ($totalSaved tersimpan)")
                }
            }
        }

        // Flush any remaining posts
        if (currentBatch.isNotEmpty()) {
            totalSaved += currentBatch.size
            onBatchReady(ArrayList(currentBatch), totalSaved)
            currentBatch.clear()
        }

        if (detectedSiteUrl == null) {
            val finalName = cleanSiteUrl(fallbackName)
            onSiteIdentified(finalName)
        }

        onStatus("Selesai mengimpor $totalSaved pos.")
        return totalSaved
    }

    private fun scanLineForTuples(
        line: String,
        tupleBuffer: StringBuilder,
        inTupleInit: Boolean,
        inStringInit: Boolean,
        escapeInit: Boolean,
        columnMap: Map<String, Int>?,
        onPost: (ParsedPost) -> Unit
    ) {
        var inTuple = inTupleInit
        var inString = inStringInit
        var escapeNext = escapeInit
        val len = line.length
        var i = 0

        while (i < len) {
            val c = line[i]
            if (escapeNext) {
                if (inTuple) tupleBuffer.append(c)
                escapeNext = false
                i++
                continue
            }
            if (c == '\\') {
                if (inTuple) tupleBuffer.append(c)
                escapeNext = true
                i++
                continue
            }
            if (c == '\'') {
                if (inString && i + 1 < len && line[i + 1] == '\'') {
                    if (inTuple) tupleBuffer.append("''")
                    i += 2
                    continue
                }
                inString = !inString
                if (inTuple) tupleBuffer.append(c)
                i++
                continue
            }
            if (!inString) {
                if (c == '(' && !inTuple) {
                    inTuple = true
                    tupleBuffer.setLength(0)
                    i++
                    continue
                } else if (c == ')' && inTuple) {
                    inTuple = false
                    val post = parseSingleTuple(tupleBuffer.toString(), columnMap)
                    if (post != null) {
                        onPost(post)
                    }
                    tupleBuffer.setLength(0)
                    i++
                    continue
                }
            }
            if (inTuple) {
                tupleBuffer.append(c)
            }
            i++
        }
    }

    private fun parseSingleTuple(tupleContent: String, columnMap: Map<String, Int>?): ParsedPost? {
        val values = splitTupleValues(tupleContent)
        return buildPostFromTuple(values, columnMap)
    }

    private fun splitTupleValues(raw: String): List<String> {
        val list = mutableListOf<String>()
        val sb = StringBuilder()
        var inString = false
        var escapeNext = false
        val len = raw.length
        var i = 0

        while (i < len) {
            val c = raw[i]
            if (escapeNext) {
                when (c) {
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    else -> sb.append(c)
                }
                escapeNext = false
                i++
                continue
            }
            if (c == '\\') {
                escapeNext = true
                i++
                continue
            }
            if (c == '\'') {
                if (inString && i + 1 < len && raw[i + 1] == '\'') {
                    sb.append('\'')
                    i += 2
                    continue
                }
                inString = !inString
                i++
                continue
            }
            if (c == ',' && !inString) {
                list.add(sb.toString().trim())
                sb.setLength(0)
                i++
                continue
            }
            sb.append(c)
            i++
        }
        list.add(sb.toString().trim())
        return list
    }

    private fun parseColumnNames(columnsRaw: String?): Map<String, Int>? {
        if (columnsRaw.isNullOrBlank()) return null
        val map = mutableMapOf<String, Int>()
        val cols = columnsRaw.split(",")
        for (i in cols.indices) {
            val colName = cols[i].trim().trim('`', '\'', '"', ' ').lowercase()
            map[colName] = i
        }
        return map
    }

    private fun buildPostFromTuple(values: List<String>, columnMap: Map<String, Int>?): ParsedPost? {
        if (values.size < 5) return null

        val idStr: String
        val title: String
        val content: String
        val date: String
        val type: String
        val status: String

        if (columnMap != null && columnMap.containsKey("post_title") && columnMap.containsKey("post_content")) {
            val idIdx = columnMap["id"] ?: 0
            val titleIdx = columnMap["post_title"] ?: 5
            val contentIdx = columnMap["post_content"] ?: 4
            val dateIdx = columnMap["post_date"] ?: 2
            val typeIdx = columnMap["post_type"] ?: 20
            val statusIdx = columnMap["post_status"] ?: 7

            idStr = values.getOrNull(idIdx).orEmpty()
            title = values.getOrNull(titleIdx).orEmpty()
            content = values.getOrNull(contentIdx).orEmpty()
            date = values.getOrNull(dateIdx).orEmpty()
            type = values.getOrNull(typeIdx).orEmpty()
            status = values.getOrNull(statusIdx).orEmpty()
        } else {
            idStr = values.getOrNull(0).orEmpty()
            date = values.getOrNull(2).orEmpty()
            content = values.getOrNull(4).orEmpty()
            title = values.getOrNull(5).orEmpty()
            status = values.getOrNull(7).orEmpty()
            type = values.getOrNull(20) ?: values.getOrNull(values.size - 3).orEmpty()
        }

        val cleanType = type.trim('\'', '"', ' ').lowercase()
        val cleanStatus = status.trim('\'', '"', ' ').lowercase()

        // Filter: accept articles, posts, pages, chapter posts
        val isPostType = cleanType.isEmpty() || cleanType == "post" || cleanType == "page" || cleanType == "chapter" || cleanType == "article"
        if (!isPostType) return null
        if (cleanStatus == "auto-draft" || cleanStatus == "trash") return null

        val idNum = idStr.trim('\'', '"', ' ').toLongOrNull() ?: System.currentTimeMillis()
        val cleanTitle = title.trim('\'', '"', ' ').ifBlank { "Bab Tanpa Judul ($idNum)" }
        val cleanContent = content.trim('\'', '"')
        val cleanDate = date.trim('\'', '"', ' ').ifBlank { "2025-01-01 00:00:00" }

        if (cleanTitle.isBlank() && cleanContent.isBlank()) return null

        return ParsedPost(
            originalId = idNum,
            postTitle = cleanTitle,
            postContent = cleanContent,
            postDate = cleanDate
        )
    }

    /**
     * Handles real SQLite .db files directly using SQLiteDatabase
     */
    private suspend fun parseRawSqliteFile(
        context: Context,
        uri: Uri,
        fallbackName: String,
        onSiteIdentified: suspend (siteUrl: String) -> Unit,
        onBatchReady: suspend (posts: List<ParsedPost>, totalSoFar: Int) -> Unit,
        onStatus: (String) -> Unit
    ): Int {
        onStatus("Membaca file database SQLite...")
        val tempFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.db")

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val db = SQLiteDatabase.openDatabase(tempFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            var siteUrl = fallbackName

            // 1. Check for options table
            try {
                val optCursor = db.rawQuery(
                    "SELECT option_value FROM wp_options WHERE option_name = 'siteurl' LIMIT 1",
                    null
                )
                if (optCursor.moveToFirst()) {
                    siteUrl = optCursor.getString(0)
                }
                optCursor.close()
            } catch (_: Exception) {}

            val cleanUrl = cleanSiteUrl(siteUrl)
            onSiteIdentified(cleanUrl)

            // 2. Query posts table
            var totalSaved = 0
            val batch = ArrayList<ParsedPost>(BATCH_SIZE)

            // Find posts table name (e.g. wp_posts or posts)
            var tableName = "wp_posts"
            val tableCursor = db.rawQuery(
                "SELECT name FROM sqlite_master WHERE type='table' AND name LIKE '%posts%' LIMIT 1",
                null
            )
            if (tableCursor.moveToFirst()) {
                tableName = tableCursor.getString(0)
            }
            tableCursor.close()

            val cursor = db.rawQuery(
                "SELECT ID, post_title, post_content, post_date FROM $tableName WHERE (post_type='post' OR post_type='page' OR post_type IS NULL) AND (post_status='publish' OR post_status='inherit' OR post_status IS NULL)",
                null
            )

            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                val title = cursor.getString(1) ?: "Bab $id"
                val content = cursor.getString(2) ?: ""
                val date = cursor.getString(3) ?: "2025-01-01"

                batch.add(
                    ParsedPost(
                        originalId = id,
                        postTitle = title,
                        postContent = content,
                        postDate = date
                    )
                )

                if (batch.size >= BATCH_SIZE) {
                    totalSaved += batch.size
                    onBatchReady(ArrayList(batch), totalSaved)
                    batch.clear()
                    if (totalSaved % 100 == 0) {
                        onStatus("Menyimpan pos novel... ($totalSaved tersimpan)")
                    }
                }
            }
            cursor.close()
            db.close()

            if (batch.isNotEmpty()) {
                totalSaved += batch.size
                onBatchReady(ArrayList(batch), totalSaved)
                batch.clear()
            }

            return totalSaved
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }
}
