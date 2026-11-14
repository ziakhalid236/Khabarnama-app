package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppLanguage
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldTint
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.Inter
import com.example.ui.theme.MutedGold
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmDivider
import com.example.ui.theme.WarmOffWhite
import com.example.util.CityPrayerInfo
import com.example.util.PrayerTimesUtil
import kotlinx.coroutines.delay
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrduAppBar(
    currentLanguage: AppLanguage,
    hijriDate: String,
    selectedCity: CityPrayerInfo,
    isSearchOpen: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onOpenPrayerDialog: () -> Unit,
    onOpenLanguageDialog: () -> Unit,
    onSearchSubmitted: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Surface(
        color = WarmOffWhite,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
        ) {
            if (isSearchOpen) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onToggleSearch(false) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PureWhite)
                            .border(1.dp, WarmBorder, CircleShape)
                            .testTag("close_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close search",
                            tint = InkPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { onSearchQueryChange("") },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search query",
                                        tint = SoftGrey,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        placeholder = {
                            Text(
                                text = "Search Pakistan, Gaza, Makkah, Economy, Cricket...",
                                fontFamily = Inter,
                                fontSize = 14.sp,
                                color = SoftGrey,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                if (searchQuery.isNotBlank()) {
                                    onSearchSubmitted(searchQuery)
                                }
                                focusManager.clearFocus()
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite,
                            focusedIndicatorColor = DeepEmerald,
                            unfocusedIndicatorColor = WarmBorder,
                            focusedTextColor = InkPrimary,
                            unfocusedTextColor = InkPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_input_field")
                    )
                }
            } else {
                // Row 1: Wordmark on left, Compact Flag + Translate Pill and Search Icon on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 10.dp, top = 4.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KhabarnamaWordmark(modifier = Modifier.weight(1f, fill = false))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isTranslated = currentLanguage != AppLanguage.ENGLISH
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { onOpenLanguageDialog() }
                                .semantics {
                                    role = Role.Button
                                    contentDescription = "News translation language: ${currentLanguage.displayName}"
                                }
                                .testTag("translate_language_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .heightIn(min = 32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isTranslated) InkPrimary else PureWhite)
                                    .border(
                                        width = 1.dp,
                                        color = if (isTranslated) InkPrimary else WarmBorder,
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = currentLanguage.flagEmoji,
                                    fontSize = 13.sp
                                )
                                Icon(
                                    imageVector = Icons.Default.Translate,
                                    contentDescription = "Translate news",
                                    tint = if (isTranslated) Color.White else InkPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = currentLanguage.code,
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isTranslated) Color.White else InkPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (isTranslated) Color.White.copy(alpha = 0.85f) else SoftGrey,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { onToggleSearch(true) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(PureWhite)
                                .border(1.dp, WarmBorder, CircleShape)
                                .testTag("search_toggle_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Open search",
                                tint = InkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Row 2 (slim): Hijri date + next prayer on one clean line, 12sp
                HeaderDateAndNextPrayerLine(
                    hijriDate = hijriDate,
                    selectedCity = selectedCity,
                    onOpenPrayerDialog = onOpenPrayerDialog
                )
            }

            HorizontalDivider(thickness = 1.5.dp, color = InkPrimary.copy(alpha = 0.85f))
        }
    }
}

@Composable
private fun HeaderDateAndNextPrayerLine(
    hijriDate: String,
    selectedCity: CityPrayerInfo,
    onOpenPrayerDialog: () -> Unit
) {
    var minuteTickMillis by remember(selectedCity) {
        mutableLongStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(selectedCity) {
        while (true) {
            val now = System.currentTimeMillis()
            val delayToNextMinute = 60_000L - (now % 60_000L) + 50L
            delay(delayToNextMinute)
            minuteTickMillis = System.currentTimeMillis()
        }
    }

    val nextPrayer = remember(selectedCity, minuteTickMillis) {
        PrayerTimesUtil.getNextPrayer(
            city = selectedCity,
            lang = AppLanguage.ENGLISH,
            nowInstant = Instant.ofEpochMilli(minuteTickMillis)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 28.dp)
            .clickable { onOpenPrayerDialog() }
            .padding(horizontal = 14.dp, vertical = 2.dp)
            .testTag("prayer_times_pill"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hijriDate.isNotBlank()) {
            Text(
                text = hijriDate,
                fontFamily = Inter,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                color = SoftGrey,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("header_hijri_date")
            )

            Text(
                text = " • ",
                fontFamily = Inter,
                fontSize = 11.sp,
                color = SoftGrey
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = "Next prayer time",
                tint = DeepEmerald,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "${nextPrayer.prayerName} ${nextPrayer.timeFormatted} (${selectedCity.name})",
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = DeepEmerald,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
