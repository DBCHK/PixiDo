package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.AppThemeOption

/** Soft Lilac light — primary look matching idea2 reference. */
private val PixiDoLightScheme = lightColorScheme(
    primary = PixiLavender,
    onPrimary = Color.White,
    primaryContainer = PixiLavenderSoft,
    onPrimaryContainer = PixiLavenderDeep,
    secondary = PixiPink,
    onSecondary = Color.White,
    secondaryContainer = PixiPinkSoft,
    onSecondaryContainer = Color(0xFF9B2D5C),
    tertiary = PixiYellowDeep,
    onTertiary = Color(0xFF1C1C1E),
    tertiaryContainer = Color(0xFFFFF6C8),
    onTertiaryContainer = Color(0xFF5C4A00),
    background = PixiLightBg,
    surface = PixiLightSurface,
    surfaceVariant = PixiLightSurfaceVariant,
    onBackground = PixiLightOnBg,
    onSurface = PixiLightOnBg,
    onSurfaceVariant = PixiLightMuted,
    outline = PixiLightBorder,
    outlineVariant = PixiLightChip,
    error = PixiCoral,
    onError = Color.White,
    errorContainer = Color(0xFFFFE4E8),
    onErrorContainer = Color(0xFF8B1E2D),
    inverseSurface = Color(0xFF2A2436),
    inverseOnSurface = Color(0xFFF5F3FA),
    inversePrimary = PixiLavenderSoft,
    surfaceTint = PixiLavender
)

/** Soft Lilac dark — same language, night surfaces. */
private val PixiDoDarkScheme = darkColorScheme(
    primary = PixiLavender,
    onPrimary = Color(0xFF1C1C1E),
    primaryContainer = Color(0xFF3D2F5C),
    onPrimaryContainer = PixiLavenderSoft,
    secondary = PixiPink,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF5C1F3A),
    onSecondaryContainer = PixiPinkSoft,
    tertiary = PixiYellow,
    onTertiary = Color(0xFF1C1C1E),
    background = PixiDarkBg,
    surface = PixiDarkSurface,
    surfaceVariant = PixiDarkSurfaceVariant,
    onBackground = PixiDarkOnBg,
    onSurface = PixiDarkOnBg,
    onSurfaceVariant = PixiDarkMuted,
    outline = PixiDarkBorder,
    error = PixiCoral,
    surfaceTint = PixiLavender
)

private val OceanScheme = darkColorScheme(
    primary = OceanPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = OceanPrimary,
    secondary = OceanAccent,
    onSecondary = Color.Black,
    background = OceanBg,
    surface = OceanSurface,
    surfaceVariant = Color(0xFF123244),
    onBackground = Color(0xFFE0F2FE),
    onSurface = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF7DD3FC),
    outline = Color(0xFF1E4A5F),
    error = PixiCoral
)

private val SunsetScheme = darkColorScheme(
    primary = SunsetPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF7C2D12),
    onPrimaryContainer = SunsetPrimary,
    secondary = SunsetAccent,
    onSecondary = Color.Black,
    background = SunsetBg,
    surface = SunsetSurface,
    surfaceVariant = Color(0xFF3B1D14),
    onBackground = Color(0xFFFFF7ED),
    onSurface = Color(0xFFFFF7ED),
    onSurfaceVariant = Color(0xFFFDBA74),
    outline = Color(0xFF5C2E1E),
    error = PixiCoral
)

private val ForestScheme = darkColorScheme(
    primary = ForestPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = ForestAccent,
    secondary = ForestAccent,
    onSecondary = Color.Black,
    background = ForestBg,
    surface = ForestSurface,
    surfaceVariant = Color(0xFF163524),
    onBackground = Color(0xFFECFDF5),
    onSurface = Color(0xFFECFDF5),
    onSurfaceVariant = Color(0xFF86EFAC),
    outline = Color(0xFF1F4A32),
    error = PixiCoral
)

private val MidnightScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF701A75),
    onPrimaryContainer = MidnightPrimary,
    secondary = MidnightAccent,
    onSecondary = Color.Black,
    background = MidnightBg,
    surface = MidnightSurface,
    surfaceVariant = Color(0xFF1F1230),
    onBackground = Color(0xFFFAE8FF),
    onSurface = Color(0xFFFAE8FF),
    onSurfaceVariant = Color(0xFFD8B4FE),
    outline = Color(0xFF3B2654),
    error = PixiCoral
)

private val RoseScheme = lightColorScheme(
    primary = RosePrimary,
    onPrimary = Color.White,
    primaryContainer = RoseContainer,
    onPrimaryContainer = Color(0xFF9D174D),
    secondary = Color(0xFFFB7185),
    onSecondary = Color.White,
    background = RoseBg,
    surface = RoseSurface,
    surfaceVariant = Color(0xFFFFF0F6),
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF8E6B7A),
    outline = Color(0xFFF3D4E2),
    error = PixiCoral
)

private val SandScheme = lightColorScheme(
    primary = SandPrimary,
    onPrimary = Color.White,
    primaryContainer = SandContainer,
    onPrimaryContainer = Color(0xFF7C4A1E),
    secondary = Color(0xFFC4A484),
    onSecondary = Color.White,
    background = SandBg,
    surface = SandSurface,
    surfaceVariant = Color(0xFFF3E8DC),
    onBackground = Color(0xFF2A2118),
    onSurface = Color(0xFF2A2118),
    onSurfaceVariant = Color(0xFF8A7A68),
    outline = Color(0xFFE8D9C8),
    error = PixiCoral
)

private val SkyScheme = lightColorScheme(
    primary = SkyPrimary,
    onPrimary = Color.White,
    primaryContainer = SkyContainer,
    onPrimaryContainer = Color(0xFF1E40AF),
    secondary = Color(0xFF38BDF8),
    onSecondary = Color.Black,
    background = SkyBg,
    surface = SkySurface,
    surfaceVariant = Color(0xFFE8F1FE),
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFD0E0F5),
    error = PixiCoral
)

private val PeachScheme = lightColorScheme(
    primary = PeachPrimary,
    onPrimary = Color.White,
    primaryContainer = PeachContainer,
    onPrimaryContainer = Color(0xFF9A3412),
    secondary = Color(0xFFFDBA74),
    onSecondary = Color(0xFF1C1C1E),
    background = PeachBg,
    surface = PeachSurface,
    surfaceVariant = Color(0xFFFFEEDD),
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFF8B7355),
    outline = Color(0xFFF0DCC8),
    error = PixiCoral
)

private val AuroraScheme = darkColorScheme(
    primary = AuroraPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF134E4A),
    onPrimaryContainer = AuroraPrimary,
    secondary = AuroraAccent,
    onSecondary = Color.Black,
    background = AuroraBg,
    surface = AuroraSurface,
    surfaceVariant = Color(0xFF1A2C38),
    onBackground = Color(0xFFE0F2FE),
    onSurface = Color(0xFFE0F2FE),
    onSurfaceVariant = Color(0xFF99F6E4),
    outline = Color(0xFF234050),
    error = PixiCoral
)

private val CherryScheme = darkColorScheme(
    primary = CherryPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF7F1D2D),
    onPrimaryContainer = Color(0xFFFFE4E8),
    secondary = CherryAccent,
    onSecondary = Color.Black,
    background = CherryBg,
    surface = CherrySurface,
    surfaceVariant = Color(0xFF2E1820),
    onBackground = Color(0xFFFFF1F2),
    onSurface = Color(0xFFFFF1F2),
    onSurfaceVariant = Color(0xFFFECDD3),
    outline = Color(0xFF4A2030),
    error = PixiCoral
)

private val GraphiteScheme = darkColorScheme(
    primary = GraphitePrimary,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = GraphiteAccent,
    secondary = GraphiteAccent,
    onSecondary = Color.Black,
    background = GraphiteBg,
    surface = GraphiteSurface,
    surfaceVariant = Color(0xFF252A33),
    onBackground = Color(0xFFF1F5F9),
    onSurface = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF3A4150),
    error = PixiCoral
)

private val MintScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF6EE7B7),
    onSecondary = Color(0xFF064E3B),
    background = MintBg,
    surface = MintSurface,
    surfaceVariant = Color(0xFFE6F9F0),
    onBackground = Color(0xFF0F1F18),
    onSurface = Color(0xFF0F1F18),
    onSurfaceVariant = Color(0xFF5A7A6A),
    outline = Color(0xFFCDEBD9),
    error = PixiCoral
)

/** Soften a color toward white for containers. */
fun Color.asSoftContainer(amount: Float = 0.78f): Color {
    val a = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * a,
        green = green + (1f - green) * a,
        blue = blue + (1f - blue) * a,
        alpha = 1f
    )
}

fun Color.asDeepOnContainer(): Color {
    // Darken for readable text on soft containers
    return Color(
        red = (red * 0.35f).coerceIn(0f, 1f),
        green = (green * 0.35f).coerceIn(0f, 1f),
        blue = (blue * 0.35f).coerceIn(0f, 1f),
        alpha = 1f
    )
}

/** Apply optional custom accent on top of a base scheme. */
fun ColorScheme.withAccentHex(hex: String?): ColorScheme {
    if (hex.isNullOrBlank()) return this
    val accent = runCatching {
        Color(android.graphics.Color.parseColor(hex.trim()))
    }.getOrNull() ?: return this
    val container = accent.asSoftContainer()
    val onContainer = accent.asDeepOnContainer()
    val luminance = 0.299f * accent.red + 0.587f * accent.green + 0.114f * accent.blue
    val onPrimary = if (luminance > 0.65f) Color(0xFF1C1C1E) else Color.White
    return copy(
        primary = accent,
        onPrimary = onPrimary,
        primaryContainer = container,
        onPrimaryContainer = onContainer,
        surfaceTint = accent
    )
}

@Composable
fun resolveColorScheme(
    themeOption: AppThemeOption,
    accentColorHex: String = "",
    systemDark: Boolean = isSystemInDarkTheme()
): ColorScheme {
    val context = LocalContext.current
    val base = when (themeOption) {
        AppThemeOption.MATERIAL_YOU -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (systemDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (systemDark) PixiDoDarkScheme else PixiDoLightScheme
            }
        }
        AppThemeOption.SYSTEM -> {
            if (systemDark) PixiDoDarkScheme else PixiDoLightScheme
        }
        AppThemeOption.PIXIDO_DARK -> PixiDoDarkScheme
        AppThemeOption.PIXIDO_LIGHT -> PixiDoLightScheme
        AppThemeOption.OCEAN -> OceanScheme
        AppThemeOption.SUNSET -> SunsetScheme
        AppThemeOption.FOREST -> ForestScheme
        AppThemeOption.MIDNIGHT -> MidnightScheme
        AppThemeOption.ROSE -> RoseScheme
        AppThemeOption.SAND -> SandScheme
        AppThemeOption.SKY -> SkyScheme
        AppThemeOption.PEACH -> PeachScheme
        AppThemeOption.AURORA -> AuroraScheme
        AppThemeOption.CHERRY -> CherryScheme
        AppThemeOption.GRAPHITE -> GraphiteScheme
        AppThemeOption.MINT -> MintScheme
    }
    return base.withAccentHex(accentColorHex)
}

fun AppThemeOption.displayName(): String = when (this) {
    AppThemeOption.MATERIAL_YOU -> "Material You"
    AppThemeOption.SYSTEM -> "System"
    AppThemeOption.PIXIDO_DARK -> "Soft Night"
    AppThemeOption.PIXIDO_LIGHT -> "Soft Lilac"
    AppThemeOption.OCEAN -> "Ocean"
    AppThemeOption.SUNSET -> "Sunset"
    AppThemeOption.FOREST -> "Forest"
    AppThemeOption.MIDNIGHT -> "Midnight"
    AppThemeOption.ROSE -> "Rose"
    AppThemeOption.SAND -> "Sand"
    AppThemeOption.SKY -> "Sky"
    AppThemeOption.PEACH -> "Peach"
    AppThemeOption.AURORA -> "Aurora"
    AppThemeOption.CHERRY -> "Cherry"
    AppThemeOption.GRAPHITE -> "Graphite"
    AppThemeOption.MINT -> "Mint"
}

fun AppThemeOption.description(): String = when (this) {
    AppThemeOption.MATERIAL_YOU -> "Wallpaper colors (Android 12+)"
    AppThemeOption.SYSTEM -> "Follow device light/dark"
    AppThemeOption.PIXIDO_DARK -> "Soft lilac on night surfaces"
    AppThemeOption.PIXIDO_LIGHT -> "Clean pastel workspace"
    AppThemeOption.OCEAN -> "Deep teal focus"
    AppThemeOption.SUNSET -> "Warm orange glow"
    AppThemeOption.FOREST -> "Calm green productivity"
    AppThemeOption.MIDNIGHT -> "Neon fuchsia edge"
    AppThemeOption.ROSE -> "Soft pink daylight"
    AppThemeOption.SAND -> "Warm beige calm"
    AppThemeOption.SKY -> "Airy blue clarity"
    AppThemeOption.PEACH -> "Gentle coral warmth"
    AppThemeOption.AURORA -> "Teal–violet night"
    AppThemeOption.CHERRY -> "Deep wine drama"
    AppThemeOption.GRAPHITE -> "Neutral charcoal"
    AppThemeOption.MINT -> "Fresh green light"
}

/** Swatch color for theme picker chips. */
fun AppThemeOption.swatchColor(): Color = when (this) {
    AppThemeOption.MATERIAL_YOU -> Color(0xFF7C8CFF)
    AppThemeOption.SYSTEM -> Color(0xFF9CA3AF)
    AppThemeOption.PIXIDO_LIGHT -> PixiLavender
    AppThemeOption.PIXIDO_DARK -> PixiLavenderDeep
    AppThemeOption.OCEAN -> OceanPrimary
    AppThemeOption.SUNSET -> SunsetPrimary
    AppThemeOption.FOREST -> ForestPrimary
    AppThemeOption.MIDNIGHT -> MidnightPrimary
    AppThemeOption.ROSE -> RosePrimary
    AppThemeOption.SAND -> SandPrimary
    AppThemeOption.SKY -> SkyPrimary
    AppThemeOption.PEACH -> PeachPrimary
    AppThemeOption.AURORA -> AuroraPrimary
    AppThemeOption.CHERRY -> CherryPrimary
    AppThemeOption.GRAPHITE -> GraphitePrimary
    AppThemeOption.MINT -> MintPrimary
}

@Composable
fun PixiDoTheme(
    themeOption: AppThemeOption = AppThemeOption.PIXIDO_LIGHT,
    accentColorHex: String = "",
    content: @Composable () -> Unit
) {
    val colorScheme = resolveColorScheme(themeOption, accentColorHex)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

/** Backward-compatible alias. */
@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val option = when {
        dynamicColor -> AppThemeOption.MATERIAL_YOU
        darkTheme -> AppThemeOption.PIXIDO_DARK
        else -> AppThemeOption.PIXIDO_LIGHT
    }
    PixiDoTheme(themeOption = option, content = content)
}
