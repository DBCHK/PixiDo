package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Soft spring — light work, settles quick on high-refresh panels. */
val PixiPopSpring = spring<Float>(
    dampingRatio = 0.86f,
    stiffness = Spring.StiffnessMedium
)

val PixiSnappySpring = spring<Float>(
    dampingRatio = 0.88f,
    stiffness = Spring.StiffnessHigh
)

/**
 * Clickable with a soft press scale — chips, cards, FABs.
 * Scale is applied in graphicsLayer (no layout invalidation).
 */
@Composable
fun PixiPopClickable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing),
        label = "popClick"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
    ) {
        content()
    }
}

/**
 * Lightweight list enter — fade only (no scale/slide stack).
 * Caps stagger so long lists stay smooth on 90/120 Hz.
 */
@Composable
fun PixiListItemEnter(
    visible: Boolean = true,
    index: Int = 0,
    content: @Composable () -> Unit
) {
    // Only animate first few items; rest appear immediately (avoids jank on big lists)
    if (index > 8) {
        content()
        return
    }
    val delay = (index * 18).coerceAtMost(120)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(160, delayMillis = delay, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(80))
    ) {
        content()
    }
}

/** One-shot soft pop when [trigger] changes (e.g. complete toggle). */
@Composable
fun rememberPopScale(trigger: Any?, from: Float = 0.94f): Float {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        scale.snapTo(from)
        scale.animateTo(
            1f,
            animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
        )
    }
    return scale.value
}

/** Soft entrance for dialogs / modals. */
fun dialogEnterTransition() =
    fadeIn(tween(180, easing = FastOutSlowInEasing)) + scaleIn(
        initialScale = 0.97f,
        animationSpec = tween(200, easing = FastOutSlowInEasing)
    )

fun dialogExitTransition() =
    fadeOut(tween(120)) + scaleOut(targetScale = 0.98f, animationSpec = tween(120))
