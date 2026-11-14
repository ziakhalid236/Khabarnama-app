package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.Inter
import com.example.ui.theme.PlayfairDisplay
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.WarmBorder

/**
 * Unified Monochrome Editorial Khabarnama Seal:
 * Displays the custom architectural Black & White Khabarnama monogram matching the launcher icon 1:1.
 */
@Composable
fun KhabarnamaBrandMark(
    size: Dp = 36.dp,
    emeraldColor: Color = DeepEmerald,
    goldColor: Color = WarmBorder,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
            .background(Color(0xFF0D0F0E))
            .border(1.dp, InkPrimary.copy(alpha = 0.25f), RoundedCornerShape(size * 0.26f)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.khabarnama_brand_icon_1794660803275),
            contentDescription = "Khabarnama editorial monogram",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

/**
 * Editorial Masthead Wordmark for Khabarnama:
 * Crisp Playfair Display serif masthead paired with the Monochrome Brand Emblem.
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
                    .size(92.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF0D0F0E))
                    .border(1.5.dp, InkPrimary, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.khabarnama_brand_icon_1794660803275),
                    contentDescription = "Khabarnama brand emblem",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "KHABARNAMA",
                fontFamily = PlayfairDisplay,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 32.sp,
                letterSpacing = 1.6.sp,
                color = InkPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(InkPrimary)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "GLOBAL & ISLAMIC WIRE",
                    fontFamily = Inter,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.5.sp,
                    letterSpacing = 1.4.sp,
                    color = Color.White
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            KhabarnamaBrandMark(size = 32.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Khabarnama",
                    fontFamily = PlayfairDisplay,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    letterSpacing = (-0.3).sp,
                    lineHeight = 20.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = InkPrimary
                )
                Text(
                    text = "GLOBAL & ISLAMIC WIRE",
                    fontFamily = Inter,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    softWrap = false,
                    color = SoftGrey
                )
            }
        }
    }
}
