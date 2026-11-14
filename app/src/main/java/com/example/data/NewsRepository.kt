package com.example.data

import android.content.Context
import com.example.data.ai.FirebaseIntelligenceService
import com.example.data.ai.GeminiNewsTranslator
import com.example.data.ai.NewsTranslator
import com.example.data.ai.SituationBrief
import com.example.data.local.ArticleBriefingEntity
import com.example.data.local.ArticleDao
import com.example.data.local.ArticleEntity
import com.example.data.local.ArticleTranslationEntity
import com.example.data.local.VideoDao
import com.example.data.remote.RemoteConfigManager
import com.example.data.remote.RemoteNewsSource
import com.example.data.remote.RemoteVideoSource
import com.example.data.remote.RssRemoteNewsSource
import com.example.data.remote.YouTubeAtomVideoSource
import com.example.model.AppLanguage
import com.example.model.ArticleBriefing
import com.example.model.IslamicSection
import com.example.model.LocalizedContent
import com.example.model.NewsArticle
import com.example.model.NewsCategory
import com.example.model.NewsVideoClip
import com.example.model.VideoChannelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

enum class SyncStatus {
    SUCCESS,
    OFFLINE_WITH_CACHE,
    ERROR_EMPTY_CACHE
}

data class PaginatedArticlesResult(
    val articles: List<NewsArticle>,
    val hasMore: Boolean,
    val nextBeforePublishedAt: Long,
    val nextAfterId: String
)

interface NewsRepository {
    suspend fun syncFastPriorityBatch(): SyncStatus = syncFromRemote()
    suspend fun syncFromRemote(): SyncStatus
    suspend fun getCachedArticleCount(): Int
    suspend fun getFeaturedIslamicArticles(limit: Int = 4): List<NewsArticle>
    suspend fun getRecentAlertArticles(limit: Int = 25): List<NewsArticle> = emptyList()
    suspend fun getSavedArticles(articleIds: Set<String>): List<NewsArticle> = emptyList()
    suspend fun getArticleById(articleId: String): NewsArticle? = null
    suspend fun syncVideosFromRemote(): Int = 0
    suspend fun getTrendingVideos(category: NewsCategory = NewsCategory.ALL, limit: Int = 15): List<NewsVideoClip> = emptyList()
    fun getTrustedVideoChannels(): List<VideoChannelConfig> = emptyList()
    suspend fun translateVideoTitle(clip: NewsVideoClip, targetLanguage: AppLanguage): String? = null
    suspend fun getArticlesPage(
        category: NewsCategory,
        islamicSection: IslamicSection? = null,
        searchQuery: String = "",
        language: AppLanguage = AppLanguage.ENGLISH,
        excludeIds: Set<String> = emptySet(),
        beforePublishedAt: Long = Long.MAX_VALUE,
        afterId: String = "",
        pageSize: Int = 8
    ): PaginatedArticlesResult

    suspend fun getOrTranslateArticle(
        article: NewsArticle,
        targetLanguage: AppLanguage
    ): LocalizedContent?

    suspend fun getOrGenerateArticleBriefing(
        article: NewsArticle,
        targetLanguage: AppLanguage,
        forceRefresh: Boolean = false
    ): Result<ArticleBriefing> = Result.failure(UnsupportedOperationException("AI Briefing not available"))

    suspend fun generateSituationBrief(
        articles: List<NewsArticle>,
        targetLanguage: AppLanguage
    ): Result<SituationBrief> = Result.failure(UnsupportedOperationException("Situation Brief not available"))
}

class DefaultNewsRepository(
    private val remoteSource: RemoteNewsSource,
    private val articleDao: ArticleDao,
    private val translator: NewsTranslator = GeminiNewsTranslator(),
    private val intelligenceService: FirebaseIntelligenceService = FirebaseIntelligenceService(),
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() },
    private val context: Context? = null,
    private val videoDao: VideoDao? = null,
    private val remoteVideoSource: RemoteVideoSource? = context?.let { YouTubeAtomVideoSource(it) }
) : NewsRepository {

    override suspend fun syncFastPriorityBatch(): SyncStatus = withContext(Dispatchers.IO) {
        try {
            articleDao.deleteLegacySeedArticles()
            val fetched = remoteSource.fetchFastPriorityArticles()
            if (fetched.isNotEmpty()) {
                upsertDeduplicatedBatch(fetched)
                extractAndCacheEmbeddedVideos(fetched)
                return@withContext SyncStatus.SUCCESS
            }
        } catch (_: Exception) {
            // Fall through to cache check
        }
        if (articleDao.getTotalCount() > 0) SyncStatus.OFFLINE_WITH_CACHE else SyncStatus.ERROR_EMPTY_CACHE
    }

    override suspend fun syncFromRemote(): SyncStatus = withContext(Dispatchers.IO) {
        try {
            articleDao.deleteLegacySeedArticles()
            val fetched = remoteSource.fetchLatestArticles()
            if (fetched.isNotEmpty()) {
                upsertDeduplicatedBatch(fetched)
                extractAndCacheEmbeddedVideos(fetched)

                // Retention controlled remotely via Firebase Remote Config (default 14 days)
                val retentionDays = RemoteConfigManager.config.value.articleRetentionDays.coerceIn(3, 30)
                val retentionCutoff = currentTimeProvider() - (retentionDays.toLong() * 24L * 3600L * 1000L)
                articleDao.deleteArticlesOlderThan(retentionCutoff)
                articleDao.deleteOrphanTranslations()
                articleDao.deleteOrphanBriefings()
                videoDao?.deleteVideosOlderThan(retentionCutoff)

                return@withContext SyncStatus.SUCCESS
            }
        } catch (_: Exception) {
            // Fall through to check local cache
        }

        val count = articleDao.getTotalCount()
        if (count > 0) {
            SyncStatus.OFFLINE_WITH_CACHE
        } else {
            SyncStatus.ERROR_EMPTY_CACHE
        }
    }

    private suspend fun upsertDeduplicatedBatch(fetched: List<ArticleEntity>) {
        val deduplicatedBatch = RssRemoteNewsSource.deduplicateArticles(fetched)
        val existingUrls = articleDao.getAllNormalizedUrls().toHashSet()
        val existingTitles = articleDao.getAllNormalizedTitles().toHashSet()

        val toUpsert = mutableListOf<ArticleEntity>()
        for (entity in deduplicatedBatch) {
            if (entity.normalizedUrl !in existingUrls && entity.normalizedTitle !in existingTitles) {
                existingUrls.add(entity.normalizedUrl)
                existingTitles.add(entity.normalizedTitle)
                toUpsert.add(entity)
            }
        }

        if (toUpsert.isNotEmpty()) {
            articleDao.insertArticles(toUpsert)
        }
    }

    private suspend fun extractAndCacheEmbeddedVideos(articles: List<ArticleEntity>) {
        val dao = videoDao ?: return
        val embedded = articles.mapNotNull { YouTubeAtomVideoSource.extractClipFromArticle(it) }
        if (embedded.isNotEmpty()) {
            dao.insertVideos(embedded)
        }
    }

    override suspend fun syncVideosFromRemote(): Int = withContext(Dispatchers.IO) {
        val source = remoteVideoSource ?: return@withContext 0
        val dao = videoDao ?: return@withContext 0
        return@withContext try {
            val fetched = source.fetchLatestVideos()
            if (fetched.isNotEmpty()) {
                dao.insertVideos(fetched)
            }
            fetched.size
        } catch (_: Exception) {
            0
        }
    }

    override suspend fun getTrendingVideos(category: NewsCategory, limit: Int): List<NewsVideoClip> = withContext(Dispatchers.IO) {
        val dao = videoDao ?: return@withContext emptyList()
        val catParam = if (category == NewsCategory.ALL) null else category.name
        dao.getLatestVideos(catParam, limit).map { it.toDomainModel() }
    }

    override fun getTrustedVideoChannels(): List<VideoChannelConfig> {
        return remoteVideoSource?.loadTrustedChannels() ?: YouTubeAtomVideoSource.DEFAULT_TRUSTED_CHANNELS
    }

    override suspend fun translateVideoTitle(clip: NewsVideoClip, targetLanguage: AppLanguage): String? = withContext(Dispatchers.IO) {
        if (targetLanguage == AppLanguage.ENGLISH) return@withContext clip.title
        translator.translateArticle(
            title = clip.title,
            summary = clip.summary.ifBlank { clip.title },
            targetLanguage = targetLanguage
        )?.title
    }

    override suspend fun getCachedArticleCount(): Int = withContext(Dispatchers.IO) {
        articleDao.getTotalCount()
    }

    override suspend fun getArticleById(articleId: String): NewsArticle? = withContext(Dispatchers.IO) {
        if (articleId.isBlank()) return@withContext null
        val entity = articleDao.getArticleById(articleId) ?: return@withContext null
        mapEntitiesWithTranslations(listOf(entity), isFeaturedIslamic = entity.category == NewsCategory.ISLAMIC.name)
            .firstOrNull()
    }

    override suspend fun getFeaturedIslamicArticles(limit: Int): List<NewsArticle> = withContext(Dispatchers.IO) {
        val pool = articleDao.getLatestIslamicArticles(limit * 4)
        // Balance diverse global scholars & outlets so no single scholar/source dominates
        val diverseSelection = mutableListOf<ArticleEntity>()
        val usedSources = HashSet<String>()
        for (item in pool) {
            if (usedSources.add(item.sourceName)) {
                diverseSelection.add(item)
                if (diverseSelection.size >= limit) break
            }
        }
        if (diverseSelection.size < limit) {
            val selectedIds = diverseSelection.map { it.id }.toHashSet()
            for (item in pool) {
                if (item.id !in selectedIds) {
                    diverseSelection.add(item)
                    if (diverseSelection.size >= limit) break
                }
            }
        }
        mapEntitiesWithTranslations(diverseSelection, isFeaturedIslamic = true)
    }

    override suspend fun getRecentAlertArticles(limit: Int): List<NewsArticle> = withContext(Dispatchers.IO) {
        val cutoff24h = currentTimeProvider() - TWENTY_FOUR_HOURS_MS
        val recentEntities = articleDao.getRecentAlertArticles(minPublishedAt = cutoff24h, limit = limit)
        val finalEntities = if (recentEntities.isNotEmpty()) {
            recentEntities
        } else {
            val cutoff72h = currentTimeProvider() - SEVENTY_TWO_HOURS_MS
            articleDao.getRecentAlertArticles(minPublishedAt = cutoff72h, limit = 15)
        }
        mapEntitiesWithTranslations(finalEntities, isFeaturedIslamic = false)
    }

    override suspend fun getSavedArticles(articleIds: Set<String>): List<NewsArticle> = withContext(Dispatchers.IO) {
        if (articleIds.isEmpty()) return@withContext emptyList()
        val entities = articleDao.getArticlesByIds(articleIds.toList())
        mapEntitiesWithTranslations(entities, isFeaturedIslamic = false)
    }

    override suspend fun getArticlesPage(
        category: NewsCategory,
        islamicSection: IslamicSection?,
        searchQuery: String,
        language: AppLanguage,
        excludeIds: Set<String>,
        beforePublishedAt: Long,
        afterId: String,
        pageSize: Int
    ): PaginatedArticlesResult = withContext(Dispatchers.IO) {
        val categoryParam = if (category == NewsCategory.ALL) null else category.name
        val sectionParam = if (category == NewsCategory.ISLAMIC && islamicSection != null && islamicSection != IslamicSection.ALL) {
            islamicSection.name
        } else {
            null
        }
        val queryParam = searchQuery.trim()
        val fetchSize = pageSize + excludeIds.size + 1

        val rawEntities = articleDao.getArticlesPage(
            category = categoryParam,
            islamicSection = sectionParam,
            searchQuery = queryParam,
            languageCode = language.code,
            beforePublishedAt = beforePublishedAt,
            afterId = afterId,
            limit = fetchSize
        )

        val filtered = if (excludeIds.isEmpty()) {
            rawEntities
        } else {
            rawEntities.filter { it.id !in excludeIds }
        }

        val hasMore = filtered.size > pageSize
        val pageEntities = filtered.take(pageSize)
        val articles = mapEntitiesWithTranslations(pageEntities, isFeaturedIslamic = false)
        val lastItem = pageEntities.lastOrNull()

        PaginatedArticlesResult(
            articles = articles,
            hasMore = hasMore,
            nextBeforePublishedAt = lastItem?.let { computeRankScore(it, category) } ?: 0L,
            nextAfterId = lastItem?.id ?: ""
        )
    }

    override suspend fun getOrTranslateArticle(
        article: NewsArticle,
        targetLanguage: AppLanguage
    ): LocalizedContent? = withContext(Dispatchers.IO) {
        if (targetLanguage == AppLanguage.ENGLISH) {
            return@withContext LocalizedContent(title = article.title, summary = article.summary)
        }

        val cached = articleDao.getTranslation(article.id, targetLanguage.code)
        if (cached != null && cached.translatedTitle.isNotBlank() && cached.translatedSummary.isNotBlank()) {
            return@withContext LocalizedContent(
                title = cached.translatedTitle,
                summary = cached.translatedSummary
            )
        }

        // Prefer richer body text up to 110 words when available so Urdu & Arabic translations have journalistic depth
        val sourceTextForTranslation = article.fullBody.takeIf { it.length > article.summary.length + 20 }
            ?: article.summary
        val translated = translator.translateArticle(
            title = article.title,
            summary = sourceTextForTranslation,
            targetLanguage = targetLanguage
        )

        if (translated != null && translated.title.isNotBlank() && translated.summary.isNotBlank()) {
            articleDao.insertTranslation(
                ArticleTranslationEntity(
                    articleId = article.id,
                    languageCode = targetLanguage.code,
                    translatedTitle = translated.title,
                    translatedSummary = translated.summary
                )
            )
            return@withContext translated
        }

        null
    }

    override suspend fun getOrGenerateArticleBriefing(
        article: NewsArticle,
        targetLanguage: AppLanguage,
        forceRefresh: Boolean
    ): Result<ArticleBriefing> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            val cached = articleDao.getBriefing(article.id, targetLanguage.code)
            if (cached != null && cached.detailedSummary.isNotBlank()) {
                return@withContext Result.success(cached.toDomainModel())
            }
        }

        val generated = intelligenceService.generateArticleBriefing(article, targetLanguage)
        generated.onSuccess { briefing ->
            val takeawaysJson = JSONArray(briefing.keyTakeaways).toString()
            articleDao.insertBriefing(
                ArticleBriefingEntity(
                    articleId = article.id,
                    languageCode = targetLanguage.code,
                    editorialHeadline = briefing.editorialHeadline,
                    detailedSummary = briefing.detailedSummary,
                    keyTakeawaysJson = takeawaysJson,
                    strategicContext = briefing.strategicContext,
                    notableVoices = briefing.notableVoices
                )
            )
        }
        generated
    }

    override suspend fun generateSituationBrief(
        articles: List<NewsArticle>,
        targetLanguage: AppLanguage
    ): Result<SituationBrief> {
        return intelligenceService.generateSituationBrief(articles, targetLanguage)
    }

    private suspend fun mapEntitiesWithTranslations(
        entities: List<ArticleEntity>,
        isFeaturedIslamic: Boolean
    ): List<NewsArticle> {
        if (entities.isEmpty()) return emptyList()
        val ids = entities.map { it.id }
        val cachedTranslations = articleDao.getTranslationsForArticles(ids)
            .groupBy { it.articleId }

        return entities.map { entity ->
            val entityTranslations = cachedTranslations[entity.id].orEmpty()
                .associate { trans ->
                    AppLanguage.fromCode(trans.languageCode) to LocalizedContent(
                        title = trans.translatedTitle,
                        summary = trans.translatedSummary
                    )
                }
            entity.toNewsArticle(
                translationsMap = entityTranslations,
                isFeaturedIslamic = isFeaturedIslamic
            )
        }
    }

    private fun computeRankScore(entity: ArticleEntity, activeCategory: NewsCategory): Long {
        if (activeCategory != NewsCategory.ALL) return entity.publishedAt
        val boost = when (entity.category) {
            NewsCategory.SECURITY.name -> 14_400_000L
            NewsCategory.PALESTINE.name -> 14_400_000L
            NewsCategory.ISLAMIC.name -> 10_800_000L
            NewsCategory.WORLD.name -> 7_200_000L
            NewsCategory.PAKISTAN.name -> 7_200_000L
            NewsCategory.TECHNOLOGY.name -> -14_400_000L
            NewsCategory.BUSINESS.name -> -64_800_000L
            NewsCategory.SPORTS.name -> -86_400_000L
            else -> 0L
        }
        return entity.publishedAt + boost
    }

    companion object {
        const val FOURTEEN_DAYS_MS = 14L * 24L * 3600L * 1000L
        const val SEVENTY_TWO_HOURS_MS = 72L * 3600L * 1000L
        const val TWENTY_FOUR_HOURS_MS = 24L * 3600L * 1000L
    }
}
