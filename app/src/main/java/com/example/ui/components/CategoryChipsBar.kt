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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldTextDark
import com.example.ui.theme.GoldTint
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.Inter
import com.example.ui.theme.MutedGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmOffWhite

/**
 * Returns the realistic country flag or emblem emoji for each NewsCategory.
 */
fun categoryFlagEmoji(category: NewsCategory): String = when (category) {
    NewsCategory.ALL -> "🌐"
    NewsCategory.PALESTINE -> "🇵🇸"
    NewsCategory.ISLAMIC -> "🕋"
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
 * Realistic Black & Gold Islamic Emblem Flag/Badge (Black standard with gold border, crescent & star).
 */
@Composable
fun RealisticIslamicBlackEmblem(
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(4.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF0B0D10),
                        Color(0xFF181C22),
                        Color(0xFF0A0C0E)
                    )
                )
            )
            .border(0.8.dp, Color(0xFFD4AF37), RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val goldColor = Color(0xFFF5D77F)
            val richGold = Color(0xFFD4AF37)

            // Subtle top gold ornamental band (Kiswah / Black Standard style)
            drawRect(
                color = richGold.copy(alpha = 0.85f),
                topLeft = Offset(w * 0.12f, h * 0.16f),
                size = Size(w * 0.76f, h * 0.10f)
            )

            // Crisp Gold Crescent in the center-lower area
            val cx = w * 0.48f
            val cy = h * 0.60f
            val outerR = minOf(w, h) * 0.26f
            drawCircle(
                color = goldColor,
                radius = outerR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color(0xFF12151A),
                radius = outerR * 0.80f,
                center = Offset(cx + outerR * 0.34f, cy - outerR * 0.12f)
            )

            // Gold Star next to crescent
            drawCircle(
                color = PureWhite,
                radius = outerR * 0.22f,
                center = Offset(cx + outerR * 0.56f, cy - outerR * 0.18f)
            )
        }
    }
}

/**
 * Realistic Flag or Full-Color Emblem composable for news cards, carousels, and category chips.
 * Displays real full-color country flags (🇵🇰, 🇵🇸), the Black & Gold Islamic emblem, and vivid category symbols.
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
                size = size,
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
                                isSelected -> MutedGold
                                isIslamic -> Color(0xFF22262C)
                                else -> WarmBorder
                            },
                            animationSpec = tween(durationMillis = 200),
                            label = "chip_border_${category.name}"
                        )
                        val chipTextColor by animateColorAsState(
                            targetValue = when {
                                isSelected -> Color.White
                                isIslamic -> Color(0xFFF5E3B3)
                                else -> InkPrimary
                            },
                            animationSpec = tween(durationMillis = 200),
                            label = "chip_text_${category.name}"
                        )

                        val cardShape = RoundedCornerShape(14.dp)
                        val backgroundBrush = when {
                            isIslamic && isSelected -> Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF0A0C0E),
                                    Color(0xFF1A1E24),
                                    Color(0xFF0D1117)
                                )
                            )
                            isIslamic -> Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF12151A),
                                    Color(0xFF1F242C)
                                )
                            )
                            isSelected -> Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF146E5C),
                                    DeepEmerald,
                                    Color(0xFF0A4237)
                                )
                            )
                            else -> Brush.linearGradient(
                                colors = listOf(
                                    PureWhite,
                                    Color(0xFFFCFBF8)
                                )
                            )
                        }

                        val iconBadgeBg = when {
                            isIslamic -> Color(0xFF0B0D10)
                            isSelected -> Color.White.copy(alpha = 0.20f)
                            else -> EmeraldTint.copy(alpha = 0.75f)
                        }

                        Box(
                            modifier = Modifier
                                .heightIn(min = 38.dp)
                                .shadow(
                                    elevation = if (isSelected) 3.dp else 1.dp,
                                    shape = cardShape,
                                    ambientColor = DeepEmerald.copy(alpha = 0.14f),
                                    spotColor = DeepEmerald.copy(alpha = 0.16f)
                                )
                                .clip(cardShape)
                                .background(backgroundBrush)
                                .border(
                                    width = if (isSelected || isIslamic) 1.2.dp else 1.dp,
                                    color = if (isIslamic) MutedGold else chipBorderColor,
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
                                            width = 0.8.dp,
                                            color = when {
                                                isIslamic -> MutedGold
                                                isSelected -> MutedGold.copy(alpha = 0.75f)
                                                else -> DeepEmerald.copy(alpha = 0.18f)
                                            },
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isIslamic) {
                                        RealisticIslamicBlackEmblem(size = 15.dp)
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

                                // Islamic chip retains its signature gold accent dot
                                if (isIslamic) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF5D77F))
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

            // Secondary Islamic Subcategory Mini-Cards with realistic flags/emblems and smooth transition
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
                                targetValue = if (isSubSelected) Color(0xFF15191F) else PureWhite,
                                animationSpec = tween(durationMillis = 200),
                                label = "sub_chip_bg_${section.name}"
                            )
                            val subBorderColor by animateColorAsState(
                                targetValue = if (isSubSelected) MutedGold else GoldBorder,
                                animationSpec = tween(durationMillis = 200),
                                label = "sub_chip_border_${section.name}"
                            )
                            val subTextColor by animateColorAsState(
                                targetValue = if (isSubSelected) Color(0xFFF5E3B3) else InkPrimary,
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
                                    Text(
                                        text = islamicSectionEmoji(section),
                                        style = TextStyle(
                                            fontSize = 12.5.sp,
                                            textAlign = TextAlign.Center,
                                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                                        )
                                    )
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
