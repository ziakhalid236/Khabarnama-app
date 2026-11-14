package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NewsCategory
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.Inter
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.WarmBorder

enum class NavTab(
    val title: String,
    val outlinedIcon: ImageVector,
    val filledIcon: ImageVector
) {
    HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
    ISLAMIC("Islamic", Icons.Outlined.Mosque, Icons.Filled.Mosque),
    NOTIFICATIONS("Notifications", Icons.Outlined.Notifications, Icons.Filled.Notifications),
    SETTINGS("Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
}

@Composable
fun KhabarnamaBottomNavBar(
    selectedCategory: NewsCategory,
    isNotificationsOpen: Boolean = false,
    isSettingsOpen: Boolean = false,
    unreadNotificationCount: Int = 0,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTab = when {
        isSettingsOpen -> NavTab.SETTINGS
        isNotificationsOpen -> NavTab.NOTIFICATIONS
        selectedCategory == NewsCategory.ISLAMIC -> NavTab.ISLAMIC
        else -> NavTab.HOME
    }

    Surface(
        color = PureWhite,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(thickness = 1.dp, color = WarmBorder)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = tab == currentTab

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTabSelected(tab) }
                            .semantics {
                                role = Role.Tab
                                selected = isSelected
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .testTag("nav_tab_${tab.name.lowercase()}")
                    ) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = if (isSelected) tab.filledIcon else tab.outlinedIcon,
                                contentDescription = "${tab.title} tab",
                                tint = if (isSelected) Color(0xFF0A0A0B) else SoftGrey,
                                modifier = Modifier.size(23.dp)
                            )

                            if (tab == NavTab.NOTIFICATIONS && unreadNotificationCount > 0) {
                                val badgeLabel = if (unreadNotificationCount > 99) "99+" else unreadNotificationCount.toString()
                                Box(
                                    modifier = Modifier
                                        .offset(x = 8.dp, y = (-4).dp)
                                        .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFC8102E))
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = badgeLabel,
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 9.sp,
                                        lineHeight = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Text(
                            text = tab.title,
                            fontFamily = Inter,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            color = if (isSelected) Color(0xFF0A0A0B) else SoftGrey
                        )
                    }
                }
            }
        }
    }
}
