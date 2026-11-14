package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.Dispatchers
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
import com.example.ui.theme.TranslucentDarkPill
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmDivider
import com.example.ui.theme.WarmOffWhite
import com.example.util.BrowserUtil
import com.example.util.PrayerTimesUtil

@Composable
fun FacebookNewsCard(
    article: NewsArticle,
    currentLanguage: AppLanguage,
    isBookmarked: Boolean = false,
    onToggleBookmark: (NewsArticle) -> Unit = {},
    onArticleClick: (NewsArticle) -> Unit,
    onSourceBadgeClick: (NewsArticle) -> Unit,
    onOpenLanguageDialog: () -> Unit = {},
    onRetryTranslation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth subtle card press animation (scale 0.98)
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 150),
        label = "card_press_scale"
    )

    val hasTargetTranslation = article.hasTranslation(currentLanguage)
    val effectiveLanguage = if (hasTargetTranslation) currentLanguage else AppLanguage.ENGLISH
    val articleTitle = article.getTitle(effectiveLanguage)
    val articleSummary = article.getSummary(effectiveLanguage)
    val isUrdu = effectiveLanguage == AppLanguage.URDU
    val isArabic = effectiveLanguage == AppLanguage.ARABIC
    val isRtlText = isUrdu || isArabic
    val isIslamic = article.category == NewsCategory.ISLAMIC

    val isBreakingLive = remember(article.publishedAt, article.category) {
        article.category != NewsCategory.SPORTS &&
            article.category != NewsCategory.BUSINESS &&
            article.publishedAt >= System.currentTimeMillis() - 2L * 3600_000L
    }

    val cleanHeadline = remember(articleTitle) { articleTitle.trim() }
    val cleanLeadSummary = remember(articleTitle, articleSummary) {
        val t = articleTitle.trim()
        val s = articleSummary.trim()
        when {
            s.isBlank() || s.equals(t, ignoreCase = true) -> ""
            s.startsWith(t, ignoreCase = true) -> s.removePrefix(t).trimStart(' ', '-', '—', ':', '۔', '.')
            t.startsWith(s, ignoreCase = true) -> ""
            else -> s
        }
    }

    val headlineFontFamily = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> PlayfairDisplay
    }

    val summaryFontFamily = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> Inter
    }

    val headlineFontSize = when {
        isUrdu -> 19.5.sp
        isArabic -> 18.sp
        else -> 17.5.sp
    }

    val headlineLineHeight = when {
        isUrdu -> 39.sp
        isArabic -> 31.sp
        else -> 24.sp
    }

    val summaryFontSize = when {
        isUrdu -> 16.sp
        isArabic -> 15.sp
        else -> 14.sp
    }

    val summaryLineHeight = when {
        isUrdu -> 34.sp
        isArabic -> 28.sp
        else -> 21.sp
    }

    val storyLetterSpacing = when {
        isUrdu -> 0.3.sp
        isArabic -> 0.15.sp
        else -> 0.05.sp
    }

    val cardBorderColor = if (isIslamic) GoldBorder else WarmBorder
    val topAccentBrush = if (isIslamic) {
        Brush.horizontalGradient(listOf(MutedGold, DeepEmerald, MutedGold))
    } else {
        Brush.horizontalGradient(listOf(DeepEmerald, DeepEmerald.copy(alpha = 0.65f), MutedGold.copy(alpha = 0.7f)))
    }
    val surfaceGradient = if (isIslamic) {
        Brush.verticalGradient(
            colors = listOf(
                GoldTint.copy(alpha = 0.65f),
                PureWhite,
                PureWhite
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                EmeraldTint.copy(alpha = 0.38f),
                PureWhite,
                PureWhite
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onArticleClick(article) }
            .testTag("news_card_${article.id}"),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 0.dp
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceGradient)
        ) {
            // Top editorial accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(topAccentBrush)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp)
            ) {
                // 1. Image on top (16:9) with 16dp corners and dark 55% translucent source badge pill
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(16.dp))
                        .border(0.5.dp, WarmBorder, RoundedCornerShape(16.dp))
                ) {
                    if (article.hasValidImage) {
                        var isImageLoading by remember(article.imageUrl) { mutableStateOf(true) }
                        var isImageError by remember(article.imageUrl) { mutableStateOf(false) }

                        val imageRequest = remember(article.imageUrl, context) {
                            ImageRequest.Builder(context)
                                .data(article.imageUrl)
                                .size(960, 540)
                                .crossfade(true)
                                .memoryCacheKey("card_img_${article.id}")
                                .diskCacheKey("card_img_${article.id}")
                                .dispatcher(Dispatchers.IO)
                                .allowHardware(true)
                                .build()
                        }
                        AsyncImage(
                            model = imageRequest,
                            contentDescription = articleTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onLoading = {
                                isImageLoading = true
                                isImageError = false
                            },
                            onSuccess = {
                                isImageLoading = false
                                isImageError = false
                            },
                            onError = {
                                isImageLoading = false
                                isImageError = true
                            }
                        )
                        if (isImageLoading) {
                            ShimmerPlaceholder()
                        } else if (isImageError) {
                            BrandedSourcePlaceholder(
                                sourceName = article.sourceName,
                                category = article.category
                            )
                        }
                    } else {
                        BrandedSourcePlaceholder(
                            sourceName = article.sourceName,
                            category = article.category
                        )
                    }

                    // Subtle bottom gradient for depth
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.36f)
                                    )
                                )
                            )
                    )

                    // Top-left editorial context pills (LIVE breaking, Social/X Quotes, Scholarly)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isBreakingLive) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFC62828))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                                Text(
                                    text = "LIVE",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }
                        if (article.hasSocialQuote) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(TranslucentDarkPill)
                                    .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = "Direct statement or X dispatch",
                                    tint = Color(0xFFF3DFA2),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Voices & X",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }
                        if (article.isScholarlySource) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF111111).copy(alpha = 0.85f))
                                    .border(0.5.dp, MutedGold, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Scholarly",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp,
                                    color = Color(0xFFF3DFA2)
                                )
                            }
                        }
                    }

                    // Real translucent pill source badge on the image (dark 55% with white text)
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .minimumInteractiveComponentSize()
                            .clickable { onSourceBadgeClick(article) }
                            .testTag("source_badge_${article.id}"),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(TranslucentDarkPill)
                                .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = article.sourceName,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                contentDescription = "Image source attribution for ${article.sourceName}",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Publisher Row (ALWAYS stays LTR): circular avatar, name, verified tick, time, normal-case category pill
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(if (isIslamic) DeepEmerald else EmeraldTint)
                                    .border(1.dp, if (isIslamic) MutedGold else DeepEmerald.copy(alpha = 0.25f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = article.sourceName.take(1).uppercase(),
                                    color = if (isIslamic) Color.White else DeepEmerald,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = article.sourceName,
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = InkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified publisher",
                                tint = DeepEmerald,
                                modifier = Modifier.size(14.dp)
                            )

                            Text(
                                text = "•",
                                fontFamily = Inter,
                                fontSize = 12.sp,
                                color = SoftGrey
                            )

                            Text(
                                text = PrayerTimesUtil.formatRelativeTime(article.publishedAt, AppLanguage.ENGLISH),
                                fontFamily = Inter,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = SoftGrey,
                                maxLines = 1
                            )
                        }

                        // Normal-case category pill with realistic flag / White & Black Islamic emblem
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isIslamic) Color.White else EmeraldTint)
                                .border(
                                    1.dp,
                                    if (isIslamic) Color.Black else DeepEmerald.copy(alpha = 0.22f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                CategoryBadgeIcon(
                                    category = article.category,
                                    tint = if (isIslamic) Color.Black else DeepEmerald,
                                    size = 14.dp
                                )
                                if (isIslamic) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black)
                                    )
                                }
                                Text(
                                    text = article.category.englishLabel,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = if (isIslamic) Color.Black else DeepEmerald
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Headline & Summary with translation shimmer or fallback label
                if (article.isTranslating && currentLanguage != AppLanguage.ENGLISH) {
                    TranslationTextShimmer(lineCount = 3)
                } else {
                    if (article.shouldShowOriginalLanguageLabel(currentLanguage)) {
                        OriginalLanguageBadge(
                            modifier = Modifier.padding(bottom = 8.dp),
                            onRetryClick = onRetryTranslation
                        )
                    }
                    CompositionLocalProvider(
                        LocalLayoutDirection provides (if (isRtlText) LayoutDirection.Rtl else LayoutDirection.Ltr)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = cleanHeadline,
                                style = TextStyle(
                                    fontFamily = headlineFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = headlineFontSize,
                                    lineHeight = headlineLineHeight,
                                    letterSpacing = storyLetterSpacing,
                                    color = InkPrimary,
                                    textAlign = TextAlign.Start,
                                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                                ),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (cleanLeadSummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(if (isUrdu) 4.dp else 6.dp))
                                Text(
                                    text = cleanLeadSummary,
                                    style = TextStyle(
                                        fontFamily = summaryFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = summaryFontSize,
                                        lineHeight = summaryLineHeight,
                                        letterSpacing = storyLetterSpacing,
                                        color = InkPrimary.copy(alpha = 0.82f),
                                        textAlign = TextAlign.Start,
                                        platformStyle = PlatformTextStyle(includeFontPadding = true)
                                    ),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Subtle full-briefing callout pill
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldTint.copy(alpha = 0.65f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Tap for full report & AI intelligence brief",
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                color = DeepEmerald
                            )
                        }
                    }
                }
            }

            // 4. Elevated Editorial Action Footer Bar (Share, Language Flag Pill, Read at source)
            HorizontalDivider(thickness = 1.dp, color = WarmDivider)

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmOffWhite.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Share action (48dp tap target)
                        Row(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .heightIn(min = 48.dp)
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, articleTitle)
                                        putExtra(Intent.EXTRA_TEXT, "$articleTitle\n\n$articleSummary\n\n${article.sourceUrl}")
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Article:"))
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("share_article_${article.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share story",
                                tint = SoftGrey,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = "Share",
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = SoftGrey
                            )
                        }

                        // Save / Bookmark action (48dp tap target)
                        Row(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .heightIn(min = 48.dp)
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onToggleBookmark(article) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("bookmark_article_${article.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isBookmarked) "Remove bookmark" else "Save story",
                                tint = if (isBookmarked) DeepEmerald else SoftGrey,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = if (isBookmarked) "Saved" else "Save",
                                fontFamily = Inter,
                                fontWeight = if (isBookmarked) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isBookmarked) DeepEmerald else SoftGrey
                            )
                        }
                    }

                    // Open source action (48dp tap target, opens Custom Tab)
                    Row(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .heightIn(min = 48.dp)
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { BrowserUtil.openCustomTab(context, article.sourceUrl) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("source_article_${article.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = "Open source article in browser",
                            tint = DeepEmerald,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Read at source",
                            fontFamily = Inter,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = DeepEmerald
                        )
                    }
                }
            }
        }
    }
}
