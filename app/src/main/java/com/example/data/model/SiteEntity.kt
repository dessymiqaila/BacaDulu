package com.example.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sites",
    indices = [
        Index(value = ["site_url"], unique = true)
    ]
)
data class SiteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "site_url")
    val siteUrl: String,
    @ColumnInfo(name = "total_posts")
    val totalPosts: Int,
    @ColumnInfo(name = "imported_at")
    val importedAt: Long = System.currentTimeMillis()
)
