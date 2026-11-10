package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NewsCategory
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldTint
import com.example.ui.theme.Inter
import com.example.ui.theme.MutedGold
import com.example.ui.theme.PlayfairDisplay
import com.example.ui.theme.PureWhite
import com.example.ui.theme.ShimmerBase
import com.example.ui.theme.ShimmerHighlight
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmDivider

/**
 * Subtle skeleton shimmer modifier for premium content loading
 */
@Composable
fun Modifier.shimmerEffect(): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        ShimmerBase,
        ShimmerHighlight,
        ShimmerBase
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim.value, y = translateAnim.value)
    )

    return this.background(brush)
}

@Composable
fun ShimmerPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().shimmerEffect())
}

@Composable
fun BrandedSourcePlaceholder(
    sourceName: String,
    category: NewsCategory,
    modifier: Modifier = Modifier
) {
    val gradientColors = when (category) {
        NewsCategory.ISLAMIC -> listOf(
            Color(0xFF0A3E34),
            Color(0xFF0F5C4D),
            Color(0xFF196B5A)
        )
        NewsCategory.PAKISTAN -> listOf(
            Color(0xFF0B4635),
            Color(0xFF135E46),
            Color(0xFF1E7056)
        )
        NewsCategory.WORLD -> listOf(
            Color(0xFF142838),
            Color(0xFF1D3B52),
            Color(0xFF274D68)
        )
        NewsCategory.BUSINESS -> listOf(
            Color(0xFF3A2B18),
            Color(0xFF523D22),
            Color(0xFF6B502E)
        )
        NewsCategory.SPORTS -> listOf(
            Color(0xFF0D4448),
            Color(0xFF145E63),
            Color(0xFF1E757B)
        )
        NewsCategory.TECHNOLOGY -> listOf(
            Color(0xFF1A243B),
            Color(0xFF253454),
            Color(0xFF32466E)
        )
        NewsCategory.ALL -> listOf(
            Color(0xFF0A3E34),
            Color(0xFF0F5C4D),
            Color(0xFF186858)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(colors = gradientColors)),
        contentAlignment = Alignment.Center
    ) {
        // Subtle editorial geometric rings in the background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = Color.White.copy(alpha = 0.08f)
            val goldAccent = MutedGold.copy(alpha = 0.18f)
            drawCircle(
                color = strokeColor,
                radius = size.minDimension * 0.58f,
                center = Offset(size.width * 0.85f, size.height * 0.2f),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = goldAccent,
                radius = size.minDimension * 0.42f,
                center = Offset(size.width * 0.15f, size.height * 0.85f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, MutedGold.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                KhabarnamaBrandMark(size = 24.dp)
            }

            Text(
                text = sourceName,
                fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(0.5.dp, MutedGold.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${category.englishLabel} Dispatch",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF3DFA2),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun TranslationTextShimmer(
    lineCount: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .testTag("translation_shimmer"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(24.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        repeat(lineCount) { idx ->
            val widthFraction = if (idx == lineCount - 1) 0.6f else 0.96f
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }
    }
}

@Composable
fun OriginalLanguageBadge(
    modifier: Modifier = Modifier,
    onRetryClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(GoldTint)
            .border(1.dp, GoldBorder, RoundedCornerShape(8.dp))
            .then(
                if (onRetryClick != null) {
                    Modifier.clickable { onRetryClick() }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag("original_language_label"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (onRetryClick != null) "Original language • Tap to translate" else "Original language",
            fontFamily = Inter,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            color = SoftGrey
        )
        if (onRetryClick != null) {
            Icon(
                imageVector = Icons.Outlined.Refresh,
                contentDescription = "Retry translation",
                tint = DeepEmerald,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun NewsCardSkeleton(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, WarmBorder, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .shimmerEffect()
                )
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .shimmerEffect()
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TranslationTextShimmer(lineCount = 3)
        }
    }
}
