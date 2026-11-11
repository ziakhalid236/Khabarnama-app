package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldTextDark
import com.example.ui.theme.GoldTint
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.Inter
import com.example.ui.theme.MutedGold
import com.example.ui.theme.PlayfairDisplay
import com.example.ui.theme.SoftGrey

/**
 * Unified Khabarnama Global News & Islamic Dispatch Emblem:
 * Matches the external Adaptive Launcher Icon 1:1 — featuring a Royal Emerald medallion,
 * Gold editorial ring, World News Globe with meridian & latitude grid, Golden Crescent arc,
 * and a Folded Broadsheet Newspaper card with a Gold masthead bar.
 */
@Composable
fun KhabarnamaBrandMark(
    size: Dp = 38.dp,
    emeraldColor: Color = DeepEmerald,
    goldColor: Color = MutedGold,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "Khabarnama global news globe and newspaper emblem" }
    ) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w * 0.5f, h * 0.5f)
        val medallionRadius = w * 0.48f

        // 1. Deep Royal Emerald Medallion Plate
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF136E5B),
                    emeraldColor,
                    Color(0xFF07332A)
                ),
                start = Offset.Zero,
                end = Offset(w, h)
            ),
            radius = medallionRadius,
            center = center
        )

        // Outer Gold Medallion Ring
        drawCircle(
            color = Color(0xFFD4AF37),
            radius = medallionRadius,
            center = center,
            style = Stroke(width = w * 0.035f)
        )

        // Inner Subtle Gold Ring
        drawCircle(
            color = Color(0xFFD4AF37).copy(alpha = 0.32f),
            radius = medallionRadius * 0.90f,
            center = center,
            style = Stroke(width = w * 0.016f)
        )

        // 2. Golden Crescent Arc Embracing Upper-Left of the Globe
        drawArc(
            color = Color(0xFFF0CF7B),
            startAngle = 168f,
            sweepAngle = 112f,
            useCenter = false,
            topLeft = Offset(w * 0.14f, h * 0.14f),
            size = Size(w * 0.62f, h * 0.62f),
            style = Stroke(width = w * 0.048f, cap = StrokeCap.Round)
        )

        // 3. World News Globe Sphere (Upper-Left / Center)
        val globeCenter = Offset(w * 0.43f, h * 0.44f)
        val globeRadius = w * 0.24f

        drawCircle(
            color = Color.White.copy(alpha = 0.10f),
            radius = globeRadius,
            center = globeCenter
        )

        drawCircle(
            color = Color(0xFFF7F5F0),
            radius = globeRadius,
            center = globeCenter,
            style = Stroke(width = w * 0.036f)
        )

        // Globe Vertical Meridian Oval
        drawOval(
            color = Color(0xFFE5C168),
            topLeft = Offset(globeCenter.x - globeRadius * 0.52f, globeCenter.y - globeRadius),
            size = Size(globeRadius * 1.04f, globeRadius * 2f),
            style = Stroke(width = w * 0.028f)
        )

        // Globe Central Axis & Equator
        drawLine(
            color = Color(0xFFF7F5F0).copy(alpha = 0.8f),
            start = Offset(globeCenter.x, globeCenter.y - globeRadius),
            end = Offset(globeCenter.x, globeCenter.y + globeRadius),
            strokeWidth = w * 0.022f
        )
        drawLine(
            color = Color(0xFFF7F5F0),
            start = Offset(globeCenter.x - globeRadius, globeCenter.y),
            end = Offset(globeCenter.x + globeRadius, globeCenter.y),
            strokeWidth = w * 0.026f
        )

        // Globe Upper & Lower Latitude Curves
        val latPath = Path().apply {
            moveTo(globeCenter.x - globeRadius * 0.85f, globeCenter.y - globeRadius * 0.45f)
            quadraticTo(
                globeCenter.x,
                globeCenter.y - globeRadius * 0.26f,
                globeCenter.x + globeRadius * 0.85f,
                globeCenter.y - globeRadius * 0.45f
            )
            moveTo(globeCenter.x - globeRadius * 0.85f, globeCenter.y + globeRadius * 0.45f)
            quadraticTo(
                globeCenter.x,
                globeCenter.y + globeRadius * 0.26f,
                globeCenter.x + globeRadius * 0.85f,
                globeCenter.y + globeRadius * 0.45f
            )
        }
        drawPath(
            path = latPath,
            color = Color(0xFFF7F5F0).copy(alpha = 0.85f),
            style = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
        )

        // 4. Folded Broadsheet Newspaper Card (Lower-Right Quadrant)
        // Rolled side spine
        drawRoundRect(
            color = Color(0xFFE2DACB),
            topLeft = Offset(w * 0.64f, h * 0.47f),
            size = Size(w * 0.14f, h * 0.31f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )
        drawRoundRect(
            color = Color(0xFF08382E),
            topLeft = Offset(w * 0.64f, h * 0.47f),
            size = Size(w * 0.14f, h * 0.31f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f),
            style = Stroke(width = w * 0.024f)
        )

        // Main Front Broadsheet Page
        val cardLeft = w * 0.40f
        val cardTop = h * 0.42f
        val cardWidth = w * 0.32f
        val cardHeight = h * 0.36f

        drawRoundRect(
            color = Color(0xFFFAF8F3),
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f)
        )
        drawRoundRect(
            color = Color(0xFF08382E),
            topLeft = Offset(cardLeft, cardTop),
            size = Size(cardWidth, cardHeight),
            cornerRadius = CornerRadius(w * 0.035f, w * 0.035f),
            style = Stroke(width = w * 0.026f)
        )

        // Gold Newspaper Masthead Header Bar
        drawRoundRect(
            color = goldColor,
            topLeft = Offset(cardLeft + w * 0.045f, cardTop + h * 0.045f),
            size = Size(cardWidth - w * 0.09f, h * 0.06f),
            cornerRadius = CornerRadius(w * 0.01f, w * 0.01f)
        )

        // Newspaper Headline & Editorial Columns in Deep Emerald
        val lineStartX = cardLeft + w * 0.05f
        val lineEndX = cardLeft + cardWidth - w * 0.05f
        drawLine(
            color = emeraldColor,
            start = Offset(lineStartX, cardTop + h * 0.155f),
            end = Offset(lineEndX, cardTop + h * 0.155f),
            strokeWidth = w * 0.032f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = emeraldColor,
            start = Offset(lineStartX, cardTop + h * 0.22f),
            end = Offset(lineEndX, cardTop + h * 0.22f),
            strokeWidth = w * 0.025f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = emeraldColor,
            start = Offset(lineStartX, cardTop + h * 0.285f),
            end = Offset(cardLeft + cardWidth * 0.68f, cardTop + h * 0.285f),
            strokeWidth = w * 0.025f,
            cap = StrokeCap.Round
        )

        // 5. Live Broadcast Gold Pulse Point at Upper-Right of Globe
        drawCircle(
            color = Color(0xFFF0CF7B),
            radius = w * 0.036f,
            center = Offset(w * 0.65f, h * 0.27f)
        )
    }
}

/**
 * Editorial Wordmark for Khabarnama:
 * Heavy Playfair Display masthead paired with the unified Global News & Newspaper Emblem.
 */
@Composable
fun KhabarnamaWordmark(
    modifier: Modifier = Modifier,
    isSplash: Boolean = false
) {
    if (isSplash) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(GoldTint)
                    .border(1.5.dp, GoldBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                KhabarnamaBrandMark(size = 76.dp)
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Khabarnama",
                fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 36.sp,
                letterSpacing = (-0.4).sp,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(GoldTint)
                    .border(1.dp, GoldBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Global & Islamic News Network",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    letterSpacing = 0.4.sp,
                    color = GoldTextDark
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KhabarnamaBrandMark(size = 30.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Khabarnama",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.2).sp,
                    lineHeight = 19.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = InkPrimary
                )
                Text(
                    text = "Global & Islamic Dispatch",
                    fontFamily = Inter,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    letterSpacing = 0.1.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = DeepEmerald
                )
            }
        }
    }
}
