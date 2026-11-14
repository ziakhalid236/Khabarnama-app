package com.example.ui.components

import android.content.Intent
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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.example.ui.AiBriefingUiState
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
import kotlinx.coroutines.Dispatchers

@Composable
fun ArticleDetailDialog(
    article: NewsArticle,
    currentLanguage: AppLanguage,
    briefingState: AiBriefingUiState = AiBriefingUiState.Idle,
    isBookmarked: Boolean = false,
    onToggleBookmark: () -> Unit = {},
    onSelectLanguage: (AppLanguage) -> Unit = {},
    onRegenerateBriefing: () -> Unit = {},
    onSourceBadgeClick: (NewsArticle) -> Unit,
    onDismiss: () -> Unit,
    onRetryTranslation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var textScaleStep by remember { mutableStateOf(0) } // 0 = Normal, 1 = Large, 2 = Extra Large
    val fontScale = when (textScaleStep) {
        1 -> 1.15f
        2 -> 1.3f
        else -> 1.0f
    }

    val hasTargetTranslation = article.hasTranslation(currentLanguage)
    val effectiveLanguage = if (hasTargetTranslation) currentLanguage else AppLanguage.ENGLISH
    val articleTitle = article.getTitle(effectiveLanguage).trim()
    val articleSummary = article.getSummary(effectiveLanguage).trim()
    val isUrdu = effectiveLanguage == AppLanguage.URDU
    val isArabic = effectiveLanguage == AppLanguage.ARABIC
    val isRtlText = isUrdu || isArabic

    val bodyText = remember(articleTitle, articleSummary, article.fullBody, effectiveLanguage) {
        val rawBody = if (effectiveLanguage == AppLanguage.ENGLISH && article.fullBody.length > articleSummary.length + 25) {
            article.fullBody.trim()
        } else {
            articleSummary
        }
        when {
            rawBody.equals(articleTitle, ignoreCase = true) -> ""
            rawBody.startsWith(articleTitle, ignoreCase = true) ->
                rawBody.removePrefix(articleTitle).trimStart(' ', '-', '—', ':', '۔', '.')
            else -> rawBody
        }
    }

    val headlineFontFamily = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> PlayfairDisplay
    }
    val bodyFontFamily = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> Inter
    }

    val headlineFontSize = when {
        isUrdu -> (22f * fontScale).sp
        isArabic -> (20f * fontScale).sp
        else -> (21f * fontScale).sp
    }
    val headlineLineHeight = when {
        isUrdu -> (44f * fontScale).sp
        isArabic -> (34f * fontScale).sp
        else -> (29f * fontScale).sp
    }

    val bodyFontSize = when {
        isUrdu -> (18.5f * fontScale).sp
        isArabic -> (17f * fontScale).sp
        else -> (15.5f * fontScale).sp
    }
    val bodyLineHeight = when {
        isUrdu -> (39f * fontScale).sp
        isArabic -> (33f * fontScale).sp
        else -> (25f * fontScale).sp
    }

    val storyLetterSpacing = when {
        isUrdu -> 0.35.sp
        isArabic -> 0.2.sp
        else -> 0.1.sp
    }

    val shareArticle = {
        val shareBody = bodyText.ifBlank { articleSummary }
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, articleTitle)
            putExtra(Intent.EXTRA_TEXT, "$articleTitle\n\n$shareBody\n\n${article.sourceUrl}")
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Article:"))
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("close_article_detail")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back to feed",
                                    tint = InkPrimary
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = article.sourceName,
                                    fontFamily = PlayfairDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = InkPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${article.readTimeMinutes} min read • ${article.category.englishLabel}",
                                    fontFamily = Inter,
                                    fontSize = 11.sp,
                                    color = SoftGrey,
                                    maxLines = 1
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { textScaleStep = (textScaleStep + 1) % 3 },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("toggle_text_size_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FormatSize,
                                        contentDescription = "Adjust text size",
                                        tint = if (textScaleStep > 0) DeepEmerald else InkPrimary
                                    )
                                }

                                IconButton(
                                    onClick = onToggleBookmark,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("detail_bookmark_button")
                                ) {
                                    Icon(
                                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = if (isBookmarked) "Remove bookmark" else "Save story",
                                        tint = if (isBookmarked) DeepEmerald else InkPrimary
                                    )
                                }

                                IconButton(
                                    onClick = shareArticle,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .testTag("top_share_article_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = "Share story",
                                        tint = InkPrimary
                                    )
                                }
                            }
                        }
                        HorizontalDivider(thickness = 1.dp, color = WarmDivider)
                    }
                }
            },
            containerColor = WarmOffWhite,
            modifier = modifier
                .fillMaxSize()
                .testTag("article_detail_dialog")
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
                // Main Story Card
                Surface(
                    color = PureWhite,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, WarmBorder, RoundedCornerShape(20.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 1. Hero Image with Dark 55% Translucent Source Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            if (article.hasValidImage) {
                                var isHeroLoading by remember(article.imageUrl) { mutableStateOf(true) }
                                var isHeroError by remember(article.imageUrl) { mutableStateOf(false) }

                                val imageRequest = remember(article.imageUrl, context) {
                                    ImageRequest.Builder(context)
                                        .data(article.imageUrl)
                                        .size(1280, 720)
                                        .crossfade(true)
                                        .memoryCacheKey("detail_img_${article.id}")
                                        .diskCacheKey("detail_img_${article.id}")
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
                                        isHeroLoading = true
                                        isHeroError = false
                                    },
                                    onSuccess = {
                                        isHeroLoading = false
                                        isHeroError = false
                                    },
                                    onError = {
                                        isHeroLoading = false
                                        isHeroError = true
                                    }
                                )
                                if (isHeroLoading) {
                                    ShimmerPlaceholder()
                                } else if (isHeroError) {
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

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.35f)
                                            )
                                        )
                                    )
                            )

                            // Translucent Dark 55% Source Badge Pill on Hero Image
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .minimumInteractiveComponentSize()
                                    .clickable { onSourceBadgeClick(article) }
                                    .testTag("detail_source_badge"),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(TranslucentDarkPill)
                                        .border(0.5.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
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
                                        contentDescription = "View image source attribution for ${article.sourceName}",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Publisher & Category Meta Row (always LTR)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val isIslamic = article.category == NewsCategory.ISLAMIC
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isIslamic) EmeraldTint else WarmDivider),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = article.sourceName.take(1).uppercase(),
                                        color = if (isIslamic) DeepEmerald else InkPrimary,
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
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
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
                                    color = SoftGrey,
                                    maxLines = 1,
                                    softWrap = false
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Published time",
                                        tint = SoftGrey,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = PrayerTimesUtil.formatRelativeTime(article.publishedAt, AppLanguage.ENGLISH),
                                        fontFamily = Inter,
                                        fontSize = 12.sp,
                                        color = SoftGrey,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            val isIslamic = article.category == NewsCategory.ISLAMIC
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

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3. Inline Quick Language Switcher Row inside the Full-Page Reader
                        val langScroll = rememberScrollState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(langScroll),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                val isSelected = lang == currentLanguage
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) DeepEmerald else WarmOffWhite)
                                        .border(
                                            1.dp,
                                            if (isSelected) DeepEmerald else WarmBorder,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectLanguage(lang) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                        .testTag("detail_lang_${lang.code.lowercase()}"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = lang.flagEmoji, fontSize = 11.sp)
                                    Text(
                                        text = lang.nativeName,
                                        fontFamily = Inter,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                        color = if (isSelected) Color.White else InkPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 4. Headline & Story Body (RTL aligned when Urdu/Arabic)
                        if (article.isTranslating && currentLanguage != AppLanguage.ENGLISH) {
                            TranslationTextShimmer(lineCount = 4)
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
                                        text = articleTitle,
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
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    if (bodyText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(if (isUrdu) 8.dp else 12.dp))
                                        Text(
                                            text = bodyText,
                                            style = TextStyle(
                                                fontFamily = bodyFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = bodyFontSize,
                                                lineHeight = bodyLineHeight,
                                                letterSpacing = storyLetterSpacing,
                                                color = InkPrimary.copy(alpha = 0.88f),
                                                textAlign = TextAlign.Start,
                                                platformStyle = PlatformTextStyle(includeFontPadding = true)
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 5. Primary Actions: "Read at source" button + "Share" button (48dp+ height)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { BrowserUtil.openCustomTab(context, article.sourceUrl) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("read_at_source_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepEmerald,
                                    contentColor = PureWhite
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = "Open full article at source",
                                    tint = PureWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Read full source",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = PureWhite,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = shareArticle,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("detail_share_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DeepEmerald)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Share article",
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Share",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = DeepEmerald
                                )
                            }
                        }
                    }
                }

                // 6. Firebase AI Intelligence & Editorial Briefing Card
                IntelligenceBriefingCard(
                    briefingState = briefingState,
                    currentLanguage = currentLanguage,
                    fontScale = fontScale,
                    onRetry = onRegenerateBriefing
                )

                // 7. Attribution Notice
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PureWhite)
                        .border(1.dp, WarmBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Publisher attribution info",
                            tint = DeepEmerald,
                            modifier = Modifier.size(18.dp)
                        )
                        val attributionText = if (effectiveLanguage == AppLanguage.ENGLISH) {
                            "${effectiveLanguage.flagEmoji} Dispatched from ${article.sourceName}. Briefing synthesized for deeper background & verified context."
                        } else {
                            "${effectiveLanguage.flagEmoji} Journalistic report & briefing in ${effectiveLanguage.displayName}. Original dispatch belongs to ${article.sourceName}."
                        }
                        Text(
                            text = attributionText,
                            fontFamily = Inter,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = SoftGrey
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IntelligenceBriefingCard(
    briefingState: AiBriefingUiState,
    currentLanguage: AppLanguage,
    fontScale: Float,
    onRetry: () -> Unit
) {
    val isUrdu = currentLanguage == AppLanguage.URDU
    val isArabic = currentLanguage == AppLanguage.ARABIC
    val isRtl = isUrdu || isArabic

    val textFamily = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> Inter
    }
    val headingFamily = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> PlayfairDisplay
    }
    val textSize = when {
        isUrdu -> (17.5f * fontScale).sp
        isArabic -> (16.5f * fontScale).sp
        else -> (14.5f * fontScale).sp
    }
    val lineHeight = when {
        isUrdu -> (37f * fontScale).sp
        isArabic -> (31f * fontScale).sp
        else -> (22f * fontScale).sp
    }

    Surface(
        color = PureWhite,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.4.dp, DeepEmerald, RoundedCornerShape(16.dp))
            .testTag("ai_intelligence_briefing_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Authoritative Obsidian Header Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepEmerald)
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
                            .size(28.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(PureWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Editorial Intelligence Briefing",
                            tint = DeepEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "EDITORIAL INTELLIGENCE",
                            fontFamily = Inter,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.8.sp,
                            color = PureWhite
                        )
                        Text(
                            text = "Deep context, takeaways & notable voices",
                            fontFamily = Inter,
                            fontSize = 11.sp,
                            color = Color(0xFFD4D4D8)
                        )
                    }
                }

                if (briefingState is AiBriefingUiState.Success || briefingState is AiBriefingUiState.Error) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("refresh_briefing_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate briefing",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (briefingState) {
                    AiBriefingUiState.Idle, AiBriefingUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = InkPrimary
                            )
                            Text(
                                text = "Compiling editorial briefing in ${currentLanguage.displayName}...",
                                fontFamily = Inter,
                                fontSize = 13.sp,
                                color = SoftGrey
                            )
                        }
                    }

                    is AiBriefingUiState.Error -> {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(WarmOffWhite)
                                    .border(1.dp, WarmBorder, RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = InkPrimary,
                                    modifier = Modifier
                                        .padding(top = 1.dp)
                                        .size(18.dp)
                                )
                                Text(
                                    text = briefingState.message,
                                    fontFamily = Inter,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = SoftGrey
                                )
                            }
                            Button(
                                onClick = onRetry,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepEmerald,
                                    contentColor = PureWhite
                                ),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Retry Briefing",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = PureWhite
                                )
                            }
                        }
                    }

                    is AiBriefingUiState.Success -> {
                        val briefing = briefingState.briefing
                        CompositionLocalProvider(
                            LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (briefing.editorialHeadline.isNotBlank()) {
                                    Text(
                                        text = briefing.editorialHeadline,
                                        style = TextStyle(
                                            fontFamily = headingFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (isUrdu) (19.5f * fontScale).sp else (17.5f * fontScale).sp,
                                            lineHeight = if (isUrdu) (38f * fontScale).sp else (24f * fontScale).sp,
                                            color = InkPrimary,
                                            textAlign = TextAlign.Start,
                                            platformStyle = PlatformTextStyle(includeFontPadding = true)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Text(
                                    text = briefing.detailedSummary,
                                    style = TextStyle(
                                        fontFamily = textFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = textSize,
                                        lineHeight = lineHeight,
                                        color = InkPrimary,
                                        textAlign = TextAlign.Start,
                                        platformStyle = PlatformTextStyle(includeFontPadding = true)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (briefing.keyTakeaways.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(WarmOffWhite)
                                            .border(1.dp, WarmBorder, RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(9.dp)
                                    ) {
                                        Text(
                                            text = when (currentLanguage) {
                                                AppLanguage.URDU -> "اہم نکات"
                                                AppLanguage.ARABIC -> "أبرز النقاط"
                                                else -> "KEY TAKEAWAYS"
                                            },
                                            fontFamily = if (isRtl) textFamily else Inter,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = if (isUrdu) 16.sp else 11.sp,
                                            letterSpacing = if (isRtl) 0.sp else 0.7.sp,
                                            color = InkPrimary
                                        )
                                        briefing.keyTakeaways.forEach { point ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(9.dp),
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .padding(top = if (isUrdu) 13.dp else 7.dp)
                                                        .size(5.dp)
                                                        .background(InkPrimary)
                                                )
                                                Text(
                                                    text = point,
                                                    style = TextStyle(
                                                        fontFamily = textFamily,
                                                        fontSize = textSize,
                                                        lineHeight = lineHeight,
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

                                if (briefing.strategicContext.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(WarmOffWhite)
                                            .border(1.dp, WarmBorder, RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(42.dp)
                                                .background(InkPrimary)
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = when (currentLanguage) {
                                                    AppLanguage.URDU -> "اسٹریٹجک پس منظر"
                                                    AppLanguage.ARABIC -> "السياق الاستراتيجي"
                                                    else -> "STRATEGIC & REGIONAL CONTEXT"
                                                },
                                                fontFamily = if (isRtl) textFamily else Inter,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = if (isUrdu) 16.sp else 11.sp,
                                                letterSpacing = if (isRtl) 0.sp else 0.7.sp,
                                                color = InkPrimary
                                            )
                                            Text(
                                                text = briefing.strategicContext,
                                                style = TextStyle(
                                                    fontFamily = textFamily,
                                                    fontSize = textSize,
                                                    lineHeight = lineHeight,
                                                    color = InkPrimary.copy(alpha = 0.9f),
                                                    textAlign = TextAlign.Start,
                                                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }

                                if (briefing.notableVoices.isNotBlank()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(PureWhite)
                                            .border(1.dp, InkPrimary, RoundedCornerShape(12.dp))
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FormatQuote,
                                            contentDescription = null,
                                            tint = InkPrimary,
                                            modifier = Modifier
                                                .padding(top = 3.dp)
                                                .size(18.dp)
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = when (currentLanguage) {
                                                    AppLanguage.URDU -> "اہم بیانات، صحافتی ذرائع اور علماء"
                                                    AppLanguage.ARABIC -> "أبرز التصريحات والمصادر"
                                                    else -> "NOTABLE VOICES, X POSTS & SCHOLARS"
                                                },
                                                fontFamily = if (isRtl) textFamily else Inter,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = if (isUrdu) 15.5.sp else 11.sp,
                                                letterSpacing = if (isRtl) 0.sp else 0.6.sp,
                                                color = InkPrimary
                                            )
                                            Text(
                                                text = briefing.notableVoices,
                                                style = TextStyle(
                                                    fontFamily = textFamily,
                                                    fontSize = textSize,
                                                    lineHeight = lineHeight,
                                                    color = InkPrimary,
                                                    textAlign = TextAlign.Start,
                                                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
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
