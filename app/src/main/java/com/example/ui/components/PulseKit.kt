package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt
import coil.compose.AsyncImage
import com.example.data.UserProfile
import com.example.ui.theme.PulseCard
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseIconFill
import com.example.ui.theme.PulseInk
import com.example.ui.theme.PulseMint
import com.example.ui.theme.PulseMuted
import com.example.ui.theme.PulseOrangeBar
import com.example.ui.theme.PulsePaper
import com.example.ui.theme.PulsePurple
import com.example.ui.theme.PulseStripe

val PulseCardShape = RoundedCornerShape(28.dp)
val PulseInnerShape = RoundedCornerShape(24.dp)

data class PulseAvatarSpec(
    val initial: String,
    val color: Color,
    val photoUrl: String = ""
)

data class PulseMenuItem(
    val label: String,
    val onClick: () -> Unit,
    val testTag: String? = null,
    val danger: Boolean = false
)

data class PulseDayStat(
    val label: String,
    val percent: Int,
    val accent: Color = PulseMint
)

data class PulseTimelineBar(
    val id: String,
    val label: String,
    val color: Color,
    val startDay: Int,
    val spanDays: Int = 1,
    val done: Boolean = false,
    val isDue: Boolean = false,
    /** @deprecated kept so older call sites compile during rollout */
    val startFrac: Float = 0f,
    val endFrac: Float = 0f
)

@Composable
fun isPulseDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.45f

@Composable
fun pulsePaper(): Color =
    if (isPulseDark()) MaterialTheme.colorScheme.background else PulsePaper

@Composable
fun pulseCard(): Color =
    if (isPulseDark()) MaterialTheme.colorScheme.surface else PulseCard

@Composable
fun pulseInk(): Color =
    if (isPulseDark()) MaterialTheme.colorScheme.onBackground else PulseInk

@Composable
fun pulseIconFill(): Color =
    if (isPulseDark()) MaterialTheme.colorScheme.surfaceVariant else PulseIconFill

@Composable
fun pulseStripe(): Color =
    if (isPulseDark()) MaterialTheme.colorScheme.outline.copy(alpha = 0.45f) else PulseStripe

@Composable
fun pulseMuted(): Color =
    if (isPulseDark()) MaterialTheme.colorScheme.onSurfaceVariant else PulseMuted

fun pulseAvatarPalette(index: Int): Color {
    val colors = listOf(
        Color(0xFFE07A5F),
        Color(0xFF3D5A80),
        Color(0xFF2A9D8F),
        Color(0xFFE9C46A),
        Color(0xFF9B5DE5),
        Color(0xFFF15BB5),
        Color(0xFF00BBF9),
        Color(0xFFF4A261)
    )
    return colors[index.floorMod(colors.size)]
}

private fun Int.floorMod(m: Int): Int = ((this % m) + m) % m

@Composable
fun PulseCircleIcon(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String,
    icon: ImageVector,
    size: Dp = 44.dp,
    iconSize: Dp = 18.dp,
    containerColor: Color = pulseIconFill(),
    contentColor: Color = pulseInk()
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun PulseAvatar(
    spec: PulseAvatarSpec,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    borderColor: Color = pulsePaper()
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(2.dp, borderColor, CircleShape)
            .background(spec.color),
        contentAlignment = Alignment.Center
    ) {
        if (spec.photoUrl.isNotBlank()) {
            AsyncImage(
                model = spec.photoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = spec.initial.take(2),
                fontSize = if (size < 28.dp) 10.sp else 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun PulseProfileAvatar(
    profile: UserProfile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    testTag: String = "profile_avatar_button"
) {
    val photo = profile.avatarUri.ifBlank { profile.googlePhotoUrl }
    val initial = profile.displayName.trim().firstOrNull()?.uppercase() ?: "P"
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(pulseIconFill())
            .clickable(onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        if (photo.isNotBlank()) {
            AsyncImage(
                model = photo,
                contentDescription = "Profile",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else if (profile.displayName.isNotBlank()) {
            Text(
                text = initial,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = pulseInk()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile",
                tint = pulseInk(),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PulseAvatarStack(
    avatars: List<PulseAvatarSpec>,
    extraCount: Int = 0,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    overlap: Dp = 16.dp
) {
    val shown = avatars.take(4)
    val extra = extraCount + (avatars.size - shown.size).coerceAtLeast(0)
    val slots = shown.size + if (extra > 0) 1 else 0
    if (slots <= 0) return
    Box(
        modifier = modifier
            .width(size + overlap * (slots - 1).coerceAtLeast(0))
            .height(size)
    ) {
        shown.forEachIndexed { index, spec ->
            PulseAvatar(
                spec = spec,
                size = size,
                borderColor = pulsePaper(),
                modifier = Modifier
                    .offset(x = overlap * index)
                    .zIndex(index.toFloat())
            )
        }
        if (extra > 0) {
            Box(
                modifier = Modifier
                    .offset(x = overlap * shown.size)
                    .zIndex(shown.size.toFloat())
                    .size(size)
                    .clip(CircleShape)
                    .border(2.dp, pulsePaper(), CircleShape)
                    .background(pulseIconFill()),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$extra",
                    fontSize = if (size < 28.dp) 9.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = pulseInk()
                )
            }
        }
    }
}

@Composable
fun PulseBlackBanner(
    text: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = Icons.Filled.ChatBubble,
    onDismiss: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(PulseInk)
            .padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF2A2A2A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (onDismiss != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun PulseSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String = "See All",
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = pulseInk()
        )
        if (onAction != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = action,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = pulseMuted()
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = pulseMuted(),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun PulseMoreMenu(
    items: List<PulseMenuItem>,
    modifier: Modifier = Modifier,
    tint: Color = pulseMuted()
) {
    if (items.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { expanded = true },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MoreHoriz,
                contentDescription = "More",
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = item.label,
                            color = if (item.danger) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        expanded = false
                        item.onClick()
                    },
                    modifier = if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier
                )
            }
        }
    }
}

@Composable
fun PulseStripes(
    modifier: Modifier = Modifier,
    color: Color = pulseStripe()
) {
    Canvas(modifier = modifier) {
        if (size.width < 1f || size.height < 1f) return@Canvas
        val spacing = 10.dp.toPx()
        val stroke = 1.15.dp.toPx()
        val extra = size.width + size.height
        var start = -extra
        while (start < extra) {
            drawLine(
                color = color,
                start = Offset(start, 0f),
                end = Offset(start + size.height, size.height),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            start += spacing
        }
    }
}

@Composable
fun PulseSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = PulseCardShape
    Column(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(pulseCard())
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(18.dp),
        content = content
    )
}

@Composable
fun PulseTaskCard(
    title: String,
    description: String,
    progress: Float,
    avatars: List<PulseAvatarSpec>,
    extraCount: Int,
    modifier: Modifier = Modifier,
    done: Boolean = false,
    menu: List<PulseMenuItem> = emptyList(),
    onClick: () -> Unit = {},
    leadingTestTag: String? = null,
    onToggle: (() -> Unit)? = null
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 480),
        label = "taskProgressBar"
    )
    val pct = (animated * 100f).toInt()
    PulseSurfaceCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (done) pulseMuted() else pulseInk(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            PulseMoreMenu(items = menu, tint = pulseMuted())
        }
        if (description.isNotBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                color = pulseMuted(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PulseAvatarStack(
                avatars = avatars,
                extraCount = extraCount,
                size = 24.dp,
                overlap = 10.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(pulseIconFill())
                    .then(
                        if (onToggle != null && leadingTestTag != null) {
                            Modifier
                                .testTag(leadingTestTag)
                                .clickable(onClick = onToggle)
                        } else if (leadingTestTag != null) {
                            Modifier.testTag(leadingTestTag)
                        } else Modifier
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animated)
                        .clip(RoundedCornerShape(50))
                        .background(if (done) pulseMuted() else PulseMint)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "$pct%",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pulseMuted()
            )
        }
    }
}

@Composable
fun PulseCardHeader(
    icon: ImageVector,
    title: String,
    menu: List<PulseMenuItem> = emptyList()
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(pulseIconFill()),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = pulseInk(),
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = pulseInk(),
            modifier = Modifier.weight(1f)
        )
        PulseMoreMenu(items = menu)
    }
}

@Composable
fun PulseProgressCard(
    days: List<PulseDayStat>,
    bannerText: String,
    modifier: Modifier = Modifier,
    title: String = "Task Progress",
    subtitle: String? = null,
    menu: List<PulseMenuItem> = emptyList(),
    phases: List<PulsePhaseItem> = emptyList(),
    onTogglePhase: (String) -> Unit = {},
    onRemovePhase: (String) -> Unit = {},
    onDismissBanner: (() -> Unit)? = null,
    onDayClick: ((Int) -> Unit)? = null
) {
    val peak = days.filter { it.percent > 0 }.maxByOrNull { it.percent }
    Column(
        modifier = modifier
            .shadow(
                elevation = 10.dp,
                shape = PulseCardShape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(PulseCardShape)
            .background(pulseCard())
            .testTag("task_progress_card")
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            PulseStripes(
                modifier = Modifier.matchParentSize(),
                color = pulseStripe()
            )
            Column(modifier = Modifier.padding(16.dp)) {
                PulseCardHeader(
                    icon = Icons.Filled.GridView,
                    title = title,
                    menu = menu
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = pulseMuted(),
                        modifier = Modifier.testTag("task_progress_subtitle")
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    days.forEachIndexed { index, day ->
                        PulseProgressColumn(
                            day = day,
                            isPeak = peak != null && day === peak,
                            modifier = Modifier.weight(1f),
                            onClick = onDayClick?.let { handler -> { handler(index) } }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    days.forEach { day ->
                        Text(
                            text = day.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = pulseMuted(),
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                PulseBlackBanner(
                    text = bannerText,
                    leadingIcon = Icons.Filled.ThumbUp,
                    onDismiss = onDismissBanner
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.testTag("task_phase_list")) {
                    if (phases.isEmpty()) {
                        Text(
                            text = "Add phases on the timeline below to track this task day by day.",
                            fontSize = 12.sp,
                            color = pulseMuted()
                        )
                    } else {
                        Text(
                            text = "Tap a phase to complete it",
                            fontSize = 12.sp,
                            color = pulseMuted()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        phases.forEach { item ->
                            PhaseRow(
                                item = item,
                                onToggle = { onTogglePhase(item.name) },
                                onRemove = { onRemovePhase(item.name) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseProgressColumn(
    day: PulseDayStat,
    isPeak: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val t by animateFloatAsState(
        targetValue = (day.percent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 480),
        label = "dayProgressBar"
    )
    val pillBg = when {
        isPeak -> PulseInk
        day.percent >= 70 -> PulseMint
        day.percent >= 30 -> PulseOrangeBar
        day.percent > 0 -> PulseInk
        else -> pulseIconFill()
    }
    val barBg = when {
        day.percent <= 0 -> pulseIconFill()
        isPeak -> PulseInk
        day.percent >= 70 -> PulseMint
        day.percent >= 30 -> PulseOrangeBar
        else -> PulseInk.copy(alpha = 0.35f)
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (day.percent > 0) {
            PercentPill(
                text = "${day.percent}%",
                container = pillBg,
                content = if (day.percent <= 0) pulseInk() else Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Box(
            modifier = Modifier
                .width(36.dp)
                .height((118f * t).coerceAtLeast(if (day.percent > 0) 16f else 6f).dp)
                .clip(RoundedCornerShape(18.dp))
                .background(barBg)
        )
    }
}

@Composable
private fun PercentPill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = content,
            maxLines = 1
        )
    }
}

data class PulseTimelineDay(
    val label: String,
    val weekday: String,
    val isToday: Boolean = false,
    val isDue: Boolean = false
)

@Composable
fun PulseTimelineCard(
    days: List<PulseTimelineDay>,
    bars: List<PulseTimelineBar>,
    weekLabel: String,
    modifier: Modifier = Modifier,
    title: String = "Task Timeline",
    menu: List<PulseMenuItem> = emptyList(),
    onPrevWeek: (() -> Unit)? = null,
    onNextWeek: (() -> Unit)? = null,
    onThisWeek: (() -> Unit)? = null,
    onBarClick: ((PulseTimelineBar) -> Unit)? = null,
    onBarMoved: ((PulseTimelineBar, Int) -> Unit)? = null,
    onAddPhase: ((String, Int) -> Unit)? = null,
    onRemovePhase: ((PulseTimelineBar) -> Unit)? = null
) {
    val dayCount = days.size.coerceAtLeast(1)
    val todayIndex = days.indexOfFirst { it.isToday }
    val dueIndex = days.indexOfFirst { it.isDue }
    var adding by remember { mutableStateOf(false) }
    var draft by remember { mutableStateOf("") }
    var selectedDay by remember(todayIndex, dueIndex) {
        mutableStateOf(
            when {
                todayIndex >= 0 -> todayIndex
                dueIndex >= 0 -> dueIndex
                else -> 0
            }
        )
    }

    PulseSurfaceCard(modifier = modifier.testTag("task_timeline_card")) {
        PulseCardHeader(
            icon = Icons.Outlined.ViewWeek,
            title = title,
            menu = menu
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onPrevWeek != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(pulseIconFill())
                        .clickable(onClick = onPrevWeek)
                        .testTag("timeline_prev_week"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous week",
                        tint = pulseInk(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = weekLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pulseInk(),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onThisWeek != null) Modifier.clickable(onClick = onThisWeek)
                        else Modifier
                    )
                    .testTag("timeline_week_label")
            )
            if (onNextWeek != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(pulseIconFill())
                        .clickable(onClick = onNextWeek)
                        .testTag("timeline_next_week"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next week",
                        tint = pulseInk(),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            days.forEachIndexed { index, day ->
                val selected = index == selectedDay
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedDay = index }
                        .testTag("timeline_day_$index"),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = day.weekday,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (day.isToday) PulseCoral else pulseMuted(),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    day.isToday -> PulseCoral
                                    day.isDue -> PulseInk
                                    selected -> pulseIconFill()
                                    else -> Color.Transparent
                                }
                            )
                            .then(
                                if (selected && !day.isToday && !day.isDue) {
                                    Modifier.border(1.5.dp, pulseInk(), CircleShape)
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (day.isToday || day.isDue) Color.White else pulseInk()
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        val visibleBars = bars.filter { bar ->
            val end = bar.startDay + bar.spanDays.coerceAtLeast(1)
            end > 0 && bar.startDay < dayCount
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height((visibleBars.size.coerceAtLeast(1) * 42 + 12).dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val colW = size.width / dayCount
                val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 10f), 0f)
                repeat(dayCount) { i ->
                    drawLine(
                        color = PulseStripe,
                        start = Offset(colW * i, 0f),
                        end = Offset(colW * i, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dash
                    )
                }
                if (todayIndex >= 0) {
                    val markerX = colW * (todayIndex + 0.5f)
                    drawLine(
                        color = PulseCoral,
                        start = Offset(markerX, 0f),
                        end = Offset(markerX, size.height),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = PulseCoral,
                        radius = 5.dp.toPx(),
                        center = Offset(markerX, 6.dp.toPx())
                    )
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (visibleBars.isEmpty()) {
                    Text(
                        text = "No phases this week — add one below or drag Due onto a day.",
                        fontSize = 12.sp,
                        color = pulseMuted(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
                    )
                } else {
                    visibleBars.take(6).forEach { bar ->
                        TimelineBarRow(
                            bar = bar,
                            dayCount = dayCount,
                            onClick = { onBarClick?.invoke(bar) },
                            onMoved = { day -> onBarMoved?.invoke(bar, day) },
                            onLongPress = if (!bar.isDue) {
                                { onRemovePhase?.invoke(bar) }
                            } else null
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Tap a phase to complete · drag to reschedule · hold to remove",
            fontSize = 11.sp,
            color = pulseMuted()
        )
        if (onAddPhase != null) {
            Spacer(modifier = Modifier.height(10.dp))
            if (adding) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = pulseInk()
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(pulseIconFill())
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("timeline_add_phase_input"),
                        decorationBox = { inner ->
                            Box {
                                if (draft.isEmpty()) {
                                    Text("Phase name", fontSize = 14.sp, color = pulseMuted())
                                }
                                inner()
                            }
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(PulseInk)
                            .clickable {
                                val name = draft.trim()
                                if (name.isNotBlank()) {
                                    onAddPhase(name, selectedDay)
                                    draft = ""
                                    adding = false
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("timeline_add_phase_confirm")
                    ) {
                        Text(
                            text = "Add",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(pulseIconFill())
                        .clickable { adding = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("timeline_add_phase"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = pulseInk(),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add phase on ${days.getOrNull(selectedDay)?.weekday ?: "day"} ${days.getOrNull(selectedDay)?.label.orEmpty()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pulseInk()
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineBarRow(
    bar: PulseTimelineBar,
    dayCount: Int,
    onClick: (() -> Unit)? = null,
    onMoved: ((Int) -> Unit)? = null,
    onLongPress: (() -> Unit)? = null
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val colPx = constraints.maxWidth.toFloat() / dayCount
        val start = bar.startDay
        val span = bar.spanDays.coerceIn(1, dayCount)
        val visibleStart = start.coerceAtLeast(0)
        val visibleEnd = (start + span).coerceAtMost(dayCount)
        if (visibleEnd <= visibleStart) return@BoxWithConstraints
        var dragDx by remember(bar.id) { mutableFloatStateOf(0f) }
        val baseXpx = colPx * visibleStart
        val widthDp = maxWidth * (visibleEnd - visibleStart) / dayCount - 6.dp
        Box(
            modifier = Modifier
                .offset { IntOffset((baseXpx + dragDx).roundToInt() + 3, 0) }
                .width(widthDp.coerceAtLeast(40.dp))
                .height(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (bar.done) bar.color.copy(alpha = 0.42f) else bar.color)
                .pointerInput(bar.id) {
                    detectTapGestures(
                        onTap = { onClick?.invoke() },
                        onLongPress = { onLongPress?.invoke() }
                    )
                }
                .pointerInput(bar.id, dayCount) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            dragDx += amount
                        },
                        onDragEnd = {
                            if (abs(dragDx) > 16f && onMoved != null) {
                                val newIndex = (start + (dragDx / colPx)).roundToInt()
                                    .coerceIn(0, dayCount - 1)
                                if (newIndex != bar.startDay) onMoved(newIndex)
                            } else {
                                onClick?.invoke()
                            }
                            dragDx = 0f
                        },
                        onDragCancel = { dragDx = 0f }
                    )
                }
                .padding(horizontal = 8.dp)
                .testTag("timeline_bar_${bar.id}"),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (bar.done) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .size(12.dp)
                    )
                }
                Text(
                    text = bar.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = if (bar.done) 0.85f else 1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

data class PulsePhaseItem(
    val name: String,
    val dayLabel: String,
    val done: Boolean,
    val color: Color
)

@Composable
private fun PhaseRow(
    item: PulsePhaseItem,
    onToggle: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(vertical = 8.dp)
            .testTag("phase_row_${item.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (item.done) PulseMint else pulseIconFill())
                .testTag("phase_check_${item.name}"),
            contentAlignment = Alignment.Center
        ) {
            if (item.done) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Done",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(item.color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (item.done) pulseMuted() else pulseInk(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.dayLabel,
                fontSize = 11.sp,
                color = pulseMuted(),
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove)
                .testTag("phase_remove_${item.name}"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove ${item.name}",
                tint = pulseMuted(),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
fun PulseTopRow(
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    trailing: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = trailing
        )
    }
}

val PulseTimelineColors = listOf(PulseOrangeBar, PulseMint, PulsePurple, PulseCoral)
