package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultNewsRepository
import com.example.data.NewsRepository
import com.example.data.SyncStatus
import com.example.data.local.AppDatabase
import com.example.data.remote.RssRemoteNewsSource
import com.example.model.AppLanguage
import com.example.model.IslamicSection
import com.example.model.LocalizedContent
import com.example.model.NewsArticle
import com.example.model.NewsCategory
import com.example.util.CityPrayerInfo
import com.example.util.NewsNotificationHelper
import com.example.util.PrayerTimesUtil
import com.example.worker.NewsSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

class NewsViewModel @JvmOverloads constructor(
    application: Application,
    customRepository: NewsRepository? = null
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("khabarnama_prefs", Context.MODE_PRIVATE)
    private val repository: NewsRepository = customRepository ?: DefaultNewsRepository(
        remoteSource = RssRemoteNewsSource(application),
        articleDao = AppDatabase.getInstance(application).articleDao(),
        context = application
    )

    private val _currentLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString(KEY_SELECTED_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code)
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(
        NewsNotificationHelper.isNotificationsEnabled(application)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    @Volatile
    private var pendingNotificationArticleId: String? = null

    private val _selectedCategory = MutableStateFlow(NewsCategory.ALL)
    val selectedCategory: StateFlow<NewsCategory> = _selectedCategory.asStateFlow()

    private val _selectedIslamicSection = MutableStateFlow(IslamicSection.ALL)
    val selectedIslamicSection: StateFlow<IslamicSection> = _selectedIslamicSection.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchOpen = MutableStateFlow(false)
    val isSearchOpen: StateFlow<Boolean> = _isSearchOpen.asStateFlow()

    private val _recentSearches = MutableStateFlow(loadRecentSearchesFromPrefs())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _selectedCity = MutableStateFlow(
        PrayerTimesUtil.findCityById(prefs.getString(KEY_SELECTED_CITY_ID, PrayerTimesUtil.supportedCities[0].id))
    )
    val selectedCity: StateFlow<CityPrayerInfo> = _selectedCity.asStateFlow()

    private val _hijriAdjustment = MutableStateFlow(
        prefs.getInt(KEY_HIJRI_ADJUSTMENT, 0).coerceIn(-2, 2)
    )
    val hijriAdjustment: StateFlow<Int> = _hijriAdjustment.asStateFlow()

    private val _activeImageSourceArticle = MutableStateFlow<NewsArticle?>(null)
    val activeImageSourceArticle: StateFlow<NewsArticle?> = _activeImageSourceArticle.asStateFlow()

    private val _selectedArticleForDetail = MutableStateFlow<NewsArticle?>(null)
    val selectedArticleForDetail: StateFlow<NewsArticle?> = _selectedArticleForDetail.asStateFlow()

    private val _isShowingPrayerDialog = MutableStateFlow(false)
    val isShowingPrayerDialog: StateFlow<Boolean> = _isShowingPrayerDialog.asStateFlow()

    private val _isShowingLanguageDialog = MutableStateFlow(false)
    val isShowingLanguageDialog: StateFlow<Boolean> = _isShowingLanguageDialog.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(false)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _featuredIslamicArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val featuredIslamicArticles: StateFlow<List<NewsArticle>> = _featuredIslamicArticles.asStateFlow()

    private val _articles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val articles: StateFlow<List<NewsArticle>> = _articles.asStateFlow()

    private val pageSize = 8

    @Volatile
    private var nextBeforePublishedAt: Long = Long.MAX_VALUE

    @Volatile
    private var nextAfterId: String = ""

    // Single reload job & request generation id to prevent races between filter changes and loadMore
    private var reloadJob: Job? = null
    private var loadMoreJob: Job? = null
    private val activeRequestId = AtomicLong(0L)

    // Thread-safe sets for failed and in-flight translations
    private val failedTranslations: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val inFlightTranslations: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val translationSemaphore = Semaphore(5)

    init {
        NewsSyncWorker.schedulePeriodicSync(application)
        initialLoadAndSync()
    }

    private fun loadRecentSearchesFromPrefs(): List<String> {
        val raw = prefs.getString(KEY_RECENT_SEARCHES, null)
        if (raw == null) {
            return DEFAULT_RECENT_SEARCHES
        }
        return raw.split(RECENT_SEARCH_DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun saveRecentSearchesToPrefs(searches: List<String>) {
        prefs.edit()
            .putString(KEY_RECENT_SEARCHES, searches.joinToString(RECENT_SEARCH_DELIMITER))
            .apply()
    }

    fun recordRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return
        val updated = (listOf(trimmed) + _recentSearches.value.filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(MAX_RECENT_SEARCHES)
        _recentSearches.value = updated
        saveRecentSearchesToPrefs(updated)
    }

    fun removeRecentSearch(query: String) {
        val updated = _recentSearches.value.filterNot { it.equals(query, ignoreCase = true) }
        _recentSearches.value = updated
        saveRecentSearchesToPrefs(updated)
    }

    fun clearRecentSearches() {
        _recentSearches.value = emptyList()
        saveRecentSearchesToPrefs(emptyList())
    }

    private fun initialLoadAndSync() {
        reloadJob?.cancel()
        loadMoreJob?.cancel()
        val requestId = activeRequestId.incrementAndGet()

        reloadJob = viewModelScope.launch {
            val cachedCount = repository.getCachedArticleCount()
            if (cachedCount == 0) {
                repository.seedInitialArticlesIfEmpty()
            }

            // Immediately display initial stories without waiting for network!
            _isInitialLoading.value = false
            reloadFromCacheInternal(requestId)

            // Live RSS network sync in background (takes only 2-4 seconds)
            val status = repository.syncFromRemote()
            if (activeRequestId.get() == requestId) {
                handleSyncStatus(status)
                reloadFromCacheInternal(requestId)
                _isInitialLoading.value = false
                if (status == SyncStatus.SUCCESS) {
                    val snapshot = _articles.value
                    val lang = _currentLanguage.value
                    viewModelScope.launch(Dispatchers.IO) {
                        NewsNotificationHelper.notifyLatestBreakingNewsIfNew(
                            context = getApplication(),
                            articles = snapshot,
                            language = lang
                        )
                    }
                }
            }
        }
    }

    private fun scheduleReload(debounceMs: Long = 0L) {
        reloadJob?.cancel()
        loadMoreJob?.cancel()
        _isLoadingMore.value = false
        val requestId = activeRequestId.incrementAndGet()

        reloadJob = viewModelScope.launch {
            if (debounceMs > 0L) {
                delay(debounceMs)
            }
            val currentQuery = _searchQuery.value.trim()
            if (debounceMs > 0L && currentQuery.length >= 3) {
                recordRecentSearch(currentQuery)
            }
            reloadFromCacheInternal(requestId)
        }
    }

    private fun handleSyncStatus(status: SyncStatus) {
        when (status) {
            SyncStatus.SUCCESS -> {
                _isOffline.value = false
                _errorMessage.value = null
            }
            SyncStatus.OFFLINE_WITH_CACHE -> {
                _isOffline.value = true
                _errorMessage.value = null
            }
            SyncStatus.ERROR_EMPTY_CACHE -> {
                _isOffline.value = true
                _errorMessage.value = "Connect to the internet to load your first edition of Khabarnama. Once stories are loaded, they are saved automatically for offline reading."
            }
        }
    }

    private suspend fun reloadFromCacheInternal(requestId: Long) {
        val category = _selectedCategory.value
        val section = _selectedIslamicSection.value
        val query = _searchQuery.value
        val lang = _currentLanguage.value

        val featured = repository.getFeaturedIslamicArticles(limit = 4)
        if (activeRequestId.get() != requestId) return

        val excludeIds = if (
            query.isBlank() &&
            (category == NewsCategory.ALL || category == NewsCategory.ISLAMIC)
        ) {
            featured.map { it.id }.toSet()
        } else {
            emptySet()
        }

        val pageResult = repository.getArticlesPage(
            category = category,
            islamicSection = section,
            searchQuery = query,
            language = lang,
            excludeIds = excludeIds,
            beforePublishedAt = Long.MAX_VALUE,
            afterId = "",
            pageSize = pageSize
        )

        if (activeRequestId.get() != requestId) return

        nextBeforePublishedAt = pageResult.nextBeforePublishedAt
        nextAfterId = pageResult.nextAfterId
        _hasMore.value = pageResult.hasMore
        _featuredIslamicArticles.value = applyRuntimeTranslationFlags(featured, lang)
        _articles.value = applyRuntimeTranslationFlags(pageResult.articles, lang)

        pendingNotificationArticleId?.let { targetId ->
            val matched = (_articles.value + _featuredIslamicArticles.value).firstOrNull { it.id == targetId }
            if (matched != null) {
                _selectedArticleForDetail.value = matched
                pendingNotificationArticleId = null
            }
        }

        enqueueMissingTranslations(lang)
    }

    fun setLanguage(language: AppLanguage) {
        failedTranslations.clear()
        _currentLanguage.value = language
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, language.code).apply()
        scheduleReload(debounceMs = 0L)
    }

    fun retryTranslations() {
        failedTranslations.clear()
        val lang = _currentLanguage.value
        _featuredIslamicArticles.update { applyRuntimeTranslationFlags(it, lang) }
        _articles.update { applyRuntimeTranslationFlags(it, lang) }
        enqueueMissingTranslations(lang)
    }

    fun selectCategory(category: NewsCategory) {
        _selectedCategory.value = category
        if (category != NewsCategory.ISLAMIC) {
            _selectedIslamicSection.value = IslamicSection.ALL
        }
        scheduleReload(debounceMs = 0L)
    }

    fun selectIslamicSection(section: IslamicSection) {
        _selectedIslamicSection.value = section
        scheduleReload(debounceMs = 0L)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        scheduleReload(debounceMs = SEARCH_DEBOUNCE_MS)
    }

    fun selectRecentSearch(query: String) {
        _searchQuery.value = query
        recordRecentSearch(query)
        scheduleReload(debounceMs = 0L)
    }

    fun toggleSearchOpen(open: Boolean) {
        _isSearchOpen.value = open
        if (!open && _searchQuery.value.isNotEmpty()) {
            _searchQuery.value = ""
            scheduleReload(debounceMs = 0L)
        }
    }

    fun refreshNews() {
        failedTranslations.clear()
        reloadJob?.cancel()
        loadMoreJob?.cancel()
        _isLoadingMore.value = false
        val requestId = activeRequestId.incrementAndGet()

        reloadJob = viewModelScope.launch {
            val hadArticles = _articles.value.isNotEmpty() || _featuredIslamicArticles.value.isNotEmpty()
            if (hadArticles) {
                _isRefreshing.value = true
            } else {
                _isInitialLoading.value = true
            }
            _errorMessage.value = null

            val status = repository.syncFromRemote()
            if (activeRequestId.get() == requestId) {
                handleSyncStatus(status)
                reloadFromCacheInternal(requestId)
                _isRefreshing.value = false
                _isInitialLoading.value = false
                if (status == SyncStatus.SUCCESS) {
                    val snapshot = _articles.value
                    val lang = _currentLanguage.value
                    viewModelScope.launch(Dispatchers.IO) {
                        NewsNotificationHelper.notifyLatestBreakingNewsIfNew(
                            context = getApplication(),
                            articles = snapshot,
                            language = lang
                        )
                    }
                }
            }
        }
    }

    fun loadMore() {
        if (_isLoadingMore.value || !_hasMore.value || _isInitialLoading.value) return
        val requestId = activeRequestId.get()
        val cursorPublishedAt = nextBeforePublishedAt
        val cursorAfterId = nextAfterId
        val category = _selectedCategory.value
        val section = _selectedIslamicSection.value
        val query = _searchQuery.value
        val lang = _currentLanguage.value
        val excludeIds = if (
            query.isBlank() &&
            (category == NewsCategory.ALL || category == NewsCategory.ISLAMIC)
        ) {
            _featuredIslamicArticles.value.map { it.id }.toSet()
        } else {
            emptySet()
        }

        _isLoadingMore.value = true
        loadMoreJob = viewModelScope.launch {
            try {
                val pageResult = repository.getArticlesPage(
                    category = category,
                    islamicSection = section,
                    searchQuery = query,
                    language = lang,
                    excludeIds = excludeIds,
                    beforePublishedAt = cursorPublishedAt,
                    afterId = cursorAfterId,
                    pageSize = pageSize
                )

                // Guard against stale loadMore completing after a category/search/language reload
                if (activeRequestId.get() != requestId) return@launch

                nextBeforePublishedAt = pageResult.nextBeforePublishedAt
                nextAfterId = pageResult.nextAfterId
                _hasMore.value = pageResult.hasMore

                val existingIds = _articles.value.map { it.id }.toHashSet()
                val newItems = applyRuntimeTranslationFlags(
                    pageResult.articles.filter { it.id !in existingIds },
                    lang
                )
                _articles.value = _articles.value + newItems

                // Enqueue translations only for newly added untranslated articles without cancelling running translations
                enqueueMissingTranslations(lang)
            } finally {
                if (activeRequestId.get() == requestId) {
                    _isLoadingMore.value = false
                }
            }
        }
    }

    private fun applyRuntimeTranslationFlags(list: List<NewsArticle>, lang: AppLanguage): List<NewsArticle> {
        if (lang == AppLanguage.ENGLISH) {
            return list.map { it.copy(isTranslating = false, translationFailed = false) }
        }
        return list.map { article ->
            val hasLang = article.hasTranslation(lang)
            val key = "${article.id}_${lang.code}"
            val failed = key in failedTranslations
            article.copy(
                isTranslating = !hasLang && !failed,
                translationFailed = !hasLang && failed
            )
        }
    }

    private fun enqueueMissingTranslations(lang: AppLanguage) {
        if (lang == AppLanguage.ENGLISH) return

        val candidates = (_featuredIslamicArticles.value + _articles.value)
            .distinctBy { it.id }
            .filter { article ->
                val key = "${article.id}_${lang.code}"
                !article.hasTranslation(lang) && key !in failedTranslations && inFlightTranslations.add(key)
            }

        for (article in candidates) {
            val key = "${article.id}_${lang.code}"
            viewModelScope.launch {
                try {
                    translationSemaphore.withPermit {
                        val content = repository.getOrTranslateArticle(article, lang)
                        if (content != null) {
                            failedTranslations.remove(key)
                            updateArticleTranslationState(
                                articleId = article.id,
                                lang = lang,
                                content = content,
                                failed = false
                            )
                        } else {
                            failedTranslations.add(key)
                            updateArticleTranslationState(
                                articleId = article.id,
                                lang = lang,
                                content = null,
                                failed = true
                            )
                        }
                    }
                } finally {
                    inFlightTranslations.remove(key)
                }
            }
        }
    }

    private fun updateArticleTranslationState(
        articleId: String,
        lang: AppLanguage,
        content: LocalizedContent?,
        failed: Boolean
    ) {
        val currentLang = _currentLanguage.value
        val updater: (NewsArticle) -> NewsArticle = { item ->
            if (item.id != articleId) {
                item
            } else {
                val updatedMap = if (content != null) {
                    item.translations + (lang to content)
                } else {
                    item.translations
                }
                val hasCurrentLang = updatedMap.containsKey(currentLang) || currentLang == AppLanguage.ENGLISH
                val currentKey = "${item.id}_${currentLang.code}"
                item.copy(
                    translations = updatedMap,
                    isTranslating = !hasCurrentLang && currentKey !in failedTranslations && currentKey in inFlightTranslations,
                    translationFailed = if (lang == currentLang) failed else item.translationFailed
                )
            }
        }

        _featuredIslamicArticles.update { list -> list.map(updater) }
        _articles.update { list -> list.map(updater) }
        _selectedArticleForDetail.update { current -> current?.let(updater) }
    }

    fun openImageSourceSheet(article: NewsArticle) {
        _activeImageSourceArticle.value = article
    }

    fun closeImageSourceSheet() {
        _activeImageSourceArticle.value = null
    }

    fun openArticleDetail(article: NewsArticle) {
        if (_isSearchOpen.value && _searchQuery.value.isNotBlank()) {
            recordRecentSearch(_searchQuery.value)
        }
        _selectedArticleForDetail.value = article
    }

    fun closeArticleDetail() {
        _selectedArticleForDetail.value = null
    }

    fun selectCity(city: CityPrayerInfo) {
        _selectedCity.value = city
        prefs.edit().putString(KEY_SELECTED_CITY_ID, city.id).apply()
    }

    fun setHijriAdjustment(days: Int) {
        val clamped = days.coerceIn(-2, 2)
        _hijriAdjustment.value = clamped
        prefs.edit().putInt(KEY_HIJRI_ADJUSTMENT, clamped).apply()
    }

    fun showPrayerDialog(show: Boolean) {
        _isShowingPrayerDialog.value = show
    }

    fun showLanguageDialog(show: Boolean) {
        _isShowingLanguageDialog.value = show
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        NewsNotificationHelper.setNotificationsEnabled(getApplication(), enabled)
    }

    fun sendTestBreakingNotification() {
        val allArticles = _articles.value + _featuredIslamicArticles.value
        val candidate = allArticles.firstOrNull { it.imageUrl.startsWith("https://") }
            ?: allArticles.firstOrNull()
            ?: return
        val lang = _currentLanguage.value
        viewModelScope.launch(Dispatchers.IO) {
            NewsNotificationHelper.postRichArticleNotification(
                context = getApplication(),
                article = candidate,
                language = lang
            )
        }
    }

    fun openArticleById(articleId: String) {
        if (articleId.isBlank()) return
        val matched = (_articles.value + _featuredIslamicArticles.value).firstOrNull { it.id == articleId }
        if (matched != null) {
            _selectedArticleForDetail.value = matched
            pendingNotificationArticleId = null
        } else {
            pendingNotificationArticleId = articleId
        }
    }

    companion object {
        private const val KEY_SELECTED_CITY_ID = "selected_city_id"
        private const val KEY_HIJRI_ADJUSTMENT = "hijri_adjustment_days"
        private const val KEY_SELECTED_LANGUAGE = "selected_news_language"
        private const val KEY_RECENT_SEARCHES = "recent_search_queries"
        private const val RECENT_SEARCH_DELIMITER = "|||"
        private const val MAX_RECENT_SEARCHES = 6
        private val DEFAULT_RECENT_SEARCHES = listOf("Ramadan", "Pakistan Economy", "Makkah", "Scholars")
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
