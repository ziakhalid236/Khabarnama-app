package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VideoDao {

    @Query(
        """
        SELECT * FROM cached_video_clips
        WHERE (:category IS NULL OR category = :category)
        ORDER BY publishedAt DESC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getLatestVideos(category: String?, limit: Int = 20): List<VideoClipEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoClipEntity>)

    @Query("DELETE FROM cached_video_clips WHERE publishedAt < :olderThan")
    suspend fun deleteVideosOlderThan(olderThan: Long)

    @Query("SELECT COUNT(*) FROM cached_video_clips")
    suspend fun getTotalCount(): Int
}
