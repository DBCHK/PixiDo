package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseInk
import com.example.ui.theme.PulseMint
import com.example.ui.theme.PulseOrangeBar
import com.example.ui.theme.PulsePurple
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun PulseCelebrate(
    burst: Int,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    val seeds = remember(burst) {
        List(22) { i ->
            val ang = (i / 22f) * Math.PI * 2.0 + Random(burst * 31 + i).nextDouble(-0.2, 0.2)
            Triple(ang, 0.55f + (i % 5) * 0.08f, listOf(PulseMint, PulseCoral, PulsePurple, PulseOrangeBar)[i % 4])
        }
    }
    LaunchedEffect(burst) {
        if (burst <= 0) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 920, easing = FastOutSlowInEasing))
    }
    val t = progress.value
    if (t <= 0.02f || t >= 0.98f) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = size.width / 2f
        val cy = size.height * 0.38f
        val dist = size.minDimension * 0.42f
        seeds.forEach { (ang, speed, color) ->
            val travel = dist * speed * t
            val x = cx + (cos(ang) * travel).toFloat()
            val y = cy + (sin(ang) * travel).toFloat() + 90f * t * t
            drawCircle(
                color = color.copy(alpha = (1f - t) * 0.95f),
                radius = (7.dp.toPx()) * (1f - t * 0.55f),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun PulseDayRing(
    progress: Float,
    center: String,
    caption: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val t by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "dayRing"
    )
    Box(
        modifier = modifier
            .size(78.dp)
            .then(if (onClick != null) Modifier.clip(CircleShape).clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = PulseIconFillSoft,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            if (t > 0f) {
                drawArc(
                    color = if (t >= 0.999f) PulseMint else PulseCoral,
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
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = pulseInk()
            )
            Text(
                text = caption,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = pulseMuted()
            )
        }
    }
}

private val PulseIconFillSoft = Color(0xFFEFEFF2)

@Composable
fun PulseWeekStrip(
    days: List<Long>,
    selectedMillis: Long,
    todayMillis: Long,
    busyDays: Set<Long>,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val selected = DayMatch(day, selectedMillis)
            val today = DayMatch(day, todayMillis)
            val number = java.util.Calendar.getInstance().apply { timeInMillis = day }
                .get(java.util.Calendar.DAY_OF_MONTH)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelect(day) }
                    .padding(horizontal = 2.dp, vertical = 4.dp)
                    .testTag("week_day_$number")
            ) {
                Text(
                    text = labels.getOrElse(index) { "" },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (today) PulseCoral else pulseMuted()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                selected -> PulseCoral
                                today -> PulseInk
                                else -> Color.Transparent
                            }
                        )
                        .then(
                            if (!selected && !today && busyDays.contains(day)) {
                                Modifier.border(1.5.dp, PulseMint, CircleShape)
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = number.toString(),
                        fontSize = 13.sp,
                        fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Medium,
                        color = if (selected || today) Color.White else pulseInk()
                    )
                }
            }
        }
    }
}

private fun DayMatch(a: Long, b: Long): Boolean {
    val ca = java.util.Calendar.getInstance().apply { timeInMillis = a }
    val cb = java.util.Calendar.getInstance().apply { timeInMillis = b }
    return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) &&
        ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
}

@Composable
fun PulseQuickCapture(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(pulseCard())
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
            .testTag("quick_capture"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = pulseInk()
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (value.isNotBlank()) onSubmit() }),
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
                .testTag("quick_capture_input"),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(placeholder, fontSize = 15.sp, color = pulseMuted())
                    }
                    inner()
                }
            }
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (value.isBlank()) pulseIconFill() else PulseCoral)
                .clickable(enabled = value.isNotBlank(), onClick = onSubmit)
                .testTag("quick_capture_send"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Add",
                tint = if (value.isBlank()) pulseMuted() else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun PulseActionRow(
    done: Boolean,
    onToggle: () -> Unit,
    onFocus: () -> Unit,
    onSnooze: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PulseActionChip(
            label = if (done) "Undo" else "Done",
            icon = if (done) Icons.Filled.Check else Icons.Filled.Check,
            filled = !done,
            color = PulseMint,
            onClick = onToggle,
            modifier = Modifier
                .weight(1f)
                .testTag("detail_done")
        )
        PulseActionChip(
            label = "Focus",
            icon = Icons.Filled.PlayArrow,
            filled = false,
            color = PulseCoral,
            onClick = onFocus,
            modifier = Modifier
                .weight(1f)
                .testTag("detail_focus")
        )
        PulseActionChip(
            label = "Snooze",
            icon = Icons.Filled.Snooze,
            filled = false,
            color = PulseInk,
            onClick = onSnooze,
            modifier = Modifier
                .weight(1f)
                .testTag("detail_snooze")
        )
    }
}

@Composable
private fun PulseActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    filled: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (filled) color else pulseIconFill())
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (filled) Color.White else color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (filled) Color.White else pulseInk()
        )
    }
}

@Composable
fun PulseLegendDot(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, fontSize = 11.sp, color = pulseMuted())
    }
}
