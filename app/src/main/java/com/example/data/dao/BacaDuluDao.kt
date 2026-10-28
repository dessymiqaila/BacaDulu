package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PostEntity
import com.example.data.model.PostWithSite
import com.example.data.model.SiteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BacaDuluDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSite(site: SiteEntity): Long

    @Update
    suspend fun updateSite(site: SiteEntity)

    @Query("SELECT * FROM sites WHERE site_url = :siteUrl LIMIT 1")
    suspend fun getSiteByUrl(siteUrl: String): SiteEntity?

    @Query("SELECT * FROM sites WHERE id = :siteId LIMIT 1")
    suspend fun getSiteById(siteId: Int): SiteEntity?

    @Query("SELECT * FROM sites ORDER BY imported_at DESC")
    fun getAllSitesFlow(): Flow<List<SiteEntity>>

    @Query("SELECT * FROM sites ORDER BY imported_at DESC")
    suspend fun getAllSites(): List<SiteEntity>

    @Query("DELETE FROM sites WHERE id = :siteId")
    suspend fun deleteSiteById(siteId: Int)

    @Query("DELETE FROM posts WHERE site_id = :siteId")
    suspend fun deletePostsBySiteId(siteId: Int)

    @Query("UPDATE sites SET total_posts = :count WHERE id = :siteId")
    suspend fun updateSiteTotalPosts(siteId: Int, count: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>): List<Long>

    @Query("""
        SELECT posts.id, posts.site_id, posts.unique_key, posts.original_id,
               posts.post_title, posts.post_content, posts.post_date,
               posts.is_bookmarked, posts.bookmarked_at, posts.last_read_at, sites.site_url
        FROM posts
        INNER JOIN sites ON posts.site_id = sites.id
        ORDER BY RANDOM()
        LIMIT 20
    """)
    suspend fun getRandom15Posts(): List<PostWithSite>

    @Query("""
        SELECT posts.id, posts.site_id, posts.unique_key, posts.original_id,
               posts.post_title, posts.post_content, posts.post_date,
               posts.is_bookmarked, posts.bookmarked_at, posts.last_read_at, sites.site_url
        FROM posts
        INNER JOIN sites ON posts.site_id = sites.id
        WHERE (:query = '' OR posts.post_title LIKE '%' || :query || '%')
        ORDER BY posts.post_title COLLATE NOCASE ASC
        LIMIT 20
    """)
    fun searchPostsAlphabetical(query: String): Flow<List<PostWithSite>>

    @Query("""
        SELECT posts.id, posts.site_id, posts.unique_key, posts.original_id,
               posts.post_title, posts.post_content, posts.post_date,
               posts.is_bookmarked, posts.bookmarked_at, posts.last_read_at, sites.site_url
        FROM posts
        INNER JOIN sites ON posts.site_id = sites.id
        WHERE posts.is_bookmarked = 1
        ORDER BY posts.bookmarked_at DESC
    """)
    fun getBookmarkedPostsFlow(): Flow<List<PostWithSite>>

    @Query("""
        SELECT posts.id, posts.site_id, posts.unique_key, posts.original_id,
               posts.post_title, posts.post_content, posts.post_date,
               posts.is_bookmarked, posts.bookmarked_at, posts.last_read_at, sites.site_url
        FROM posts
        INNER JOIN sites ON posts.site_id = sites.id
        WHERE posts.last_read_at IS NOT NULL
        ORDER BY posts.last_read_at DESC
        LIMIT 100
    """)
    fun getReadingHistoryFlow(): Flow<List<PostWithSite>>

    @Query("""
        SELECT posts.id, posts.site_id, posts.unique_key, posts.original_id,
               posts.post_title, posts.post_content, posts.post_date,
               posts.is_bookmarked, posts.bookmarked_at, posts.last_read_at, sites.site_url
        FROM posts
        INNER JOIN sites ON posts.site_id = sites.id
        WHERE posts.id = :postId
        LIMIT 1
    """)
    fun getPostWithSiteByIdFlow(postId: Int): Flow<PostWithSite?>

    @Query("""
        SELECT posts.id, posts.site_id, posts.unique_key, posts.original_id,
               posts.post_title, posts.post_content, posts.post_date,
               posts.is_bookmarked, posts.bookmarked_at, posts.last_read_at, sites.site_url
        FROM posts
        INNER JOIN sites ON posts.site_id = sites.id
        WHERE posts.id = :postId
        LIMIT 1
    """)
    suspend fun getPostWithSiteById(postId: Int): PostWithSite?

    @Query("SELECT id FROM posts WHERE site_id = :siteId AND id < :currentId ORDER BY id DESC LIMIT 1")
    suspend fun getPreviousPostId(siteId: Int, currentId: Int): Int?

    @Query("SELECT id FROM posts WHERE site_id = :siteId AND id > :currentId ORDER BY id ASC LIMIT 1")
    suspend fun getNextPostId(siteId: Int, currentId: Int): Int?

    @Query("UPDATE posts SET is_bookmarked = :isBookmarked, bookmarked_at = :bookmarkedAt WHERE id = :postId")
    suspend fun setBookmark(postId: Int, isBookmarked: Int, bookmarkedAt: Long?)

    @Query("UPDATE posts SET last_read_at = :timestamp WHERE id = :postId")
    suspend fun updateLastRead(postId: Int, timestamp: Long)

    @Query("""
        UPDATE posts
        SET last_read_at = NULL
        WHERE last_read_at IS NOT NULL
        AND id NOT IN (
            SELECT id FROM posts
            WHERE last_read_at IS NOT NULL
            ORDER BY last_read_at DESC
            LIMIT 100
        )
    """)
    suspend fun pruneOldHistory()

    @Query("UPDATE posts SET last_read_at = NULL")
    suspend fun clearReadingHistory()

    @Query("SELECT COUNT(*) FROM posts")
    fun getTotalPostsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM posts WHERE site_id = :siteId")
    suspend fun getPostsCountForSite(siteId: Int): Int

    @Query("SELECT COUNT(*) FROM sites")
    fun getTotalSitesCount(): Flow<Int>
}
