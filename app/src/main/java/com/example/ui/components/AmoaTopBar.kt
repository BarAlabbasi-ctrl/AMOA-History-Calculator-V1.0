package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalAmoaColors

@Composable
fun AmoaTopBar(
    selectedTab: AppTab,
    isFunctionsOpen: Boolean,
    isDarkTheme: Boolean,
    onTabSelected: (AppTab) -> Unit,
    onToggleTheme: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAmoaColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // App Emblem Thumbnail (clickable opens about)
        Image(
            painter = painterResource(id = R.drawable.amoa_logo_1791042759153),
            contentDescription = "AMOA Emblem",
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenAbout() }
        )

        // Modes Bar (scrollable horizontally if on small screens, so it fits perfectly on all devices)
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppTab.values().forEach { tab ->
                val isSelected = if (isFunctionsOpen && (selectedTab == AppTab.BASIC || selectedTab == AppTab.FUNCTIONS)) {
                    tab == AppTab.FUNCTIONS
                } else {
                    selectedTab == tab && (!isFunctionsOpen || tab != AppTab.BASIC)
                }

                val isFunc = tab == AppTab.FUNCTIONS

                val bg = when {
                    isSelected -> colors.equalsBg
                    isFunc -> colors.funcBg
                    else -> colors.keyBg
                }

                val borderColor = when {
                    isSelected -> colors.brass
                    isFunc -> colors.funcBorder
                    else -> colors.keyBorder
                }

                val contentColor = when {
                    isSelected -> colors.equalsText
                    isFunc -> colors.funcText
                    else -> colors.textDim
                }

                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(bg)
                        .border(1.dp, borderColor, RoundedCornerShape(7.dp))
                        .clickable { onTabSelected(tab) }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (tab == AppTab.FUNCTIONS) "Func" else tab.title,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor
                    )
                }
            }
        }

        // Theme Toggle Icon Button
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.keyBg)
                .border(1.dp, colors.keyBorder, CircleShape)
                .clickable { onToggleTheme() }
                .testTag("theme_toggle_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Theme",
                tint = colors.brass,
                modifier = Modifier.size(16.dp)
            )
        }

        // About Button (ℹ)
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(colors.keyBg)
                .border(1.dp, colors.keyBorder, CircleShape)
                .clickable { onOpenAbout() }
                .testTag("about_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "About",
                tint = colors.textDim,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
