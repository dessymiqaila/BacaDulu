package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = SiteEntity::class,
            parentColumns = ["id"],
            childColumns = ["site_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["site_id"]),
        Index(value = ["unique_key"], unique = true),
        Index(value = ["post_title"]),
        Index(value = ["is_bookmarked", "bookmarked_at"]),
        Index(value = ["last_read_at"])
    ]
)
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
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
    val isBookmarked: Int = 0,
    @ColumnInfo(name = "bookmarked_at")
    val bookmarkedAt: Long? = null,
    @ColumnInfo(name = "last_read_at")
    val lastReadAt: Long? = null
)
