package com.example.ui.components

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.remember
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
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.model.AppLanguage
import com.example.model.NewsArticle
import com.example.model.NewsCategory
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldTint
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
fun ArticleDetailDialog(
    article: NewsArticle,
    currentLanguage: AppLanguage,
    onSourceBadgeClick: (NewsArticle) -> Unit,
    onDismiss: () -> Unit,
    onRetryTranslation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val hasTargetTranslation = article.hasTranslation(currentLanguage)
    val effectiveLanguage = if (hasTargetTranslation) currentLanguage else AppLanguage.ENGLISH
    val articleTitle = article.getTitle(effectiveLanguage)
    val articleSummary = article.getSummary(effectiveLanguage)
    val isUrdu = effectiveLanguage == AppLanguage.URDU
    val isArabic = effectiveLanguage == AppLanguage.ARABIC
    val isRtlText = isUrdu || isArabic

    val headlineFontFamily = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> PlayfairDisplay
    }
    val bodyFontFamily = when {
        isUrdu || isArabic -> NotoNaskhArabic
        else -> Inter
    }

    val headlineFontSize = if (isUrdu) 25.sp else 24.sp
    val headlineLineHeight = when {
        isUrdu -> 46.sp
        isArabic -> 40.sp
        else -> 34.sp
    }
    val bodyFontSize = if (isUrdu) 17.sp else 16.sp
    val bodyLineHeight = when {
        isUrdu -> 32.sp
        isArabic -> 30.sp
        else -> 26.sp
    }

    val shareArticle = {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, articleTitle)
            putExtra(Intent.EXTRA_TEXT, "$articleTitle\n\n$articleSummary\n\n${article.sourceUrl}")
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

                            Text(
                                text = article.sourceName,
                                fontFamily = PlayfairDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = InkPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = shareArticle,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("top_share_article_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "Share story",
                                    tint = InkPrimary
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
                .testTag("article_detail_dialog")
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(scrollState)
                    .navigationBarsPadding()
                    .padding(16.dp)
            ) {
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
                                val imageRequest = remember(article.imageUrl, context) {
                                    ImageRequest.Builder(context)
                                        .data(article.imageUrl)
                                        .crossfade(250)
                                        .build()
                                }
                                SubcomposeAsyncImage(
                                    model = imageRequest,
                                    contentDescription = articleTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = { ShimmerPlaceholder() },
                                    error = {
                                        BrandedSourcePlaceholder(
                                            sourceName = article.sourceName,
                                            category = article.category
                                        )
                                    }
                                )
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

                        Spacer(modifier = Modifier.height(16.dp))

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
                                        color = SoftGrey
                                    )
                                }
                            }

                            val isIslamic = article.category == NewsCategory.ISLAMIC
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
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
                                        text = article.category.englishLabel,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = if (isIslamic) DeepEmerald else SoftGrey
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 3. Headline & Summary (RTL aligned when Urdu/Arabic)
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
                                            color = InkPrimary,
                                            textAlign = TextAlign.Start,
                                            platformStyle = PlatformTextStyle(includeFontPadding = true)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    HorizontalDivider(thickness = 1.dp, color = WarmDivider)

                                    Spacer(modifier = Modifier.height(16.dp))

                                    Text(
                                        text = articleSummary,
                                        style = TextStyle(
                                            fontFamily = bodyFontFamily,
                                            fontWeight = FontWeight.Normal,
                                            fontSize = bodyFontSize,
                                            lineHeight = bodyLineHeight,
                                            color = InkPrimary,
                                            textAlign = TextAlign.Start,
                                            platformStyle = PlatformTextStyle(includeFontPadding = true)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 4. Translation & Attribution Notice
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(WarmOffWhite)
                                .border(1.dp, WarmBorder, RoundedCornerShape(12.dp))
                                .padding(16.dp)
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
                                    "${effectiveLanguage.flagEmoji} Concise editorial summary from ${article.sourceName}. Read the full dispatch on the publisher's website."
                                } else {
                                    "${effectiveLanguage.flagEmoji} Summary translated to ${effectiveLanguage.displayName}. Original report belongs to ${article.sourceName}."
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

                        Spacer(modifier = Modifier.height(24.dp))

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
                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = "Open full article at source",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Read at source",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
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
            }
        }
    }
}
