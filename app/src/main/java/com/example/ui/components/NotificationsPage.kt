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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.util.PrayerTimesUtil
import kotlinx.coroutines.Dispatchers

@Composable
fun NotificationsPage(
    articles: List<NewsArticle>,
    featuredIslamicArticles: List<NewsArticle>,
    currentLanguage: AppLanguage,
    notificationsEnabled: Boolean,
    readNotificationIds: Set<String>,
    onMarkAllRead: () -> Unit,
    onNotificationArticleClick: (NewsArticle) -> Unit,
    onToggleNotifications: (Boolean) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackToHome)

    var selectedFilterCategory by remember { mutableStateOf(NewsCategory.ALL) }

    val allAlertArticles = remember(articles, featuredIslamicArticles) {
        (articles + featuredIslamicArticles)
            .distinctBy { it.id }
            .sortedByDescending { it.publishedAt }
    }

    val filteredNotifications = remember(allAlertArticles, selectedFilterCategory) {
        if (selectedFilterCategory == NewsCategory.ALL) {
            allAlertArticles
        } else {
            allAlertArticles.filter { it.category == selectedFilterCategory }
        }
    }

    val unreadCount = remember(allAlertArticles, readNotificationIds) {
        allAlertArticles.count { it.id !in readNotificationIds }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(WarmOffWhite)
                .testTag("notifications_page")
        ) {
            // 1. Top Header Bar for Notifications Page
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldTint)
                                    .border(1.dp, DeepEmerald.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NotificationsActive,
                                    contentDescription = "Notifications",
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
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
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
                                    text = "Breaking dispatches & priority alerts",
                                    fontFamily = Inter,
                                    fontSize = 12.sp,
                                    color = SoftGrey
                                )
                            }
                        }

                        if (unreadCount > 0) {
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
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DoneAll,
                                    contentDescription = "Mark all read",
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "Mark read",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = DeepEmerald
                                )
                            }
                        }
                    }

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

            // 2. Notification Alerts List
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
                        onToggleNotifications = onToggleNotifications
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
                                    imageVector = Icons.Outlined.Notifications,
                                    contentDescription = null,
                                    tint = SoftGrey,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "No notifications in this category yet",
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = InkPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "Select another category above or pull to refresh on the Home screen for new dispatches.",
                                    fontFamily = Inter,
                                    fontSize = 13.sp,
                                    color = SoftGrey,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredNotifications, key = { "notif_${it.id}" }) { article ->
                        val isUnread = article.id !in readNotificationIds
                        NotificationDispatchCard(
                            article = article,
                            currentLanguage = currentLanguage,
                            isUnread = isUnread,
                            onClick = { onNotificationArticleClick(article) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PushAlertControlBanner(
    notificationsEnabled: Boolean,
    onToggleNotifications: (Boolean) -> Unit
) {
    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (notificationsEnabled) DeepEmerald.copy(alpha = 0.35f) else WarmBorder, RoundedCornerShape(18.dp))
    ) {
        Row(
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
                        text = if (notificationsEnabled) "Breaking Push Alerts Active" else "Breaking Push Alerts Muted",
                        fontFamily = Inter,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = InkPrimary
                    )
                    Text(
                        text = "Instant alerts for Palestine, Islamic, Pakistan & World news",
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
    }
}

@Composable
private fun NotificationDispatchCard(
    article: NewsArticle,
    currentLanguage: AppLanguage,
    isUnread: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val hasTargetTranslation = article.hasTranslation(currentLanguage)
    val effectiveLanguage = if (hasTargetTranslation) currentLanguage else AppLanguage.ENGLISH
    val articleTitle = article.getTitle(effectiveLanguage)
    val articleSummary = article.getSummary(effectiveLanguage)
    val isUrdu = effectiveLanguage == AppLanguage.URDU
    val isArabic = effectiveLanguage == AppLanguage.ARABIC
    val isRtlText = isUrdu || isArabic
    val isIslamic = article.category == NewsCategory.ISLAMIC

    val unifiedStoryText = remember(articleTitle, articleSummary) {
        val cleanTitle = articleTitle.trim()
        val cleanSummary = articleSummary.trim()
        when {
            cleanSummary.isBlank() -> cleanTitle
            cleanTitle.isBlank() -> cleanSummary
            cleanSummary.equals(cleanTitle, ignoreCase = true) -> cleanTitle
            cleanSummary.startsWith(cleanTitle, ignoreCase = true) -> cleanSummary
            cleanTitle.startsWith(cleanSummary, ignoreCase = true) -> cleanTitle
            cleanTitle.endsWith("۔") || cleanTitle.endsWith(".") || cleanTitle.endsWith("!") || cleanTitle.endsWith("؟") ->
                "$cleanTitle $cleanSummary"
            else ->
                "$cleanTitle — $cleanSummary"
        }
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
                width = if (isUnread) 1.2.dp else 1.dp,
                color = if (isIslamic) Color.Black else if (isUnread) DeepEmerald.copy(alpha = 0.45f) else WarmBorder,
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
                                EmeraldTint.copy(alpha = 0.35f),
                                PureWhite
                            )
                        )
                    } else {
                        Brush.verticalGradient(listOf(PureWhite, PureWhite))
                    }
                )
                .padding(14.dp)
        ) {
            // Top Meta Row: Unread Dot + Breaking Label + Source + Time + Category Flag Badge
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
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(DeepEmerald)
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

                // Category Flag Pill (White & Black for Islamic, Full-Color Flag for others)
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Middle Row: Thumbnail + Story Text
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Compact Thumbnail on left
                Box(
                    modifier = Modifier
                        .size(width = 92.dp, height = 68.dp)
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

                // Story text in selected language
                CompositionLocalProvider(
                    LocalLayoutDirection provides (if (isRtlText) LayoutDirection.Rtl else LayoutDirection.Ltr)
                ) {
                    Text(
                        text = unifiedStoryText,
                        style = TextStyle(
                            fontFamily = storyFontFamily,
                            fontWeight = if (isUrdu) FontWeight.Normal else FontWeight.Medium,
                            fontSize = if (isUrdu) 16.sp else if (isArabic) 15.sp else 14.sp,
                            lineHeight = if (isUrdu) 33.sp else if (isArabic) 28.sp else 20.sp,
                            color = InkPrimary,
                            textAlign = TextAlign.Start,
                            platformStyle = PlatformTextStyle(includeFontPadding = true)
                        ),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(thickness = 0.5.dp, color = WarmDivider)

            // Quick Actions Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
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
