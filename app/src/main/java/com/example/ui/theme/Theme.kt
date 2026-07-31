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
    onErrorContainer = Color(0xFF8B1E2D)
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
    error = PixiCoral
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

@Composable
fun resolveColorScheme(
    themeOption: AppThemeOption,
    systemDark: Boolean = isSystemInDarkTheme()
): ColorScheme {
    val context = LocalContext.current
    return when (themeOption) {
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
    }
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
}

@Composable
fun PixiDoTheme(
    themeOption: AppThemeOption = AppThemeOption.PIXIDO_LIGHT,
    content: @Composable () -> Unit
) {
    val colorScheme = resolveColorScheme(themeOption)

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
