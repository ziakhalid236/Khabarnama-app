package com.example.model

data class VideoChannelConfig(
    val channelId: String,
    val channelName: String,
    val category: NewsCategory,
    val countryFlag: String = "📺",
    val watchLiveUrl: String = "",
    val feedUrl: String = "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
)

data class NewsVideoClip(
    val id: String,
    val videoId: String,
    val title: String,
    val translatedTitle: String? = null,
    val channelName: String,
    val category: NewsCategory,
    val thumbnailUrl: String,
    val watchUrl: String,
    val embedUrl: String,
    val publishedAt: Long,
    val summary: String = "",
    val sourceArticleUrl: String = "",
    val isLiveBadge: Boolean = false
) {
    fun getDisplayTitle(language: AppLanguage): String {
        return if (language != AppLanguage.ENGLISH && !translatedTitle.isNullOrBlank()) {
            translatedTitle
        } else {
            title
        }
    }

    val hasValidEmbedUrl: Boolean
        get() = embedUrl.startsWith("https://", ignoreCase = true)
}
