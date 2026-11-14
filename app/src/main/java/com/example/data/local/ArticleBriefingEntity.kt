package com.example.data.local

import androidx.room.Entity
import com.example.model.ArticleBriefing
import org.json.JSONArray

@Entity(
    tableName = "article_briefings",
    primaryKeys = ["articleId", "languageCode"]
)
data class ArticleBriefingEntity(
    val articleId: String,
    val languageCode: String,
    val editorialHeadline: String,
    val detailedSummary: String,
    val keyTakeawaysJson: String,
    val strategicContext: String,
    val notableVoices: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): ArticleBriefing {
        val bullets = runCatching {
            val arr = JSONArray(keyTakeawaysJson)
            (0 until arr.length()).mapNotNull { idx ->
                arr.optString(idx).trim().takeIf { it.isNotBlank() }
            }
        }.getOrDefault(emptyList())

        return ArticleBriefing(
            editorialHeadline = editorialHeadline,
            detailedSummary = detailedSummary,
            keyTakeaways = bullets,
            strategicContext = strategicContext,
            notableVoices = notableVoices
        )
    }
}
