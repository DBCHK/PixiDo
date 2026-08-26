package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * Discrete show/hide for the island tab bar.
 *
 * Only finger-drag (not fling) is counted, with strong hysteresis so a
 * deceleration / overscroll cannot flicker the island on and off.
 */
@Stable
class ScrollHideBarState {
    var visible by mutableStateOf(true)
        private set

    private var downAccum = 0f
    private var upAccum = 0f

    fun onScrollDelta(dy: Float) {
        if (abs(dy) < 2f) return
        if (dy < 0f) {
            upAccum = 0f
            downAccum += -dy
            if (visible && downAccum > 80f) {
                visible = false
                downAccum = 0f
            }
        } else {
            downAccum = 0f
            upAccum += dy
            if (!visible && upAccum > 64f) {
                visible = true
                upAccum = 0f
            }
        }
    }

    fun snapShow() {
        visible = true
        downAccum = 0f
        upAccum = 0f
    }
}

@Composable
fun rememberScrollHideBarState(): ScrollHideBarState {
    return remember { ScrollHideBarState() }
}

fun scrollHideNestedConnection(state: ScrollHideBarState): NestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            // Ignore fling / programmatic scroll — those reverse sign and cause jitter
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            state.onScrollDelta(consumed.y)
            return Offset.Zero
        }
    }

@Composable
fun BoxScope.AutoHideBottomNavigation(
    state: ScrollHideBarState,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onCenterAdd: () -> Unit,
    contentAlpha: Float = 1f,
    reduceMotion: Boolean = false
) {
    LaunchedEffect(selectedTab) {
        state.snapShow()
    }

    val density = LocalDensity.current
    val hideTravel = with(density) { 96.dp.toPx() }
    val progress by animateFloatAsState(
        targetValue = if (state.visible) 1f else 0f,
        animationSpec = if (reduceMotion) {
            tween(durationMillis = 140)
        } else {
            spring(
                dampingRatio = 0.92f,
                stiffness = Spring.StiffnessMedium
            )
        },
        label = "islandPop"
    )

    AuraBottomNavigation(
        selectedTab = selectedTab,
        onTabSelected = {
            state.snapShow()
            onTabSelected(it)
        },
        onCenterAdd = {
            state.snapShow()
            onCenterAdd()
        },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .graphicsLayer {
                val t = 1f - progress
                translationY = t * hideTravel
                val s = 0.92f + 0.08f * progress
                scaleX = s
                scaleY = s
                alpha = contentAlpha * progress
                clip = false
            }
    )
}
