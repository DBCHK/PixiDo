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
import androidx.compose.animation.slideInVertically
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

/** Soft spring — gentle settle, not bouncy-retro. */
val PixiPopSpring = spring<Float>(
    dampingRatio = 0.78f,
    stiffness = Spring.StiffnessMediumLow
)

val PixiSnappySpring = spring<Float>(
    dampingRatio = 0.82f,
    stiffness = Spring.StiffnessMedium
)

/**
 * Clickable with a soft press scale — chips, cards, FABs.
 */
@Composable
fun PixiPopClickable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    content: @Composable () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = PixiSnappySpring,
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

/** Staggered enter for list items — soft fade + slight rise. */
@Composable
fun PixiListItemEnter(
    visible: Boolean = true,
    index: Int = 0,
    content: @Composable () -> Unit
) {
    val delay = (index * 28).coerceAtMost(220)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(240, delayMillis = delay, easing = FastOutSlowInEasing)) +
            scaleIn(
                initialScale = 0.96f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) +
            slideInVertically(
                animationSpec = tween(260, delayMillis = delay, easing = FastOutSlowInEasing),
                initialOffsetY = { it / 12 }
            ),
        exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.98f)
    ) {
        content()
    }
}

/** One-shot soft pop when [trigger] changes (e.g. complete toggle). */
@Composable
fun rememberPopScale(trigger: Any?, from: Float = 0.92f): Float {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == null) return@LaunchedEffect
        scale.snapTo(from)
        scale.animateTo(1f, PixiPopSpring)
    }
    return scale.value
}

/** Soft entrance for dialogs / modals. */
fun dialogEnterTransition() =
    fadeIn(tween(200, easing = FastOutSlowInEasing)) + scaleIn(
        initialScale = 0.95f,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        )
    )

fun dialogExitTransition() =
    fadeOut(tween(160)) + scaleOut(targetScale = 0.97f, animationSpec = tween(160))
