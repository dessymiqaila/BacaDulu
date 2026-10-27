package com.example.data.model

import androidx.room.ColumnInfo

data class PostWithSite(
    val id: Int,
    @ColumnInfo(name = "site_id")
    val siteId: Int,
    @ColumnInfo(name = "unique_key")
    val uniqueKey: String,
    @ColumnInfo(name = "original_id")
    val originalId: Long,
    @ColumnInfo(name = "post_title")
    val postTitle: String,
    @ColumnInfo(name = "post_content")
    val postContent: String,
    @ColumnInfo(name = "post_date")
    val postDate: String,
    @ColumnInfo(name = "is_bookmarked")
    val isBookmarked: Int,
    @ColumnInfo(name = "bookmarked_at")
    val bookmarkedAt: Long?,
    @ColumnInfo(name = "site_url")
    val siteUrl: String,
    @ColumnInfo(name = "last_read_at")
    val lastReadAt: Long? = null
)
