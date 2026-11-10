package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ArticleDao {

    @Query(
        """
        SELECT * FROM cached_articles
        WHERE category = 'ISLAMIC'
        ORDER BY publishedAt DESC, id ASC
        LIMIT :limit
        """
    )
    suspend fun getLatestIslamicArticles(limit: Int = 4): List<ArticleEntity>

    @Query(
        """
        SELECT cached_articles.* FROM cached_articles
        LEFT JOIN article_translations t
          ON t.articleId = cached_articles.id AND t.languageCode = :languageCode
        WHERE (:category IS NULL OR cached_articles.category = :category)
          AND (:islamicSection IS NULL OR cached_articles.islamicSection = :islamicSection)
          AND (
            :searchQuery = ''
            OR LOWER(cached_articles.title) LIKE '%' || LOWER(:searchQuery) || '%'
            OR LOWER(cached_articles.summary) LIKE '%' || LOWER(:searchQuery) || '%'
            OR LOWER(cached_articles.sourceName) LIKE '%' || LOWER(:searchQuery) || '%'
            OR (t.translatedTitle IS NOT NULL AND LOWER(t.translatedTitle) LIKE '%' || LOWER(:searchQuery) || '%')
            OR (t.translatedSummary IS NOT NULL AND LOWER(t.translatedSummary) LIKE '%' || LOWER(:searchQuery) || '%')
          )
          AND (cached_articles.publishedAt < :beforePublishedAt OR (cached_articles.publishedAt = :beforePublishedAt AND cached_articles.id > :afterId))
        ORDER BY cached_articles.publishedAt DESC, cached_articles.id ASC
        LIMIT :limit
        """
    )
    suspend fun getArticlesPage(
        category: String?,
        islamicSection: String?,
        searchQuery: String,
        languageCode: String,
        beforePublishedAt: Long,
        afterId: String,
        limit: Int
    ): List<ArticleEntity>

    @Query(
        """
        SELECT COUNT(DISTINCT cached_articles.id) FROM cached_articles
        LEFT JOIN article_translations t
          ON t.articleId = cached_articles.id AND t.languageCode = :languageCode
        WHERE (:category IS NULL OR cached_articles.category = :category)
          AND (:islamicSection IS NULL OR cached_articles.islamicSection = :islamicSection)
          AND (
            :searchQuery = ''
            OR LOWER(cached_articles.title) LIKE '%' || LOWER(:searchQuery) || '%'
            OR LOWER(cached_articles.summary) LIKE '%' || LOWER(:searchQuery) || '%'
            OR LOWER(cached_articles.sourceName) LIKE '%' || LOWER(:searchQuery) || '%'
            OR (t.translatedTitle IS NOT NULL AND LOWER(t.translatedTitle) LIKE '%' || LOWER(:searchQuery) || '%')
            OR (t.translatedSummary IS NOT NULL AND LOWER(t.translatedSummary) LIKE '%' || LOWER(:searchQuery) || '%')
          )
        """
    )
    suspend fun countMatchingArticles(
        category: String?,
        islamicSection: String?,
        searchQuery: String,
        languageCode: String = "EN"
    ): Int

    @Query("SELECT COUNT(*) FROM cached_articles")
    suspend fun getTotalCount(): Int

    @Query("SELECT * FROM cached_articles WHERE normalizedUrl = :normalizedUrl OR normalizedTitle = :normalizedTitle LIMIT 1")
    suspend fun findDuplicate(normalizedUrl: String, normalizedTitle: String): ArticleEntity?

    @Query("SELECT normalizedUrl FROM cached_articles")
    suspend fun getAllNormalizedUrls(): List<String>

    @Query("SELECT normalizedTitle FROM cached_articles")
    suspend fun getAllNormalizedTitles(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: List<ArticleEntity>)

    @Query("DELETE FROM cached_articles WHERE publishedAt < :olderThan")
    suspend fun deleteArticlesOlderThan(olderThan: Long)

    @Query("DELETE FROM article_translations WHERE articleId NOT IN (SELECT id FROM cached_articles)")
    suspend fun deleteOrphanTranslations()

    @Query("DELETE FROM cached_articles")
    suspend fun clearAllArticles()

    @Query("SELECT * FROM article_translations WHERE articleId = :articleId AND languageCode = :languageCode LIMIT 1")
    suspend fun getTranslation(articleId: String, languageCode: String): ArticleTranslationEntity?

    @Query("SELECT * FROM article_translations WHERE articleId IN (:articleIds)")
    suspend fun getTranslationsForArticles(articleIds: List<String>): List<ArticleTranslationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(translation: ArticleTranslationEntity)
}
