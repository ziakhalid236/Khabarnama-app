package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
 * Returns the dedicated Material icon for each NewsCategory.
 */
fun categoryIcon(category: NewsCategory): ImageVector = when (category) {
    NewsCategory.ALL -> Icons.Default.Newspaper
    NewsCategory.ISLAMIC -> Icons.Default.Mosque
    NewsCategory.PAKISTAN -> Icons.Default.Flag
    NewsCategory.WORLD -> Icons.Default.Public
    NewsCategory.BUSINESS -> Icons.AutoMirrored.Filled.TrendingUp
    NewsCategory.SPORTS -> Icons.Default.SportsCricket
    NewsCategory.TECHNOLOGY -> Icons.Default.Memory
}

/**
 * Returns the dedicated Material icon for each IslamicSection sub-category.
 */
fun islamicSectionIcon(section: IslamicSection): ImageVector = when (section) {
    IslamicSection.ALL -> Icons.Default.AutoAwesome
    IslamicSection.MUSLIM_WORLD -> Icons.Default.Public
    IslamicSection.HAJJ_UMRAH -> Icons.Default.Mosque
    IslamicSection.ISLAMIC_EVENTS -> Icons.Default.Event
    IslamicSection.FATWA_SCHOLARS -> Icons.AutoMirrored.Filled.MenuBook
    IslamicSection.RAMADAN_EID -> Icons.Default.NightsStay
}

/**
 * Reusable Category Icon composable for news cards, carousels, and category chips.
 */
@Composable
fun CategoryBadgeIcon(
    category: NewsCategory,
    tint: Color,
    size: Dp = 13.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        imageVector = categoryIcon(category),
        contentDescription = null,
        tint = tint,
        modifier = modifier.size(size)
    )
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
                                isIslamic -> GoldBorder
                                else -> WarmBorder
                            },
                            animationSpec = tween(durationMillis = 200),
                            label = "chip_border_${category.name}"
                        )
                        val chipTextColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else InkPrimary,
                            animationSpec = tween(durationMillis = 200),
                            label = "chip_text_${category.name}"
                        )

                        val cardShape = RoundedCornerShape(14.dp)
                        val backgroundBrush = if (isSelected) {
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF146E5C),
                                    DeepEmerald,
                                    Color(0xFF0A4237)
                                )
                            )
                        } else if (isIslamic) {
                            Brush.linearGradient(
                                colors = listOf(
                                    PureWhite,
                                    GoldTint.copy(alpha = 0.55f)
                                )
                            )
                        } else {
                            Brush.linearGradient(
                                colors = listOf(
                                    PureWhite,
                                    Color(0xFFFCFBF8)
                                )
                            )
                        }

                        val iconBadgeBg = when {
                            isSelected -> Color.White.copy(alpha = 0.16f)
                            isIslamic -> GoldTint
                            else -> EmeraldTint
                        }
                        val iconTint = when {
                            isSelected -> Color(0xFFF5E3B3)
                            isIslamic -> GoldTextDark
                            else -> DeepEmerald
                        }

                        Box(
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .shadow(
                                    elevation = if (isSelected) 3.dp else 1.dp,
                                    shape = cardShape,
                                    ambientColor = DeepEmerald.copy(alpha = 0.14f),
                                    spotColor = DeepEmerald.copy(alpha = 0.16f)
                                )
                                .clip(cardShape)
                                .background(backgroundBrush)
                                .border(
                                    width = if (isSelected) 1.2.dp else 1.dp,
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
                                // Dedicated Icon Badge inside the Category Mini-Card
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(iconBadgeBg)
                                        .border(
                                            width = 0.75.dp,
                                            color = if (isSelected) MutedGold.copy(alpha = 0.7f) else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(category),
                                        contentDescription = null,
                                        tint = iconTint,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }

                                // Islamic chip also retains its signature muted gold accent dot
                                if (isIslamic) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFFF5E3B3) else MutedGold)
                                    )
                                }

                                Text(
                                    text = category.englishLabel,
                                    fontFamily = Inter,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 12.5.sp,
                                    color = chipTextColor
                                )
                            }
                        }
                    }
                }

                // Soft right-edge fade indicating more horizontally scrollable category cards
                if (scrollState.canScrollForward || scrollState.maxValue == 0) {
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

            // Secondary Islamic Subcategory Mini-Cards with relevant icons and smooth transition
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
                                targetValue = if (isSubSelected) EmeraldTint else PureWhite,
                                animationSpec = tween(durationMillis = 200),
                                label = "sub_chip_bg_${section.name}"
                            )
                            val subBorderColor by animateColorAsState(
                                targetValue = if (isSubSelected) DeepEmerald else GoldBorder,
                                animationSpec = tween(durationMillis = 200),
                                label = "sub_chip_border_${section.name}"
                            )
                            val subTextColor by animateColorAsState(
                                targetValue = if (isSubSelected) DeepEmerald else SoftGrey,
                                animationSpec = tween(durationMillis = 200),
                                label = "sub_chip_text_${section.name}"
                            )

                            val subShape = RoundedCornerShape(12.dp)

                            Box(
                                modifier = Modifier
                                    .heightIn(min = 32.dp)
                                    .shadow(
                                        elevation = if (isSubSelected) 1.5.dp else 0.5.dp,
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
                                    Icon(
                                        imageVector = islamicSectionIcon(section),
                                        contentDescription = null,
                                        tint = if (isSubSelected) DeepEmerald else MutedGold,
                                        modifier = Modifier.size(13.dp)
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

                    if (subScrollState.canScrollForward || subScrollState.maxValue == 0) {
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
