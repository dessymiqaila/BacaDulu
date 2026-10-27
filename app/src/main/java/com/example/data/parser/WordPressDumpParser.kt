package com.example.data.parser

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream

data class ParsedPost(
    val originalId: Long,
    val postTitle: String,
    val postContent: String,
    val postDate: String
)

data class DumpScanResult(
    val siteUrl: String,
    val posts: List<ParsedPost>
)

object WordPressDumpParser {

    /**
     * Cleans raw site URL by removing protocol (http://, https://) and trailing slashes.
     * Example: "https://kuliner-nusantara.net/" -> "kuliner-nusantara.net"
     */
    fun cleanSiteUrl(rawUrl: String): String {
        var cleaned = rawUrl.trim()
        if (cleaned.startsWith("https://", ignoreCase = true)) {
            cleaned = cleaned.substring(8)
        } else if (cleaned.startsWith("http://", ignoreCase = true)) {
            cleaned = cleaned.substring(7)
        }
        cleaned = cleaned.trimEnd('/')
        // Remove port or path if any, but preserve domain or clean path
        return cleaned.ifBlank { "unknown-site.local" }
    }

    /**
     * Decides whether stream is GZIP compressed by inspecting magic bytes (0x1f, 0x8b)
     */
    fun getDecodedStream(rawStream: InputStream): InputStream {
        val buffered = rawStream.buffered()
        buffered.mark(2)
        val b1 = buffered.read()
        val b2 = buffered.read()
        buffered.reset()
        return if (b1 == 0x1f && b2 == 0x8b) {
            GZIPInputStream(buffered)
        } else {
            buffered
        }
    }

    /**
     * Parses an SQL dump stream (from .db.gz, .sql.gz, or .sql) extracting siteurl and posts.
     */
    fun parseDump(
        inputStream: InputStream,
        onProgress: (postsFound: Int, currentStage: String) -> Unit = { _, _ -> }
    ): DumpScanResult {
        val stream = getDecodedStream(inputStream)
        val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8), 32 * 1024)

        var detectedSiteUrl: String? = null
        val parsedPosts = mutableListOf<ParsedPost>()

        val optionsTableRegex = Regex("""INSERT\s+INTO\s+[`"']?(\w*options)[`"']?""", RegexOption.IGNORE_CASE)
        val postsTableRegex = Regex("""INSERT\s+INTO\s+[`"']?(\w*posts)[`"']?\s*(?:\(([^)]+)\))?\s*VALUES""", RegexOption.IGNORE_CASE)

        val siteUrlOptionRegex = Regex("""['"]siteurl['"]\s*,\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)
        val homeOptionRegex = Regex("""['"]home['"]\s*,\s*['"]([^'"]+)['"]""", RegexOption.IGNORE_CASE)

        var line: String?
        val statementBuffer = StringBuilder()
        var insideInsertPosts = false
        var postColumnMap: Map<String, Int>? = null

        var lineCount = 0

        while (reader.readLine().also { line = it } != null) {
            val currentLine = line ?: break
            lineCount++
            val trimmed = currentLine.trim()

            if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("/*")) {
                continue
            }

            // 1. Look for siteurl in options table
            if (detectedSiteUrl == null && (trimmed.contains("options", ignoreCase = true) || trimmed.contains("siteurl", ignoreCase = true))) {
                val siteMatch = siteUrlOptionRegex.find(trimmed) ?: homeOptionRegex.find(trimmed)
                if (siteMatch != null) {
                    val rawUrl = siteMatch.groupValues[1]
                    detectedSiteUrl = cleanSiteUrl(rawUrl)
                }
            }

            // 2. Check for start of posts INSERT statement
            val postsMatch = postsTableRegex.find(trimmed)
            if (postsMatch != null) {
                insideInsertPosts = true
                val columnsPart = postsMatch.groupValues.getOrNull(2)
                postColumnMap = parseColumnNames(columnsPart)

                // Start buffering SQL from after "VALUES"
                val valuesIdx = trimmed.indexOf("VALUES", ignoreCase = true)
                if (valuesIdx != -1) {
                    statementBuffer.setLength(0)
                    statementBuffer.append(trimmed.substring(valuesIdx + 6))
                }
            } else if (insideInsertPosts) {
                statementBuffer.append(" ").append(trimmed)
            }

            if (insideInsertPosts) {
                if (trimmed.endsWith(";")) {
                    // Completed INSERT statement
                    val postsInStatement = extractPostsFromValues(
                        statementBuffer.toString(),
                        postColumnMap
                    )
                    parsedPosts.addAll(postsInStatement)
                    statementBuffer.setLength(0)
                    insideInsertPosts = false

                    if (parsedPosts.size % 20 == 0) {
                        onProgress(parsedPosts.size, "Mengekstrak pos...")
                    }
                }
            }
        }

        val finalUrl = detectedSiteUrl ?: "wordpress-site.local"
        onProgress(parsedPosts.size, "Selesai membaca file")

        return DumpScanResult(
            siteUrl = finalUrl,
            posts = parsedPosts
        )
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

    /**
     * Extracts tuples from SQL string like:
     * (1, 1, '2023-01-01', ...), (2, 1, '2023-01-02', ...)
     */
    private fun extractPostsFromValues(
        valuesSql: String,
        columnMap: Map<String, Int>?
    ): List<ParsedPost> {
        val result = mutableListOf<ParsedPost>()
        val len = valuesSql.length
        var i = 0

        while (i < len) {
            // Find opening '('
            while (i < len && valuesSql[i] != '(') {
                i++
            }
            if (i >= len) break

            i++ // Skip '('
            val tupleValues = mutableListOf<String>()
            val currentVal = StringBuilder()
            var inString = false
            var escapeNext = false

            while (i < len) {
                val c = valuesSql[i]

                if (escapeNext) {
                    when (c) {
                        'n' -> currentVal.append('\n')
                        'r' -> currentVal.append('\r')
                        't' -> currentVal.append('\t')
                        '0' -> currentVal.append('\u0000')
                        else -> currentVal.append(c)
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
                    // Check for SQL standard quote escaping ''
                    if (inString && i + 1 < len && valuesSql[i + 1] == '\'') {
                        currentVal.append('\'')
                        i += 2
                        continue
                    }
                    inString = !inString
                    i++
                    continue
                }

                if (!inString) {
                    if (c == ',') {
                        tupleValues.add(currentVal.toString().trim())
                        currentVal.setLength(0)
                        i++
                        continue
                    } else if (c == ')') {
                        tupleValues.add(currentVal.toString().trim())
                        currentVal.setLength(0)
                        i++
                        break
                    }
                }

                currentVal.append(c)
                i++
            }

            val post = buildPostFromTuple(tupleValues, columnMap)
            if (post != null) {
                result.add(post)
            }
        }

        return result
    }

    /**
     * Converts raw parsed values tuple to ParsedPost
     */
    private fun buildPostFromTuple(
        values: List<String>,
        columnMap: Map<String, Int>?
    ): ParsedPost? {
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
            // Standard wp_posts column index layout:
            // 0: ID
            // 1: post_author
            // 2: post_date
            // 3: post_date_gmt
            // 4: post_content
            // 5: post_title
            // 6: post_excerpt
            // 7: post_status
            // ...
            // 20: post_type
            idStr = values.getOrNull(0).orEmpty()
            date = values.getOrNull(2).orEmpty()
            content = values.getOrNull(4).orEmpty()
            title = values.getOrNull(5).orEmpty()
            status = values.getOrNull(7).orEmpty()
            type = values.getOrNull(20) ?: values.getOrNull(values.size - 3).orEmpty()
        }

        val cleanType = type.trim('\'', '"', ' ').lowercase()
        val cleanStatus = status.trim('\'', '"', ' ').lowercase()

        // Filter: only accept posts (ignore attachment, revision, nav_menu_item, auto-draft)
        val isPostType = cleanType.isEmpty() || cleanType == "post" || cleanType == "page" || cleanType == "article"
        val isPublished = cleanStatus.isEmpty() || cleanStatus == "publish" || cleanStatus == "inherit"

        if (!isPostType) return null
        if (cleanStatus == "auto-draft" || cleanStatus == "trash") return null

        val idNum = idStr.trim('\'', '"', ' ').toLongOrNull() ?: System.currentTimeMillis()
        val cleanTitle = title.trim('\'', '"', ' ').ifBlank { "Tanpa Judul ($idNum)" }
        val cleanContent = content.trim('\'', '"')
        val cleanDate = date.trim('\'', '"', ' ').ifBlank { "2025-01-01 00:00:00" }

        // Skip completely empty posts
        if (cleanTitle.isBlank() && cleanContent.isBlank()) return null

        return ParsedPost(
            originalId = idNum,
            postTitle = cleanTitle,
            postContent = cleanContent,
            postDate = cleanDate
        )
    }
}
