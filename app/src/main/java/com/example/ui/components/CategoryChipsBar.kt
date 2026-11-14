package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IslamicSection
import com.example.model.NewsCategory
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.Inter
import com.example.ui.theme.MutedGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmOffWhite
import kotlin.math.cos
import kotlin.math.sin

/**
 * Returns the realistic country flag or emblem emoji for each NewsCategory.
 */
fun categoryFlagEmoji(category: NewsCategory): String = when (category) {
    NewsCategory.ALL -> "🌐"
    NewsCategory.PALESTINE -> "🇵🇸"
    NewsCategory.ISLAMIC -> "☪"
    NewsCategory.PAKISTAN -> "🇵🇰"
    NewsCategory.SECURITY -> "🛡️"
    NewsCategory.WORLD -> "🌍"
    NewsCategory.TECHNOLOGY -> "💻"
    NewsCategory.BUSINESS -> "📈"
    NewsCategory.SPORTS -> "🏏"
}

/**
 * Returns the realistic flag or emblem emoji for each IslamicSection sub-category.
 */
fun islamicSectionEmoji(section: IslamicSection): String = when (section) {
    IslamicSection.ALL -> "✨"
    IslamicSection.PALESTINE_QUDS -> "🇵🇸"
    IslamicSection.MUSLIM_WORLD -> "🌍"
    IslamicSection.HAJJ_UMRAH -> "🕋"
    IslamicSection.ISLAMIC_EVENTS -> "🕌"
    IslamicSection.FATWA_SCHOLARS -> "📚"
    IslamicSection.RAMADAN_EID -> "🌙"
}

/**
 * Returns the dedicated Material icon for each NewsCategory (retained for compatibility).
 */
fun categoryIcon(category: NewsCategory): ImageVector = when (category) {
    NewsCategory.ALL -> Icons.Default.Newspaper
    NewsCategory.PALESTINE -> Icons.Default.LocalFireDepartment
    NewsCategory.ISLAMIC -> Icons.Default.Mosque
    NewsCategory.PAKISTAN -> Icons.Default.Flag
    NewsCategory.SECURITY -> Icons.Default.Security
    NewsCategory.WORLD -> Icons.Default.Language
    NewsCategory.TECHNOLOGY -> Icons.Default.Memory
    NewsCategory.BUSINESS -> Icons.Default.BusinessCenter
    NewsCategory.SPORTS -> Icons.Default.SportsCricket
}

/**
 * Returns the dedicated Material icon for each IslamicSection sub-category (retained for compatibility).
 */
fun islamicSectionIcon(section: IslamicSection): ImageVector = when (section) {
    IslamicSection.ALL -> Icons.Default.AutoAwesome
    IslamicSection.PALESTINE_QUDS -> Icons.Default.Mosque
    IslamicSection.MUSLIM_WORLD -> Icons.Default.Public
    IslamicSection.HAJJ_UMRAH -> Icons.Default.Mosque
    IslamicSection.ISLAMIC_EVENTS -> Icons.Default.Event
    IslamicSection.FATWA_SCHOLARS -> Icons.AutoMirrored.Filled.MenuBook
    IslamicSection.RAMADAN_EID -> Icons.Default.NightsStay
}

/**
 * Realistic White & Black Islamic Crescent-and-Star Flag Emblem
 * Matches the user's reference image: pure white background with a sharp black crescent moon
 * on the left opening to the right, and a sharp 5-pointed black star on the right.
 */
@Composable
fun RealisticIslamicBlackEmblem(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(PureWhite)
            .border(1.dp, Color(0xFF111111), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val minDim = minOf(w, h)

            // Draw pure white flag canvas
            drawCircle(
                color = Color.White,
                radius = minDim * 0.48f,
                center = Offset(w * 0.5f, h * 0.5f)
            )

            // Sharp Black Crescent Moon using Path.op difference
            val outerRadius = minDim * 0.35f
            val outerCenter = Offset(w * 0.46f, h * 0.50f)
            val innerRadius = minDim * 0.285f
            val innerCenter = Offset(w * 0.565f, h * 0.475f)

            val outerCircle = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center = outerCenter,
                        radius = outerRadius
                    )
                )
            }
            val innerCircle = Path().apply {
                addOval(
                    androidx.compose.ui.geometry.Rect(
                        center = innerCenter,
                        radius = innerRadius
                    )
                )
            }
            val crescentPath = Path().apply {
                op(outerCircle, innerCircle, PathOperation.Difference)
            }
            drawPath(
                path = crescentPath,
                color = Color.Black
            )

            // Sharp 5-Pointed Black Star inside the crescent opening on the right
            val starCenterX = w * 0.65f
            val starCenterY = h * 0.46f
            val starOuterR = minDim * 0.145f
            val starInnerR = starOuterR * 0.382f
            val starPath = Path()
            val startAngleRad = Math.toRadians(-90.0)

            for (i in 0 until 10) {
                val r = if (i % 2 == 0) starOuterR else starInnerR
                val angle = startAngleRad + i * Math.PI / 5.0
                val px = starCenterX + (r * cos(angle)).toFloat()
                val py = starCenterY + (r * sin(angle)).toFloat()
                if (i == 0) {
                    starPath.moveTo(px, py)
                } else {
                    starPath.lineTo(px, py)
                }
            }
            starPath.close()

            drawPath(
                path = starPath,
                color = Color.Black
            )
        }
    }
}

/**
 * Realistic Flag or Full-Color Emblem composable for news cards, carousels, and category chips.
 * Displays real full-color country flags (🇵🇰, 🇵🇸), the White & Black Islamic Crescent-and-Star flag, and vivid category symbols.
 */
@Composable
fun CategoryBadgeIcon(
    category: NewsCategory,
    tint: Color = Color.Unspecified,
    size: Dp = 15.dp,
    modifier: Modifier = Modifier
) {
    when (category) {
        NewsCategory.ISLAMIC -> {
            RealisticIslamicBlackEmblem(
                size = size + 2.dp,
                modifier = modifier
            )
        }
        else -> {
            val emoji = categoryFlagEmoji(category)
            Box(
                modifier = modifier.size(size + 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = TextStyle(
                        fontSize = (size.value * 0.92f).sp,
                        textAlign = TextAlign.Center,
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

@Composable
fun CategoryChipsBar(
    selectedCategory: NewsCategory,
    selectedIslamicSection: IslamicSection,
    onCategorySelected: (NewsCategory) -> Unit,
    onIslamicSectionSelected: (IslamicSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = WarmOffWhite,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            // Main category mini-cards row with soft right-edge fade overlay
            val scrollState = rememberScrollState()
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(start = 14.dp, end = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NewsCategory.entries.forEach { category ->
                        val isSelected = category == selectedCategory
                        val isIslamic = category == NewsCategory.ISLAMIC

                        val chipBorderColor by animateColorAsState(
                            targetValue = when {
                                isIslamic && isSelected -> Color(0xFF0A0A0B)
                                isIslamic -> Color(0xFF18181B)
                                isSelected -> Color(0xFF0A0A0B)
                                else -> WarmBorder
                            },
                            animationSpec = tween(durationMillis = 200),
                            label = "chip_border_${category.name}"
                        )
                        val chipTextColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> Color.White
                                else -> InkPrimary
                            },
                            animationSpec = tween(durationMillis = 200),
                            label = "chip_text_${category.name}"
                        )

                        val cardShape = RoundedCornerShape(14.dp)
                        val chipBackground = when {
                            isSelected -> Color(0xFF0D0D0E)
                            else -> PureWhite
                        }

                        val iconBadgeBg = when {
                            isIslamic -> Color.White
                            isSelected -> Color.White.copy(alpha = 0.16f)
                            else -> EmeraldTint
                        }

                        Box(
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .shadow(
                                    elevation = if (isSelected) 2.dp else 0.5.dp,
                                    shape = cardShape,
                                    ambientColor = Color.Black.copy(alpha = 0.08f),
                                    spotColor = Color.Black.copy(alpha = 0.10f)
                                )
                                .clip(cardShape)
                                .background(chipBackground)
                                .border(
                                    width = if (isSelected || isIslamic) 1.2.dp else 1.dp,
                                    color = chipBorderColor,
                                    shape = cardShape
                                )
                                .clickable { onCategorySelected(category) }
                                .semantics {
                                    role = Role.Tab
                                    selected = isSelected
                                }
                                .padding(start = 6.dp, end = 12.dp, top = 5.dp, bottom = 5.dp)
                                .testTag("category_chip_${category.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Realistic Flag / Emblem Badge inside the Category Mini-Card
                                Box(
                                    modifier = Modifier
                                        .size(25.dp)
                                        .clip(CircleShape)
                                        .background(iconBadgeBg)
                                        .border(
                                            width = if (isIslamic) 1.dp else 0.8.dp,
                                            color = when {
                                                isIslamic -> Color.Black
                                                isSelected -> Color.White.copy(alpha = 0.25f)
                                                else -> WarmBorder
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isIslamic) {
                                        RealisticIslamicBlackEmblem(size = 23.dp)
                                    } else {
                                        Text(
                                            text = categoryFlagEmoji(category),
                                            style = TextStyle(
                                                fontSize = 14.sp,
                                                textAlign = TextAlign.Center,
                                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                                            )
                                        )
                                    }
                                }

                                // Islamic chip retains a crisp black/white accent dot
                                if (isIslamic) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color.White else Color.Black)
                                    )
                                }

                                Text(
                                    text = category.englishLabel,
                                    fontFamily = Inter,
                                    fontWeight = if (isSelected || isIslamic) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = chipTextColor
                                )
                            }
                        }
                    }
                }

                // Soft right-edge fade indicating more horizontally scrollable category cards
                val showRightFade by remember {
                    derivedStateOf { scrollState.canScrollForward || scrollState.maxValue == 0 }
                }
                if (showRightFade) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .width(32.dp)
                            .heightIn(min = 44.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        WarmOffWhite.copy(alpha = 0.85f),
                                        WarmOffWhite
                                    )
                                )
                            )
                            .testTag("category_chips_right_fade")
                    )
                }
            }

            // Secondary Islamic Subcategory Mini-Cards with white & black styling and smooth transition
            AnimatedVisibility(
                visible = selectedCategory == NewsCategory.ISLAMIC,
                enter = fadeIn(animationSpec = tween(220)) + expandVertically(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(180)) + shrinkVertically(animationSpec = tween(180))
            ) {
                val subScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(subScrollState)
                            .padding(start = 16.dp, end = 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IslamicSection.entries.forEach { section ->
                            val isSubSelected = section == selectedIslamicSection

                            val subBgColor by animateColorAsState(
                                targetValue = if (isSubSelected) Color(0xFF111111) else PureWhite,
                                animationSpec = tween(durationMillis = 200),
                                label = "sub_chip_bg_${section.name}"
                            )
                            val subBorderColor by animateColorAsState(
                                targetValue = if (isSubSelected) Color.Black else Color(0xFF333333),
                                animationSpec = tween(durationMillis = 200),
                                label = "sub_chip_border_${section.name}"
                            )
                            val subTextColor by animateColorAsState(
                                targetValue = if (isSubSelected) Color.White else Color.Black,
                                animationSpec = tween(durationMillis = 200),
                                label = "sub_chip_text_${section.name}"
                            )

                            val subShape = RoundedCornerShape(12.dp)

                            Box(
                                modifier = Modifier
                                    .heightIn(min = 32.dp)
                                    .shadow(
                                        elevation = if (isSubSelected) 2.dp else 0.5.dp,
                                        shape = subShape
                                    )
                                    .clip(subShape)
                                    .background(subBgColor)
                                    .border(
                                        width = if (isSubSelected) 1.2.dp else 1.dp,
                                        color = subBorderColor,
                                        shape = subShape
                                    )
                                    .clickable { onIslamicSectionSelected(section) }
                                    .semantics {
                                        role = Role.Tab
                                        selected = isSubSelected
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("islamic_subchip_${section.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    RealisticIslamicBlackEmblem(size = 14.dp)
                                    Text(
                                        text = section.englishLabel,
                                        fontFamily = Inter,
                                        fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                        color = subTextColor
                                    )
                                }
                            }
                        }
                    }

                    val showSubRightFade by remember {
                        derivedStateOf { subScrollState.canScrollForward || subScrollState.maxValue == 0 }
                    }
                    if (showSubRightFade) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .width(32.dp)
                                .heightIn(min = 38.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            WarmOffWhite.copy(alpha = 0.85f),
                                            WarmOffWhite
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}
