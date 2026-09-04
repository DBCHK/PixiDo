package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.example.data.UserProfile
import com.example.ui.theme.WalletInk
import com.example.ui.theme.WalletMuted
import com.example.ui.theme.WalletOnInk
import com.example.ui.theme.WalletPill
import com.example.ui.theme.rememberPixiDimens
import kotlin.math.abs

val InkSheetShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)

val InkCardPaletteWallet = listOf(Color(0xFFF4E24C), Color(0xFF5BE0A0), Color(0xFF4DA3FF))

private val CollapseSpring = spring<Float>(
    dampingRatio = 0.84f,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Ink top bar + unique collapsing hero + sticky white sheet.
 * Scroll up collapses the hero so the sheet fills the screen; scroll down at the
 * top of the list expands it back, with a spring snap.
 */
@Composable
fun InkHeroScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable RowScope.() -> Unit,
    collapsing: @Composable ColumnScope.() -> Unit,
    sheetTitle: String,
    sheetLeading: @Composable (() -> Unit)? = null,
    sheetTrailing: @Composable (() -> Unit)? = null,
    sheetBanner: @Composable ColumnScope.() -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(
        start = 8.dp,
        end = 8.dp,
        bottom = PixiIslandContentInset + 12.dp
    ),
    listState: LazyListState = rememberLazyListState(),
    sheetContent: LazyListScope.() -> Unit
) {
    val d = rememberPixiDimens()
    val density = LocalDensity.current
    val collapsePx = remember { mutableFloatStateOf(0f) }
    val fullHeightPx = remember { mutableIntStateOf(0) }
    val listStateRef = rememberUpdatedState(listState)

    val connection = remember(density) {
        object : NestedScrollConnection {
            private fun range() = fullHeightPx.intValue.toFloat().coerceAtLeast(0f)

            private fun atTop(): Boolean {
                val s = listStateRef.value
                return s.firstVisibleItemIndex == 0 && s.firstVisibleItemScrollOffset == 0
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val range = range()
                if (range <= 0f) return Offset.Zero
                val dy = available.y
                if (dy < 0f) {
                    val next = (collapsePx.floatValue - dy).coerceIn(0f, range)
                    val consumed = next - collapsePx.floatValue
                    if (consumed != 0f) {
                        collapsePx.floatValue = next
                        return Offset(0f, -consumed)
                    }
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val range = range()
                if (range <= 0f) return Offset.Zero
                val dy = available.y
                if (dy > 0f && atTop()) {
                    val next = (collapsePx.floatValue - dy).coerceIn(0f, range)
                    val consumedY = collapsePx.floatValue - next
                    if (consumedY != 0f) {
                        collapsePx.floatValue = next
                        return Offset(0f, consumedY)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val range = range()
                if (range <= 0f) return Velocity.Zero
                val frac = collapsePx.floatValue / range
                val target = when {
                    available.y < -900f -> range
                    available.y > 900f && atTop() -> 0f
                    frac >= 0.46f -> range
                    else -> 0f
                }
                animate(
                    initialValue = collapsePx.floatValue,
                    targetValue = target,
                    animationSpec = CollapseSpring
                ) { value, _ ->
                    collapsePx.floatValue = value
                }
                return if (abs(available.y) > 1f) Velocity(0f, available.y) else Velocity.Zero
            }
        }
    }

    val measured = fullHeightPx.intValue
    val visiblePx = if (measured == 0) 0f
    else (measured - collapsePx.floatValue).coerceAtLeast(0f)
    val frac = if (measured == 0) 0f
    else (collapsePx.floatValue / measured).coerceIn(0f, 1f)
    val sheetGap = with(density) { (16f * (1f - frac)).dp }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WalletInk)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = d.screenHorizontal)
                .padding(top = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                content = topBar
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (measured == 0) Modifier.wrapContentHeight()
                        else Modifier.height(with(density) { visiblePx.toDp() })
                    )
                    .then(if (frac > 0.02f) Modifier.clipToBounds() else Modifier)
                    .graphicsLayer { alpha = (1f - frac * 1.15f).coerceIn(0f, 1f) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(unbounded = measured > 0, align = Alignment.Top)
                        .onSizeChanged { size ->
                            if (size.height > fullHeightPx.intValue) {
                                fullHeightPx.intValue = size.height
                            }
                        }
                        .padding(top = 18.dp, bottom = 8.dp),
                    content = collapsing
                )
            }
        }

        Spacer(modifier = Modifier.height(sheetGap))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(InkSheetShape)
                .background(MaterialTheme.colorScheme.surface)
                .nestedScroll(connection)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sheetLeading != null) sheetLeading()
                Text(
                    text = sheetTitle,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (sheetTrailing != null) sheetTrailing()
            }
            sheetBanner()
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                content = sheetContent
            )
        }
    }
}

@Composable
fun InkAvatar(
    profile: UserProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "profile_avatar_button"
) {
    val photo = profile.avatarUri.ifBlank { profile.googlePhotoUrl }
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(WalletPill)
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (photo.isNotBlank()) {
            AsyncImage(
                model = photo,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (profile.displayName.isNotBlank()) {
            Text(
                text = profile.displayName.take(1).uppercase(),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = WalletOnInk
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile",
                tint = WalletOnInk,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun InkRoundIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String,
    icon: ImageVector
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(WalletPill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = WalletOnInk,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun InkHeroFigure(
    primary: String,
    secondary: String = "",
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            text = primary,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = WalletOnInk,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (secondary.isNotBlank()) {
            Text(
                text = secondary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = WalletMuted,
                modifier = Modifier.padding(bottom = 6.dp, start = 1.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Wallet-only decorative cards. Other tabs should not use this. */
@Composable
fun InkStackedCards(
    colors: List<Color>,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val palette = (colors + InkCardPaletteWallet).take(3)
    val rotations = listOf(16f, 6f, -10f)
    val offsetsX = listOf(18.dp, 8.dp, 0.dp)
    val offsetsY = listOf(10.dp, 4.dp, 0.dp)
    Box(
        modifier = modifier
            .width(92.dp)
            .height(88.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.CenterEnd
    ) {
        palette.asReversed().forEachIndexed { reverseIndex, color ->
            val i = palette.lastIndex - reverseIndex
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = offsetsX.getOrElse(i) { 0.dp }, y = offsetsY.getOrElse(i) { 0.dp })
                    .zIndex(i.toFloat())
                    .rotate(rotations.getOrElse(i) { 0f })
                    .size(width = 46.dp, height = 70.dp)
                    .shadow(8.dp, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun InkActionPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(WalletPill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leadingIcon != null) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = WalletOnInk,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = WalletOnInk
        )
    }
}

@Composable
fun InkStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    vibe: String? = null,
    vibeColor: Color = Color(0xFF34D399),
    avatars: List<Pair<String, Color>> = emptyList(),
    extraCount: Int = 0
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(WalletPill)
            .padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(text = label, fontSize = 13.sp, color = WalletMuted, maxLines = 1)
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = WalletOnInk,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!vibe.isNullOrBlank()) {
                Text(
                    text = vibe,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = vibeColor,
                    maxLines = 1
                )
            }
        }
        if (avatars.isNotEmpty() || extraCount > 0) {
            val shown = avatars.take(4)
            val overlap = 14.dp
            val slots = shown.size + if (extraCount > 0) 1 else 0
            Box(modifier = Modifier.width(28.dp + overlap * (slots - 1).coerceAtLeast(0)).height(28.dp)) {
                shown.forEachIndexed { index, (initial, accent) ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = overlap * index)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(accent)
                            .border(1.5.dp, WalletPill, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                if (extraCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = overlap * shown.size)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2A2D36))
                            .border(1.5.dp, WalletPill, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "+$extraCount",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = WalletOnInk
                        )
                    }
                }
            }
        }
    }
}

/** Tasks hero: three counts, not a spending chip. */
@Composable
fun InkMetricRow(
    items: List<InkMetric>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        items.forEach { metric ->
            Column {
                Text(
                    text = metric.value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = metric.color ?: WalletOnInk,
                    maxLines = 1
                )
                Text(
                    text = metric.label,
                    fontSize = 12.sp,
                    color = WalletMuted,
                    maxLines = 1
                )
            }
        }
    }
}

data class InkMetric(
    val label: String,
    val value: String,
    val color: Color? = null
)

/** Goals hero: ring, not stacked cards. */
@Composable
fun InkProgressRing(
    progress: Float,
    center: String,
    caption: String,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF34D399)
) {
    val t = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier.size(84.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = WalletPill,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            if (t > 0f) {
                drawArc(
                    color = accent,
                    startAngle = -90f,
                    sweepAngle = 360f * t,
                    useCenter = false,
                    style = stroke
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WalletOnInk,
                maxLines = 1
            )
            Text(
                text = caption,
                fontSize = 10.sp,
                color = WalletMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
fun InkSheetSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 18.dp)
            .padding(bottom = 10.dp)
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        androidx.compose.foundation.text.BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
