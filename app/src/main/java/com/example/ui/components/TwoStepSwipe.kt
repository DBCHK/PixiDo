package com.example.ui.components

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** First swipe sticks here — halfway through a dismiss/complete. */
internal const val TwoStepPeekFraction = 0.5f

/** How far (as a fraction of the current gap) you must travel to snap to the next anchor. */
internal const val TwoStepPositionalFraction = 0.42f

/** Fling speed that counts as a committed swipe, in dp per second. */
internal const val TwoStepVelocityDp = 1100f

internal enum class TwoStepAnchor {
    Rest,
    PeekComplete,
    PeekDelete,
    Complete,
    Delete
}

internal fun twoStepPeekPx(width: Float): Float = width * TwoStepPeekFraction

internal fun twoStepAnchorOffset(anchor: TwoStepAnchor, width: Float): Float {
    val peek = twoStepPeekPx(width)
    return when (anchor) {
        TwoStepAnchor.Rest -> 0f
        TwoStepAnchor.PeekComplete -> peek
        TwoStepAnchor.PeekDelete -> -peek
        TwoStepAnchor.Complete -> width
        TwoStepAnchor.Delete -> -width
    }
}

/**
 * Rubber-band past the current swipe bounds so the first swipe hits a sticky wall
 * instead of flying off the row.
 */
internal fun twoStepRubberOffset(
    raw: Float,
    settled: TwoStepAnchor,
    width: Float
): Float {
    if (width <= 0f) return 0f
    val peek = twoStepPeekPx(width)
    val (lo, hi) = when (settled) {
        TwoStepAnchor.PeekComplete -> 0f to width
        TwoStepAnchor.PeekDelete -> -width to 0f
        TwoStepAnchor.Rest, TwoStepAnchor.Complete, TwoStepAnchor.Delete -> -peek to peek
    }
    val range = peek * 0.65f
    return when {
        raw > hi -> hi + rubberBand(raw - hi, range)
        raw < lo -> lo - rubberBand(lo - raw, range)
        else -> raw
    }
}

internal fun rubberBand(overshoot: Float, range: Float): Float {
    if (overshoot <= 0f || range <= 0f) return 0f
    val c = 0.58f
    return (overshoot * range * c) / (range + overshoot)
}

/**
 * Pick the snap target after a gesture. From [TwoStepAnchor.Rest] a fling can only
 * land on a peek — never complete or delete — so a tab swipe cannot remove a task.
 */
internal fun settleTwoStepSwipe(
    offset: Float,
    velocity: Float,
    width: Float,
    settled: TwoStepAnchor,
    velocityThreshold: Float
): TwoStepAnchor {
    if (width <= 1f) return TwoStepAnchor.Rest
    val peek = twoStepPeekPx(width)

    fun crossed(from: Float, to: Float, current: Float): Boolean {
        val dist = abs(to - from)
        if (dist < 1f) return false
        val traveled = if (to > from) current - from else from - current
        return traveled >= dist * TwoStepPositionalFraction
    }

    fun flungToward(target: Float): Boolean {
        if (abs(velocity) < velocityThreshold) return false
        return if (target > offset) velocity > 0f else velocity < 0f
    }

    return when (settled) {
        TwoStepAnchor.Rest, TwoStepAnchor.Complete, TwoStepAnchor.Delete -> when {
            offset >= 0f && (crossed(0f, peek, offset) || flungToward(peek)) ->
                TwoStepAnchor.PeekComplete
            offset < 0f && (crossed(0f, -peek, offset) || flungToward(-peek)) ->
                TwoStepAnchor.PeekDelete
            else -> TwoStepAnchor.Rest
        }
        TwoStepAnchor.PeekComplete -> when {
            crossed(peek, width, offset) || flungToward(width) -> TwoStepAnchor.Complete
            offset < peek * 0.45f || flungToward(0f) -> TwoStepAnchor.Rest
            else -> TwoStepAnchor.PeekComplete
        }
        TwoStepAnchor.PeekDelete -> when {
            crossed(-peek, -width, offset) || flungToward(-width) -> TwoStepAnchor.Delete
            offset > -peek * 0.45f || flungToward(0f) -> TwoStepAnchor.Rest
            else -> TwoStepAnchor.PeekDelete
        }
    }
}

private val PeekSpring = spring<Float>(
    dampingRatio = 0.52f,
    stiffness = 340f
)

private val CommitSpring = spring<Float>(
    dampingRatio = 0.68f,
    stiffness = 480f
)

private val ReturnSpring = spring<Float>(
    dampingRatio = 0.48f,
    stiffness = 300f
)

/**
 * Horizontal swipe that always stops at the midpoint on the first gesture.
 * A second swipe in the same direction commits; a tap or reverse swipe cancels.
 */
@Composable
fun TwoStepSwipeBox(
    onCommitStartToEnd: () -> Unit,
    onCommitEndToStart: () -> Unit,
    startToEndColor: Color,
    endToStartColor: Color,
    startToEndIcon: ImageVector,
    endToStartIcon: ImageVector,
    modifier: Modifier = Modifier,
    onArmed: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val velocityThreshold = remember(density) {
        with(density) { TwoStepVelocityDp.dp.toPx() }
    }

    var widthPx by remember { mutableFloatStateOf(0f) }
    var offsetPx by remember { mutableFloatStateOf(0f) }
    var stage by remember { mutableStateOf(TwoStepAnchor.Rest) }
    var committing by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    fun cancelSettle() {
        settleJob?.cancel()
        settleJob = null
    }

    val dragState = rememberDraggableState { delta ->
        if (committing) return@rememberDraggableState
        cancelSettle()
        if (widthPx <= 0f) return@rememberDraggableState
        offsetPx = twoStepRubberOffset(offsetPx + delta, stage, widthPx)
    }

    fun snapTo(anchor: TwoStepAnchor, velocity: Float, spec: SpringSpec<Float>) {
        cancelSettle()
        val target = twoStepAnchorOffset(anchor, widthPx)
        settleJob = scope.launch {
            animate(
                initialValue = offsetPx,
                targetValue = target,
                initialVelocity = velocity,
                animationSpec = spec
            ) { value, _ ->
                offsetPx = value
            }
        }
    }

    val armed = stage == TwoStepAnchor.PeekComplete || stage == TwoStepAnchor.PeekDelete
    val towardEnd = offsetPx >= 0f
    val peek = twoStepPeekPx(widthPx).coerceAtLeast(1f)
    val intensity = (abs(offsetPx) / peek).coerceIn(0f, 1.35f)
    val bgAlpha = (intensity * 0.34f).coerceAtMost(0.46f)
    val iconScale = (0.72f + intensity * 0.46f).coerceAtMost(1.22f)
    val revealColor = if (towardEnd) startToEndColor else endToStartColor

    Box(
        modifier = modifier.onSizeChanged { size ->
            widthPx = size.width.toFloat()
        }
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = 12.dp)
                .clip(PulseCardShape)
                .background(revealColor.copy(alpha = bgAlpha)),
            contentAlignment = if (towardEnd) Alignment.CenterStart else Alignment.CenterEnd
        ) {
            if (abs(offsetPx) > 8f) {
                Icon(
                    imageVector = if (towardEnd) startToEndIcon else endToStartIcon,
                    contentDescription = null,
                    tint = revealColor,
                    modifier = Modifier
                        .padding(horizontal = 22.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                            alpha = intensity.coerceIn(0.25f, 1f)
                        }
                )
            }
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = offsetPx
                    val w = widthPx.coerceAtLeast(1f)
                    rotationZ = (offsetPx / w) * 1.8f
                }
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    enabled = !committing && widthPx > 0f,
                    onDragStopped = { velocity ->
                        if (committing || widthPx <= 0f) return@draggable
                        val next = settleTwoStepSwipe(
                            offset = offsetPx,
                            velocity = velocity,
                            width = widthPx,
                            settled = stage,
                            velocityThreshold = velocityThreshold
                        )
                        when (next) {
                            TwoStepAnchor.PeekComplete, TwoStepAnchor.PeekDelete -> {
                                val newlyArmed = stage == TwoStepAnchor.Rest
                                stage = next
                                if (newlyArmed) onArmed()
                                snapTo(next, velocity, PeekSpring)
                            }
                            TwoStepAnchor.Rest -> {
                                stage = TwoStepAnchor.Rest
                                snapTo(TwoStepAnchor.Rest, velocity, ReturnSpring)
                            }
                            TwoStepAnchor.Complete -> {
                                committing = true
                                stage = TwoStepAnchor.Rest
                                cancelSettle()
                                settleJob = scope.launch {
                                    try {
                                        animate(
                                            initialValue = offsetPx,
                                            targetValue = widthPx * 0.88f,
                                            initialVelocity = velocity,
                                            animationSpec = CommitSpring
                                        ) { value, _ ->
                                            offsetPx = value
                                        }
                                    } finally {
                                        onCommitStartToEnd()
                                    }
                                    try {
                                        animate(
                                            initialValue = offsetPx,
                                            targetValue = 0f,
                                            animationSpec = ReturnSpring
                                        ) { value, _ ->
                                            offsetPx = value
                                        }
                                    } finally {
                                        committing = false
                                    }
                                }
                            }
                            TwoStepAnchor.Delete -> {
                                committing = true
                                cancelSettle()
                                settleJob = scope.launch {
                                    try {
                                        animate(
                                            initialValue = offsetPx,
                                            targetValue = -widthPx,
                                            initialVelocity = velocity,
                                            animationSpec = CommitSpring
                                        ) { value, _ ->
                                            offsetPx = value
                                        }
                                    } finally {
                                        onCommitEndToStart()
                                        committing = false
                                    }
                                }
                            }
                        }
                    }
                )
        ) {
            content()
            if (armed && !committing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            stage = TwoStepAnchor.Rest
                            snapTo(TwoStepAnchor.Rest, 0f, ReturnSpring)
                        }
                )
            }
        }
    }
}
