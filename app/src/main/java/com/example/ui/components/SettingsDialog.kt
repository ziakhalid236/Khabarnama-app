package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.RssRemoteNewsSource
import com.example.model.AppLanguage
import com.example.model.NewsCategory
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldTextDark
import com.example.ui.theme.GoldTint
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.Inter
import com.example.ui.theme.MutedGold
import com.example.ui.theme.PlayfairDisplay
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmDivider
import com.example.ui.theme.WarmOffWhite
import com.example.util.BrowserUtil
import com.example.util.CityPrayerInfo
import com.example.util.PrayerTimesUtil

const val PRIVACY_POLICY_URL = "https://ais-pre-3hjo7cwekaqntolbnr6c4h-828619519318.asia-southeast1.run.app/privacy"

@Composable
fun SettingsDialog(
    currentLanguage: AppLanguage,
    selectedCity: CityPrayerInfo,
    hijriAdjustment: Int = 0,
    notificationsEnabled: Boolean = true,
    onToggleNotifications: (Boolean) -> Unit = {},
    onSendTestNotification: () -> Unit = {},
    onHijriAdjustmentSelected: (Int) -> Unit = {},
    onOpenLanguageSelect: () -> Unit,
    onOpenPrayerCitySelect: () -> Unit,
    onRefreshNews: () -> Unit = {},
    isRefreshing: Boolean = false,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Distinct publishers from whitelisted RSS sources
    val distinctSources = remember(context) {
        runCatching {
            RssRemoteNewsSource(context).loadWhitelistedSources()
                .distinctBy { it.name }
        }.getOrDefault(emptyList())
    }

    val liveHijriPreview = remember(selectedCity, hijriAdjustment) {
        PrayerTimesUtil.getTodayHijriDate(
            lang = AppLanguage.ENGLISH,
            adjustmentDays = hijriAdjustment,
            zoneId = selectedCity.zoneId
        )
    }

    val nextPrayerPreview = remember(selectedCity) {
        PrayerTimesUtil.getNextPrayer(
            city = selectedCity,
            lang = AppLanguage.ENGLISH
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Scaffold(
            topBar = {
                Surface(
                    color = WarmOffWhite,
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
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("close_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to news feed",
                                    tint = InkPrimary
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Settings",
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = InkPrimary
                                )
                                Text(
                                    text = "Languages, Breaking Alerts, Prayer Times & Publishers",
                                    fontFamily = Inter,
                                    fontSize = 12.sp,
                                    color = SoftGrey
                                )
                            }
                        }
                        HorizontalDivider(thickness = 1.dp, color = WarmDivider)
                    }
                }
            },
            containerColor = WarmOffWhite,
            modifier = modifier
                .fillMaxSize()
                .testTag("settings_dialog")
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: READING, ALERTS & PRAYER PREFERENCES
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(DeepEmerald, MutedGold, DeepEmerald)
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "Reading, Alerts & Prayer",
                                fontFamily = PlayfairDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = InkPrimary
                            )

                            // 1. Language Row (opens LanguageSelectDialog)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, WarmBorder, RoundedCornerShape(16.dp))
                                    .clickable { onOpenLanguageSelect() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("settings_language_row"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldTint)
                                        .border(1.dp, GoldBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Translate,
                                        contentDescription = "News translation language",
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Language",
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = "Translates headlines & summaries instantly",
                                        fontFamily = Inter,
                                        fontSize = 12.sp,
                                        color = SoftGrey
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = currentLanguage.flagEmoji,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = currentLanguage.displayName,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DeepEmerald,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Change language",
                                    tint = SoftGrey,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 2. Breaking News Notifications (with Rich Hero Images)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, WarmBorder, RoundedCornerShape(16.dp))
                                    .padding(14.dp)
                                    .testTag("settings_notifications_section"),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(if (notificationsEnabled) EmeraldTint else WarmDivider),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (notificationsEnabled) {
                                                Icons.Default.NotificationsActive
                                            } else {
                                                Icons.Outlined.Notifications
                                            },
                                            contentDescription = "Breaking news notifications",
                                            tint = if (notificationsEnabled) DeepEmerald else SoftGrey,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Breaking News Alerts",
                                            fontFamily = Inter,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = InkPrimary
                                        )
                                        Text(
                                            text = "Receive rich photo alerts for top dispatches",
                                            fontFamily = Inter,
                                            fontSize = 12.sp,
                                            color = SoftGrey
                                        )
                                    }

                                    Switch(
                                        checked = notificationsEnabled,
                                        onCheckedChange = { onToggleNotifications(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.White,
                                            checkedTrackColor = DeepEmerald,
                                            uncheckedThumbColor = SoftGrey,
                                            uncheckedTrackColor = WarmDivider
                                        ),
                                        modifier = Modifier.testTag("settings_notifications_switch")
                                    )
                                }

                                OutlinedButton(
                                    onClick = onSendTestNotification,
                                    enabled = notificationsEnabled,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, if (notificationsEnabled) DeepEmerald else WarmBorder),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .testTag("settings_test_notification_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = "Send breaking news alert preview",
                                        tint = if (notificationsEnabled) DeepEmerald else SoftGrey,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Send Breaking News Image Alert Now",
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (notificationsEnabled) DeepEmerald else SoftGrey
                                    )
                                }
                            }

                            // 2. Prayer City Row with Country Flag & Live Next Prayer
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 56.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, WarmBorder, RoundedCornerShape(16.dp))
                                    .clickable { onOpenPrayerCitySelect() }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("settings_prayer_city_row"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldTint),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = "Prayer times city",
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Prayer city",
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = InkPrimary
                                    )
                                    Text(
                                        text = "Next: ${nextPrayerPreview.prayerName} at ${nextPrayerPreview.timeFormatted}",
                                        fontFamily = Inter,
                                        fontSize = 12.sp,
                                        color = DeepEmerald
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Text(
                                    text = "${cityFlagEmoji(selectedCity.id)} ${selectedCity.name}",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = DeepEmerald,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Change prayer city",
                                    tint = SoftGrey,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // 3. Hijri Date Adjustment (-2, -1, 0, +1, +2 days) with live date preview
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, WarmBorder, RoundedCornerShape(16.dp))
                                    .padding(16.dp)
                                    .testTag("hijri_adjustment_section")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldTint),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "Hijri calendar adjustment",
                                            tint = DeepEmerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Hijri adjustment",
                                            fontFamily = Inter,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = InkPrimary
                                        )
                                        Text(
                                            text = "Today: $liveHijriPreview",
                                            fontFamily = Inter,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 12.sp,
                                            color = DeepEmerald
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    val label = when {
                                        hijriAdjustment > 0 -> "+$hijriAdjustment d"
                                        hijriAdjustment < 0 -> "$hijriAdjustment d"
                                        else -> "0 d"
                                    }
                                    Text(
                                        text = label,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = DeepEmerald
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PrayerTimesUtil.hijriAdjustments.forEach { offset ->
                                        val isSelected = offset == hijriAdjustment
                                        val textLabel = if (offset > 0) "+$offset" else offset.toString()

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                                .defaultMinSize(minHeight = 48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) DeepEmerald else WarmDivider)
                                                .clickable { onHijriAdjustmentSelected(offset) }
                                                .padding(vertical = 8.dp)
                                                .testTag("hijri_offset_$offset"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = textLabel,
                                                fontFamily = Inter,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 13.sp,
                                                color = if (isSelected) Color.White else InkPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 2: CONTENT & SOURCES DISCLAIMER
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
                        .testTag("settings_content_disclaimer_section")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "Content and sources disclaimer",
                                tint = DeepEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Content & sources",
                                fontFamily = PlayfairDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = InkPrimary
                            )
                        }

                        Text(
                            text = "All news stories, headlines, and photographs belong to their respective publishers. Khabarnama provides concise editorial summaries and links directly back to the original articles on each publisher's website.",
                            fontFamily = Inter,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = SoftGrey
                        )
                    }
                }

                // SECTION 3: SOURCES LIST & LIVE FEED REFRESH
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
                        .testTag("settings_sources_section")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Newspaper,
                                    contentDescription = "Trusted news sources",
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Sources list",
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = InkPrimary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EmeraldTint)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${distinctSources.size} Publishers",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = DeepEmerald
                                )
                            }
                        }

                        Text(
                            text = "Khabarnama aggregates automatic live RSS dispatches from these trusted international, Pakistani, Middle Eastern, and Islamic scholarly publications:",
                            fontFamily = Inter,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = SoftGrey
                        )

                        Button(
                            onClick = onRefreshNews,
                            enabled = !isRefreshing,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("settings_refresh_feeds_button")
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = "Refresh all news feeds",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRefreshing) "Refreshing live feeds..." else "Refresh All News Feeds Now",
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        distinctSources.forEachIndexed { index, source ->
                            if (index > 0) {
                                HorizontalDivider(thickness = 1.dp, color = WarmDivider)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(WarmOffWhite)
                                            .border(1.dp, WarmBorder, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = publisherFlagEmoji(source.name),
                                            fontSize = 14.sp
                                        )
                                    }

                                    Text(
                                        text = source.name,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = InkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Verified source",
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (source.isScholarlySource) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(GoldTint)
                                                .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "Scholarly",
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = GoldTextDark
                                            )
                                        }
                                    }

                                    val isIslamic = source.category == NewsCategory.ISLAMIC
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isIslamic) EmeraldTint else WarmDivider)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (isIslamic) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(MutedGold)
                                                )
                                            }
                                            Text(
                                                text = source.category.englishLabel,
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = if (isIslamic) DeepEmerald else SoftGrey
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 4: ABOUT
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
                        .testTag("settings_about_section")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = "About Khabarnama",
                                tint = DeepEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "About",
                                fontFamily = PlayfairDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = InkPrimary
                            )
                        }

                        Text(
                            text = "Khabarnama v1.0 • Global & Islamic Dispatch",
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = InkPrimary
                        )

                        Text(
                            text = "Khabarnama delivers curated international, Pakistani, and Islamic reporting with an English-locked editorial interface and on-demand headline and summary translation into Urdu, Arabic, Turkish, and Hindi. Full articles open directly on each publisher's website with complete journalistic attribution.",
                            fontFamily = Inter,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = SoftGrey
                        )
                    }
                }

                // SECTION 5: PRIVACY POLICY (with external Privacy Policy link)
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
                        .testTag("settings_privacy_section")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Policy,
                                contentDescription = "Privacy Policy",
                                tint = DeepEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Privacy Policy",
                                fontFamily = PlayfairDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = InkPrimary
                            )
                        }

                        Text(
                            text = "• No personal accounts or tracking: Khabarnama does not require registration or collect personal identity data.\n" +
                                "• On-device preferences: Your selected translation language, prayer city, Hijri adjustment, and recent searches are stored locally on your device.\n" +
                                "• Direct publisher attribution: RSS feeds and article images are loaded directly from whitelisted publishers. Opening a full story uses Chrome Custom Tabs governed by the publisher's privacy policy.\n" +
                                "• On-demand translation: Article headlines and summaries are translated on-demand and cached locally in Room for offline reading.",
                            fontFamily = Inter,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = SoftGrey
                        )

                        OutlinedButton(
                            onClick = { BrowserUtil.openCustomTab(context, PRIVACY_POLICY_URL) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, DeepEmerald),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("privacy_policy_link_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "Open Privacy Policy in browser",
                                tint = DeepEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Read Privacy Policy Online",
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
}

private fun cityFlagEmoji(cityId: String): String {
    return when (cityId.lowercase()) {
        "karachi", "lahore", "islamabad" -> "🇵🇰"
        "makkah", "madinah" -> "🇸🇦"
        "london" -> "🇬🇧"
        "new_york" -> "🇺🇸"
        else -> "🌍"
    }
}

private fun publisherFlagEmoji(publisherName: String): String {
    return when (publisherName) {
        "Dawn", "The Express Tribune", "Geo News", "ARY News", "Pakistan Today", "Business Recorder" -> "🇵🇰"
        "Arab News", "Saudi Gazette" -> "🇸🇦"
        "Al Jazeera" -> "🇶🇦"
        "Anadolu Agency", "Daily Sabah" -> "🇹🇷"
        "BBC News", "BBC Asia", "BBC Business", "BBC Sport", "Middle East Eye", "5Pillars", "Islam21c", "IlmFeed" -> "🇬🇧"
        "TechCrunch", "The Verge", "Ars Technica", "CNBC World", "ESPNcricinfo", "MuslimMatters", "ProductiveMuslim" -> "🇺🇸"
        "SeekersGuidance", "AboutIslam" -> "🕌"
        "UN News" -> "🇺🇳"
        else -> "🌐"
    }
}
