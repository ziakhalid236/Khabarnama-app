package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Calm, Editorial Premium Color Scheme (Apple News / The Economist / Al Jazeera vibe)
private val EditorialColorScheme = lightColorScheme(
    primary = DeepEmerald,
    onPrimary = Color.White,
    primaryContainer = EmeraldTint,
    onPrimaryContainer = DeepEmeraldDark,
    secondary = MutedGold,
    onSecondary = Color.White,
    secondaryContainer = GoldTint,
    onSecondaryContainer = Color(0xFF4A3A12),
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
