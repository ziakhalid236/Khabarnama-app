package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.AppLanguage
import com.example.model.NewsArticle
import com.example.model.NewsCategory
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
import com.example.util.BrowserUtil
import com.example.util.NotificationFocus
import com.example.util.PrayerTimesUtil
import kotlinx.coroutines.Dispatchers

private enum class InboxTab(val label: String) {
    ALL("All Alerts"),
    UNREAD("Unread"),
    SAVED("Saved")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsPage(
    notificationArticles: List<NewsArticle>,
    savedArticles: List<NewsArticle> = emptyList(),
    savedArticleIds: Set<String> = emptySet(),
    currentLanguage: AppLanguage,
    notificationsEnabled: Boolean,
    notificationFocus: NotificationFocus = NotificationFocus.ALL_PRIORITY,
    readNotificationIds: Set<String>,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onMarkAllRead: () -> Unit,
    onClearAllNotifications: () -> Unit = {},
    onDismissNotification: (String) -> Unit = {},
    onToggleBookmark: (NewsArticle) -> Unit = {},
    onNotificationArticleClick: (NewsArticle) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onSelectNotificationFocus: (NotificationFocus) -> Unit = {},
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    var selectedInboxTab by remember { mutableStateOf(InboxTab.ALL) }
    var selectedFilterCategory by remember { mutableStateOf(NewsCategory.ALL) }

    val sortedAlerts = remember(notificationArticles) {
        notificationArticles
            .distinctBy { it.id }
            .sortedByDescending { it.publishedAt }
    }

    val unreadCount = remember(sortedAlerts, readNotificationIds) {
        sortedAlerts.count { it.id !in readNotificationIds }
    }

    val baseListForTab = remember(sortedAlerts, savedArticles, selectedInboxTab, readNotificationIds) {
        when (selectedInboxTab) {
            InboxTab.ALL -> sortedAlerts
            InboxTab.UNREAD -> sortedAlerts.filter { it.id !in readNotificationIds }
            InboxTab.SAVED -> savedArticles.distinctBy { it.id }.sortedByDescending { it.publishedAt }
        }
    }

    val filteredNotifications = remember(baseListForTab, selectedFilterCategory) {
        if (selectedFilterCategory == NewsCategory.ALL) {
            baseListForTab
        } else {
            baseListForTab.filter { it.category == selectedFilterCategory }
        }
    }

    // Group notifications into time buckets for a genuine user Notification Center feel
    val nowMs = remember(filteredNotifications) { System.currentTimeMillis() }
    val sixHoursAgo = nowMs - 6L * 3600L * 1000L
    val twentyFourHoursAgo = nowMs - 24L * 3600L * 1000L

    val breakingRecentGroup = remember(filteredNotifications, sixHoursAgo) {
        filteredNotifications.filter { it.publishedAt >= sixHoursAgo }
    }
    val earlierTodayGroup = remember(filteredNotifications, sixHoursAgo, twentyFourHoursAgo) {
        filteredNotifications.filter { it.publishedAt in twentyFourHoursAgo until sixHoursAgo }
    }
    val olderGroup = remember(filteredNotifications, twentyFourHoursAgo) {
        filteredNotifications.filter { it.publishedAt < twentyFourHoursAgo }
    }

    val pullRefreshState = rememberPullToRefreshState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .testTag("notifications_page")
        ) {
            // 1. Top Header Bar for Notifications Center
            Surface(
                color = WarmOffWhite,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldTint)
                                    .border(1.dp, DeepEmerald.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "Notifications Center",
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Notifications",
                                        fontFamily = PlayfairDisplay,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp,
                                        color = InkPrimary
                                    )
                                    if (unreadCount > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(DeepEmerald)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "$unreadCount new",
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "Live breaking dispatches & saved stories",
                                    fontFamily = Inter,
                                    fontSize = 12.sp,
                                    color = SoftGrey
                                )
                            }
                        }

                        // Header Actions: Mark Read & Clear All
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (unreadCount > 0 && selectedInboxTab != InboxTab.SAVED) {
                                Row(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PureWhite)
                                        .border(1.dp, WarmBorder, RoundedCornerShape(12.dp))
                                        .clickable { onMarkAllRead() }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("mark_all_notifications_read"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DoneAll,
                                        contentDescription = "Mark all read",
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Read all",
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = DeepEmerald
                                    )
                                }
                            }

                            if (sortedAlerts.isNotEmpty() && selectedInboxTab != InboxTab.SAVED) {
                                Row(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PureWhite)
                                        .border(1.dp, WarmBorder, RoundedCornerShape(12.dp))
                                        .clickable { onClearAllNotifications() }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                        .testTag("clear_all_notifications_button"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = "Clear all notifications",
                                        tint = SoftGrey,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Clear",
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = SoftGrey
                                    )
                                }
                            }
                        }
                    }

                    // Inbox Mode Tabs: All Alerts | Unread | Saved
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InboxTab.entries.forEach { tab ->
                            val isSelected = selectedInboxTab == tab
                            val badgeCount = when (tab) {
                                InboxTab.ALL -> sortedAlerts.size
                                InboxTab.UNREAD -> unreadCount
                                InboxTab.SAVED -> savedArticles.size
                            }
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) DeepEmerald else PureWhite)
                                    .border(
                                        1.dp,
                                        if (isSelected) DeepEmerald else WarmBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedInboxTab = tab }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("inbox_tab_${tab.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (tab == InboxTab.SAVED) {
                                    Icon(
                                        imageVector = Icons.Filled.Bookmark,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MutedGold,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = tab.label,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else InkPrimary
                                )
                                if (badgeCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) Color.White.copy(alpha = 0.22f)
                                                else EmeraldTint
                                            )
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = badgeCount.toString(),
                                            fontFamily = Inter,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White else DeepEmerald
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Category Filter Row with Realistic Flags & White-and-Black Islamic Flag
                    val filterScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(filterScrollState)
                            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NewsCategory.entries.forEach { category ->
                            val isSelected = category == selectedFilterCategory
                            val isIslamic = category == NewsCategory.ISLAMIC

                            val chipBg by animateColorAsState(
                                targetValue = when {
                                    isIslamic && isSelected -> Color(0xFF111111)
                                    isIslamic -> Color.White
                                    isSelected -> DeepEmerald
                                    else -> PureWhite
                                },
                                animationSpec = tween(180),
                                label = "notif_filter_bg_${category.name}"
                            )
                            val chipBorder by animateColorAsState(
                                targetValue = when {
                                    isIslamic -> Color.Black
                                    isSelected -> MutedGold
                                    else -> WarmBorder
                                },
                                animationSpec = tween(180),
                                label = "notif_filter_border_${category.name}"
                            )
                            val chipText by animateColorAsState(
                                targetValue = when {
                                    isIslamic && isSelected -> Color.White
                                    isIslamic -> Color.Black
                                    isSelected -> Color.White
                                    else -> InkPrimary
                                },
                                animationSpec = tween(180),
                                label = "notif_filter_text_${category.name}"
                            )

                            Row(
                                modifier = Modifier
                                    .heightIn(min = 34.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(chipBg)
                                    .border(1.dp, chipBorder, RoundedCornerShape(14.dp))
                                    .clickable { selectedFilterCategory = category }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("notif_category_${category.name.lowercase()}"),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CategoryBadgeIcon(
                                    category = category,
                                    size = 14.dp
                                )
                                Text(
                                    text = category.englishLabel,
                                    fontFamily = Inter,
                                    fontWeight = if (isSelected || isIslamic) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = chipText
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 1.dp, color = WarmDivider)
                }
            }

            // 2. Pull-to-Refresh Notification Alerts List
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                state = pullRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("notifications_list")
                ) {
                    // Breaking News Push Alerts Control Banner Card
                    item(key = "push_alerts_control_card") {
                        PushAlertControlBanner(
                            notificationsEnabled = notificationsEnabled,
                            notificationFocus = notificationFocus,
                            onToggleNotifications = onToggleNotifications,
                            onSelectFocus = onSelectNotificationFocus
                        )
                    }

                    if (filteredNotifications.isEmpty()) {
                        item(key = "empty_notifications_box") {
                            Surface(
                                color = PureWhite,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, WarmBorder, RoundedCornerShape(18.dp))
                                    .padding(24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = if (selectedInboxTab == InboxTab.SAVED) Icons.Outlined.BookmarkBorder else Icons.Outlined.Notifications,
                                        contentDescription = null,
                                        tint = SoftGrey,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = when (selectedInboxTab) {
                                            InboxTab.SAVED -> "No saved stories yet"
                                            InboxTab.UNREAD -> "You're all caught up!"
                                            InboxTab.ALL -> "No active notifications"
                                        },
                                        fontFamily = PlayfairDisplay,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = InkPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = when (selectedInboxTab) {
                                            InboxTab.SAVED -> "Tap the Save icon on any news card or notification to bookmark stories for offline reading."
                                            InboxTab.UNREAD -> "All recent breaking alerts have been read. Switch to 'All Alerts' or pull down to refresh."
                                            InboxTab.ALL -> "Pull down to check for new breaking dispatches or choose another category above."
                                        },
                                        fontFamily = Inter,
                                        fontSize = 13.sp,
                                        color = SoftGrey,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else if (selectedInboxTab == InboxTab.SAVED) {
                        item(key = "header_saved_stories") {
                            NotificationSectionHeader(
                                title = "Saved Stories (${filteredNotifications.size})",
                                isLive = false
                            )
                        }
                        items(filteredNotifications, key = { "saved_${it.id}" }) { article ->
                            NotificationDispatchCard(
                                article = article,
                                currentLanguage = currentLanguage,
                                isUnread = false,
                                isBookmarked = article.id in savedArticleIds,
                                showDismissButton = false,
                                onDismiss = {},
                                onToggleBookmark = { onToggleBookmark(article) },
                                onClick = { onNotificationArticleClick(article) }
                            )
                        }
                    } else {
                        if (breakingRecentGroup.isNotEmpty()) {
                            item(key = "header_breaking_now") {
                                NotificationSectionHeader(
                                    title = "Breaking & Recent (${breakingRecentGroup.size})",
                                    isLive = true
                                )
                            }
                            items(breakingRecentGroup, key = { "notif_recent_${it.id}" }) { article ->
                                val isUnread = article.id !in readNotificationIds
                                NotificationDispatchCard(
                                    article = article,
                                    currentLanguage = currentLanguage,
                                    isUnread = isUnread,
                                    isBookmarked = article.id in savedArticleIds,
                                    showDismissButton = true,
                                    onDismiss = { onDismissNotification(article.id) },
                                    onToggleBookmark = { onToggleBookmark(article) },
                                    onClick = { onNotificationArticleClick(article) }
                                )
                            }
                        }

                        if (earlierTodayGroup.isNotEmpty()) {
                            item(key = "header_earlier_today") {
                                NotificationSectionHeader(
                                    title = "Earlier Today (${earlierTodayGroup.size})",
                                    isLive = false
                                )
                            }
                            items(earlierTodayGroup, key = { "notif_today_${it.id}" }) { article ->
                                val isUnread = article.id !in readNotificationIds
                                NotificationDispatchCard(
                                    article = article,
                                    currentLanguage = currentLanguage,
                                    isUnread = isUnread,
                                    isBookmarked = article.id in savedArticleIds,
                                    showDismissButton = true,
                                    onDismiss = { onDismissNotification(article.id) },
                                    onToggleBookmark = { onToggleBookmark(article) },
                                    onClick = { onNotificationArticleClick(article) }
                                )
                            }
                        }

                        if (olderGroup.isNotEmpty()) {
                            item(key = "header_earlier_dispatches") {
                                NotificationSectionHeader(
                                    title = "Recent Dispatches (${olderGroup.size})",
                                    isLive = false
                                )
                            }
                            items(olderGroup, key = { "notif_older_${it.id}" }) { article ->
                                val isUnread = article.id !in readNotificationIds
                                NotificationDispatchCard(
                                    article = article,
                                    currentLanguage = currentLanguage,
                                    isUnread = isUnread,
                                    isBookmarked = article.id in savedArticleIds,
                                    showDismissButton = true,
                                    onDismiss = { onDismissNotification(article.id) },
                                    onToggleBookmark = { onToggleBookmark(article) },
                                    onClick = { onNotificationArticleClick(article) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSectionHeader(
    title: String,
    isLive: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD93025))
            )
        } else {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(2.dp)
                    .background(MutedGold)
            )
        }
        Text(
            text = title,
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 12.5.sp,
            color = if (isLive) DeepEmerald else SoftGrey
        )
    }
}

@Composable
private fun PushAlertControlBanner(
    notificationsEnabled: Boolean,
    notificationFocus: NotificationFocus,
    onToggleNotifications: (Boolean) -> Unit,
    onSelectFocus: (NotificationFocus) -> Unit
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (notificationsEnabled) DeepEmerald.copy(alpha = 0.35f) else WarmBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            EmeraldTint.copy(alpha = 0.45f),
                            PureWhite
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (notificationsEnabled) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = if (notificationsEnabled) DeepEmerald else SoftGrey,
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = if (notificationsEnabled) "Priority Push Alerts Active" else "Priority Push Alerts Muted",
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = InkPrimary
                        )
                        Text(
                            text = "Alert focus: ${notificationFocus.subtitle}",
                            fontFamily = Inter,
                            fontSize = 12.sp,
                            color = SoftGrey
                        )
                    }
                }

                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = onToggleNotifications,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = DeepEmerald
                    ),
                    modifier = Modifier.testTag("notifications_page_toggle")
                )
            }

            // Alert Focus Selector Pills
            val focusScroll = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(focusScroll),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NotificationFocus.entries.forEach { focus ->
                    val selected = focus == notificationFocus
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) DeepEmerald else PureWhite)
                            .border(
                                1.dp,
                                if (selected) DeepEmerald else WarmBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectFocus(focus) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("notif_focus_${focus.name.lowercase()}")
                    ) {
                        Text(
                            text = focus.label,
                            fontFamily = Inter,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp,
                            color = if (selected) Color.White else InkPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationDispatchCard(
    article: NewsArticle,
    currentLanguage: AppLanguage,
    isUnread: Boolean,
    isBookmarked: Boolean,
    showDismissButton: Boolean,
    onDismiss: () -> Unit,
    onToggleBookmark: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val hasTargetTranslation = article.hasTranslation(currentLanguage)
    val effectiveLanguage = if (hasTargetTranslation) currentLanguage else AppLanguage.ENGLISH
    val articleTitle = article.getTitle(effectiveLanguage).trim()
    val articleSummary = article.getSummary(effectiveLanguage).trim()
    val isUrdu = effectiveLanguage == AppLanguage.URDU
    val isArabic = effectiveLanguage == AppLanguage.ARABIC
    val isRtlText = isUrdu || isArabic
    val isIslamic = article.category == NewsCategory.ISLAMIC
    val isPriorityAlert = article.category == NewsCategory.SECURITY || article.category == NewsCategory.PALESTINE

    val cleanLead = remember(articleTitle, articleSummary) {
        when {
            articleSummary.isBlank() || articleSummary.equals(articleTitle, ignoreCase = true) -> ""
            articleSummary.startsWith(articleTitle, ignoreCase = true) ->
                articleSummary.removePrefix(articleTitle).trimStart(' ', '-', '—', ':', '۔', '.')
            else -> articleSummary
        }
    }

    val headlineFontFamily = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> PlayfairDisplay
    }
    val storyFontFamily = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> Inter
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = if (isUnread) 1.3.dp else 1.dp,
                color = if (isIslamic) Color.Black else if (isUnread) DeepEmerald.copy(alpha = 0.5f) else WarmBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .testTag("notification_card_${article.id}"),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 2.dp else 1.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isUnread) {
                        Brush.verticalGradient(
                            colors = listOf(
                                EmeraldTint.copy(alpha = 0.42f),
                                PureWhite
                            )
                        )
                    } else {
                        Brush.verticalGradient(listOf(PureWhite, PureWhite))
                    }
                )
                .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 10.dp)
        ) {
            // Top Meta Row: Unread NEW Badge + Publisher Avatar + Name + Time + Category Flag Pill + Dismiss (X)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isUnread) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DeepEmerald)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                fontFamily = Inter,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    if (isPriorityAlert) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFC62828))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "PRIORITY",
                                fontFamily = Inter,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(if (isIslamic) Color(0xFF111111) else EmeraldTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = article.sourceName.take(1).uppercase(),
                            fontFamily = Inter,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isIslamic) Color.White else DeepEmerald
                        )
                    }

                    Text(
                        text = article.sourceName,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = DeepEmerald,
                        modifier = Modifier.size(13.dp)
                    )

                    Text(
                        text = "• ${PrayerTimesUtil.formatRelativeTime(article.publishedAt, AppLanguage.ENGLISH)}",
                        fontFamily = Inter,
                        fontSize = 11.5.sp,
                        color = SoftGrey,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Category Flag Pill (White & Black for Islamic, Full-Color Flag for others)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isIslamic) Color.White else EmeraldTint)
                            .border(
                                1.dp,
                                if (isIslamic) Color.Black else DeepEmerald.copy(alpha = 0.22f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CategoryBadgeIcon(
                                category = article.category,
                                size = 13.dp
                            )
                            Text(
                                text = article.category.englishLabel,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = if (isIslamic) Color.Black else DeepEmerald
                            )
                        }
                    }

                    if (showDismissButton) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("dismiss_notif_${article.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss notification",
                                tint = SoftGrey,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle Row: Thumbnail + Story Text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Compact Thumbnail on left
                Box(
                    modifier = Modifier
                        .size(width = 96.dp, height = 70.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(0.5.dp, WarmBorder, RoundedCornerShape(12.dp))
                ) {
                    if (article.hasValidImage) {
                        val imageRequest = remember(article.imageUrl, context) {
                            ImageRequest.Builder(context)
                                .data(article.imageUrl)
                                .size(320, 240)
                                .crossfade(true)
                                .memoryCacheKey("notif_img_${article.id}")
                                .diskCacheKey("notif_img_${article.id}")
                                .dispatcher(Dispatchers.IO)
                                .allowHardware(true)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = articleTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        BrandedSourcePlaceholder(
                            sourceName = article.sourceName,
                            category = article.category
                        )
                    }
                }

                // Story headline & lead preview in selected language
                CompositionLocalProvider(
                    LocalLayoutDirection provides (if (isRtlText) LayoutDirection.Rtl else LayoutDirection.Ltr)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = articleTitle,
                            style = TextStyle(
                                fontFamily = headlineFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (isUrdu) 16.5.sp else if (isArabic) 15.5.sp else 14.5.sp,
                                lineHeight = if (isUrdu) 33.sp else if (isArabic) 27.sp else 20.sp,
                                color = InkPrimary,
                                textAlign = TextAlign.Start,
                                platformStyle = PlatformTextStyle(includeFontPadding = true)
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (cleanLead.isNotBlank()) {
                            Text(
                                text = cleanLead,
                                style = TextStyle(
                                    fontFamily = storyFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = if (isUrdu) 14.5.sp else if (isArabic) 13.5.sp else 12.5.sp,
                                    lineHeight = if (isUrdu) 29.sp else if (isArabic) 24.sp else 17.sp,
                                    color = SoftGrey,
                                    textAlign = TextAlign.Start,
                                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp, color = WarmDivider)

            // Quick Actions Row: Save/Bookmark | Share | Read at source
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Save / Bookmark Action
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleBookmark() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("bookmark_notif_${article.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isBookmarked) "Remove from saved" else "Save story",
                            tint = if (isBookmarked) DeepEmerald else SoftGrey,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isBookmarked) "Saved" else "Save",
                            fontFamily = Inter,
                            fontWeight = if (isBookmarked) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isBookmarked) DeepEmerald else SoftGrey
                        )
                    }

                    // Share Action
                    Row(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, articleTitle)
                                    putExtra(Intent.EXTRA_TEXT, "$articleTitle\n\n$articleSummary\n\n${article.sourceUrl}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Alert:"))
                            }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share notification",
                            tint = SoftGrey,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Share",
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = SoftGrey
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { BrowserUtil.openCustomTab(context, article.sourceUrl) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "Read at source",
                        tint = DeepEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Read at source",
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = DeepEmerald
                    )
                }
            }
        }
    }
}
