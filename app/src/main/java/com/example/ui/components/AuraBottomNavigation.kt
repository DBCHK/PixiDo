package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    NavigationTab("Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "nav_tab_budget"),
    NavigationTab("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "nav_tab_calendar"),
    NavigationTab("Goals", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents, "nav_tab_goals")
)

/** Alias for existing references. */
val auraTabs = pixiDoTabs

/**
 * Floating island tab bar — capsule that sits above the home indicator,
 * with a circular + in the middle.
 */
@Composable
fun AuraBottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onCenterAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val d = rememberPixiDimens()
    val iconSize = if (d.isCompact) 22.dp else 24.dp
    val labelSize = 10.sp
    val fabSize = if (d.isCompact) 40.dp else 44.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
    ) {
        PixiGlass(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = PixiIslandShape,
            role = PixiGlassRole.Chrome,
            liquid = true,
            elevation = 20.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavItem(
                    tab = pixiDoTabs[0],
                    selected = selectedTab == 0,
                    onClick = { onTabSelected(0) },
                    iconSize = iconSize,
                    labelSize = labelSize
                )
                NavItem(
                    tab = pixiDoTabs[1],
                    selected = selectedTab == 1,
                    onClick = { onTabSelected(1) },
                    iconSize = iconSize,
                    labelSize = labelSize
                )

                PixiYellowFab(
                    onClick = onCenterAdd,
                    size = fabSize,
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .testTag("center_add_fab")
                )

                NavItem(
                    tab = pixiDoTabs[2],
                    selected = selectedTab == 2,
                    onClick = { onTabSelected(2) },
                    iconSize = iconSize,
                    labelSize = labelSize
                )
                NavItem(
                    tab = pixiDoTabs[3],
                    selected = selectedTab == 3,
                    onClick = { onTabSelected(3) },
                    iconSize = iconSize,
                    labelSize = labelSize
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: NavigationTab,
    selected: Boolean,
    onClick: () -> Unit,
    iconSize: androidx.compose.ui.unit.Dp,
    labelSize: androidx.compose.ui.unit.TextUnit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 500f),
        label = "tabScale"
    )
    val iconColor by animateColorAsState(
        targetValue = if (selected) com.example.ui.theme.PulseMint
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabIconColor"
    )
    val labelColor by animateColorAsState(
        targetValue = if (selected) com.example.ui.theme.PulseMint
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tabLabelColor"
    )
    val glassOn = LocalGlassEnabled.current
    val bubble by animateColorAsState(
        targetValue = when {
            !selected -> Color.Transparent
            glassOn -> Color.White.copy(alpha = 0.32f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        label = "tabBubble"
    )

    Column(
        modifier = Modifier
            .testTag(tab.testTag)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .width(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bubble)
                .then(
                    if (selected && glassOn) {
                        Modifier.border(
                            width = 0.7.dp,
                            color = Color.White.copy(alpha = 0.55f),
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                ),
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
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
