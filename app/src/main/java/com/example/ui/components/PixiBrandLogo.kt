package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val PixiLogoBg = Color(0xFF0B0F19)
val PixiLogoTrack = Color(0xFF182838)
val PixiLogoCyan = Color(0xFF06B6D4)
val PixiLogoGreen = Color(0xFF10B981)

private const val ArcStartDeg = 165f
private const val ArcSweepDeg = 150f

/**
 * Interactive PixiDo mark from the app logo: dark tile, progress arc, green cap, check.
 * First play draws the arc then the check; tap replays. The green cap idles with a pulse.
 */
@Composable
fun PixiBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    animated: Boolean = true,
    onTap: (() -> Unit)? = null
) {
    var playId by remember { mutableIntStateOf(0) }
    val arc = remember { Animatable(if (animated) 0f else 1f) }
    val check = remember { Animatable(if (animated) 0f else 1f) }
    val dot = remember { Animatable(if (animated) 0f else 1f) }
    val pop = remember { Animatable(0.86f) }

    LaunchedEffect(playId, animated) {
        if (!animated) {
            arc.snapTo(1f)
            check.snapTo(1f)
            dot.snapTo(1f)
            pop.snapTo(1f)
            return@LaunchedEffect
        }
        arc.snapTo(0f)
        check.snapTo(0f)
        dot.snapTo(0f)
        pop.snapTo(0.86f)
        launch {
            pop.animateTo(1f, spring(dampingRatio = 0.56f, stiffness = 380f))
        }
        launch {
            arc.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
        }
        delay(280)
        launch {
            check.animateTo(1f, tween(420, easing = FastOutSlowInEasing))
        }
        delay(360)
        dot.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 520f))
    }

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale = if (pressed) 0.94f else 1f

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                val s = pop.value * pressScale
                scaleX = s
                scaleY = s
            }
            .clickable(
                interactionSource = interaction,
                indication = null
            ) {
                playId++
                onTap?.invoke()
            }
            .testTag("pixi_brand_logo")
            .drawWithCache {
                val s = min(this.size.width, this.size.height)
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val corner = s * 0.22f
                val radius = s * 0.255f
                val stroke = s * 0.068f
                val checkPath = Path().apply {
                    moveTo(cx - s * 0.125f, cy + s * 0.018f)
                    lineTo(cx - s * 0.018f, cy + s * 0.125f)
                    lineTo(cx + s * 0.162f, cy - s * 0.105f)
                }
                val measure = PathMeasure().apply { setPath(checkPath, false) }
                onDrawBehind {
                    val arcValue = arc.value
                    val checkValue = check.value
                    val dotValue = dot.value
                    drawRoundRect(
                        color = PixiLogoBg,
                        cornerRadius = CornerRadius(corner, corner)
                    )
                    drawCircle(
                        color = PixiLogoTrack,
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    if (arcValue > 0.01f) {
                        drawArc(
                            color = PixiLogoCyan,
                            startAngle = ArcStartDeg,
                            sweepAngle = ArcSweepDeg * arcValue,
                            useCenter = false,
                            topLeft = Offset(cx - radius, cy - radius),
                            size = Size(radius * 2f, radius * 2f),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    val ang = Math.toRadians(
                        (ArcStartDeg + ArcSweepDeg * arcValue).toDouble()
                    )
                    val cap = Offset(
                        cx + radius * cos(ang).toFloat(),
                        cy + radius * sin(ang).toFloat()
                    )
                    val capR = stroke * 0.58f * (0.15f + 0.85f * maxOf(dotValue, arcValue))
                    if (arcValue > 0.04f) {
                        drawCircle(color = PixiLogoGreen, radius = capR, center = cap)
                    }
                    if (checkValue > 0.01f && measure.length > 0f) {
                        val drawn = Path()
                        measure.getSegment(0f, measure.length * checkValue, drawn, true)
                        drawPath(
                            path = drawn,
                            color = Color.White,
                            style = Stroke(
                                width = s * 0.078f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }
    )
}
