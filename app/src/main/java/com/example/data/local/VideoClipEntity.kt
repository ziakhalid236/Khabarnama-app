package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.NewsCategory
import com.example.model.NewsVideoClip

@Entity(
    tableName = "cached_video_clips",
    indices = [
        Index(value = ["videoId"], unique = true),
        Index(value = ["publishedAt"]),
        Index(value = ["category", "publishedAt"])
    ]
)
data class VideoClipEntity(
    @PrimaryKey val id: String,
    val videoId: String,
    val title: String,
    val channelName: String,
    val category: String,
    val thumbnailUrl: String,
    val watchUrl: String,
    val embedUrl: String,
    val publishedAt: Long,
    val summary: String = "",
    val sourceArticleUrl: String = "",
    val isLiveBadge: Boolean = false
) {
    fun toDomainModel(translatedTitle: String? = null): NewsVideoClip {
        return NewsVideoClip(
            id = id,
            videoId = videoId,
            title = title,
            translatedTitle = translatedTitle,
            channelName = channelName,
            category = NewsCategory.fromString(category),
            thumbnailUrl = thumbnailUrl,
            watchUrl = watchUrl,
            embedUrl = embedUrl,
            publishedAt = publishedAt,
            summary = summary,
            sourceArticleUrl = sourceArticleUrl,
            isLiveBadge = isLiveBadge
        )
    }
}
