package com.example.data.local

import androidx.room.Entity

@Entity(
    tableName = "article_translations",
    primaryKeys = ["articleId", "languageCode"]
)
data class ArticleTranslationEntity(
    val articleId: String,
    val languageCode: String,
    val translatedTitle: String,
    val translatedSummary: String,
    val updatedAt: Long = System.currentTimeMillis()
)
