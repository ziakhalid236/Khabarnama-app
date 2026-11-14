package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Crisp Monochrome Editorial Color Scheme (Reuters / NYT / Monocle inspired)
private val EditorialColorScheme = lightColorScheme(
    primary = DeepEmerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldTint,
    onPrimaryContainer = DeepEmeraldDark,
    secondary = MutedGold,
    onSecondary = Color.White,
    secondaryContainer = GoldTint,
    onSecondaryContainer = Color(0xFF18181B),
    background = WarmOffWhite,
    onBackground = InkPrimary,
    surface = PureWhite,
    onSurface = InkPrimary,
    surfaceVariant = WarmOffWhite,
    onSurfaceVariant = SoftGrey,
    outline = WarmBorder,
    outlineVariant = WarmDivider
)

@Composable
fun UrduNewsTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EditorialColorScheme,
        typography = AppTypography,
        content = content
    )
}
