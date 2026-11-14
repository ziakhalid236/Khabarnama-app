package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.example.model.NewsCategory
import com.example.model.NewsSourceConfig
import com.example.model.VideoChannelConfig
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.remoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import java.util.concurrent.atomic.AtomicBoolean

data class LiveBulletinConfig(
    val enabled: Boolean = false,
    val badge: String = "LIVE DISPATCH",
    val title: String = "",
    val message: String = "",
    val sourceUrl: String = ""
)

data class AppRemoteConfig(
    val aiModelName: String = RemoteConfigManager.DEFAULT_AI_MODEL,
    val aiBriefingEnabled: Boolean = true,
    val aiTranslationEnabled: Boolean = true,
    val situationBriefEnabled: Boolean = true,
    val videoSectionEnabled: Boolean = false,
    val articleRetentionDays: Int = 14,
    val extraNewsSources: List<NewsSourceConfig> = emptyList(),
    val disabledFeedUrls: Set<String> = emptySet(),
    val extraVideoChannels: List<VideoChannelConfig> = emptyList(),
    val disabledVideoChannelIds: Set<String> = emptySet(),
    val liveBulletin: LiveBulletinConfig = LiveBulletinConfig(),
    val isRemoteSynced: Boolean = false,
    val lastSyncedAtMs: Long = 0L
)

object RemoteConfigManager {

    private const val TAG = "RemoteConfigManager"
    const val DEFAULT_AI_MODEL = "gemini-3.8-flash"

    const val KEY_AI_MODEL_NAME = "ai_model_name"
    const val KEY_AI_BRIEFING_ENABLED = "ai_briefing_enabled"
    const val KEY_AI_TRANSLATION_ENABLED = "ai_translation_enabled"
    const val KEY_SITUATION_BRIEF_ENABLED = "situation_brief_enabled"
    const val KEY_VIDEO_SECTION_ENABLED = "video_section_enabled"
    const val KEY_ARTICLE_RETENTION_DAYS = "article_retention_days"
    const val KEY_BREAKING_BANNER_ENABLED = "breaking_banner_enabled"
    const val KEY_BREAKING_BANNER_BADGE = "breaking_banner_badge"
    const val KEY_BREAKING_BANNER_TITLE = "breaking_banner_title"
    const val KEY_BREAKING_BANNER_MESSAGE = "breaking_banner_message"
    const val KEY_BREAKING_BANNER_URL = "breaking_banner_url"
    const val KEY_REMOTE_NEWS_SOURCES_JSON = "remote_news_sources_json"
    const val KEY_DISABLED_FEED_URLS_JSON = "disabled_feed_urls_json"
    const val KEY_REMOTE_VIDEO_CHANNELS_JSON = "remote_video_channels_json"
    const val KEY_DISABLED_VIDEO_CHANNEL_IDS_JSON = "disabled_video_channel_ids_json"

    private val initialized = AtomicBoolean(false)
    private val listenerRegistered = AtomicBoolean(false)

    private val _config = MutableStateFlow(AppRemoteConfig())
    val config: StateFlow<AppRemoteConfig> = _config.asStateFlow()

    private val _liveSourcesVersion = MutableStateFlow(0L)
    val liveSourcesVersion: StateFlow<Long> = _liveSourcesVersion.asStateFlow()

    fun initialize(context: Context) {
        if (!initialized.compareAndSet(false, true)) return
        runCatching {
            if (!com.example.util.FirebaseInitHelper.ensureInitialized(context)) {
                Log.w(TAG, "FirebaseApp unavailable during RemoteConfigManager.initialize")
                return
            }
            val rc = Firebase.remoteConfig
            val settings = remoteConfigSettings {
                minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) 300L else 3600L
            }
            rc.setConfigSettingsAsync(settings)
            rc.setDefaultsAsync(R.xml.remote_config_defaults)
            publishSnapshot(rc, isRemoteSynced = rc.info.lastFetchStatus == FirebaseRemoteConfig.LAST_FETCH_STATUS_SUCCESS)

            rc.fetchAndActivate().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val prevSourcesSig = sourcesSignature(_config.value)
                    publishSnapshot(rc, isRemoteSynced = true)
                    if (sourcesSignature(_config.value) != prevSourcesSig) {
                        _liveSourcesVersion.value = System.currentTimeMillis()
                    }
                    Log.d(TAG, "Remote Config fetched and activated (updated=${task.result})")
                } else {
                    Log.w(TAG, "Remote Config initial fetch skipped/offline: ${task.exception?.message}")
                }
            }

            registerRealtimeListener(rc)
        }.onFailure { err ->
            Log.w(TAG, "Remote Config unavailable in current environment: ${err.message}")
        }
    }

    suspend fun refreshNow(): Boolean {
        return try {
            val rc = Firebase.remoteConfig
            val updated = rc.fetchAndActivate().await()
            val prevSourcesSig = sourcesSignature(_config.value)
            publishSnapshot(rc, isRemoteSynced = true)
            if (updated || sourcesSignature(_config.value) != prevSourcesSig) {
                _liveSourcesVersion.value = System.currentTimeMillis()
            }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Remote Config manual refresh failed: ${e.message}")
            false
        }
    }

    private fun registerRealtimeListener(rc: FirebaseRemoteConfig) {
        if (!listenerRegistered.compareAndSet(false, true)) return
        runCatching {
            rc.addOnConfigUpdateListener(object : ConfigUpdateListener {
                override fun onUpdate(configUpdate: ConfigUpdate) {
                    Log.d(TAG, "Real-time Remote Config update received: ${configUpdate.updatedKeys}")
                    rc.activate().addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            publishSnapshot(rc, isRemoteSynced = true)
                            val sourceKeys = setOf(
                                KEY_REMOTE_NEWS_SOURCES_JSON,
                                KEY_DISABLED_FEED_URLS_JSON,
                                KEY_REMOTE_VIDEO_CHANNELS_JSON,
                                KEY_DISABLED_VIDEO_CHANNEL_IDS_JSON
                            )
                            if (configUpdate.updatedKeys.any { it in sourceKeys }) {
                                _liveSourcesVersion.value = System.currentTimeMillis()
                            }
                        }
                    }
                }

                override fun onError(error: FirebaseRemoteConfigException) {
                    Log.w(TAG, "Real-time Remote Config listener error (${error.code}): ${error.message}")
                }
            })
        }
    }

    private fun publishSnapshot(rc: FirebaseRemoteConfig, isRemoteSynced: Boolean) {
        val modelName = rc.getString(KEY_AI_MODEL_NAME).trim().ifBlank { DEFAULT_AI_MODEL }
        val retentionDays = rc.getLong(KEY_ARTICLE_RETENTION_DAYS).toInt().coerceIn(3, 30)
        val bannerEnabled = rc.getBoolean(KEY_BREAKING_BANNER_ENABLED)
        val bannerBadge = rc.getString(KEY_BREAKING_BANNER_BADGE).trim().ifBlank { "LIVE DISPATCH" }
        val bannerTitle = rc.getString(KEY_BREAKING_BANNER_TITLE).trim()
        val bannerMessage = rc.getString(KEY_BREAKING_BANNER_MESSAGE).trim()
        val bannerUrl = rc.getString(KEY_BREAKING_BANNER_URL).trim()

        val extraSources = parseRemoteNewsSourcesJson(rc.getString(KEY_REMOTE_NEWS_SOURCES_JSON))
        val disabledFeedUrls = parseStringSetJson(rc.getString(KEY_DISABLED_FEED_URLS_JSON))
        val extraVideoChannels = parseRemoteVideoChannelsJson(rc.getString(KEY_REMOTE_VIDEO_CHANNELS_JSON))
        val disabledVideoChannelIds = parseStringSetJson(rc.getString(KEY_DISABLED_VIDEO_CHANNEL_IDS_JSON))

        _config.value = AppRemoteConfig(
            aiModelName = modelName,
            aiBriefingEnabled = rc.getBoolean(KEY_AI_BRIEFING_ENABLED),
            aiTranslationEnabled = rc.getBoolean(KEY_AI_TRANSLATION_ENABLED),
            situationBriefEnabled = rc.getBoolean(KEY_SITUATION_BRIEF_ENABLED),
            videoSectionEnabled = rc.getBoolean(KEY_VIDEO_SECTION_ENABLED),
            articleRetentionDays = retentionDays,
            extraNewsSources = extraSources,
            disabledFeedUrls = disabledFeedUrls,
            extraVideoChannels = extraVideoChannels,
            disabledVideoChannelIds = disabledVideoChannelIds,
            liveBulletin = LiveBulletinConfig(
                enabled = bannerEnabled && bannerTitle.isNotBlank(),
                badge = bannerBadge,
                title = bannerTitle,
                message = bannerMessage,
                sourceUrl = bannerUrl
            ),
            isRemoteSynced = isRemoteSynced,
            lastSyncedAtMs = System.currentTimeMillis()
        )
    }

    private fun sourcesSignature(cfg: AppRemoteConfig): Int {
        return (cfg.extraNewsSources.hashCode() * 31) +
            (cfg.disabledFeedUrls.hashCode() * 17) +
            (cfg.extraVideoChannels.hashCode() * 7) +
            cfg.disabledVideoChannelIds.hashCode()
    }

    fun parseRemoteNewsSourcesJson(jsonString: String): List<NewsSourceConfig> {
        val trimmed = jsonString.trim()
        if (trimmed.isBlank() || trimmed == "[]") return emptyList()
        return runCatching {
            val array = JSONArray(trimmed)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val name = obj.optString("name").trim()
                    val feedUrl = obj.optString("feedUrl").trim()
                    if (name.isNotEmpty() && feedUrl.startsWith("https://", ignoreCase = true)) {
                        add(
                            NewsSourceConfig(
                                name = name,
                                feedUrl = feedUrl,
                                category = NewsCategory.fromString(obj.optString("category", "WORLD")),
                                language = obj.optString("language", "EN").trim().ifBlank { "EN" },
                                isIslamicSource = obj.optBoolean("isIslamicSource", false),
                                isScholarlySource = obj.optBoolean("isScholarlySource", false)
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun parseRemoteVideoChannelsJson(jsonString: String): List<VideoChannelConfig> {
        val trimmed = jsonString.trim()
        if (trimmed.isBlank() || trimmed == "[]") return emptyList()
        return runCatching {
            val array = JSONArray(trimmed)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val channelId = obj.optString("channelId").trim()
                    val channelName = obj.optString("channelName").trim()
                    if (channelId.isNotEmpty() && channelName.isNotEmpty()) {
                        val customFeedUrl = obj.optString("feedUrl").trim()
                        val feedUrl = if (customFeedUrl.startsWith("https://", ignoreCase = true)) {
                            customFeedUrl
                        } else {
                            "https://www.youtube.com/feeds/videos.xml?channel_id=$channelId"
                        }
                        val watchLiveUrl = obj.optString("watchLiveUrl").trim()
                        add(
                            VideoChannelConfig(
                                channelId = channelId,
                                channelName = channelName,
                                category = NewsCategory.fromString(obj.optString("category", "WORLD")),
                                countryFlag = obj.optString("countryFlag", "📺").ifBlank { "📺" },
                                watchLiveUrl = watchLiveUrl,
                                feedUrl = feedUrl
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun parseStringSetJson(jsonString: String): Set<String> {
        val trimmed = jsonString.trim()
        if (trimmed.isBlank() || trimmed == "[]") return emptySet()
        return runCatching {
            val array = JSONArray(trimmed)
            buildSet {
                for (i in 0 until array.length()) {
                    val item = array.optString(i).trim()
                    if (item.isNotEmpty()) add(item)
                }
            }
        }.getOrDefault(emptySet())
    }
}
