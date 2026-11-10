package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.model.AppLanguage
import com.example.model.IslamicSection
import com.example.model.LocalizedContent
import com.example.model.NewsArticle
import com.example.model.NewsCategory

@Entity(
    tableName = "cached_articles",
    indices = [
        Index(value = ["normalizedUrl"], unique = true),
        Index(value = ["normalizedTitle"]),
        Index(value = ["publishedAt"]),
        Index(value = ["category"])
    ]
)
data class ArticleEntity(
    @PrimaryKey
    val id: String,
    val normalizedUrl: String,
    val normalizedTitle: String,
    val title: String,
    val summary: String,
    val category: String,
    val islamicSection: String?,
    val imageUrl: String,
    val sourceName: String,
    val sourceUrl: String,
    val publishedAt: Long,
    val isIslamicSource: Boolean,
    val isScholarlySource: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
) {
    fun toNewsArticle(
        translationsMap: Map<AppLanguage, LocalizedContent> = emptyMap(),
        isFeaturedIslamic: Boolean = false,
        isTranslating: Boolean = false,
        translationFailed: Boolean = false
    ): NewsArticle {
        val mergedTranslations = mutableMapOf(
            AppLanguage.ENGLISH to LocalizedContent(title = title, summary = summary)
        )
        mergedTranslations.putAll(translationsMap)

        val wordCount = summary.split(Regex("\\s+")).count { it.isNotBlank() }
        val estimatedMinutes = ((wordCount / 40) + 1).coerceIn(1, 5)

        return NewsArticle(
            id = id,
            translations = mergedTranslations,
            category = NewsCategory.fromString(category),
            islamicSection = IslamicSection.fromString(islamicSection),
            imageUrl = imageUrl,
            sourceName = sourceName,
            sourceUrl = sourceUrl,
            publishedAt = publishedAt,
            isFeaturedIslamic = isFeaturedIslamic,
            isIslamicSource = isIslamicSource,
            isScholarlySource = isScholarlySource,
            isBreaking = (System.currentTimeMillis() - publishedAt) < 2 * 3600 * 1000L,
            readTimeMinutes = estimatedMinutes,
            isTranslating = isTranslating,
            translationFailed = translationFailed
        )
    }
}
