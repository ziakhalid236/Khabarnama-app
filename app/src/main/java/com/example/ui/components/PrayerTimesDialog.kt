package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.Inter
import com.example.ui.theme.MutedGold
import com.example.ui.theme.PlayfairDisplay
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmDivider
import com.example.util.CityPrayerInfo
import com.example.util.PrayerTimesUtil

@Composable
fun PrayerTimesDialog(
    selectedCity: CityPrayerInfo,
    hijriDate: String,
    onCitySelected: (CityPrayerInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val schedule = remember(selectedCity) { PrayerTimesUtil.getDailySchedule(selectedCity) }
    val nextPrayer = remember(selectedCity) { PrayerTimesUtil.getNextPrayer(selectedCity) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("prayer_times_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header with Mosque Icon, Title, and Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(EmeraldTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = "Prayer schedule",
                                tint = DeepEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "Prayer Schedule",
                                fontFamily = PlayfairDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = InkPrimary
                            )
                            if (hijriDate.isNotBlank()) {
                                Text(
                                    text = hijriDate,
                                    fontFamily = Inter,
                                    fontSize = 12.sp,
                                    color = SoftGrey
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close prayer schedule",
                            tint = SoftGrey
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // City Selector Chips (48dp touch target)
                val cityScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(cityScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PrayerTimesUtil.supportedCities.forEach { city ->
                        val isSelected = city.id == selectedCity.id
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .defaultMinSize(minHeight = 40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) DeepEmerald else WarmDivider)
                                .clickable { onCitySelected(city) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("city_chip_${city.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = "City ${city.name}",
                                    tint = if (isSelected) Color.White else SoftGrey,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = city.name,
                                    fontFamily = Inter,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White else InkPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Next Prayer Highlight Card (Always one of the 5 obligatory prayers, never Sunrise)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(EmeraldTint)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Next prayer",
                                fontFamily = Inter,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = DeepEmerald
                            )
                            Text(
                                text = nextPrayer.prayerName,
                                fontFamily = PlayfairDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = InkPrimary
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DeepEmerald)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = nextPrayer.timeFormatted,
                                fontFamily = Inter,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Five Obligatory Prayers List
                val obligatoryPrayers = listOf(
                    "Fajr" to schedule.fajr,
                    "Dhuhr" to schedule.dhuhr,
                    "Asr" to schedule.asr,
                    "Maghrib" to schedule.maghrib,
                    "Isha" to schedule.isha
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    obligatoryPrayers.forEach { (name, time) ->
                        val isNext = nextPrayer.prayerName == name
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isNext) EmeraldTint.copy(alpha = 0.5f) else Color.Transparent)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = "$name prayer time",
                                    tint = if (isNext) DeepEmerald else SoftGrey,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = name,
                                    fontFamily = Inter,
                                    fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (isNext) DeepEmerald else InkPrimary
                                )
                            }

                            Text(
                                text = time,
                                fontFamily = Inter,
                                fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isNext) DeepEmerald else SoftGrey
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(thickness = 1.dp, color = WarmDivider)
                Spacer(modifier = Modifier.height(16.dp))

                // Separate Sunrise Row (Non-prayer informational row, never highlighted as next prayer)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, WarmBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("sunrise_info_row"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WbSunny,
                            contentDescription = "Sunrise time",
                            tint = MutedGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Sunrise",
                            fontFamily = Inter,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = SoftGrey
                        )
                    }

                    Text(
                        text = schedule.sunrise,
                        fontFamily = Inter,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = SoftGrey
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Small calculation disclaimer note (12sp minimum)
                Text(
                    text = "Times are calculated; they may differ slightly from your local mosque.",
                    fontFamily = Inter,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = SoftGrey,
                    modifier = Modifier.testTag("prayer_calculation_note")
                )
            }
        }
    }
}
