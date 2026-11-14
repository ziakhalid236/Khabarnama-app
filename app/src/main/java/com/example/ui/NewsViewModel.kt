package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultNewsRepository
import com.example.data.NewsRepository
import com.example.data.SyncStatus
import com.example.data.ai.SituationBrief
import com.example.data.local.AppDatabase
import com.example.data.remote.AppRemoteConfig
import com.example.data.remote.RemoteConfigManager
import com.example.data.remote.RssRemoteNewsSource
import com.example.model.AppLanguage
import com.example.model.ArticleBriefing
import com.example.model.IslamicSection
import com.example.model.LocalizedContent
import com.example.model.NewsArticle
import com.example.model.NewsCategory
import com.example.model.NewsVideoClip
import com.example.model.VideoChannelConfig
import com.example.util.CityPrayerInfo
import com.example.util.NewsNotificationHelper
import com.example.util.NotificationFocus
import com.example.util.PrayerTimesUtil
import com.example.worker.NewsSyncWorker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
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

sealed interface AiBriefingUiState {
    data object Idle : AiBriefingUiState
    data object Loading : AiBriefingUiState
    data class Success(val briefing: ArticleBriefing) : AiBriefingUiState
    data class Error(val message: String) : AiBriefingUiState
}

sealed interface SituationBriefUiState {
    data object Idle : SituationBriefUiState
    data object Loading : SituationBriefUiState
    data class Success(val brief: SituationBrief) : SituationBriefUiState
    data class Error(val message: String) : SituationBriefUiState
}

class NewsViewModel @JvmOverloads constructor(
    application: Application,
    customRepository: NewsRepository? = null
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("khabarnama_prefs", Context.MODE_PRIVATE)
    private val repository: NewsRepository = customRepository ?: DefaultNewsRepository(
        remoteSource = RssRemoteNewsSource(application),
        articleDao = AppDatabase.getInstance(application).articleDao(),
        context = application,
        videoDao = AppDatabase.getInstance(application).videoDao()
    )

    val remoteConfig: StateFlow<AppRemoteConfig> = RemoteConfigManager.config

    private val _currentLanguage = MutableStateFlow(
        AppLanguage.fromCode(prefs.getString(KEY_SELECTED_LANGUAGE, AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code)
    )
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(
        NewsNotificationHelper.isNotificationsEnabled(application)
    )
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _notificationFocus = MutableStateFlow(
        NewsNotificationHelper.getNotificationFocus(application)
    )
    val notificationFocus: StateFlow<NotificationFocus> = _notificationFocus.asStateFlow()

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

    private val _activeBriefingState = MutableStateFlow<AiBriefingUiState>(AiBriefingUiState.Idle)
    val activeBriefingState: StateFlow<AiBriefingUiState> = _activeBriefingState.asStateFlow()

    private val _situationBriefState = MutableStateFlow<SituationBriefUiState>(SituationBriefUiState.Idle)
    val situationBriefState: StateFlow<SituationBriefUiState> = _situationBriefState.asStateFlow()

    private val _isShowingPrayerDialog = MutableStateFlow(false)
    val isShowingPrayerDialog: StateFlow<Boolean> = _isShowingPrayerDialog.asStateFlow()

    private val _isShowingLanguageDialog = MutableStateFlow(false)
    val isShowingLanguageDialog: StateFlow<Boolean> = _isShowingLanguageDialog.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _isBackgroundSyncing = MutableStateFlow(false)
    val isBackgroundSyncing: StateFlow<Boolean> = _isBackgroundSyncing.asStateFlow()

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

    private val _notificationArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val notificationArticles: StateFlow<List<NewsArticle>> = _notificationArticles.asStateFlow()

    private val _readNotificationIds = MutableStateFlow(loadStringSetFromPrefs(KEY_READ_NOTIFICATION_IDS))
    val readNotificationIds: StateFlow<Set<String>> = _readNotificationIds.asStateFlow()

    private val _dismissedNotificationIds = MutableStateFlow(loadStringSetFromPrefs(KEY_DISMISSED_NOTIFICATION_IDS))
    val dismissedNotificationIds: StateFlow<Set<String>> = _dismissedNotificationIds.asStateFlow()

    private val _savedArticleIds = MutableStateFlow(loadStringSetFromPrefs(KEY_SAVED_ARTICLE_IDS))
    val savedArticleIds: StateFlow<Set<String>> = _savedArticleIds.asStateFlow()

    private val _savedArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val savedArticles: StateFlow<List<NewsArticle>> = _savedArticles.asStateFlow()

    private val _videoClips = MutableStateFlow<List<NewsVideoClip>>(emptyList())
    val videoClips: StateFlow<List<NewsVideoClip>> = _videoClips.asStateFlow()

    private val _liveChannels = MutableStateFlow<List<VideoChannelConfig>>(emptyList())
    val liveChannels: StateFlow<List<VideoChannelConfig>> = _liveChannels.asStateFlow()

    private val _selectedVideoClip = MutableStateFlow<NewsVideoClip?>(null)
    val selectedVideoClip: StateFlow<NewsVideoClip?> = _selectedVideoClip.asStateFlow()

    private val videoTitleCache = ConcurrentHashMap<String, String>()
    private val inFlightVideoTranslations: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val pageSize = 8

    @Volatile
    private var nextBeforePublishedAt: Long = Long.MAX_VALUE

    @Volatile
    private var nextAfterId: String = ""

    // Single reload job & request generation id to prevent races between filter changes and loadMore
    private var reloadJob: Job? = null
    private var loadMoreJob: Job? = null
    private var briefingJob: Job? = null
    private var situationJob: Job? = null
    private val activeRequestId = AtomicLong(0L)

    // Thread-safe sets for failed and in-flight translations
    private val failedTranslations: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val inFlightTranslations: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val translationSemaphore = Semaphore(5)

    init {
        RemoteConfigManager.initialize(application)
        _liveChannels.value = repository.getTrustedVideoChannels()
        NewsSyncWorker.schedulePeriodicSync(application)
        initialLoadAndSync()
        observeLiveRemoteConfigUpdates()
    }

    private fun observeLiveRemoteConfigUpdates() {
        viewModelScope.launch {
            var lastSeenVersion = RemoteConfigManager.liveSourcesVersion.value
            RemoteConfigManager.liveSourcesVersion.collect { version ->
                if (version > 0L && version != lastSeenVersion) {
                    lastSeenVersion = version
                    _liveChannels.value = repository.getTrustedVideoChannels()
                    refreshNews()
                }
            }
        }
    }

    private fun loadRecentSearchesFromPrefs(): List<String> {
        val raw = prefs.getString(KEY_RECENT_SEARCHES, null) ?: return DEFAULT_RECENT_SEARCHES
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

    private fun loadStringSetFromPrefs(key: String): Set<String> {
        val raw = prefs.getString(key, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    private fun saveStringSetToPrefs(key: String, ids: Set<String>, maxCount: Int = 200) {
        val trimmed = ids.take(maxCount).joinToString(",")
        prefs.edit().putString(key, trimmed).apply()
    }

    fun markNotificationRead(articleId: String) {
        if (articleId.isBlank() || articleId in _readNotificationIds.value) return
        val updated = _readNotificationIds.value + articleId
        _readNotificationIds.value = updated
        saveStringSetToPrefs(KEY_READ_NOTIFICATION_IDS, updated)
    }

    fun markAllNotificationsRead() {
        val allIds = _notificationArticles.value.map { it.id }.toSet()
        if (allIds.isEmpty()) return
        val updated = _readNotificationIds.value + allIds
        _readNotificationIds.value = updated
        saveStringSetToPrefs(KEY_READ_NOTIFICATION_IDS, updated)
    }

    fun dismissNotification(articleId: String) {
        if (articleId.isBlank()) return
        val updatedDismissed = _dismissedNotificationIds.value + articleId
        _dismissedNotificationIds.value = updatedDismissed
        saveStringSetToPrefs(KEY_DISMISSED_NOTIFICATION_IDS, updatedDismissed)
        _notificationArticles.update { list -> list.filterNot { it.id == articleId } }
    }

    fun clearAllNotifications() {
        val currentIds = _notificationArticles.value.map { it.id }.toSet()
        if (currentIds.isEmpty()) return
        val updatedDismissed = _dismissedNotificationIds.value + currentIds
        _dismissedNotificationIds.value = updatedDismissed
        saveStringSetToPrefs(KEY_DISMISSED_NOTIFICATION_IDS, updatedDismissed)
        val updatedRead = _readNotificationIds.value + currentIds
        _readNotificationIds.value = updatedRead
        saveStringSetToPrefs(KEY_READ_NOTIFICATION_IDS, updatedRead)
        _notificationArticles.value = emptyList()
    }

    fun toggleBookmarkArticle(article: NewsArticle) {
        val currentIds = _savedArticleIds.value
        val isRemoving = article.id in currentIds
        val updatedIds = if (isRemoving) currentIds - article.id else setOf(article.id) + currentIds
        _savedArticleIds.value = updatedIds
        saveStringSetToPrefs(KEY_SAVED_ARTICLE_IDS, updatedIds, maxCount = 150)
        if (isRemoving) {
            _savedArticles.update { list -> list.filterNot { it.id == article.id } }
        } else {
            val lang = _currentLanguage.value
            val prepared = applyRuntimeTranslationFlags(listOf(article), lang).first()
            _savedArticles.update { list -> (listOf(prepared) + list.filterNot { it.id == article.id }) }
        }
    }

    private fun initialLoadAndSync() {
        reloadJob?.cancel()
        loadMoreJob?.cancel()
        val requestId = activeRequestId.incrementAndGet()

        reloadJob = viewModelScope.launch {
            val cachedCount = repository.getCachedArticleCount()
            if (cachedCount > 0) {
                _isInitialLoading.value = false
                reloadFromCacheInternal(requestId)
                _isBackgroundSyncing.value = true
            } else {
                // First-time cold start: fetch top priority feeds fast (~6-8 feeds) so the user doesn't wait
                val fastStatus = repository.syncFastPriorityBatch()
                if (activeRequestId.get() == requestId && fastStatus == SyncStatus.SUCCESS) {
                    _isOffline.value = false
                    _errorMessage.value = null
                    // Establish install baseline silently so pre-existing RSS stories don't flood Notifications
                    if (!NewsNotificationHelper.isBaselineEstablished(getApplication())) {
                        val baselinePool = repository.getRecentAlertArticles(limit = 50)
                        NewsNotificationHelper.establishInstallBaselineIfNeeded(getApplication(), baselinePool)
                    }
                    reloadFromCacheInternal(requestId)
                    _isInitialLoading.value = false
                    _isBackgroundSyncing.value = true
                }
            }

            // Full live RSS sync
            val status = repository.syncFromRemote()

            if (activeRequestId.get() == requestId) {
                _isBackgroundSyncing.value = false
                handleSyncStatus(status)
                val wasBaselineAlreadySet = NewsNotificationHelper.isBaselineEstablished(getApplication())
                if (!wasBaselineAlreadySet && status == SyncStatus.SUCCESS) {
                    val baselinePool = repository.getRecentAlertArticles(limit = 60)
                    NewsNotificationHelper.establishInstallBaselineIfNeeded(getApplication(), baselinePool)
                }
                reloadFromCacheInternal(requestId)
                _isInitialLoading.value = false
                // Never fire a push alert on the initial baseline-establishing sync
                if (status == SyncStatus.SUCCESS && wasBaselineAlreadySet) {
                    val alertPool = _notificationArticles.value
                    if (alertPool.isNotEmpty()) {
                        val lang = _currentLanguage.value
                        viewModelScope.launch(Dispatchers.IO) {
                            NewsNotificationHelper.notifyLatestBreakingNewsIfNew(
                                context = getApplication(),
                                articles = alertPool,
                                language = lang
                            )
                        }
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

        val rawNotifications = repository.getRecentAlertArticles(limit = 30)
        val savedList = repository.getSavedArticles(_savedArticleIds.value)
        val videos = repository.getTrendingVideos(category = category, limit = 16)
        if (activeRequestId.get() != requestId) return

        nextBeforePublishedAt = pageResult.nextBeforePublishedAt
        nextAfterId = pageResult.nextAfterId
        _hasMore.value = pageResult.hasMore
        _featuredIslamicArticles.value = applyRuntimeTranslationFlags(featured, lang)
        _articles.value = applyRuntimeTranslationFlags(pageResult.articles, lang)
        _liveChannels.value = repository.getTrustedVideoChannels()
        _videoClips.value = applyCachedVideoTranslations(videos, lang)

        val dismissed = _dismissedNotificationIds.value
        val appCtx = getApplication<Application>()
        val baselineEstablished = NewsNotificationHelper.isBaselineEstablished(appCtx)
        val filteredAlerts = if (!baselineEstablished) {
            // Until initial baseline is established, keep notification inbox cleanly at 0
            emptyList()
        } else {
            val baselineMs = NewsNotificationHelper.getInstallBaselineMs(appCtx)
            val baselineIds = NewsNotificationHelper.getBaselineArticleIds(appCtx)
            val alertSourceList = rawNotifications.filter { article ->
                article.id !in dismissed &&
                    article.id !in baselineIds &&
                    article.publishedAt > baselineMs &&
                    article.category != NewsCategory.SPORTS &&
                    article.category != NewsCategory.BUSINESS
            }
            alertSourceList
        }
        _notificationArticles.value = applyRuntimeTranslationFlags(filteredAlerts, lang)
        _savedArticles.value = applyRuntimeTranslationFlags(savedList, lang)

        pendingNotificationArticleId?.let { targetId ->
            val matched = (_articles.value + _featuredIslamicArticles.value + _notificationArticles.value + _savedArticles.value)
                .firstOrNull { it.id == targetId }
                ?: repository.getArticleById(targetId)?.let {
                    applyRuntimeTranslationFlags(listOf(it), lang).first()
                }
            if (matched != null) {
                pendingNotificationArticleId = null
                openArticleDetail(matched)
            }
        }

        enqueueMissingTranslations(lang)
        enqueueMissingVideoTranslations(lang)
    }

    fun setLanguage(language: AppLanguage) {
        failedTranslations.clear()
        _currentLanguage.value = language
        prefs.edit().putString(KEY_SELECTED_LANGUAGE, language.code).apply()
        scheduleReload(debounceMs = 0L)
        _selectedArticleForDetail.value?.let { openArticle ->
            loadArticleBriefing(openArticle, forceRefresh = false)
        }
        if (_situationBriefState.value is SituationBriefUiState.Success) {
            generateSituationBrief()
        }
    }

    fun retryTranslations() {
        failedTranslations.clear()
        val lang = _currentLanguage.value
        _featuredIslamicArticles.update { applyRuntimeTranslationFlags(it, lang) }
        _articles.update { applyRuntimeTranslationFlags(it, lang) }
        _notificationArticles.update { applyRuntimeTranslationFlags(it, lang) }
        _savedArticles.update { applyRuntimeTranslationFlags(it, lang) }
        _selectedArticleForDetail.update { current ->
            current?.let { applyRuntimeTranslationFlags(listOf(it), lang).first() }
        }
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

            val rcDeferred = async(Dispatchers.IO) { RemoteConfigManager.refreshNow() }
            val status = repository.syncFromRemote()
            rcDeferred.await()

            if (activeRequestId.get() == requestId) {
                _isBackgroundSyncing.value = false
                handleSyncStatus(status)
                val wasBaselineAlreadySet = NewsNotificationHelper.isBaselineEstablished(getApplication())
                if (!wasBaselineAlreadySet && status == SyncStatus.SUCCESS) {
                    val baselinePool = repository.getRecentAlertArticles(limit = 60)
                    NewsNotificationHelper.establishInstallBaselineIfNeeded(getApplication(), baselinePool)
                }
                reloadFromCacheInternal(requestId)
                _isRefreshing.value = false
                _isInitialLoading.value = false
                if (status == SyncStatus.SUCCESS && wasBaselineAlreadySet) {
                    val alertPool = _notificationArticles.value
                    if (alertPool.isNotEmpty()) {
                        val lang = _currentLanguage.value
                        viewModelScope.launch(Dispatchers.IO) {
                            NewsNotificationHelper.notifyLatestBreakingNewsIfNew(
                                context = getApplication(),
                                articles = alertPool,
                                language = lang
                            )
                        }
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

        val candidates = (_featuredIslamicArticles.value + _articles.value + _notificationArticles.value + _savedArticles.value)
            .distinctBy { it.id }
            .filter { article ->
                val key = "${article.id}_${lang.code}"
                !article.hasTranslation(lang) && key !in failedTranslations && inFlightTranslations.add(key)
            }
            .take(28)

        for (article in candidates) {
            translateSingleArticleAsync(article, lang)
        }
    }

    private fun translateSingleArticleAsync(article: NewsArticle, lang: AppLanguage) {
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
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failedTranslations.add(key)
            } finally {
                inFlightTranslations.remove(key)
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
        _notificationArticles.update { list -> list.map(updater) }
        _savedArticles.update { list -> list.map(updater) }
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
        markNotificationRead(article.id)
        val lang = _currentLanguage.value
        val prepared = applyRuntimeTranslationFlags(listOf(article), lang).first()
        _selectedArticleForDetail.value = prepared
        if (lang != AppLanguage.ENGLISH && !prepared.hasTranslation(lang)) {
            val key = "${prepared.id}_${lang.code}"
            if (inFlightTranslations.add(key)) {
                translateSingleArticleAsync(prepared, lang)
            }
        }
        loadArticleBriefing(prepared, forceRefresh = false)
    }

    fun loadArticleBriefing(article: NewsArticle, forceRefresh: Boolean = false) {
        briefingJob?.cancel()
        val lang = _currentLanguage.value
        _activeBriefingState.value = AiBriefingUiState.Loading
        briefingJob = viewModelScope.launch {
            try {
                val result = repository.getOrGenerateArticleBriefing(article, lang, forceRefresh)
                if (_selectedArticleForDetail.value?.id == article.id) {
                    result.onSuccess { briefing ->
                        _activeBriefingState.value = AiBriefingUiState.Success(briefing)
                    }.onFailure { err ->
                        _activeBriefingState.value = AiBriefingUiState.Error(
                            err.message ?: "Unable to prepare intelligence briefing."
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (_selectedArticleForDetail.value?.id == article.id) {
                    _activeBriefingState.value = AiBriefingUiState.Error(
                        e.message ?: "Unable to prepare intelligence briefing."
                    )
                }
            }
        }
    }

    fun generateSituationBrief() {
        val pool = (_articles.value + _notificationArticles.value)
            .distinctBy { it.id }
            .filter { it.category != NewsCategory.SPORTS && it.category != NewsCategory.BUSINESS }
            .ifEmpty { _articles.value }
            .take(8)
        if (pool.isEmpty()) return

        situationJob?.cancel()
        val lang = _currentLanguage.value
        _situationBriefState.value = SituationBriefUiState.Loading
        situationJob = viewModelScope.launch {
            try {
                val result = repository.generateSituationBrief(pool, lang)
                result.onSuccess { brief ->
                    _situationBriefState.value = SituationBriefUiState.Success(brief)
                }.onFailure { err ->
                    _situationBriefState.value = SituationBriefUiState.Error(
                        err.message ?: "Situation brief could not be generated right now."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _situationBriefState.value = SituationBriefUiState.Error(
                    e.message ?: "Situation brief could not be generated right now."
                )
            }
        }
    }

    fun dismissSituationBrief() {
        situationJob?.cancel()
        _situationBriefState.value = SituationBriefUiState.Idle
    }

    fun closeArticleDetail() {
        briefingJob?.cancel()
        _activeBriefingState.value = AiBriefingUiState.Idle
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

    fun setNotificationFocus(focus: NotificationFocus) {
        _notificationFocus.value = focus
        NewsNotificationHelper.setNotificationFocus(getApplication(), focus)
    }

    fun openVideoClip(clip: NewsVideoClip) {
        _selectedVideoClip.value = clip
    }

    fun closeVideoClip() {
        _selectedVideoClip.value = null
    }

    fun syncRemoteConfigAndRefresh() {
        viewModelScope.launch {
            RemoteConfigManager.refreshNow()
            _liveChannels.value = repository.getTrustedVideoChannels()
            refreshNews()
        }
    }

    private fun applyCachedVideoTranslations(
        clips: List<NewsVideoClip>,
        lang: AppLanguage
    ): List<NewsVideoClip> {
        if (lang == AppLanguage.ENGLISH) {
            return clips.map { it.copy(translatedTitle = null) }
        }
        return clips.map { clip ->
            val cached = videoTitleCache["${clip.id}_${lang.code}"]
            clip.copy(translatedTitle = cached)
        }
    }

    private fun enqueueMissingVideoTranslations(lang: AppLanguage) {
        if (lang == AppLanguage.ENGLISH) return
        val pending = _videoClips.value
            .filter { clip ->
                val key = "${clip.id}_${lang.code}"
                clip.translatedTitle.isNullOrBlank() &&
                    !videoTitleCache.containsKey(key) &&
                    inFlightVideoTranslations.add(key)
            }
            .take(10)

        for (clip in pending) {
            val key = "${clip.id}_${lang.code}"
            viewModelScope.launch {
                try {
                    translationSemaphore.withPermit {
                        val translated = repository.translateVideoTitle(clip, lang)
                        if (!translated.isNullOrBlank()) {
                            videoTitleCache[key] = translated
                            _videoClips.update { currentList ->
                                currentList.map { item ->
                                    if (item.id == clip.id && _currentLanguage.value == lang) {
                                        item.copy(translatedTitle = translated)
                                    } else {
                                        item
                                    }
                                }
                            }
                            _selectedVideoClip.update { open ->
                                if (open?.id == clip.id && _currentLanguage.value == lang) {
                                    open.copy(translatedTitle = translated)
                                } else {
                                    open
                                }
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Keep original English title gracefully
                } finally {
                    inFlightVideoTranslations.remove(key)
                }
            }
        }
    }

    fun openArticleById(articleId: String) {
        if (articleId.isBlank()) return
        val matched = (_articles.value + _featuredIslamicArticles.value + _notificationArticles.value + _savedArticles.value)
            .firstOrNull { it.id == articleId }
        if (matched != null) {
            pendingNotificationArticleId = null
            openArticleDetail(matched)
        } else {
            pendingNotificationArticleId = articleId
            viewModelScope.launch {
                val fromDb = repository.getArticleById(articleId)
                if (fromDb != null && pendingNotificationArticleId == articleId) {
                    pendingNotificationArticleId = null
                    val prepared = applyRuntimeTranslationFlags(listOf(fromDb), _currentLanguage.value).first()
                    openArticleDetail(prepared)
                }
            }
        }
    }

    companion object {
        private const val KEY_SELECTED_CITY_ID = "selected_city_id"
        private const val KEY_HIJRI_ADJUSTMENT = "hijri_adjustment_days"
        private const val KEY_SELECTED_LANGUAGE = "selected_news_language"
        private const val KEY_RECENT_SEARCHES = "recent_search_queries"
        private const val KEY_READ_NOTIFICATION_IDS = "read_notification_article_ids"
        private const val KEY_DISMISSED_NOTIFICATION_IDS = "dismissed_notification_article_ids"
        private const val KEY_SAVED_ARTICLE_IDS = "saved_bookmarked_article_ids"
        private const val RECENT_SEARCH_DELIMITER = "|||"
        private const val MAX_RECENT_SEARCHES = 6
        private val DEFAULT_RECENT_SEARCHES = emptyList<String>()
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
