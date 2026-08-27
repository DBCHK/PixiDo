package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.HazeDialog
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * Shared blur source for overlays (island, banners, snackbar, sheets).
 * Null when glass is off, or the caller sits inside the source.
 */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/** Master switch from Settings. When false, chrome uses solid surfaces. */
val LocalGlassEnabled = compositionLocalOf { true }

/** How heavy the frost is — bars stay airy; sheets must hide text behind them. */
enum class PixiGlassWeight { Bar, Sheet }

/**
 * iOS-style water glass. Real backdrop blur via Haze when glass is enabled
 * and a [LocalHazeState] is in scope.
 *
 * [frost] = false keeps a visual-only fill — for chrome inside the haze source.
 * [liquid] adds a slow water sheen — nav pill and sheets.
 */
@Composable
fun PixiGlass(
    modifier: Modifier = Modifier,
    shape: Shape = PixiIslandShape,
    liquid: Boolean = false,
    elevation: Dp = 10.dp,
    frost: Boolean = true,
    weight: PixiGlassWeight = PixiGlassWeight.Bar,
    content: @Composable BoxScope.() -> Unit
) {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.45f
    val glassOn = LocalGlassEnabled.current
    val blurSource = LocalHazeState.current.takeIf { glassOn && frost }
    val frosted = blurSource != null
    val fill = glassFill(light, glassOn, frosted, weight)
    val highlight = glassHighlight(light, glassOn, frosted, weight)
    val rim = when {
        !glassOn -> MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        light -> Color.White.copy(alpha = if (weight == PixiGlassWeight.Sheet) 0.78f else 0.70f)
        else -> Color.White.copy(alpha = 0.22f)
    }
    val shadow = Color.Black.copy(alpha = if (light) 0.12f else 0.50f)
    val frostBlur = when (weight) {
        PixiGlassWeight.Bar -> 40.dp
        PixiGlassWeight.Sheet -> 48.dp
    }
    val glassStyle = waterGlassStyle(light, MaterialTheme.colorScheme.background, frostBlur, weight)

    Box(
        modifier = modifier
            .shadow(
                elevation = if (glassOn) elevation else elevation.coerceAtMost(8.dp),
                shape = shape,
                ambientColor = shadow,
                spotColor = shadow
            )
            .clip(shape)
            .then(
                if (blurSource != null) {
                    Modifier.hazeEffect(state = blurSource, style = glassStyle) {
                        blurEnabled = true
                        blurRadius = frostBlur
                    }
                } else {
                    Modifier.background(fill)
                }
            )
            .then(
                if (glassOn) {
                    Modifier.background(
                        Brush.verticalGradient(
                            0f to highlight,
                            0.28f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1f to Color.Black.copy(alpha = if (light) 0.06f else 0.22f)
                        )
                    )
                } else {
                    Modifier
                }
            )
            .border(0.5.dp, rim, shape)
    ) {
        if (liquid && glassOn) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .liquidSheen(light)
            )
        }
        content()
    }
}

/**
 * Full-screen water-glass dialog: the page behind is heavily frosted so its
 * text cannot be read through Settings / sheets.
 */
@Composable
fun PixiGlassHost(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(
        usePlatformDefaultWidth = false,
        decorFitsSystemWindows = false
    ),
    content: @Composable BoxScope.() -> Unit
) {
    val hazeState = LocalHazeState.current
    val glassOn = LocalGlassEnabled.current
    val body = @Composable {
        Box(modifier = Modifier.fillMaxSize()) {
            PixiWaterScrim(
                onDismiss = onDismissRequest,
                hazeState = hazeState,
                glassOn = glassOn
            )
            content()
        }
    }
    if (hazeState != null && glassOn) {
        HazeDialog(
            hazeState = hazeState,
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = body
        )
    } else {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = body
        )
    }
}

@Composable
private fun PixiWaterScrim(
    onDismiss: () -> Unit,
    hazeState: HazeState?,
    glassOn: Boolean
) {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.45f
    val frostBlur = 56.dp
    val style = waterGlassStyle(
        light = light,
        container = MaterialTheme.colorScheme.background,
        blurRadius = frostBlur,
        weight = PixiGlassWeight.Sheet
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (glassOn && hazeState != null) {
                    Modifier.hazeEffect(state = hazeState, style = style) {
                        blurEnabled = true
                        blurRadius = frostBlur
                    }
                } else {
                    Modifier.background(Color.Black.copy(alpha = 0.48f))
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    )
}

@Composable
private fun glassFill(
    light: Boolean,
    glassOn: Boolean,
    frosted: Boolean,
    weight: PixiGlassWeight
): Color {
    if (!glassOn) return MaterialTheme.colorScheme.surface
    if (frosted) return Color.Transparent
    return when (weight) {
        PixiGlassWeight.Sheet ->
            if (light) Color.White.copy(alpha = 0.92f) else Color(0xFF1C1C1E).copy(alpha = 0.94f)
        PixiGlassWeight.Bar ->
            if (light) Color.White.copy(alpha = 0.55f) else Color(0xFF2C2C2E).copy(alpha = 0.62f)
    }
}

private fun glassHighlight(
    light: Boolean,
    glassOn: Boolean,
    frosted: Boolean,
    weight: PixiGlassWeight
): Color {
    if (!glassOn) return Color.Transparent
    val base = if (weight == PixiGlassWeight.Sheet) 0.42f else 0.28f
    return if (light) Color.White.copy(alpha = if (frosted) base else 0.55f)
    else Color.White.copy(alpha = 0.12f)
}

private fun waterGlassStyle(
    light: Boolean,
    container: Color,
    blurRadius: Dp,
    weight: PixiGlassWeight
): HazeStyle {
    val tint = when {
        weight == PixiGlassWeight.Sheet && light -> Color.White.copy(alpha = 0.62f)
        weight == PixiGlassWeight.Sheet && !light -> Color(0xFF1C1C1E).copy(alpha = 0.72f)
        light -> Color.White.copy(alpha = 0.20f)
        else -> Color(0xFF1C1C1E).copy(alpha = 0.30f)
    }
    val water = if (light) Color(0xFFD4ECFF).copy(alpha = 0.10f)
    else Color(0xFF88AACC).copy(alpha = 0.08f)
    val fallback = if (light) Color.White.copy(alpha = 0.88f)
    else Color(0xFF1C1C1E).copy(alpha = 0.90f)
    return HazeStyle(
        backgroundColor = container,
        blurRadius = blurRadius,
        noiseFactor = 0.10f,
        tints = listOf(HazeTint(tint), HazeTint(water)),
        fallbackTint = HazeTint(fallback)
    )
}

@Composable
private fun Modifier.liquidSheen(light: Boolean): Modifier {
    val infinite = rememberInfiniteTransition(label = "liquidGlass")
    val t by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sheen"
    )
    val caustic by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 17000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "caustic"
    )
    val sheen = if (light) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.10f)
    val water = if (light) Color(0xFFD8E8FF).copy(alpha = 0.14f) else Color(0xFF88AACC).copy(alpha = 0.10f)
    return drawBehind {
        val w = size.width
        val h = size.height
        val x = -w * 0.3f + (w * 1.6f) * t
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, sheen, Color.Transparent),
                start = Offset(x, 0f),
                end = Offset(x + w * 0.35f, h)
            )
        )
        val cx = w * (0.2f + 0.6f * caustic)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(water, Color.Transparent),
                center = Offset(cx, h * 0.28f),
                radius = h * 1.8f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(sheen.copy(alpha = sheen.alpha * 0.5f), Color.Transparent),
                center = Offset(w * (0.75f - 0.4f * caustic), h * 0.7f),
                radius = h * 1.1f
            )
        )
    }
}

@Composable
fun glassFillColor(): Color {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.45f
    return if (light) Color.White.copy(alpha = 0.45f) else Color(0xFF2C2C2E).copy(alpha = 0.5f)
}
