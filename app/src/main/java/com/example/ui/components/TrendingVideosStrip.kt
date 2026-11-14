package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
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
import com.example.model.NewsVideoClip
import com.example.model.VideoChannelConfig
import com.example.ui.theme.CrimsonBreaking
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.Inter
import com.example.ui.theme.NotoNaskhArabic
import com.example.ui.theme.NotoNastaliqUrdu
import com.example.ui.theme.PlayfairDisplay
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.TranslucentDarkPill
import com.example.ui.theme.WarmBorder
import com.example.util.BrowserUtil
import com.example.util.PrayerTimesUtil
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers

private val CinemaCardSurface = Color(0xFF111113)
private val CinemaCardBorder = Color(0xFF27272A)
private val CinemaSecondaryText = Color(0xFFA1A1AA)

@Composable
fun TrendingVideosStrip(
    videos: List<NewsVideoClip>,
    liveChannels: List<VideoChannelConfig>,
    currentLanguage: AppLanguage,
    onVideoClick: (NewsVideoClip) -> Unit,
    modifier: Modifier = Modifier,
    isSectionVisibleOnScreen: Boolean = true
) {
    if (videos.isEmpty() && liveChannels.isEmpty()) return

    val context = LocalContext.current
    val rowState = rememberLazyListState()
    var isMuted by remember { mutableStateOf(true) }

    val centeredVideoIndex by remember {
        derivedStateOf {
            val info = rowState.layoutInfo
            val visible = info.visibleItemsInfo
            if (visible.isEmpty()) return@derivedStateOf -1
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            visible.minByOrNull { item ->
                abs((item.offset + item.size / 2) - viewportCenter)
            }?.index ?: 0
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("trending_videos_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Editorial Section Masthead
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DeepEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LiveTv,
                        contentDescription = "Broadcast Video Dispatches",
                        tint = PureWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column {
                    Text(
                        text = "VIDEO DISPATCHES",
                        fontFamily = PlayfairDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = 0.4.sp,
                        color = InkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Auto-plays inline • Tap card for full cinema view",
                        fontFamily = Inter,
                        fontSize = 11.sp,
                        color = SoftGrey,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CinemaCardSurface)
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${videos.size} VIDEOS",
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = PureWhite
                )
            }
        }

        // Official Live Broadcast Quick-Launch Strip
        if (liveChannels.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_channels_row")
            ) {
                itemsIndexed(liveChannels, key = { _, channel -> channel.channelId }) { _, channel ->
                    val targetUrl = channel.watchLiveUrl.ifBlank {
                        "https://www.youtube.com/channel/${channel.channelId}"
                    }
                    val hasLiveStreamUrl = targetUrl.contains("/live", ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .heightIn(min = 38.dp)
                            .clip(RoundedCornerShape(19.dp))
                            .background(PureWhite)
                            .border(1.dp, WarmBorder, RoundedCornerShape(19.dp))
                            .clickable { BrowserUtil.openCustomTab(context, targetUrl) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("live_channel_chip_${channel.channelId}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (hasLiveStreamUrl) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonBreaking)
                            )
                        }
                        Text(
                            text = channel.countryFlag,
                            fontSize = 13.sp
                        )
                        Text(
                            text = channel.channelName,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = InkPrimary,
                            maxLines = 1
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = "Open ${channel.channelName} broadcast",
                            tint = InkPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }

        // Dark Obsidian Video Cards Carousel (visually unmistakable from white news articles)
        if (videos.isNotEmpty()) {
            LazyRow(
                state = rowState,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(end = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("trending_videos_row")
            ) {
                itemsIndexed(videos, key = { _, clip -> clip.id }) { index, clip ->
                    val shouldAutoPlayInline = isSectionVisibleOnScreen &&
                        index == centeredVideoIndex &&
                        clip.videoId.isNotBlank()

                    TrendingVideoCard(
                        clip = clip,
                        currentLanguage = currentLanguage,
                        isPlayingInline = shouldAutoPlayInline,
                        isMuted = isMuted,
                        onToggleMute = { isMuted = !isMuted },
                        onExpandClick = { onVideoClick(clip) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TrendingVideoCard(
    clip: NewsVideoClip,
    currentLanguage: AppLanguage,
    isPlayingInline: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onExpandClick: () -> Unit
) {
    val context = LocalContext.current
    val hasTranslated = currentLanguage != AppLanguage.ENGLISH && !clip.translatedTitle.isNullOrBlank()
    val effectiveLang = if (hasTranslated) currentLanguage else AppLanguage.ENGLISH
    val displayTitle = clip.getDisplayTitle(effectiveLang)
    val isUrdu = effectiveLang == AppLanguage.URDU
    val isArabic = effectiveLang == AppLanguage.ARABIC
    val isRtl = isUrdu || isArabic

    val headlineFont = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> Inter
    }

    Card(
        modifier = Modifier
            .width(304.dp)
            .clip(RoundedCornerShape(18.dp))
            .border(1.2.dp, CinemaCardBorder, RoundedCornerShape(18.dp))
            .clickable { onExpandClick() }
            .testTag("video_clip_card_${clip.videoId}"),
        colors = CardDefaults.cardColors(containerColor = CinemaCardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 16:9 Dark Cinema Viewport (Inline Auto-Playing WebView when visible & centered, else Thumbnail)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF050506))
            ) {
                if (isPlayingInline) {
                    YouTubeEmbedPlayer(
                        videoId = clip.videoId,
                        muted = isMuted,
                        onOpenExternal = { BrowserUtil.openCustomTab(context, clip.watchUrl) },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    var isLoading by remember(clip.thumbnailUrl) { mutableStateOf(true) }
                    var isError by remember(clip.thumbnailUrl) { mutableStateOf(false) }

                    val request = remember(clip.thumbnailUrl, context) {
                        ImageRequest.Builder(context)
                            .data(clip.thumbnailUrl)
                            .size(640, 360)
                            .crossfade(true)
                            .dispatcher(Dispatchers.IO)
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = clip.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        onLoading = {
                            isLoading = true
                            isError = false
                        },
                        onSuccess = {
                            isLoading = false
                            isError = false
                        },
                        onError = {
                            isLoading = false
                            isError = true
                        }
                    )

                    if (isLoading) {
                        ShimmerPlaceholder()
                    } else if (isError) {
                        BrandedSourcePlaceholder(
                            sourceName = clip.channelName,
                            category = clip.category
                        )
                    }

                    // Bottom gradient scrim for crisp contrast
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.2f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    )

                    // Centered crisp Play Circle
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.78f))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play video from ${clip.channelName}",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Top-left VIDEO / LIVE badge overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (clip.isLiveBadge) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CrimsonBreaking)
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                            Text(
                                text = "LIVE",
                                fontFamily = Inter,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.6.sp,
                                color = Color.White
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TranslucentDarkPill)
                                .border(0.5.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = if (isPlayingInline) "PLAYING" else "VIDEO",
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.5.sp,
                                letterSpacing = 0.5.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                // Top-right Inline Controls (Mute/Unmute when playing + Fullscreen Expand)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isPlayingInline) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(TranslucentDarkPill)
                                .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                .clickable { onToggleMute() }
                                .testTag("video_mute_toggle_${clip.videoId}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = if (isMuted) "Unmute inline video" else "Mute inline video",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(TranslucentDarkPill)
                            .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                            .clickable { onExpandClick() }
                            .testTag("video_expand_button_${clip.videoId}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fullscreen,
                            contentDescription = "Open full video player",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Bottom-left Channel Badge (when showing thumbnail)
                if (!isPlayingInline) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(TranslucentDarkPill)
                            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = clip.channelName,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Dark Matte Obsidian Headline & Footer Body (Stark contrast vs White Articles)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = PureWhite,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = clip.channelName,
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = PureWhite,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = PrayerTimesUtil.formatRelativeTime(clip.publishedAt, AppLanguage.ENGLISH),
                        fontFamily = Inter,
                        fontSize = 11.sp,
                        color = CinemaSecondaryText
                    )
                }

                CompositionLocalProvider(
                    LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
                ) {
                    Text(
                        text = displayTitle,
                        style = TextStyle(
                            fontFamily = headlineFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = if (isUrdu) 15.sp else if (isArabic) 14.5.sp else 14.sp,
                            lineHeight = if (isUrdu) 27.sp else if (isArabic) 22.sp else 19.sp,
                            color = PureWhite,
                            textAlign = TextAlign.Start,
                            platformStyle = PlatformTextStyle(includeFontPadding = true)
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 38.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1C1C1F))
                        .border(0.5.dp, Color(0xFF3F3F46), RoundedCornerShape(8.dp))
                        .clickable { onExpandClick() }
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = PureWhite,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Watch Full Video",
                        fontFamily = Inter,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = PureWhite,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}
