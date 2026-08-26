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

/**
 * Discrete show/hide for the island tab bar.
 *
 * Scroll down past a threshold → pop out. Scroll up → spring pop back in.
 * State writes happen here; only [AutoHideBottomNavigation] reads [visible]
 * so list screens do not recompose on every scroll pixel.
 */
@Stable
class ScrollHideBarState {
    var visible by mutableStateOf(true)
        private set

    private var downAccum = 0f
    private var upAccum = 0f

    fun onScrollDelta(dy: Float) {
        if (dy == 0f) return
        if (dy < -4f) {
            downAccum += -dy
            upAccum = 0f
            if (downAccum > 42f) visible = false
        } else if (dy > 4f) {
            upAccum += dy
            downAccum = 0f
            if (upAccum > 18f) visible = true
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
            state.onScrollDelta(consumed.y)
            return Offset.Zero
        }
    }

/**
 * Island pops out (scale + drop) when scrolling down, springs back in
 * with a slight overshoot when scrolling up.
 */
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
                dampingRatio = 0.58f,
                stiffness = Spring.StiffnessMediumLow
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
                val s = 0.72f + 0.28f * progress
                scaleX = s
                scaleY = s
                alpha = contentAlpha * progress
                clip = false
            }
    )
}
