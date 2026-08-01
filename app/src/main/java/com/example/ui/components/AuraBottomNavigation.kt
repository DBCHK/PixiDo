package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.rememberPixiDimens

data class NavigationTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val pixiDoTabs = listOf(
    NavigationTab("Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle, "nav_tab_tasks"),
    NavigationTab("Budget", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_tab_budget"),
    NavigationTab("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_calendar"),
    NavigationTab("Goals", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_goals")
)

/** Alias for existing references. */
val auraTabs = pixiDoTabs

/**
 * Soft Lilac bottom bar matching idea2:
 *  floating soft bar · [Tasks] [Budget]  [yellow +]  [Calendar] [Goals]
 *  selected tab = lavender circle, unselected = muted outline icons
 */
@Composable
fun AuraBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onCenterAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val d = rememberPixiDimens()
    val iconCircle = if (d.isCompact) 36.dp else 42.dp
    val iconSize = if (d.isCompact) 18.dp else 22.dp
    val labelSize = if (d.isCompact) 9.sp else 10.sp
    val fabSize = d.fab

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 16.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = if (d.isCompact) 6.dp else 10.dp,
                    vertical = if (d.isCompact) 8.dp else 12.dp
                ),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                tab = pixiDoTabs[0],
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                iconCircle = iconCircle,
                iconSize = iconSize,
                labelSize = labelSize,
                compact = d.isCompact
            )
            NavItem(
                tab = pixiDoTabs[1],
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                iconCircle = iconCircle,
                iconSize = iconSize,
                labelSize = labelSize,
                compact = d.isCompact
            )

            PixiYellowFab(
                onClick = onCenterAdd,
                size = fabSize,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .testTag("center_add_fab")
            )

            NavItem(
                tab = pixiDoTabs[2],
                selected = selectedTab == 2,
                onClick = { onTabSelected(2) },
                iconCircle = iconCircle,
                iconSize = iconSize,
                labelSize = labelSize,
                compact = d.isCompact
            )
            NavItem(
                tab = pixiDoTabs[3],
                selected = selectedTab == 3,
                onClick = { onTabSelected(3) },
                iconCircle = iconCircle,
                iconSize = iconSize,
                labelSize = labelSize,
                compact = d.isCompact
            )
        }
    }
}

@Composable
private fun NavItem(
    tab: NavigationTab,
    selected: Boolean,
    onClick: () -> Unit,
    iconCircle: androidx.compose.ui.unit.Dp,
    iconSize: androidx.compose.ui.unit.Dp,
    labelSize: androidx.compose.ui.unit.TextUnit,
    compact: Boolean
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = PixiSnappySpring,
        label = "tabScale"
    )
    val selectBoost by animateFloatAsState(
        targetValue = if (selected) 1.05f else 1f,
        animationSpec = PixiSnappySpring,
        label = "tabSelectBoost"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabIconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabLabelColor"
    )
    val circleBg by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
        label = "tabCircleBg"
    )

    Column(
        modifier = Modifier
            .testTag(tab.testTag)
            .graphicsLayer {
                val s = scale * selectBoost
                scaleX = s
                scaleY = s
            }
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(
                horizontal = if (compact) 4.dp else 8.dp,
                vertical = 4.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(iconCircle)
                .clip(CircleShape)
                .background(circleBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                contentDescription = tab.title,
                tint = iconColor,
                modifier = Modifier.size(iconSize)
            )
        }
        Text(
            text = tab.title,
            fontSize = labelSize,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}
