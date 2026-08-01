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
 * Soft center-bloom palette by utilization (no linear stripes).
 * Inspired by smooth radial color fields (see PixiDoGradientReference).
 *
 *  0%   → soft mint / green glow
 *  mid  → green + peach / orange
 *  high → orange / coral
 *  full → rose-red with a soft orange core
 */
private fun coreColors(utilization: Float): List<Color> {
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

    return when {
        t < 0.3f -> {
            val k = t / 0.3f
            listOf(
                Color.White.copy(alpha = 0.9f),
                lerp(mint, softGreen, k),
                lerp(softGreen, lime, k * 0.6f),
                Color.Transparent
            )
        }
        t < 0.65f -> {
            val k = (t - 0.3f) / 0.35f
            listOf(
                lerp(Color.White, cream, k * 0.5f),
                lerp(softGreen, peach, k),
                lerp(lime, orange, k),
                lerp(mint, Color.Transparent, 0.4f)
            )
        }
        t < 0.88f -> {
            val k = (t - 0.65f) / 0.23f
            listOf(
                lerp(cream, peach, k),
                lerp(peach, orange, k),
                lerp(orange, coral, k),
                lerp(softGreen, Color.Transparent, 0.7f)
            )
        }
        else -> {
            val k = ((t - 0.88f) / 0.12f).coerceIn(0f, 1f)
            listOf(
                lerp(peach, coral, k),
                lerp(orange, rose, k),
                lerp(coral, deepRose, k),
                Color.Transparent
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
 * Living credit fill — color blooms from the **center** of the card (radial, soft),
 * never linear stripes. A transparent edge vignette sits over the whole face;
 * as [utilization] rises the bloom expands and the vignette softens so the card
 * gradually fills with color.
 */
@Composable
fun LivingCreditGradientBox(
    utilization: Float,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val util = utilization.coerceIn(0f, 1f)
    val infinite = rememberInfiniteTransition(label = "creditBloom")

    // Gentle center drift + breathe (soothing motion, no harsh lines)
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val phase2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )
    val breathe by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val cx = w * 0.5f + w * 0.04f * cos(phase)
            val cy = h * 0.5f + h * 0.05f * sin(phase2)
            val center = Offset(cx, cy)
            val maxR = hypot(w, h) * 0.72f

            // Soft neutral base so empty credit still looks like a card
            drawRect(Color(0xFFF7F8FA))

            // ── Center bloom radius grows with utilization ────────────
            // Low util: small soft green core · High util: fills almost entire card
            val fill = 0.18f + util * 0.82f
            val bloomR = maxR * fill * breathe

            val colors = coreColors(util)

            // Primary soft radial bloom from center (smooth field, no lines)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to colors[0].copy(alpha = 0.95f),
                        0.28f to colors[1].copy(alpha = 0.88f),
                        0.58f to colors[2].copy(alpha = 0.72f),
                        1.0f to Color.Transparent
                    ),
                    center = center,
                    radius = bloomR
                ),
                radius = bloomR,
                center = center
            )

            // Secondary soft accent lobe (gives organic mesh feel like the reference)
            val lobeCenter = Offset(
                cx + w * 0.12f * cos(phase2 * 0.7f),
                cy + h * 0.10f * sin(phase * 0.9f)
            )
            val lobeR = bloomR * (0.55f + 0.15f * util)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colors.getOrElse(1) { colors[0] }.copy(alpha = 0.35f + util * 0.2f),
                        Color.Transparent
                    ),
                    center = lobeCenter,
                    radius = lobeR
                ),
                radius = lobeR,
                center = lobeCenter
            )

            // Warm highlight near center (reference soft glow)
            val hiR = bloomR * 0.35f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.35f - util * 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(cx - w * 0.06f, cy - h * 0.08f),
                    radius = hiR
                ),
                radius = hiR,
                center = Offset(cx - w * 0.06f, cy - h * 0.08f)
            )

            // ── Transparent vignette (strong when empty, softens as filled) ──
            drawVignette(
                strength = (1f - util * 0.88f).coerceIn(0.08f, 1f),
                w = w,
                h = h
            )
        }

        content()
    }
}

/**
 * Soft edge vignette: transparent center, darker/softer edges.
 * [strength] 1 = strong vignette (low utilization), 0 = nearly gone (full).
 */
private fun DrawScope.drawVignette(strength: Float, w: Float, h: Float) {
    val cx = w / 2f
    val cy = h / 2f
    val r = hypot(w, h) * 0.62f
    // Outer veil
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.42f to Color.Transparent,
                0.72f to Color(0xFF0A0A12).copy(alpha = 0.10f * strength),
                1.0f to Color(0xFF0A0A12).copy(alpha = 0.28f * strength)
            ),
            center = Offset(cx, cy),
            radius = r
        )
    )
    // Soft white rim fade for airy card look at low utilization
    drawRect(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color.Transparent,
                0.55f to Color.Transparent,
                1.0f to Color.White.copy(alpha = 0.22f * strength)
            ),
            center = Offset(cx, cy),
            radius = r * 1.05f
        )
    )
}

/** Readable text on the soft radial card face. */
fun creditGradientOnColor(utilization: Float): Color {
    return Color(0xFF1C1C1E).copy(alpha = 0.90f)
}

fun creditGradientMutedOnColor(utilization: Float): Color {
    return Color(0xFF1C1C1E).copy(alpha = 0.52f)
}
