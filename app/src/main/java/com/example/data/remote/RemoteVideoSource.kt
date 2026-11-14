package com.example.data.remote

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.VideoClipEntity
import com.example.model.NewsCategory
import com.example.model.VideoChannelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

interface RemoteVideoSource {
    suspend fun fetchLatestVideos(): List<VideoClipEntity>
    fun loadTrustedChannels(): List<VideoChannelConfig>
}

class YouTubeAtomVideoSource(
    private val context: Context,
    private val httpClient: OkHttpClient = RssRemoteNewsSource.defaultOkHttpClient(),
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() }
) : RemoteVideoSource {

    private val videoHealthPrefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_VIDEO_HEALTH, Context.MODE_PRIVATE)
    }

    override fun loadTrustedChannels(): List<VideoChannelConfig> {
        val remoteCfg = RemoteConfigManager.config.value
        val disabled = remoteCfg.disabledVideoChannelIds
        return (remoteCfg.extraVideoChannels + DEFAULT_TRUSTED_CHANNELS)
            .distinctBy { it.channelId }
            .filterNot { it.channelId in disabled }
    }

    override suspend fun fetchLatestVideos(): List<VideoClipEntity> = withContext(Dispatchers.IO) {
        val channels = loadTrustedChannels()
        if (channels.isEmpty()) return@withContext emptyList()

        val semaphore = Semaphore(6)
        val fetched = coroutineScope {
            channels.map { channel ->
                async {
                    semaphore.withPermit {
                        fetchChannelFeedWithBackoff(channel)
                    }
                }
            }.awaitAll().flatten()
        }

        fetched
            .distinctBy { it.videoId }
            .sortedByDescending { it.publishedAt }
    }

    private suspend fun fetchChannelFeedWithBackoff(channel: VideoChannelConfig): List<VideoClipEntity> {
        val now = currentTimeProvider()
        val cooldownKey = "cooldown_${channel.channelId}"
        val failKey = "fails_${channel.channelId}"
        if (videoHealthPrefs.getLong(cooldownKey, 0L) > now) {
            return emptyList()
        }

        val xmlContent = try {
            val request = Request.Builder()
                .url(channel.feedUrl)
                .header("User-Agent", RssRemoteNewsSource.USER_AGENT)
                .header("Accept", "application/atom+xml, application/xml, text/xml, */*")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    recordOutcome(failKey, cooldownKey, succeeded = false, nowMs = now)
                    return emptyList()
                }
                val stream = response.body?.charStream() ?: run {
                    recordOutcome(failKey, cooldownKey, succeeded = false, nowMs = now)
                    return emptyList()
                }
                RssRemoteNewsSource.readTextUpToLimit(stream, RssRemoteNewsSource.MAX_RSS_XML_CHARS)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            recordOutcome(failKey, cooldownKey, succeeded = false, nowMs = now)
            return emptyList()
        }

        val parsed = parseYouTubeAtomFeed(xmlContent, channel)
        if (parsed.isEmpty()) {
            recordOutcome(failKey, cooldownKey, succeeded = false, nowMs = now)
            return emptyList()
        }

        recordOutcome(failKey, cooldownKey, succeeded = true, nowMs = now)
        val sevenDaysAgo = now - SEVEN_DAYS_MS
        return parsed.filter { it.publishedAt >= sevenDaysAgo }.take(MAX_CLIPS_PER_CHANNEL)
    }

    private fun recordOutcome(failKey: String, cooldownKey: String, succeeded: Boolean, nowMs: Long) {
        synchronized(this) {
            if (succeeded) {
                videoHealthPrefs.edit()
                    .putInt(failKey, 0)
                    .putLong(cooldownKey, 0L)
                    .apply()
            } else {
                val fails = videoHealthPrefs.getInt(failKey, 0) + 1
                val editor = videoHealthPrefs.edit().putInt(failKey, fails)
                if (fails >= RssRemoteNewsSource.MAX_CONSECUTIVE_FAILURES) {
                    editor.putLong(cooldownKey, nowMs + RssRemoteNewsSource.BACKOFF_DURATION_MS)
                }
                editor.apply()
            }
        }
    }

    companion object {
        private const val PREFS_VIDEO_HEALTH = "khabarnama_video_health"
        private const val MAX_CLIPS_PER_CHANNEL = 6
        private const val SEVEN_DAYS_MS = 7L * 24L * 3600L * 1000L

        /**
         * Verified official international news broadcast YouTube channels.
         * Can be extended or disabled dynamically anytime via Firebase Remote Config
         * (`remote_video_channels_json` / `disabled_video_channel_ids_json`).
         */
        val DEFAULT_TRUSTED_CHANNELS: List<VideoChannelConfig> = listOf(
            VideoChannelConfig(
                channelId = "UCNye-wNBqNL5ZzHSJj3l8Bg",
                channelName = "Al Jazeera English",
                category = NewsCategory.WORLD,
                countryFlag = "🇶🇦",
                watchLiveUrl = "https://www.youtube.com/@AlJazeeraEnglish/live"
            ),
            VideoChannelConfig(
                channelId = "UC7fWeaHhqgM4Ry-RMpM2YYw",
                channelName = "TRT World",
                category = NewsCategory.WORLD,
                countryFlag = "🇹🇷",
                watchLiveUrl = "https://www.youtube.com/@TRTWorld/live"
            ),
            VideoChannelConfig(
                channelId = "UCR0fZh5SBxxMNYdg0VzRFkg",
                channelName = "Middle East Eye",
                category = NewsCategory.PALESTINE,
                countryFlag = "🇵🇸",
                watchLiveUrl = "https://www.youtube.com/@MiddleEastEye"
            ),
            VideoChannelConfig(
                channelId = "UC16niRr50-MSBwiO3YDb3RA",
                channelName = "BBC News",
                category = NewsCategory.WORLD,
                countryFlag = "🇬🇧",
                watchLiveUrl = "https://www.youtube.com/@BBCNews/live"
            )
        )

        fun parseYouTubeAtomFeed(xmlContent: String, channel: VideoChannelConfig): List<VideoClipEntity> {
            val clips = mutableListOf<VideoClipEntity>()
            try {
                val sanitizedXml = xmlContent.trimStart('\uFEFF', ' ', '\n', '\r', '\t')
                val factory = XmlPullParserFactory.newInstance().apply {
                    isNamespaceAware = false
                }
                val parser = factory.newPullParser()
                parser.setInput(StringReader(sanitizedXml))

                var eventType = parser.eventType
                var inEntry = false
                var videoId = ""
                var title = ""
                var watchUrl = ""
                var publishedRaw = ""
                var thumbnailUrl = ""
                var description = ""
                var authorName = ""

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            val rawName = parser.name ?: ""
                            val lowerRaw = rawName.lowercase()
                            val localName = rawName.substringAfterLast(':').lowercase()

                            if (localName == "entry") {
                                inEntry = true
                                videoId = ""
                                title = ""
                                watchUrl = ""
                                publishedRaw = ""
                                thumbnailUrl = ""
                                description = ""
                                authorName = ""
                            } else if (inEntry) {
                                when {
                                    lowerRaw == "yt:videoid" || localName == "videoid" -> {
                                        videoId = safeNextText(parser).trim()
                                    }
                                    localName == "title" -> {
                                        if (title.isBlank()) {
                                            title = safeNextText(parser).trim()
                                        }
                                    }
                                    localName == "name" -> {
                                        if (authorName.isBlank()) {
                                            authorName = safeNextText(parser).trim()
                                        }
                                    }
                                    localName == "link" -> {
                                        val href = parser.getAttributeValue(null, "href")?.trim().orEmpty()
                                        val rel = parser.getAttributeValue(null, "rel")
                                        if (href.startsWith("https://") && (rel == null || rel == "alternate")) {
                                            watchUrl = href
                                        }
                                    }
                                    localName == "published" || localName == "updated" -> {
                                        if (publishedRaw.isBlank()) {
                                            publishedRaw = safeNextText(parser).trim()
                                        }
                                    }
                                    lowerRaw == "media:thumbnail" || localName == "thumbnail" -> {
                                        val url = parser.getAttributeValue(null, "url")?.trim().orEmpty()
                                        if (url.startsWith("https://") && thumbnailUrl.isBlank()) {
                                            thumbnailUrl = url
                                        }
                                    }
                                    lowerRaw == "media:description" || localName == "description" -> {
                                        if (description.isBlank()) {
                                            description = safeNextText(parser).trim()
                                        }
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            val localName = (parser.name ?: "").substringAfterLast(':').lowercase()
                            if (localName == "entry" && inEntry) {
                                inEntry = false
                                buildVideoClipEntity(
                                    videoId = videoId,
                                    rawTitle = title,
                                    rawWatchUrl = watchUrl,
                                    rawPublished = publishedRaw,
                                    rawThumbnail = thumbnailUrl,
                                    rawDescription = description,
                                    channelName = authorName.ifBlank { channel.channelName },
                                    channel = channel
                                )?.let { clips.add(it) }
                            }
                        }
                    }
                    eventType = parser.next()
                }
            } catch (_: Exception) {
                // Return clips parsed cleanly before any XML error
            }
            return clips
        }

        private fun safeNextText(parser: XmlPullParser): String {
            return try {
                parser.nextText() ?: ""
            } catch (_: Exception) {
                ""
            }
        }

        private fun buildVideoClipEntity(
            videoId: String,
            rawTitle: String,
            rawWatchUrl: String,
            rawPublished: String,
            rawThumbnail: String,
            rawDescription: String,
            channelName: String,
            channel: VideoChannelConfig
        ): VideoClipEntity? {
            val cleanTitle = RssRemoteNewsSource.cleanHtmlText(rawTitle)
            if (videoId.isBlank() || cleanTitle.isBlank()) return null

            val watchUrl = rawWatchUrl.ifBlank { "https://www.youtube.com/watch?v=$videoId" }
            val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?rel=0&playsinline=1&autoplay=1"
            val thumbUrl = rawThumbnail.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" }
            val cleanSummary = RssRemoteNewsSource.formatConciseSummary(rawDescription, cleanTitle, maxWords = 50)
            val publishedAt = RssRemoteNewsSource.parsePubDateMillis(rawPublished)

            val channelSourceConfig = com.example.model.NewsSourceConfig(
                name = channelName,
                feedUrl = channel.feedUrl,
                category = channel.category,
                language = "EN"
            )
            val (classifiedCategory, _) = RssRemoteNewsSource.classifyArticle(
                title = cleanTitle,
                summary = cleanSummary,
                itemCategories = emptyList(),
                source = channelSourceConfig
            )

            val isLiveHint = watchUrl.contains("/live", ignoreCase = true) ||
                LIVE_BROADCAST_REGEX.containsMatchIn(cleanTitle)

            return VideoClipEntity(
                id = "yt_${videoId}",
                videoId = videoId,
                title = cleanTitle,
                channelName = channelName,
                category = classifiedCategory.name,
                thumbnailUrl = thumbUrl,
                watchUrl = watchUrl,
                embedUrl = embedUrl,
                publishedAt = publishedAt,
                summary = cleanSummary,
                sourceArticleUrl = watchUrl,
                isLiveBadge = isLiveHint
            )
        }

        private val LIVE_BROADCAST_REGEX = Regex(
            """\b(?:livestream|live\s*stream|live\s*tv|watch\s*live|live\s*now|live\s*coverage)\b|^\s*live\s*[:|\-]""",
            RegexOption.IGNORE_CASE
        )

        private val YOUTUBE_URL_REGEX = Regex(
            """(?:https?://)?(?:www\.|m\.)?(?:youtube\.com/(?:watch\?(?:[^"'\s<>]*&)?v=|embed/|shorts/|live/)|youtu\.be/)([A-Za-z0-9_-]{11})""",
            RegexOption.IGNORE_CASE
        )

        fun extractYouTubeVideoId(textOrUrl: String): String? {
            if (textOrUrl.isBlank()) return null
            return YOUTUBE_URL_REGEX.find(textOrUrl)?.groupValues?.getOrNull(1)
        }

        fun extractClipFromArticle(article: com.example.data.local.ArticleEntity): VideoClipEntity? {
            val videoId = extractYouTubeVideoId(article.sourceUrl)
                ?: extractYouTubeVideoId(article.fullBody)
                ?: extractYouTubeVideoId(article.summary)
                ?: return null
            return VideoClipEntity(
                id = "yt_$videoId",
                videoId = videoId,
                title = article.title,
                channelName = article.sourceName,
                category = article.category,
                thumbnailUrl = article.imageUrl.ifBlank { "https://i.ytimg.com/vi/$videoId/hqdefault.jpg" },
                watchUrl = "https://www.youtube.com/watch?v=$videoId",
                embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?rel=0&playsinline=1&autoplay=1",
                publishedAt = article.publishedAt,
                summary = article.summary,
                sourceArticleUrl = article.sourceUrl,
                isLiveBadge = article.sourceUrl.contains("/live", ignoreCase = true) ||
                    LIVE_BROADCAST_REGEX.containsMatchIn(article.title)
            )
        }
    }
}
