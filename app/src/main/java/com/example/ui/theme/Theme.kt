package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EditorialAcidLime,
    onPrimary = EditorialBlack,
    primaryContainer = EditorialPrimaryContainer,
    onPrimaryContainer = EditorialOnPrimaryContainer,
    secondary = EditorialCyan,
    onSecondary = EditorialBlack,
    secondaryContainer = EditorialSurfaceVariant,
    onSecondaryContainer = EditorialOnBackground,
    tertiary = EditorialAcidLime,
    onTertiary = EditorialBlack,
    background = EditorialBackground,
    surface = EditorialSurface,
    surfaceVariant = EditorialSurfaceVariant,
    onBackground = EditorialOnBackground,
    onSurface = EditorialOnBackground,
    onSurfaceVariant = EditorialOnSurfaceVariant,
    outline = EditorialBorder,
    error = AuraErrorRose
)

private val LightColorScheme = darkColorScheme(
    primary = EditorialAcidLime,
    onPrimary = EditorialBlack,
    primaryContainer = EditorialPrimaryContainer,
    onPrimaryContainer = EditorialOnPrimaryContainer,
    secondary = EditorialCyan,
    onSecondary = EditorialBlack,
    secondaryContainer = EditorialSurfaceVariant,
    onSecondaryContainer = EditorialOnBackground,
    background = EditorialBackground,
    surface = EditorialSurface,
    surfaceVariant = EditorialSurfaceVariant,
    onBackground = EditorialOnBackground,
    onSurface = EditorialOnBackground,
    onSurfaceVariant = EditorialOnSurfaceVariant,
    outline = EditorialBorder,
    error = AuraErrorRose
)

@Composable
fun AuraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to prioritize bold vibrant brand identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
