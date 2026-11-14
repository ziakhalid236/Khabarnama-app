package com.example.ui.components

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.util.PrayerTimesUtil

@Composable
fun FeaturedIslamicCarousel(
    articles: List<NewsArticle>,
    currentLanguage: AppLanguage,
    onArticleClick: (NewsArticle) -> Unit,
    onSourceBadgeClick: (NewsArticle) -> Unit,
    onRetryTranslation: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (articles.isEmpty()) return

    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { articles.size })

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Section Header (Aligned with 16dp feed padding)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MutedGold)
                )

                Text(
                    text = "Islamic Highlights",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = InkPrimary,
                    maxLines = 1
                )
            }

            // Clean page indicator (e.g. "1 of 4") that never wraps
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(GoldTint)
                    .border(1.dp, GoldBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${articles.size}",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = GoldTextDark
                )
            }
        }

        // Manual Horizontal Pager (no auto-scroll) with 24dp right peek and 16dp spacing
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(end = 24.dp),
            pageSpacing = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("featured_islamic_carousel")
        ) { page ->
            val article = articles[page]
            val interactionSource = remember(article.id) { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.98f else 1f,
                animationSpec = tween(durationMillis = 150),
                label = "featured_card_press_scale"
            )

            val hasTargetTranslation = article.hasTranslation(currentLanguage)
            val effectiveLanguage = if (hasTargetTranslation) currentLanguage else AppLanguage.ENGLISH
            val rawTitle = article.getTitle(effectiveLanguage)
            val articleSummary = article.getSummary(effectiveLanguage)
            val isUrdu = effectiveLanguage == AppLanguage.URDU
            val isArabic = effectiveLanguage == AppLanguage.ARABIC
            val isRtlText = isUrdu || isArabic

            val cleanHeadline = remember(rawTitle) { rawTitle.trim() }
            val cleanLeadSummary = remember(rawTitle, articleSummary) {
                val t = rawTitle.trim()
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
            val storyFontFamily = when {
                isUrdu -> NotoNastaliqUrdu
                isArabic -> NotoNaskhArabic
                else -> Inter
            }

            val headlineFontSize = when {
                isUrdu -> 18.5.sp
                isArabic -> 17.sp
                else -> 16.5.sp
            }
            val headlineLineHeight = when {
                isUrdu -> 37.sp
                isArabic -> 29.sp
                else -> 22.5.sp
            }

            val storyFontSize = when {
                isUrdu -> 15.5.sp
                isArabic -> 14.5.sp
                else -> 13.5.sp
            }
            val storyLineHeight = when {
                isUrdu -> 32.sp
                isArabic -> 26.sp
                else -> 19.5.sp
            }

            val storyLetterSpacing = when {
                isUrdu -> 0.3.sp
                isArabic -> 0.15.sp
                else -> 0.05.sp
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.2.dp, InkPrimary, RoundedCornerShape(18.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null
                    ) { onArticleClick(article) }
                    .testTag("featured_card_${article.id}"),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PureWhite)
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Cover Image (16:9) with Dark 55% Translucent Source Badge
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            if (article.hasValidImage) {
                                var isCarouselImgLoading by remember(article.imageUrl) { mutableStateOf(true) }
                                var isCarouselImgError by remember(article.imageUrl) { mutableStateOf(false) }

                                val imageRequest = remember(article.imageUrl, context) {
                                    ImageRequest.Builder(context)
                                        .data(article.imageUrl)
                                        .size(960, 540)
                                        .crossfade(true)
                                        .memoryCacheKey("carousel_img_${article.id}")
                                        .diskCacheKey("carousel_img_${article.id}")
                                        .dispatcher(Dispatchers.IO)
                                        .allowHardware(true)
                                        .build()
                                }
                                AsyncImage(
                                    model = imageRequest,
                                    contentDescription = rawTitle,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    onLoading = {
                                        isCarouselImgLoading = true
                                        isCarouselImgError = false
                                    },
                                    onSuccess = {
                                        isCarouselImgLoading = false
                                        isCarouselImgError = false
                                    },
                                    onError = {
                                        isCarouselImgLoading = false
                                        isCarouselImgError = true
                                    }
                                )
                                if (isCarouselImgLoading) {
                                    ShimmerPlaceholder()
                                } else if (isCarouselImgError) {
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

                            if (article.isScholarlySource) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PureWhite.copy(alpha = 0.94f))
                                        .border(0.8.dp, InkPrimary, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Scholarly",
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        color = InkPrimary
                                    )
                                }
                            }

                            // Dark 55% translucent pill for image source
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .minimumInteractiveComponentSize()
                                    .clickable { onSourceBadgeClick(article) },
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
                                        contentDescription = "Image source attribution for ${article.sourceName}",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Publisher & Meta Row (always LTR)
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
                                    Text(
                                        text = article.sourceName,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp,
                                        color = DeepEmerald,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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
                                        fontSize = 12.sp,
                                        color = SoftGrey,
                                        maxLines = 1
                                    )
                                }

                                article.islamicSection?.let { section ->
                                    Box(
                                        modifier = Modifier
                                            .padding(start = 8.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White)
                                            .border(1.dp, Color.Black, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            RealisticIslamicBlackEmblem(size = 13.dp)
                                            Text(
                                                text = section.englishLabel,
                                                fontFamily = Inter,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.5.sp,
                                                color = Color.Black
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Headline & Summary with translation shimmer or fallback label
                        if (article.isTranslating && currentLanguage != AppLanguage.ENGLISH) {
                            TranslationTextShimmer(lineCount = 2)
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
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (cleanLeadSummary.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(if (isUrdu) 3.dp else 5.dp))
                                        Text(
                                            text = cleanLeadSummary,
                                            style = TextStyle(
                                                fontFamily = storyFontFamily,
                                                fontWeight = FontWeight.Normal,
                                                fontSize = storyFontSize,
                                                lineHeight = storyLineHeight,
                                                letterSpacing = storyLetterSpacing,
                                                color = InkPrimary.copy(alpha = 0.82f),
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
                    }
                }
            }
        }

        // Page indicator dots below the carousel (one clearly visible dot per page)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .testTag("featured_carousel_dots"),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(articles.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .width(if (isSelected) 16.dp else 8.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) DeepEmerald
                            else SoftGrey.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}
