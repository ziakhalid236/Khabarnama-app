package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AppLanguage
import com.example.model.NewsCategory
import com.example.ui.components.ArticleDetailDialog
import com.example.ui.components.CategoryChipsBar
import com.example.ui.components.FacebookNewsCard
import com.example.ui.components.FeaturedIslamicCarousel
import com.example.ui.components.ImageSourceBottomSheet
import com.example.ui.components.IslamicPortalHeaderCard
import com.example.ui.components.KhabarnamaBottomNavBar
import com.example.ui.components.LanguageSelectDialog
import com.example.ui.components.NavTab
import com.example.ui.components.NewsCardSkeleton
import com.example.ui.components.NotificationsPage
import com.example.ui.components.PrayerTimesDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.SplashScreen
import com.example.ui.components.TrendingVideosStrip
import com.example.ui.components.UrduAppBar
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.theme.CrimsonBreaking
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldTextDark
import com.example.ui.theme.GoldTint
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.Inter
import com.example.ui.theme.MutedGold
import com.example.ui.theme.NotoNaskhArabic
import com.example.ui.theme.NotoNastaliqUrdu
import com.example.ui.theme.PlayfairDisplay
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmDivider
import com.example.ui.theme.WarmOffWhite
import com.example.util.PrayerTimesUtil
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrduNewsMainScreen(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier
) {
    var isShowingSplash by remember { mutableStateOf(true) }

    Crossfade(
        targetState = isShowingSplash,
        label = "splash_crossfade"
    ) { showSplash ->
        if (showSplash) {
            SplashScreen(
                onFinish = { isShowingSplash = false },
                modifier = modifier
            )
        } else {
            MainNewsContent(
                viewModel = viewModel,
                modifier = modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainNewsContent(
    viewModel: NewsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsStateWithLifecycle()
    val notificationFocus by viewModel.notificationFocus.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedIslamicSection by viewModel.selectedIslamicSection.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
    val recentSearches by viewModel.recentSearches.collectAsStateWithLifecycle()
    val featuredIslamicArticles by viewModel.featuredIslamicArticles.collectAsStateWithLifecycle()
    val articles by viewModel.articles.collectAsStateWithLifecycle()
    val notificationArticles by viewModel.notificationArticles.collectAsStateWithLifecycle()
    val readNotificationIds by viewModel.readNotificationIds.collectAsStateWithLifecycle()
    val savedArticleIds by viewModel.savedArticleIds.collectAsStateWithLifecycle()
    val savedArticles by viewModel.savedArticles.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val hijriAdjustment by viewModel.hijriAdjustment.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()
    val isBackgroundSyncing by viewModel.isBackgroundSyncing.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLoadingMore by viewModel.isLoadingMore.collectAsStateWithLifecycle()
    val hasMore by viewModel.hasMore.collectAsStateWithLifecycle()
    val isOffline by viewModel.isOffline.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val activeImageSourceArticle by viewModel.activeImageSourceArticle.collectAsStateWithLifecycle()
    val selectedArticleForDetail by viewModel.selectedArticleForDetail.collectAsStateWithLifecycle()
    val selectedVideoClip by viewModel.selectedVideoClip.collectAsStateWithLifecycle()
    val remoteConfig by viewModel.remoteConfig.collectAsStateWithLifecycle()
    val videoClips by viewModel.videoClips.collectAsStateWithLifecycle()
    val liveChannels by viewModel.liveChannels.collectAsStateWithLifecycle()
    val activeBriefingState by viewModel.activeBriefingState.collectAsStateWithLifecycle()
    val situationBriefState by viewModel.situationBriefState.collectAsStateWithLifecycle()
    val isShowingPrayerDialog by viewModel.isShowingPrayerDialog.collectAsStateWithLifecycle()
    val isShowingLanguageDialog by viewModel.isShowingLanguageDialog.collectAsStateWithLifecycle()
    var isShowingSettingsDialog by remember { mutableStateOf(false) }
    var isNotificationsPageOpen by remember { mutableStateOf(false) }

    val unreadNotificationCount by remember(notificationArticles, readNotificationIds) {
        derivedStateOf {
            notificationArticles
                .distinctBy { it.id }
                .count { it.id !in readNotificationIds }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            // Keep app functional if user declines runtime permission
        }
    }

    LaunchedEffect(notificationsEnabled) {
        if (notificationsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val hijriDate = remember(selectedCity, hijriAdjustment) {
        PrayerTimesUtil.getTodayHijriDate(
            lang = AppLanguage.ENGLISH,
            adjustmentDays = hijriAdjustment,
            zoneId = selectedCity.zoneId
        )
    }
    val listState = rememberLazyListState()

    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    var headerOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (headerHeightPx > 0f) {
                    val newOffset = (headerOffsetPx + delta).coerceIn(-headerHeightPx, 0f)
                    headerOffsetPx = newOffset
                }
                return Offset.Zero // Let the LazyColumn scroll naturally!
            }
        }
    }

    // Scroll back to the top and restore header whenever category, section, search, or language changes
    LaunchedEffect(selectedCategory, selectedIslamicSection, searchQuery, currentLanguage) {
        listState.scrollToItem(0)
        headerOffsetPx = 0f
    }

    // Restore header whenever user reaches the very top of the list
    // Uses derivedStateOf so that rapid scrolling does NOT trigger LaunchedEffect on every pixel offset change!
    val isScrolledToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }
    }

    LaunchedEffect(isScrolledToTop) {
        if (isScrolledToTop) {
            headerOffsetPx = 0f
        }
    }

    // The app shell ALWAYS remains LTR English
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        // Infinite scroll detection using derivedStateOf
        val shouldLoadMore by remember {
            derivedStateOf {
                val totalItems = listState.layoutInfo.totalItemsCount
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                totalItems > 0 && lastVisibleItem >= totalItems - 2
            }
        }

        LaunchedEffect(shouldLoadMore, hasMore, isLoadingMore) {
            if (shouldLoadMore && hasMore && !isLoadingMore) {
                viewModel.loadMore()
            }
        }

        val density = LocalDensity.current
        val topContentPaddingDp by remember(density) {
            derivedStateOf {
                val hDp = with(density) { headerHeightPx.toDp() }
                if (hDp > 0.dp) hDp + 8.dp else 120.dp
            }
        }
        val topDialogPaddingDp by remember(density) {
            derivedStateOf {
                val hDp = with(density) { headerHeightPx.toDp() }
                if (hDp > 0.dp) hDp + 16.dp else 120.dp
            }
        }

        // Header shadow elevation derived state: only triggers recomposition when switching between 0.dp and 3.dp!
        val headerShadowElevation by remember {
            derivedStateOf {
                if (headerOffsetPx < 0f) 3.dp else 0.dp
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection)
        ) {
            Scaffold(
                bottomBar = {
                    KhabarnamaBottomNavBar(
                        selectedCategory = selectedCategory,
                        isNotificationsOpen = isNotificationsPageOpen,
                        isSettingsOpen = isShowingSettingsDialog,
                        unreadNotificationCount = unreadNotificationCount,
                        onTabSelected = { tab ->
                            when (tab) {
                                NavTab.HOME -> {
                                    isShowingSettingsDialog = false
                                    isNotificationsPageOpen = false
                                    viewModel.closeArticleDetail()
                                    viewModel.selectCategory(NewsCategory.ALL)
                                    viewModel.toggleSearchOpen(false)
                                }
                                NavTab.ISLAMIC -> {
                                    isShowingSettingsDialog = false
                                    isNotificationsPageOpen = false
                                    viewModel.closeArticleDetail()
                                    viewModel.selectCategory(NewsCategory.ISLAMIC)
                                    viewModel.toggleSearchOpen(false)
                                }
                                NavTab.NOTIFICATIONS -> {
                                    isShowingSettingsDialog = false
                                    viewModel.closeArticleDetail()
                                    viewModel.toggleSearchOpen(false)
                                    isNotificationsPageOpen = true
                                }
                                NavTab.SETTINGS -> {
                                    viewModel.closeArticleDetail()
                                    isShowingSettingsDialog = true
                                }
                            }
                        }
                    )
                },
                containerColor = WarmOffWhite,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                if (isNotificationsPageOpen) {
                    NotificationsPage(
                        notificationArticles = notificationArticles,
                        savedArticles = savedArticles,
                        savedArticleIds = savedArticleIds,
                        currentLanguage = currentLanguage,
                        notificationsEnabled = notificationsEnabled,
                        notificationFocus = notificationFocus,
                        readNotificationIds = readNotificationIds,
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshNews() },
                        onMarkAllRead = { viewModel.markAllNotificationsRead() },
                        onClearAllNotifications = { viewModel.clearAllNotifications() },
                        onDismissNotification = { viewModel.dismissNotification(it) },
                        onToggleBookmark = { viewModel.toggleBookmarkArticle(it) },
                        onNotificationArticleClick = { clickedArticle ->
                            viewModel.markNotificationRead(clickedArticle.id)
                            viewModel.openArticleDetail(clickedArticle)
                        },
                        onToggleNotifications = { enabled ->
                            viewModel.setNotificationsEnabled(enabled)
                            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (!hasPermission) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        },
                        onSelectNotificationFocus = { viewModel.setNotificationFocus(it) },
                        onBackToHome = { isNotificationsPageOpen = false },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    )
                } else {
                    val pullRefreshState = rememberPullToRefreshState()

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshNews() },
                        state = pullRefreshState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = innerPadding.calculateBottomPadding())
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                        // Offline Banner when showing cached stories without active network
                        if (isOffline && (articles.isNotEmpty() || featuredIslamicArticles.isNotEmpty())) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        top = topContentPaddingDp,
                                        start = 16.dp,
                                        end = 16.dp,
                                        bottom = 8.dp
                                    )
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GoldTint)
                                    .border(1.dp, GoldBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("offline_banner")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudOff,
                                        contentDescription = "Offline mode active",
                                        tint = MutedGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Offline • Showing cached stories",
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = InkPrimary
                                    )
                                }
                            }
                        }

                        when {
                            // 1. Loading Skeleton State
                            isInitialLoading && articles.isEmpty() -> {
                                LazyColumn(
                                    contentPadding = PaddingValues(
                                        top = topContentPaddingDp,
                                        bottom = 16.dp,
                                        start = 16.dp,
                                        end = 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("loading_skeleton_list")
                                ) {
                                    items(3) {
                                        NewsCardSkeleton()
                                    }
                                }
                            }

                            // 2. Friendly First-Launch Offline / Error State (when cache is empty and network is unavailable)
                            errorMessage != null && articles.isEmpty() && featuredIslamicArticles.isEmpty() -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top = topDialogPaddingDp,
                                            bottom = 24.dp,
                                            start = 24.dp,
                                            end = 24.dp
                                        )
                                        .testTag("error_state_box"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        color = PureWhite,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(28.dp))
                                                    .background(GoldTint)
                                                    .border(1.dp, GoldBorder, RoundedCornerShape(28.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.CloudOff,
                                                    contentDescription = "No internet connection",
                                                    tint = MutedGold,
                                                    modifier = Modifier.size(26.dp)
                                                )
                                            }

                                            Text(
                                                text = "You're currently offline",
                                                fontFamily = PlayfairDisplay,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp,
                                                color = InkPrimary,
                                                textAlign = TextAlign.Center
                                            )

                                            Text(
                                                text = errorMessage ?: "Connect to the internet to load your first edition of Khabarnama. Once stories are loaded, they are saved automatically for offline reading.",
                                                fontFamily = Inter,
                                                fontSize = 15.sp,
                                                lineHeight = 22.sp,
                                                color = SoftGrey,
                                                textAlign = TextAlign.Center
                                            )

                                            Button(
                                                onClick = { viewModel.refreshNews() },
                                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .heightIn(min = 48.dp)
                                                    .testTag("retry_button")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Refresh,
                                                    contentDescription = "Retry loading news",
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Retry",
                                                    fontFamily = Inter,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // 3. Empty State
                            articles.isEmpty() && (selectedCategory != NewsCategory.ALL && selectedCategory != NewsCategory.ISLAMIC || featuredIslamicArticles.isEmpty() || searchQuery.isNotBlank()) -> {
                                val noResultsTitle = if (searchQuery.isNotBlank()) {
                                    "No matching dispatches"
                                } else {
                                    "No dispatches in this section"
                                }

                                val noResultsSubtitle = if (searchQuery.isNotBlank()) {
                                    "No stories matched \"$searchQuery\". Try broader keywords or choose one of the suggested topics above."
                                } else {
                                    "Pull down to refresh the feed or select another editorial section above."
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(
                                            top = topDialogPaddingDp,
                                            bottom = 24.dp,
                                            start = 24.dp,
                                            end = 24.dp
                                        )
                                        .testTag("empty_state_box"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        color = PureWhite,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp),
                                            modifier = Modifier.padding(24.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(36.dp)
                                                    .height(3.dp)
                                                    .background(MutedGold)
                                            )

                                            Text(
                                                text = noResultsTitle,
                                                fontFamily = PlayfairDisplay,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp,
                                                color = InkPrimary,
                                                textAlign = TextAlign.Center
                                            )

                                            Text(
                                                text = noResultsSubtitle,
                                                fontFamily = Inter,
                                                fontSize = 15.sp,
                                                lineHeight = 22.sp,
                                                color = SoftGrey,
                                                textAlign = TextAlign.Center
                                            )

                                            if (searchQuery.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = { viewModel.setSearchQuery("") },
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, DeepEmerald),
                                                    modifier = Modifier
                                                        .heightIn(min = 48.dp)
                                                        .testTag("clear_empty_search_button")
                                                ) {
                                                    Text(
                                                        text = "Clear search",
                                                        fontFamily = Inter,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 14.sp,
                                                        color = DeepEmerald
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 4. Populated Feed (Live or Cached)
                            else -> {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(
                                        top = topContentPaddingDp,
                                        bottom = 16.dp,
                                        start = 16.dp,
                                        end = 16.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("news_feed_list")
                                ) {
                                    // Subtle background sync status pill when live dispatches are updating quietly
                                    if (isBackgroundSyncing && !isRefreshing) {
                                        item(key = "live_sync_status_pill") {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(EmeraldTint.copy(alpha = 0.7f))
                                                    .border(1.dp, DeepEmerald.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(13.dp),
                                                    strokeWidth = 2.dp,
                                                    color = DeepEmerald
                                                )
                                                Text(
                                                    text = "Syncing latest war, security & global dispatches...",
                                                    fontFamily = Inter,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 11.5.sp,
                                                    color = DeepEmerald
                                                )
                                            }
                                        }
                                    }

                                    // Remote Config Over-the-Air Live Editorial Bulletin Banner
                                    if (!isSearchOpen && remoteConfig.liveBulletin.enabled) {
                                        item(key = "ota_live_bulletin_banner") {
                                            LiveRemoteBulletinBanner(
                                                badge = remoteConfig.liveBulletin.badge,
                                                title = remoteConfig.liveBulletin.title,
                                                message = remoteConfig.liveBulletin.message,
                                                sourceUrl = remoteConfig.liveBulletin.sourceUrl
                                            )
                                        }
                                    }

                                    // Live Intelligence Situation Brief Banner (on All feed when not searching & enabled via Remote Config)
                                    if (!isSearchOpen && searchQuery.isBlank() && selectedCategory == NewsCategory.ALL && articles.isNotEmpty() && remoteConfig.situationBriefEnabled) {
                                        item(key = "live_situation_brief_card") {
                                            LiveSituationBriefBanner(
                                                state = situationBriefState,
                                                currentLanguage = currentLanguage,
                                                onGenerate = { viewModel.generateSituationBrief() },
                                                onDismiss = { viewModel.dismissSituationBrief() }
                                            )
                                        }
                                    }

                                    // Trending News Video Clips & Official Live Broadcast Channels
                                    if (!isSearchOpen && searchQuery.isBlank() && remoteConfig.videoSectionEnabled && (videoClips.isNotEmpty() || liveChannels.isNotEmpty())) {
                                        item(key = "trending_videos_strip") {
                                            val isVideoStripVisible by remember {
                                                derivedStateOf {
                                                    selectedArticleForDetail == null &&
                                                        selectedVideoClip == null &&
                                                        !isShowingSettingsDialog &&
                                                        listState.layoutInfo.visibleItemsInfo.any {
                                                            it.key == "trending_videos_strip"
                                                        }
                                                }
                                            }
                                            TrendingVideosStrip(
                                                videos = videoClips,
                                                liveChannels = liveChannels,
                                                currentLanguage = currentLanguage,
                                                isSectionVisibleOnScreen = isVideoStripVisible,
                                                onVideoClick = { viewModel.openVideoClip(it) }
                                            )
                                        }
                                    }

                                    // Dedicated Islamic Portal Banner (distinguishes the Islamic page completely from Home)
                                    if (!isSearchOpen && searchQuery.isBlank() && selectedCategory == NewsCategory.ISLAMIC) {
                                        item(key = "islamic_portal_header_banner") {
                                            IslamicPortalHeaderCard(
                                                hijriDate = hijriDate,
                                                currentLanguage = currentLanguage,
                                                onOpenPrayerDialog = { viewModel.showPrayerDialog(true) }
                                            )
                                        }
                                    }

                                    // Featured Islamic Highlights Carousel (only in Islamic section when not searching)
                                    val showFeaturedIslamic = !isSearchOpen &&
                                        searchQuery.isBlank() &&
                                        selectedCategory == NewsCategory.ISLAMIC &&
                                        featuredIslamicArticles.isNotEmpty()

                                    if (showFeaturedIslamic) {
                                        item(key = "featured_islamic_carousel") {
                                            FeaturedIslamicCarousel(
                                                articles = featuredIslamicArticles,
                                                currentLanguage = currentLanguage,
                                                onArticleClick = { viewModel.openArticleDetail(it) },
                                                onSourceBadgeClick = { viewModel.openImageSourceSheet(it) },
                                                onRetryTranslation = { viewModel.retryTranslations() }
                                            )
                                        }
                                    }

                                    // Vertical News Feed Cards
                                    items(articles, key = { it.id }) { article ->
                                        FacebookNewsCard(
                                            article = article,
                                            currentLanguage = currentLanguage,
                                            isBookmarked = article.id in savedArticleIds,
                                            onToggleBookmark = { viewModel.toggleBookmarkArticle(it) },
                                            onArticleClick = { viewModel.openArticleDetail(it) },
                                            onSourceBadgeClick = { viewModel.openImageSourceSheet(it) },
                                            onOpenLanguageDialog = { viewModel.showLanguageDialog(true) },
                                            onRetryTranslation = { viewModel.retryTranslations() }
                                        )
                                    }

                                    // Footer: Show "Loading more stories..." while loading more, otherwise "You're all caught up"
                                    item(key = "load_more_footer") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isLoadingMore) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(16.dp),
                                                        strokeWidth = 2.dp,
                                                        color = DeepEmerald
                                                    )
                                                    Text(
                                                        text = "Loading more stories...",
                                                        fontFamily = Inter,
                                                        fontSize = 12.sp,
                                                        color = SoftGrey
                                                    )
                                                }
                                            } else if (!hasMore) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.CheckCircleOutline,
                                                        contentDescription = "End of feed reached",
                                                        tint = DeepEmerald,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "You're all caught up",
                                                        fontFamily = Inter,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = 12.sp,
                                                        color = SoftGrey
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }

            // Collapsible Top Header: smoothly slides up on scroll down, glides back down on scroll up (hidden when Notifications page is open)
            if (!isNotificationsPageOpen) {
                Surface(
                    color = WarmOffWhite,
                    shadowElevation = headerShadowElevation,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .onGloballyPositioned { coordinates ->
                            val h = coordinates.size.height.toFloat()
                            if (h > 0f && headerHeightPx != h) {
                                headerHeightPx = h
                            }
                        }
                        .offset {
                            IntOffset(x = 0, y = headerOffsetPx.roundToInt())
                        }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        UrduAppBar(
                            currentLanguage = currentLanguage,
                            hijriDate = hijriDate,
                            selectedCity = selectedCity,
                            isSearchOpen = isSearchOpen,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { viewModel.setSearchQuery(it) },
                            onToggleSearch = { viewModel.toggleSearchOpen(it) },
                            onOpenPrayerDialog = { viewModel.showPrayerDialog(true) },
                            onOpenLanguageDialog = { viewModel.showLanguageDialog(true) },
                            onSearchSubmitted = { viewModel.recordRecentSearch(it) }
                        )

                        // Hide category chips while searching
                        AnimatedVisibility(
                            visible = !isSearchOpen,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
                        ) {
                            CategoryChipsBar(
                                selectedCategory = selectedCategory,
                                selectedIslamicSection = selectedIslamicSection,
                                onCategorySelected = { viewModel.selectCategory(it) },
                                onIslamicSectionSelected = { viewModel.selectIslamicSection(it) }
                            )
                        }

                        // Search Discovery & Recent Searches Panel while searching
                        AnimatedVisibility(
                            visible = isSearchOpen,
                            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
                            exit = fadeOut(tween(180)) + shrinkVertically(tween(180))
                        ) {
                            SearchDiscoveryHeader(
                                searchQuery = searchQuery,
                                resultCount = articles.size,
                                recentSearches = recentSearches,
                                onSelectTopicOrSearch = { viewModel.selectRecentSearch(it) },
                                onRemoveRecentSearch = { viewModel.removeRecentSearch(it) },
                                onClearRecentSearches = { viewModel.clearRecentSearches() },
                                onClearActiveQuery = { viewModel.setSearchQuery("") }
                            )
                        }
                    }
                }
            }

            // Full-Screen Article Detail
            selectedArticleForDetail?.let { article ->
                ArticleDetailDialog(
                    article = article,
                    currentLanguage = currentLanguage,
                    briefingState = activeBriefingState,
                    isBookmarked = article.id in savedArticleIds,
                    onToggleBookmark = { viewModel.toggleBookmarkArticle(article) },
                    onSelectLanguage = { viewModel.setLanguage(it) },
                    onRegenerateBriefing = { viewModel.loadArticleBriefing(article, forceRefresh = true) },
                    onSourceBadgeClick = { viewModel.openImageSourceSheet(it) },
                    onDismiss = { viewModel.closeArticleDetail() },
                    onRetryTranslation = { viewModel.retryTranslations() }
                )
            }

            // Full-Screen Settings
            if (isShowingSettingsDialog) {
                SettingsDialog(
                    currentLanguage = currentLanguage,
                    selectedCity = selectedCity,
                    hijriAdjustment = hijriAdjustment,
                    notificationsEnabled = notificationsEnabled,
                    remoteConfig = remoteConfig,
                    videoChannelsCount = liveChannels.size,
                    onSyncRemoteConfig = { viewModel.syncRemoteConfigAndRefresh() },
                    onToggleNotifications = { enabled ->
                        viewModel.setNotificationsEnabled(enabled)
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasPermission) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                    onHijriAdjustmentSelected = { viewModel.setHijriAdjustment(it) },
                    onOpenLanguageSelect = { viewModel.showLanguageDialog(true) },
                    onOpenPrayerCitySelect = { viewModel.showPrayerDialog(true) },
                    onRefreshNews = { viewModel.refreshNews() },
                    isRefreshing = isRefreshing,
                    onDismiss = { isShowingSettingsDialog = false }
                )
            }

            // Embedded Official Source Video Player Dialog
            selectedVideoClip?.let { clip ->
                VideoPlayerDialog(
                    clip = clip,
                    currentLanguage = currentLanguage,
                    onDismiss = { viewModel.closeVideoClip() }
                )
            }
        }

        // Image Source Bottom Sheet
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        activeImageSourceArticle?.let { article ->
            ImageSourceBottomSheet(
                article = article,
                sheetState = bottomSheetState,
                onDismiss = { viewModel.closeImageSourceSheet() }
            )
        }

        // Prayer Times Dialog
        if (isShowingPrayerDialog) {
            PrayerTimesDialog(
                selectedCity = selectedCity,
                hijriDate = hijriDate,
                onCitySelected = { viewModel.selectCity(it) },
                onDismiss = { viewModel.showPrayerDialog(false) }
            )
        }

        // Language Select Dialog
        if (isShowingLanguageDialog) {
            LanguageSelectDialog(
                currentLanguage = currentLanguage,
                onSelectLanguage = {
                    viewModel.setLanguage(it)
                    viewModel.showLanguageDialog(false)
                },
                onDismiss = { viewModel.showLanguageDialog(false) }
            )
        }
    }
}

private data class SuggestedSearchTopic(
    val icon: String,
    val label: String,
    val query: String
)

private val SUGGESTED_SEARCH_TOPICS = listOf(
    SuggestedSearchTopic("🛡️", "Security & War", "Security"),
    SuggestedSearchTopic("🇵🇸", "Palestine & Gaza", "Gaza"),
    SuggestedSearchTopic("🕌", "Islamic Scholars", "Scholars"),
    SuggestedSearchTopic("🇵🇰", "Pakistan", "Pakistan"),
    SuggestedSearchTopic("🌍", "Middle East", "Middle East"),
    SuggestedSearchTopic("🌙", "Makkah & Haramain", "Makkah"),
    SuggestedSearchTopic("🛰️", "Defense & OSINT", "Defense"),
    SuggestedSearchTopic("💻", "Technology", "Technology")
)

@Composable
private fun SearchDiscoveryHeader(
    searchQuery: String,
    resultCount: Int,
    recentSearches: List<String>,
    onSelectTopicOrSearch: (String) -> Unit,
    onRemoveRecentSearch: (String) -> Unit,
    onClearRecentSearches: () -> Unit,
    onClearActiveQuery: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = WarmOffWhite,
        modifier = modifier
            .fillMaxWidth()
            .testTag("recent_searches_bar")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Trending / Suggested Topics Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 6.dp)
            ) {
                Text(
                    text = "Trending topics",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = DeepEmerald,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val topicScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(topicScrollState)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SUGGESTED_SEARCH_TOPICS.forEach { topic ->
                        val isActive = searchQuery.equals(topic.query, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .heightIn(min = 34.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(if (isActive) DeepEmerald else GoldTint)
                                .border(
                                    1.dp,
                                    if (isActive) DeepEmerald else GoldBorder,
                                    RoundedCornerShape(17.dp)
                                )
                                .clickable { onSelectTopicOrSearch(topic.query) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = topic.icon,
                                fontSize = 12.sp
                            )
                            Text(
                                text = topic.label,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = if (isActive) Color.White else GoldTextDark
                            )
                        }
                    }
                }
            }

            // Recent Searches Section
            if (recentSearches.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent searches",
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = SoftGrey
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onClearRecentSearches() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("clear_recent_searches_button")
                    ) {
                        Text(
                            text = "Clear all",
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = DeepEmerald
                        )
                    }
                }

                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    recentSearches.forEach { query ->
                        Row(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .defaultMinSize(minHeight = 36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(PureWhite)
                                .border(1.dp, WarmBorder, RoundedCornerShape(18.dp))
                                .clickable { onSelectTopicOrSearch(query) }
                                .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                                .testTag("recent_search_chip_$query"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Recent search $query",
                                tint = SoftGrey,
                                modifier = Modifier.size(14.dp)
                            )

                            Text(
                                text = query,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = InkPrimary
                            )

                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onRemoveRecentSearch(query) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove $query from recent searches",
                                    tint = SoftGrey,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Active Search Filter Status Pill
            if (searchQuery.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldTint)
                        .border(1.dp, DeepEmerald.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = DeepEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Filtering by \"$searchQuery\" ($resultCount stories)",
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = DeepEmerald
                        )
                    }

                    Text(
                        text = "Reset",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = DeepEmerald,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onClearActiveQuery() }
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = WarmDivider)
        }
    }
}

@Composable
private fun LiveSituationBriefBanner(
    state: SituationBriefUiState,
    currentLanguage: AppLanguage,
    onGenerate: () -> Unit,
    onDismiss: () -> Unit
) {
    val isUrdu = currentLanguage == AppLanguage.URDU
    val isArabic = currentLanguage == AppLanguage.ARABIC
    val isRtl = isUrdu || isArabic

    val bodyFont = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> Inter
    }

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, DeepEmerald, RoundedCornerShape(16.dp))
            .testTag("live_situation_brief_banner")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Crisp Monochrome Obsidian Masthead Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepEmerald)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(PureWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Live Situation Desk",
                            tint = DeepEmerald,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIVE SITUATION DESK",
                            fontFamily = Inter,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.8.sp,
                            color = PureWhite,
                            maxLines = 1
                        )
                        Text(
                            text = "Security, conflicts & scholars • ${currentLanguage.displayName}",
                            fontFamily = Inter,
                            fontSize = 11.sp,
                            color = Color(0xFFD4D4D8),
                            maxLines = 1
                        )
                    }
                }

                when (state) {
                    SituationBriefUiState.Idle -> {
                        Button(
                            onClick = onGenerate,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PureWhite,
                                contentColor = DeepEmerald
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .testTag("generate_situation_brief_button")
                        ) {
                            Text(
                                text = "Brief Me",
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    SituationBriefUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = PureWhite
                        )
                    }

                    is SituationBriefUiState.Success, is SituationBriefUiState.Error -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onGenerate,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh situation brief",
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close situation brief",
                                    tint = Color(0xFFD4D4D8),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (state != SituationBriefUiState.Idle) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (state) {
                        SituationBriefUiState.Idle -> Unit
                        SituationBriefUiState.Loading -> {
                            Text(
                                text = "Synthesizing top security, geopolitical & scholarly dispatches...",
                                fontFamily = Inter,
                                fontSize = 12.5.sp,
                                color = SoftGrey
                            )
                        }

                        is SituationBriefUiState.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WarmOffWhite)
                                    .border(1.dp, WarmBorder, RoundedCornerShape(10.dp))
                                    .padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = InkPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = state.message,
                                    fontFamily = Inter,
                                    fontSize = 12.sp,
                                    color = SoftGrey,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        is SituationBriefUiState.Success -> {
                            val brief = state.brief
                            CompositionLocalProvider(
                                LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(9.dp)
                                ) {
                                    if (brief.headline.isNotBlank()) {
                                        Text(
                                            text = brief.headline,
                                            style = TextStyle(
                                                fontFamily = if (isRtl) bodyFont else PlayfairDisplay,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = if (isUrdu) 17.5.sp else if (isArabic) 16.sp else 16.sp,
                                                lineHeight = if (isUrdu) 34.sp else if (isArabic) 27.sp else 22.sp,
                                                color = InkPrimary,
                                                textAlign = TextAlign.Start,
                                                platformStyle = PlatformTextStyle(includeFontPadding = true)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    brief.bullets.forEach { bullet ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .padding(top = if (isUrdu) 12.dp else 7.dp)
                                                    .size(5.dp)
                                                    .background(InkPrimary)
                                            )
                                            Text(
                                                text = bullet,
                                                style = TextStyle(
                                                    fontFamily = bodyFont,
                                                    fontSize = if (isUrdu) 15.5.sp else if (isArabic) 14.5.sp else 13.sp,
                                                    lineHeight = if (isUrdu) 32.sp else if (isArabic) 26.sp else 19.sp,
                                                    color = InkPrimary,
                                                    textAlign = TextAlign.Start,
                                                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                    if (brief.securityFocusNote.isNotBlank()) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(WarmOffWhite)
                                                .border(1.dp, WarmBorder, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(28.dp)
                                                    .background(InkPrimary)
                                            )
                                            Text(
                                                text = brief.securityFocusNote,
                                                style = TextStyle(
                                                    fontFamily = bodyFont,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = if (isUrdu) 14.5.sp else if (isArabic) 13.5.sp else 12.sp,
                                                    lineHeight = if (isUrdu) 29.sp else if (isArabic) 24.sp else 17.sp,
                                                    color = InkPrimary,
                                                    textAlign = TextAlign.Start,
                                                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                                                ),
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveRemoteBulletinBanner(
    badge: String,
    title: String,
    message: String,
    sourceUrl: String
) {
    val context = LocalContext.current
    val hasLink = sourceUrl.startsWith("https://", ignoreCase = true)

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.2.dp, CrimsonBreaking.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            .then(
                if (hasLink) {
                    Modifier.clickable { com.example.util.BrowserUtil.openCustomTab(context, sourceUrl) }
                } else {
                    Modifier
                }
            )
            .testTag("ota_live_bulletin_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CrimsonBreaking.copy(alpha = 0.06f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CrimsonBreaking)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = badge,
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.6.sp,
                    color = Color.White
                )
            }

            Text(
                text = title,
                fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = InkPrimary
            )

            if (message.isNotBlank()) {
                Text(
                    text = message,
                    fontFamily = Inter,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = SoftGrey
                )
            }
        }
    }
}


