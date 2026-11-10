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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Translate
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
import androidx.compose.runtime.remember
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
import coil.compose.SubcomposeAsyncImage
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

    val headlineFontFamily = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> PlayfairDisplay
    }

    val bodyFontFamily = when {
        isUrdu || isArabic -> NotoNaskhArabic
        else -> Inter
    }

    // Minimum sizes: headline 20sp, summary 15sp, meta text 12sp; generous line height for Urdu/Arabic
    val headlineFontSize = if (isUrdu) 21.sp else 20.sp
    val headlineLineHeight = when {
        isUrdu -> 40.sp
        isArabic -> 34.sp
        else -> 28.sp
    }
    val bodyFontSize = if (isUrdu) 16.sp else 15.sp
    val bodyLineHeight = when {
        isUrdu -> 30.sp
        isArabic -> 28.sp
        else -> 24.sp
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
                            loading = {
                                ShimmerPlaceholder()
                            },
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

                        // Normal-case category pill (no tiny all-caps labels)
                        Box(
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isIslamic) GoldTint else EmeraldTint)
                                .border(
                                    1.dp,
                                    if (isIslamic) GoldBorder else DeepEmerald.copy(alpha = 0.22f),
                                    RoundedCornerShape(12.dp)
                                )
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
                                    color = if (isIslamic) GoldTextDark else DeepEmerald
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
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = articleSummary,
                                style = TextStyle(
                                    fontFamily = bodyFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = bodyFontSize,
                                    lineHeight = bodyLineHeight,
                                    color = SoftGrey,
                                    textAlign = TextAlign.Start,
                                    platformStyle = PlatformTextStyle(includeFontPadding = true)
                                ),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
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
