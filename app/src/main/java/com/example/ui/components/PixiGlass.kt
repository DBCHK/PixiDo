package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDialog
import dev.chrisbanes.haze.HazeInputScale
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlin.math.min

/**
 * Shared blur source for overlays (island, banners, snackbar, sheets).
 * Null when glass is off, or the caller sits inside the source.
 */
val LocalHazeState = compositionLocalOf<HazeState?> { null }

/** Master switch from Settings. When false, chrome uses solid surfaces. */
val LocalGlassEnabled = compositionLocalOf { true }

/** When true, skip infinite sheens / doodle orbits / fluid blobs / tilt specular. */
val LocalReduceMotion = compositionLocalOf { false }

/** How heavy the frost is — bars stay airy; sheets must hide text behind them. */
enum class PixiGlassWeight { Bar, Sheet }

/**
 * Liquid Glass roles, matching Apple HIG (WWDC25 / Materials):
 * - [Chrome]: tab bars, snackbars — Regular variant, floats above content
 * - [Control]: buttons, search, chips — Clear-leaning, reacts to light
 * - [Sheet]: dialogs, settings — thicker Regular, built for readability
 * - [Content]: standard thin material in the content layer (not Liquid Glass)
 *
 * Apple: "Don't use Liquid Glass in the content layer."
 */
enum class PixiGlassRole { Chrome, Control, Sheet, Content }

fun resolvePixiGlassRole(
    role: PixiGlassRole?,
    liquid: Boolean,
    weight: PixiGlassWeight
): PixiGlassRole {
    if (role != null) return role
    return when {
        weight == PixiGlassWeight.Sheet -> PixiGlassRole.Sheet
        liquid -> PixiGlassRole.Chrome
        else -> PixiGlassRole.Content
    }
}

/**
 * iOS 26 Liquid Glass.
 *
 * Optical stack (bottom → top), from Apple's "Meet Liquid Glass":
 *  1. Backdrop blur that samples content behind (Haze)
 *  2. Cupertino ColorDodge/Overlay + gray wash (iOS 18 Figma / Haze CupertinoMaterials)
 *  3. Edge lensing — a specular rim that bends with the key light
 *  4. Moving specular highlight (device tilt on chrome/controls)
 *  5. Inner thickness shadow
 *  6. Hairline
 *
 * Content-role surfaces skip tilt and refraction so lists stay cheap.
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun PixiGlass(
    modifier: Modifier = Modifier,
    shape: Shape = PixiIslandShape,
    role: PixiGlassRole? = null,
    liquid: Boolean = false,
    elevation: Dp = 10.dp,
    frost: Boolean? = null,
    weight: PixiGlassWeight = PixiGlassWeight.Bar,
    tint: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit
) {
    val resolved = resolvePixiGlassRole(role, liquid, weight)
    val lightTheme = MaterialTheme.colorScheme.background.luminance() > 0.45f
    val glassOn = LocalGlassEnabled.current
    val reduceMotion = LocalReduceMotion.current
    val liveLight = resolved != PixiGlassRole.Content && glassOn && !reduceMotion
    val glassLight = if (liveLight) LocalGlassLight.current else GlassLight.Rest
    val wantFrost = frost ?: (resolved != PixiGlassRole.Content)
    val blurSource = LocalHazeState.current.takeIf { glassOn && wantFrost }
    val frosted = blurSource != null
    val fill = glassFill(lightTheme, glassOn, frosted, resolved)
    val glassStyle = cupertinoGlassStyle(
        light = lightTheme,
        surface = MaterialTheme.colorScheme.surface,
        role = resolved,
        tint = tint
    )
    val shadowAlpha = when (resolved) {
        PixiGlassRole.Chrome -> if (lightTheme) 0.14f else 0.50f
        PixiGlassRole.Control -> if (lightTheme) 0.10f else 0.36f
        PixiGlassRole.Sheet -> if (lightTheme) 0.18f else 0.55f
        PixiGlassRole.Content -> if (lightTheme) 0.06f else 0.28f
    }
    val wantsOptics = glassOn && resolved != PixiGlassRole.Content
    val wantsContentSheen = glassOn && resolved == PixiGlassRole.Content

    Box(
        modifier = modifier
            .shadow(
                elevation = if (glassOn) elevation else elevation.coerceAtMost(6.dp),
                shape = shape,
                ambientColor = Color.Black.copy(alpha = shadowAlpha * 0.55f),
                spotColor = Color.Black.copy(alpha = shadowAlpha)
            )
            .clip(shape)
            .then(
                if (blurSource != null) {
                    Modifier.hazeEffect(state = blurSource, style = glassStyle) {
                        blurEnabled = true
                        inputScale = HazeInputScale.Auto
                    }
                } else {
                    Modifier.background(fill)
                }
            )
            .then(
                if (wantsOptics) {
                    Modifier.drawWithCache {
                        val outline = shape.createOutline(size, layoutDirection, this)
                        val nx = glassLight.nx
                        val ny = glassLight.ny
                        val specX = size.width * (0.50f + nx * 0.34f)
                        val specY = size.height * (0.16f + ny * 0.18f).coerceIn(0.06f, 0.48f)
                        val specR = min(size.width, size.height) *
                            if (resolved == PixiGlassRole.Control) 0.62f else 0.48f
                        val specAlpha = if (lightTheme) 0.42f else 0.26f
                        val rim = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (lightTheme) 0.62f else 0.34f),
                                Color.White.copy(alpha = 0.08f),
                                Color.White.copy(alpha = if (lightTheme) 0.28f else 0.14f)
                            ),
                            start = Offset(
                                size.width * (0.5f + nx * 0.55f),
                                size.height * (0.05f + ny * 0.2f)
                            ),
                            end = Offset(
                                size.width * (0.5f - nx * 0.55f),
                                size.height
                            )
                        )
                        val thickness = Brush.verticalGradient(
                            0f to Color.White.copy(
                                alpha = if (lightTheme) 0.20f else 0.10f
                            ),
                            0.18f to Color.Transparent,
                            0.72f to Color.Transparent,
                            1f to Color.Black.copy(
                                alpha = if (lightTheme) 0.07f else 0.26f
                            )
                        )
                        val specular = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = specAlpha),
                                Color.White.copy(alpha = specAlpha * 0.22f),
                                Color.Transparent
                            ),
                            center = Offset(specX, specY),
                            radius = specR
                        )
                        val caustic = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = if (lightTheme) 0.14f else 0.08f),
                                Color.Transparent
                            ),
                            start = Offset(size.width * (0.12f + nx * 0.2f), 0f),
                            end = Offset(size.width * (0.48f + nx * 0.15f), size.height * 0.55f)
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(thickness)
                            drawRect(specular)
                            drawRect(caustic)
                            drawOutline(
                                outline = outline,
                                brush = rim,
                                style = Stroke(width = 1.6.dp.toPx())
                            )
                        }
                    }
                } else if (wantsContentSheen) {
                    Modifier.drawWithCache {
                        val highlight = Brush.verticalGradient(
                            0f to Color.White.copy(alpha = if (lightTheme) 0.22f else 0.10f),
                            0.28f to Color.White.copy(alpha = 0.05f),
                            1f to Color.Transparent
                        )
                        onDrawWithContent {
                            drawContent()
                            drawRect(highlight)
                        }
                    }
                } else {
                    Modifier
                }
            )
            .border(iosHairline(lightTheme, resolved, glassLight), shape)
    ) {
        content()
    }
}

/**
 * Full-screen water-glass dialog: the page behind is heavily frosted so its
 * text cannot be read through Settings / sheets.
 */
@OptIn(ExperimentalHazeApi::class)
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

@OptIn(ExperimentalHazeApi::class)
@Composable
private fun PixiWaterScrim(
    onDismiss: () -> Unit,
    hazeState: HazeState?,
    glassOn: Boolean
) {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.45f
    val style = cupertinoGlassStyle(
        light = light,
        surface = MaterialTheme.colorScheme.surface,
        role = PixiGlassRole.Sheet,
        tint = Color.Unspecified
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (glassOn && hazeState != null) {
                    Modifier.hazeEffect(state = hazeState, style = style) {
                        blurEnabled = true
                        inputScale = HazeInputScale.Auto
                    }
                } else {
                    Modifier.background(
                        if (light) Color.Black.copy(alpha = 0.28f)
                        else Color.Black.copy(alpha = 0.52f)
                    )
                }
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    )
}

/**
 * Apple iOS 18/26 material tints (iOS 18 Figma / Haze CupertinoMaterials),
 * with Liquid Glass Regular vs Clear wash strengths.
 */
private fun cupertinoGlassStyle(
    light: Boolean,
    surface: Color,
    role: PixiGlassRole,
    tint: Color
): HazeStyle {
    val (dodge, wash, blur, noise) = when (role) {
        PixiGlassRole.Sheet -> if (light) {
            Quad(Color(0xFF333333), Color(0xA6A6A6).copy(alpha = 0.58f), 36.dp, 0.12f)
        } else {
            Quad(Color(0xFF9C9C9C), Color(0x252525).copy(alpha = 0.68f), 36.dp, 0.12f)
        }
        PixiGlassRole.Control -> if (light) {
            Quad(Color(0xFF0D0D0D), Color(0xBFBFBF).copy(alpha = 0.22f), 20.dp, 0.08f)
        } else {
            Quad(Color(0xFF9C9C9C), Color(0x252525).copy(alpha = 0.32f), 20.dp, 0.08f)
        }
        PixiGlassRole.Content -> if (light) {
            Quad(Color(0xFF0D0D0D), Color(0xBFBFBF).copy(alpha = 0.28f), 18.dp, 0.06f)
        } else {
            Quad(Color(0xFF9C9C9C), Color(0x252525).copy(alpha = 0.36f), 18.dp, 0.06f)
        }
        PixiGlassRole.Chrome -> if (light) {
            Quad(Color(0xFF0D0D0D), Color(0xBFBFBF).copy(alpha = 0.30f), 28.dp, 0.10f)
        } else {
            Quad(Color(0xFF9C9C9C), Color(0x252525).copy(alpha = 0.42f), 28.dp, 0.10f)
        }
    }
    val fallback = if (light) Color.White.copy(alpha = 0.72f)
    else Color(0xFF1C1C1E).copy(alpha = 0.78f)
    val tints = buildList {
        add(
            HazeTint(
                color = dodge,
                blendMode = if (light) BlendMode.ColorDodge else BlendMode.Overlay
            )
        )
        add(HazeTint(color = wash))
        if (tint != Color.Unspecified && tint.alpha > 0.01f) {
            add(HazeTint(color = tint.copy(alpha = tint.alpha.coerceAtMost(0.28f))))
        }
    }
    return HazeStyle(
        backgroundColor = surface,
        blurRadius = blur,
        noiseFactor = noise,
        tints = tints,
        fallbackTint = HazeTint(fallback)
    )
}

private data class Quad(
    val dodge: Color,
    val wash: Color,
    val blur: Dp,
    val noise: Float
)

private fun iosHairline(
    light: Boolean,
    role: PixiGlassRole,
    glassLight: GlassLight
): BorderStroke {
    val top = when (role) {
        PixiGlassRole.Chrome, PixiGlassRole.Control -> if (light) 0.70f else 0.32f
        PixiGlassRole.Sheet -> if (light) 0.55f else 0.24f
        PixiGlassRole.Content -> if (light) 0.40f else 0.16f
    }
    val bot = if (light) 0.08f else 0.05f
    val nx = glassLight.nx
    return BorderStroke(
        width = if (role == PixiGlassRole.Content) 0.5.dp else 0.7.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = top),
                Color.White.copy(alpha = top * 0.35f),
                Color.White.copy(alpha = bot)
            ),
            start = Offset(80f * (1f + nx), 0f),
            end = Offset(0f, 240f)
        )
    )
}

@Composable
private fun glassFill(
    light: Boolean,
    glassOn: Boolean,
    frosted: Boolean,
    role: PixiGlassRole
): Color {
    if (!glassOn) return MaterialTheme.colorScheme.surface
    if (frosted) return Color.Transparent
    return when (role) {
        PixiGlassRole.Sheet ->
            if (light) Color.White.copy(alpha = 0.86f) else Color(0xFF1C1C1E).copy(alpha = 0.88f)
        PixiGlassRole.Chrome ->
            if (light) Color.White.copy(alpha = 0.55f) else Color(0xFF2C2C2E).copy(alpha = 0.58f)
        PixiGlassRole.Control ->
            if (light) Color.White.copy(alpha = 0.42f) else Color(0xFF2C2C2E).copy(alpha = 0.48f)
        PixiGlassRole.Content ->
            if (light) Color.White.copy(alpha = 0.78f) else Color(0xFF1C1C1E).copy(alpha = 0.72f)
    }
}

@Composable
fun glassFillColor(): Color {
    val light = MaterialTheme.colorScheme.background.luminance() > 0.45f
    return if (light) Color.White.copy(alpha = 0.45f) else Color(0xFF2C2C2E).copy(alpha = 0.5f)
}
