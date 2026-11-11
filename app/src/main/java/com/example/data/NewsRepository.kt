package com.example.data

import android.content.Context
import com.example.data.ai.GeminiNewsTranslator
import com.example.data.ai.NewsTranslator
import com.example.data.local.ArticleDao
import com.example.data.local.ArticleEntity
import com.example.data.local.ArticleTranslationEntity
import com.example.data.remote.RemoteNewsSource
import com.example.data.remote.RssRemoteNewsSource
import com.example.model.AppLanguage
import com.example.model.IslamicSection
import com.example.model.LocalizedContent
import com.example.model.NewsArticle
import com.example.model.NewsCategory
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
    suspend fun syncFromRemote(): SyncStatus
    suspend fun getCachedArticleCount(): Int
    suspend fun seedInitialArticlesIfEmpty(): Boolean
    suspend fun getFeaturedIslamicArticles(limit: Int = 4): List<NewsArticle>
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
}

class DefaultNewsRepository(
    private val remoteSource: RemoteNewsSource,
    private val articleDao: ArticleDao,
    private val translator: NewsTranslator = GeminiNewsTranslator(),
    private val currentTimeProvider: () -> Long = { System.currentTimeMillis() },
    private val context: Context? = null
) : NewsRepository {

    override suspend fun seedInitialArticlesIfEmpty(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            articleDao.deleteDemoSeedArticles()
            articleDao.deleteOrphanTranslations()
        }
        false
    }

    override suspend fun syncFromRemote(): SyncStatus = withContext(Dispatchers.IO) {
        try {
            articleDao.deleteDemoSeedArticles()
            val fetched = remoteSource.fetchLatestArticles()
            if (fetched.isNotEmpty()) {
                val deduplicatedBatch = RssRemoteNewsSource.deduplicateArticles(fetched)
                val existingUrls = articleDao.getAllNormalizedUrls().toHashSet()
                val existingTitles = articleDao.getAllNormalizedTitles().toHashSet()

                val toUpsert = mutableListOf<ArticleEntity>()
                for (entity in deduplicatedBatch) {
                    if (entity.normalizedUrl in existingUrls || entity.normalizedTitle !in existingTitles) {
                        existingUrls.add(entity.normalizedUrl)
                        existingTitles.add(entity.normalizedTitle)
                        toUpsert.add(entity)
                    }
                }

                if (toUpsert.isNotEmpty()) {
                    articleDao.insertArticles(toUpsert)
                }

                // Cleanup articles older than 14 days and delete orphan translations
                val fourteenDaysAgo = currentTimeProvider() - FOURTEEN_DAYS_MS
                articleDao.deleteArticlesOlderThan(fourteenDaysAgo)
                articleDao.deleteOrphanTranslations()

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

    override suspend fun getCachedArticleCount(): Int = withContext(Dispatchers.IO) {
        articleDao.getTotalCount()
    }

    override suspend fun getFeaturedIslamicArticles(limit: Int): List<NewsArticle> = withContext(Dispatchers.IO) {
        val entities = articleDao.getLatestIslamicArticles(limit)
        mapEntitiesWithTranslations(entities, isFeaturedIslamic = true)
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
            nextBeforePublishedAt = lastItem?.publishedAt ?: 0L,
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

        val translated = translator.translateArticle(
            title = article.title,
            summary = article.summary,
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

    companion object {
        const val FOURTEEN_DAYS_MS = 14L * 24L * 3600L * 1000L
    }
}
