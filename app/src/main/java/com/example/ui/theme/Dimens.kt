package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Screen-size aware spacing & type scale.
 * Compact phones (<360dp), normal (360–400), comfortable (400–600), large (tablets).
 */
@Immutable
data class PixiDimens(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isCompact: Boolean,
    val isLarge: Boolean,
    val screenHorizontal: Dp,
    val screenVertical: Dp,
    val sectionGap: Dp,
    val cardPadding: Dp,
    val cardRadius: Dp,
    val listGap: Dp,
    val chipHeight: Dp,
    val buttonHeight: Dp,
    val iconSm: Dp,
    val iconMd: Dp,
    val iconLg: Dp,
    val avatar: Dp,
    val fab: Dp,
    val title: TextUnit,
    val headline: TextUnit,
    val body: TextUnit,
    val caption: TextUnit,
    val label: TextUnit,
    val emptyDoodle: Dp,
    val heatmapCell: Dp
)

@Composable
fun rememberPixiDimens(): PixiDimens {
    val config = LocalConfiguration.current
    val w = config.screenWidthDp
    val h = config.screenHeightDp
    return remember(w, h) {
        val compact = w < 360
        val large = w >= 600
        when {
            compact -> PixiDimens(
                screenWidthDp = w,
                screenHeightDp = h,
                isCompact = true,
                isLarge = false,
                screenHorizontal = 14.dp,
                screenVertical = 10.dp,
                sectionGap = 12.dp,
                cardPadding = 12.dp,
                cardRadius = 20.dp,
                listGap = 8.dp,
                chipHeight = 34.dp,
                buttonHeight = 46.dp,
                iconSm = 14.dp,
                iconMd = 20.dp,
                iconLg = 26.dp,
                avatar = 40.dp,
                fab = 48.dp,
                title = 28.sp,
                headline = 17.sp,
                body = 13.sp,
                caption = 11.sp,
                label = 10.sp,
                emptyDoodle = 140.dp,
                heatmapCell = 10.dp
            )
            large -> PixiDimens(
                screenWidthDp = w,
                screenHeightDp = h,
                isCompact = false,
                isLarge = true,
                screenHorizontal = 32.dp,
                screenVertical = 16.dp,
                sectionGap = 20.dp,
                cardPadding = 20.dp,
                cardRadius = 26.dp,
                listGap = 12.dp,
                chipHeight = 40.dp,
                buttonHeight = 54.dp,
                iconSm = 16.dp,
                iconMd = 24.dp,
                iconLg = 32.dp,
                avatar = 56.dp,
                fab = 60.dp,
                title = 34.sp,
                headline = 22.sp,
                body = 16.sp,
                caption = 13.sp,
                label = 12.sp,
                emptyDoodle = 220.dp,
                heatmapCell = 14.dp
            )
            else -> PixiDimens(
                screenWidthDp = w,
                screenHeightDp = h,
                isCompact = false,
                isLarge = false,
                screenHorizontal = 20.dp,
                screenVertical = 14.dp,
                sectionGap = 18.dp,
                cardPadding = 18.dp,
                cardRadius = 24.dp,
                listGap = 10.dp,
                chipHeight = 38.dp,
                buttonHeight = 52.dp,
                iconSm = 14.dp,
                iconMd = 22.dp,
                iconLg = 28.dp,
                avatar = 48.dp,
                fab = 56.dp,
                title = 34.sp,
                headline = 17.sp,
                body = 14.sp,
                caption = 12.sp,
                label = 11.sp,
                emptyDoodle = 180.dp,
                heatmapCell = 12.dp
            )
        }
    }
}

/** Clamp long titles so they don't overflow tiny screens. */
fun String.ellipsize(max: Int = 48): String =
    if (length <= max) this else take(max - 1).trimEnd() + "…"
