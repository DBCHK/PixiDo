package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Soft palette by utilization — green → orange → red as credit fills.
 */
private fun fluidPalette(utilization: Float): List<Color> {
    val t = utilization.coerceIn(0f, 1f)
    val mint = Color(0xFFB8F5D4)
    val softGreen = Color(0xFF8FE0B8)
    val lime = Color(0xFFD4F0A8)
    val cream = Color(0xFFFFF0C2)
    val peach = Color(0xFFFFC896)
    val orange = Color(0xFFFFA86A)
    val coral = Color(0xFFFF8A78)
    val rose = Color(0xFFFF6B82)
    val deepRose = Color(0xFFE85A72)
    val white = Color.White

    return when {
        t < 0.3f -> {
            val k = t / 0.3f
            listOf(
                white.copy(alpha = 0.95f),
                lerp(mint, softGreen, k),
                lerp(softGreen, lime, k * 0.6f),
                lerp(lime, cream, k * 0.3f)
            )
        }
        t < 0.65f -> {
            val k = (t - 0.3f) / 0.35f
            listOf(
                lerp(white, cream, k * 0.5f),
                lerp(softGreen, peach, k),
                lerp(lime, orange, k),
                lerp(mint, peach, k * 0.7f)
            )
        }
        t < 0.88f -> {
            val k = (t - 0.65f) / 0.23f
            listOf(
                lerp(cream, peach, k),
                lerp(peach, orange, k),
                lerp(orange, coral, k),
                lerp(softGreen, peach, 1f - k * 0.5f)
            )
        }
        else -> {
            val k = ((t - 0.88f) / 0.12f).coerceIn(0f, 1f)
            listOf(
                lerp(peach, coral, k),
                lerp(orange, rose, k),
                lerp(coral, deepRose, k),
                lerp(lime, coral, 0.4f)
            )
        }
    }
}

private fun lerp(a: Color, b: Color, t: Float): Color {
    val x = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * x,
        green = a.green + (b.green - a.green) * x,
        blue = a.blue + (b.blue - a.blue) * x,
        alpha = a.alpha + (b.alpha - a.alpha) * x
    )
}

/**
 * Fluid multi-blob credit fill — independent orbits create a lava-lamp / liquid feel
 * (not a single rigid radial). Vignette softens as utilization rises.
 */
@Composable
fun LivingCreditGradientBox(
    utilization: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val util = utilization.coerceIn(0f, 1f)
    val palette = remember(util) { fluidPalette(util) }

    val infinite = rememberInfiniteTransition(label = "fluidCredit")
    val p1 by infinite.animateFloat(
        0f, (2f * PI).toFloat(),
        infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart),
        label = "p1"
    )
    val morph = 1f + 0.06f * sin(p1)
    val p2 = p1 * 0.73f
    val p3 = p1 * 1.17f
    val p4 = p1 * 0.91f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val maxR = hypot(w, h)
            val fill = 0.20f + util * 0.80f

            // Soft base that tints slightly with fill color
            drawRect(lerp(Color(0xFFF7F8FA), palette[1].copy(alpha = 1f), util * 0.12f))

            // ── Fluid blobs: Lissajous-like paths so they swirl, not orbit rigidly ──
            data class Blob(
                val cx: Float,
                val cy: Float,
                val radius: Float,
                val color: Color,
                val alpha: Float
            )

            val blobs = listOf(
                // Core bloom — drifts slowly around center
                Blob(
                    cx = w * (0.50f + 0.12f * sin(p1) + 0.06f * cos(p2)),
                    cy = h * (0.48f + 0.14f * cos(p1 * 0.9f) + 0.05f * sin(p3)),
                    radius = maxR * (0.42f + 0.28f * fill) * morph,
                    color = palette[0],
                    alpha = 0.92f
                ),
                // Secondary mass — counter-rotates
                Blob(
                    cx = w * (0.38f + 0.22f * cos(p2) + 0.08f * sin(p4)),
                    cy = h * (0.55f + 0.18f * sin(p2 * 1.1f) + 0.07f * cos(p1)),
                    radius = maxR * (0.36f + 0.22f * fill) * (2f - morph * 0.85f),
                    color = palette[1],
                    alpha = 0.72f + util * 0.12f
                ),
                Blob(
                    cx = w * (0.62f + 0.18f * sin(p3 * 0.8f) + 0.1f * cos(p1)),
                    cy = h * (0.38f + 0.20f * cos(p3) + 0.08f * sin(p2)),
                    radius = maxR * (0.30f + 0.20f * fill) * (0.9f + 0.1f * sin(p4)),
                    color = palette[2],
                    alpha = 0.55f + util * 0.2f
                )
            )

            blobs.forEach { blob ->
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to blob.color.copy(alpha = blob.alpha),
                            0.45f to blob.color.copy(alpha = blob.alpha * 0.55f),
                            1.0f to Color.Transparent
                        ),
                        center = Offset(blob.cx, blob.cy),
                        radius = blob.radius
                    ),
                    radius = blob.radius,
                    center = Offset(blob.cx, blob.cy)
                )
            }

            // Soft cross-fade veil that slowly shifts hue feel
            val veilCx = w * (0.5f + 0.08f * sin(p2 * 0.5f))
            val veilCy = h * (0.5f + 0.08f * cos(p3 * 0.5f))
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        palette[1].copy(alpha = 0.12f + util * 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(veilCx, veilCy),
                    radius = maxR * 0.55f
                ),
                radius = maxR * 0.55f,
                center = Offset(veilCx, veilCy)
            )

            drawVignette(
                strength = (1f - util * 0.88f).coerceIn(0.08f, 1f),
                w = w,
                h = h
            )
        }

        content()
    }
}

private fun DrawScope.drawVignette(strength: Float, w: Float, h: Float) {
    val cx = w / 2f
    val cy = h / 2f
    val r = hypot(w, h) * 0.62f
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.42f to Color.Transparent,
                0.72f to Color(0xFF0A0A12).copy(alpha = 0.10f * strength),
                1.0f to Color(0xFF0A0A12).copy(alpha = 0.26f * strength)
            ),
            center = Offset(cx, cy),
            radius = r
        )
    )
}

fun creditGradientOnColor(utilization: Float): Color {
    return Color(0xFF1C1C1E).copy(alpha = 0.90f)
}

fun creditGradientMutedOnColor(utilization: Float): Color {
    return Color(0xFF1C1C1E).copy(alpha = 0.52f)
}
