package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
// Soft Lilac design system — 1:1 with idea2 reference screenshots
// Pure white surfaces · pastel lavender primary · yellow FAB · pink accents
// Clean, airy, friendly — no harsh neo-brutalist edges
// ─────────────────────────────────────────────────────────────

// Brand accents (sampled from idea2)
val PixiLavender = Color(0xFFC4A8F5)       // Primary CTA fill
val PixiLavenderDeep = Color(0xFF9B7AE8)   // Emphasis / selected text
val PixiLavenderMid = Color(0xFFD4C4F8)    // Soft secondary buttons
val PixiLavenderSoft = Color(0xFFEDE4FF)   // Primary containers / chips unselected
val PixiLavenderMist = Color(0xFFF7F3FF)   // Soft tinted backgrounds
val PixiPink = Color(0xFFFF6BA8)           // Selected list highlight
val PixiPinkSoft = Color(0xFFFFE0EE)
val PixiYellow = Color(0xFFFFE566)         // Center FAB
val PixiYellowDeep = Color(0xFFFFD84D)
val PixiMint = Color(0xFF6EE7B7)
val PixiCoral = Color(0xFFFF7A8A)

// Light surfaces (reference default — pure white, airy)
val PixiLightBg = Color(0xFFFFFFFF)
val PixiLightSurface = Color(0xFFFFFFFF)
val PixiLightSurfaceVariant = Color(0xFFF3F1F7)
val PixiLightBorder = Color(0xFFEDEBF2)
val PixiLightOnBg = Color(0xFF1C1C1E)
val PixiLightMuted = Color(0xFF8E8E9A)
val PixiLightChip = Color(0xFFF0EDF6)
val PixiLightSearch = Color(0xFFF3F3F6)

// Dark surfaces (soft dark companion of the same language)
val PixiDarkBg = Color(0xFF121018)
val PixiDarkSurface = Color(0xFF1C1824)
val PixiDarkSurfaceVariant = Color(0xFF2A2436)
val PixiDarkBorder = Color(0xFF3A3348)
val PixiDarkOnBg = Color(0xFFF5F3FA)
val PixiDarkMuted = Color(0xFFA39BB5)

// Legacy aliases used across the app
val PixiViolet = PixiLavenderDeep
val PixiVioletLight = PixiLavender
val PixiCyan = Color(0xFF67D4E8)
val PixiRose = PixiPink
val PixiAmber = Color(0xFFFBBF24)
val PixiEmerald = Color(0xFF34D399)

// Ocean theme
val OceanPrimary = Color(0xFF0EA5E9)
val OceanBg = Color(0xFF04131C)
val OceanSurface = Color(0xFF0B2230)
val OceanAccent = Color(0xFF2DD4BF)

// Sunset theme
val SunsetPrimary = Color(0xFFF97316)
val SunsetBg = Color(0xFF1A0B0B)
val SunsetSurface = Color(0xFF2A1410)
val SunsetAccent = Color(0xFFFBBF24)

// Forest theme
val ForestPrimary = Color(0xFF22C55E)
val ForestBg = Color(0xFF06140C)
val ForestSurface = Color(0xFF0F2418)
val ForestAccent = Color(0xFF86EFAC)

// Midnight theme
val MidnightPrimary = Color(0xFFE879F9)
val MidnightBg = Color(0xFF05030A)
val MidnightSurface = Color(0xFF120A1C)
val MidnightAccent = Color(0xFF67E8F9)

// Rose (soft pink light)
val RosePrimary = Color(0xFFF472B6)
val RoseBg = Color(0xFFFFF7FB)
val RoseSurface = Color(0xFFFFFFFF)
val RoseContainer = Color(0xFFFFE4F1)

// Sand (warm beige light)
val SandPrimary = Color(0xFFD4A574)
val SandBg = Color(0xFFFBF7F2)
val SandSurface = Color(0xFFFFFCF9)
val SandContainer = Color(0xFFF5E6D3)

// Sky (airy blue light)
val SkyPrimary = Color(0xFF60A5FA)
val SkyBg = Color(0xFFF5F9FF)
val SkySurface = Color(0xFFFFFFFF)
val SkyContainer = Color(0xFFDCEBFF)

// Peach (soft coral light)
val PeachPrimary = Color(0xFFFB923C)
val PeachBg = Color(0xFFFFF8F3)
val PeachSurface = Color(0xFFFFFFFF)
val PeachContainer = Color(0xFFFFE8D6)

// Aurora (teal–violet dark)
val AuroraPrimary = Color(0xFF5EEAD4)
val AuroraBg = Color(0xFF0A1214)
val AuroraSurface = Color(0xFF12202A)
val AuroraAccent = Color(0xFFA78BFA)

// Cherry (deep wine dark)
val CherryPrimary = Color(0xFFF43F5E)
val CherryBg = Color(0xFF14080C)
val CherrySurface = Color(0xFF221018)
val CherryAccent = Color(0xFFFBBF24)

// Graphite (neutral charcoal)
val GraphitePrimary = Color(0xFF94A3B8)
val GraphiteBg = Color(0xFF0F1115)
val GraphiteSurface = Color(0xFF1A1D24)
val GraphiteAccent = Color(0xFFE2E8F0)

// Mint (fresh green light)
val MintPrimary = Color(0xFF34D399)
val MintBg = Color(0xFFF4FDF8)
val MintSurface = Color(0xFFFFFFFF)
val MintContainer = Color(0xFFD1FAE5)

/** Preset accent colors users can apply on top of any theme. */
val AccentPalette = listOf(
    "#C4A8F5", // lilac
    "#9B7AE8", // deep lilac
    "#FF6BA8", // pink
    "#F472B6", // rose
    "#FB923C", // peach
    "#FBBF24", // amber
    "#FFE566", // yellow
    "#34D399", // mint
    "#2DD4BF", // teal
    "#60A5FA", // sky
    "#67D4E8", // cyan
    "#A78BFA", // violet
    "#F43F5E", // cherry
    "#94A3B8"  // slate
)

// Contribution heatmap — soft lilac scale
val HeatmapEmpty = Color(0xFF2A2436)
val HeatmapL1 = Color(0xFF3D2F5C)
val HeatmapL2 = Color(0xFF6B4FA8)
val HeatmapL3 = Color(0xFF9B7AE8)
val HeatmapL4 = Color(0xFFC4A8F5)

val HeatmapEmptyLight = Color(0xFFF3F1F7)
val HeatmapL1Light = Color(0xFFEDE4FF)
val HeatmapL2Light = Color(0xFFD4C4F5)
val HeatmapL3Light = Color(0xFFB8A0F0)
val HeatmapL4Light = Color(0xFF9B7AE8)
