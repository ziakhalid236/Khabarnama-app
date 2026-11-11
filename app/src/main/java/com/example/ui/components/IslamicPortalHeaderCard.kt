package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepEmeraldDark
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldTint
import com.example.ui.theme.Inter
import com.example.ui.theme.MutedGold
import com.example.ui.theme.NotoNaskhArabic
import com.example.ui.theme.NotoNastaliqUrdu
import com.example.ui.theme.PlayfairDisplay
import com.example.ui.theme.PureWhite

@Composable
fun IslamicPortalHeaderCard(
    hijriDate: String,
    currentLanguage: AppLanguage,
    onOpenPrayerDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUrdu = currentLanguage == AppLanguage.URDU
    val isArabic = currentLanguage == AppLanguage.ARABIC

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.2.dp, GoldBorder, RoundedCornerShape(20.dp))
            .testTag("islamic_portal_header_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepEmeraldDark),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF09392F),
                            DeepEmerald,
                            Color(0xFF135A4C)
                        )
                    )
                )
        ) {
            // Gold accent top line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(MutedGold, Color(0xFFF5E3B3), MutedGold)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Top row: Emblem + Category Label + Hijri Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GoldTint)
                                .border(1.dp, MutedGold, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = "Islamic Portal",
                                tint = DeepEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "ISLAMIC SANCTUARY",
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = Color(0xFFE2C98D)
                            )
                            Text(
                                text = "بوابۃ العالم الإسلامی",
                                fontFamily = NotoNaskhArabic,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = PureWhite.copy(alpha = 0.9f)
                            )
                        }
                    }

                    // Clickable Hijri & Prayer indicator pill
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(0.8.dp, Color(0xFFE2C98D).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .clickable { onOpenPrayerDialog() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("islamic_portal_prayer_pill")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = "Prayer times",
                                tint = Color(0xFFF5E3B3),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = hijriDate.substringBefore(" •"),
                                fontFamily = Inter,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.5.sp,
                                color = PureWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Spiritual description in clear font
                val narrativeText = when {
                    isUrdu -> "قرآنی تعلیمات، مساجدِ مقدسہ (مکہ مکرمہ، مدینہ منورہ، بیت المقدس)، فتاویٰ و بیانات اور دنیا بھر کے مسلم اسکالرز کی تصانیف"
                    isArabic -> "القرآن الكريم والسنة النبوية، الحرمين الشريفين والمسجد الأقصى، وفتاوى وبحوث علماء الأمة الإسلامية"
                    else -> "Scholarly guidance, Sacred Sanctuaries (Makkah, Madinah, Al-Quds) and authentic dispatches from across the Muslim Ummah."
                }

                Text(
                    text = narrativeText,
                    fontFamily = if (isUrdu) NotoNastaliqUrdu else if (isArabic) NotoNaskhArabic else Inter,
                    fontSize = if (isUrdu) 16.sp else 14.5.sp,
                    lineHeight = if (isUrdu) 32.sp else 22.sp,
                    letterSpacing = if (isUrdu) 0.3.sp else 0.sp,
                    color = PureWhite.copy(alpha = 0.95f),
                    textAlign = if (isUrdu || isArabic) TextAlign.Right else TextAlign.Left,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
