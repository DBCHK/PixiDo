package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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


 * Scroll-linked bottom bar hide state.
 *
 * Hold this in a parent without *reading* [hidePx] so scroll updates only recompose
 * the floating bar — not the whole screen tree (major jank fix).
 */
@Stable
class ScrollHideBarState(val maxHidePx: Float) {
    /** 0 = fully visible, [maxHidePx] = fully tucked away. */
    var hidePx by mutableFloatStateOf(0f)
        private set

    fun onScrollDelta(dy: Float) {
        if (dy == 0f || maxHidePx <= 0f) return
        // Slight dampening for buttery feel on 90/120 Hz
        hidePx = (hidePx - dy * 0.55f).coerceIn(0f, maxHidePx)
    }

    fun snapShow() {
        hidePx = 0f
    }

    fun setHide(px: Float) {
        hidePx = px.coerceIn(0f, maxHidePx)
    }
}

@Composable
fun rememberScrollHideBarState(): ScrollHideBarState {
    val density = LocalDensity.current
    val maxPx = with(density) { 120.dp.toPx() }
    return remember(maxPx) { ScrollHideBarState(maxPx) }
}

/** Nested scroll connection that only writes into [state] (no composition reads). */
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
 * Floating bottom nav that slides with scroll. Reading [state].hidePx is isolated here
 * so list screens do not recompose on every scroll pixel.
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
    // Smooth ease-back when tab changes (does not drive scroll path)
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(selectedTab) {
        val start = state.hidePx
        if (start <= 0.5f) {
            state.snapShow()
            reveal.snapTo(0f)
            return@LaunchedEffect
        }
        reveal.snapTo(start)
        reveal.animateTo(
            0f,
            animationSpec = tween(
                durationMillis = if (reduceMotion) 120 else 280,
                easing = FastOutSlowInEasing
            )
        ) {
            state.setHide(value)
        }
        state.snapShow()
    }

    val max = state.maxHidePx.coerceAtLeast(1f)
    val hide = state.hidePx
    val frac = (hide / max).coerceIn(0f, 1f)

    AuraBottomNavigation(
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        onCenterAdd = {
            state.snapShow()
            onCenterAdd()
        },
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .graphicsLayer {
                // GPU-only transform — no layout pass
                translationY = hide
                alpha = contentAlpha * (1f - frac * 0.28f)
                val s = 1f - frac * 0.03f
                scaleX = s
                scaleY = s
                // Prefer continuous layer on high-refresh displays
                clip = false
            }
    )
}
