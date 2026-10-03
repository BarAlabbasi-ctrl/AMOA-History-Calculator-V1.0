package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAmoaColors

enum class AppTab(val title: String, val icon: ImageVector) {
    BASIC("Basic", Icons.Default.Calculate),
    FUNCTIONS("Functions", Icons.Default.Functions),
    UNITS("Units", Icons.Default.Scale),
    TIME("Time", Icons.Default.Schedule),
    HISTORY("History", Icons.Default.History)
}

@Composable
fun AmoaTabNav(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAmoaColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        AppTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val isFuncTab = tab == AppTab.FUNCTIONS

            val bg = when {
                isSelected -> colors.equalsBg
                isFuncTab -> colors.funcBg
                else -> colors.keyBg
            }

            val borderColor = when {
                isSelected -> colors.brass
                isFuncTab -> colors.funcBorder
                else -> colors.keyBorder
            }

            val contentColor = when {
                isSelected -> colors.equalsText
                isFuncTab -> colors.funcText
                else -> colors.textDim
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(tab) }
                    .testTag("tab_${tab.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = contentColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
