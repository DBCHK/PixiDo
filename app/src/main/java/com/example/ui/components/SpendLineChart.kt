package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.BudgetItemEntity
import com.example.data.Currencies
import com.example.data.SpendBucket
import com.example.data.SpendChartModel
import com.example.data.SpendRange
import com.example.data.SpendSeries
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private val SpendLine = Color(0xFFFF7A8A)
private val ChartShape = RoundedCornerShape(24.dp)

@Composable
fun SpendTrendCard(
    items: List<BudgetItemEntity>,
    currencyCode: String,
    monthlyAllowance: Double = 0.0,
    modifier: Modifier = Modifier
) {
    val sound = LocalSoundEngine.current
    val now = remember { System.currentTimeMillis() }
    var range by remember { mutableStateOf(SpendRange.WEEK) }
    var offset by remember { mutableIntStateOf(0) }
    val model = remember(items, range, offset, now) {
        SpendSeries.build(items, range, offset, now)
    }
    var selected by remember(model.windowStart, range) {
        mutableStateOf(model.buckets.indices.lastOrNull { model.buckets[it].amount > 0.0 })
    }
    val daysInMonth = remember(now) {
        java.util.Calendar.getInstance().apply { timeInMillis = now }
            .getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            .coerceAtLeast(1)
    }
    val dailyBudget = if (monthlyAllowance > 0) monthlyAllowance / daysInMonth else 0.0
    val selectedBucket = selected?.let { model.buckets.getOrNull(it) }
    val shownAmount = selectedBucket?.amount ?: model.total
    val shownLabel = selectedBucket?.label ?: "Total"
    val symbol = Currencies.symbolOf(currencyCode)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .clip(ChartShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(16.dp)
            .testTag("spend_trend_card")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Spending",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            SpendRangePills(
                selected = range,
                onSelect = {
                    sound.play(Sfx.FILTER_SELECT)
                    range = it
                    offset = 0
                }
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PeriodChevron(
                enabled = true,
                forward = false,
                onClick = {
                    sound.play(Sfx.TAP_SOFT)
                    offset -= 1
                },
                testTag = "spend_prev_period"
            )
            Text(
                text = model.windowLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = offset != 0) {
                        sound.play(Sfx.TAP_SOFT)
                        offset = 0
                    }
                    .testTag("spend_period_label"),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            PeriodChevron(
                enabled = offset < 0,
                forward = true,
                onClick = {
                    sound.play(Sfx.TAP_SOFT)
                    offset = (offset + 1).coerceAtMost(0)
                },
                testTag = "spend_next_period"
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = Currencies.format(shownAmount, currencyCode),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("spend_chart_amount")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = shownLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
                maxLines = 1
            )
        }
        Text(
            text = comparisonCaption(model, range, symbol),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        SmoothSpendChart(
            buckets = model.buckets,
            range = range,
            selectedIndex = selected,
            dailyBudget = dailyBudget,
            onSelect = { selected = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .testTag("spend_line_chart")
        )
        if (!model.hasSpend) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No expenses in this ${range.label.lowercase()}. Log a spend to see the line fill in.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val peak = model.peak
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = buildString {
                    append("Avg ${Currencies.format(model.average, currencyCode)}")
                    if (peak != null) append("  ·  Peak ${peak.label} ${Currencies.format(peak.amount, currencyCode)}")
                    if (dailyBudget > 0) append("  ·  Pace ${Currencies.format(dailyBudget, currencyCode)}/day")
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SpendRangePills(
    selected: SpendRange,
    onSelect: (SpendRange) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        SpendRange.entries.forEach { option ->
            val on = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) MaterialTheme.colorScheme.onSurface else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("spend_range_${option.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (on) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PeriodChevron(
    enabled: Boolean,
    forward: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = enabled, onClick = onClick)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (forward) Icons.AutoMirrored.Filled.KeyboardArrowRight
            else Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = if (forward) "Next period" else "Previous period",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.28f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SmoothSpendChart(
    buckets: List<SpendBucket>,
    range: SpendRange,
    selectedIndex: Int?,
    dailyBudget: Double,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(buckets, range) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 720, easing = FastOutSlowInEasing))
    }
    val t = progress.value
    val yMax = remember(buckets, dailyBudget) {
        val peak = buckets.maxOfOrNull { it.amount } ?: 0.0
        niceMax(maxOf(peak, if (dailyBudget > 0) dailyBudget else 0.0, 1.0))
    }
    val axis = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val n = buckets.size.coerceAtLeast(1)

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(buckets, widthPx) {
                    fun pick(x: Float) {
                        if (buckets.isEmpty() || widthPx <= 0f) return
                        val pad = 12.dp.toPx()
                        val inner = (widthPx - pad * 2f).coerceAtLeast(1f)
                        val idx = if (n == 1) 0
                        else ((x - pad) / inner * (n - 1)).roundToInt().coerceIn(0, n - 1)
                        onSelect(idx)
                    }
                    detectTapGestures { pick(it.x) }
                }
                .pointerInput(buckets, widthPx) {
                    detectHorizontalDragGestures(
                        onDragStart = { pick ->
                            if (buckets.isEmpty() || widthPx <= 0f) return@detectHorizontalDragGestures
                            val pad = 12.dp.toPx()
                            val inner = (widthPx - pad * 2f).coerceAtLeast(1f)
                            val idx = if (n == 1) 0
                            else ((pick.x - pad) / inner * (n - 1)).roundToInt().coerceIn(0, n - 1)
                            onSelect(idx)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (buckets.isEmpty() || widthPx <= 0f) return@detectHorizontalDragGestures
                            val pad = 12.dp.toPx()
                            val inner = (widthPx - pad * 2f).coerceAtLeast(1f)
                            val idx = if (n == 1) 0
                            else ((change.position.x - pad) / inner * (n - 1))
                                .roundToInt().coerceIn(0, n - 1)
                            onSelect(idx)
                        }
                    )
                }
        ) {
            val padX = 12.dp.toPx()
            val padTop = 16.dp.toPx()
            val padBottom = 28.dp.toPx()
            val w = size.width
            val h = size.height
            val innerW = (w - padX * 2f).coerceAtLeast(1f)
            val innerH = (h - padTop - padBottom).coerceAtLeast(1f)
            fun xOf(i: Int): Float =
                if (n <= 1) padX + innerW / 2f else padX + innerW * (i / (n - 1).toFloat())
            fun yOf(amount: Double): Float {
                val frac = (amount / yMax).toFloat().coerceIn(0f, 1f) * t
                return padTop + innerH * (1f - frac)
            }

            drawLine(
                color = axis,
                start = Offset(padX, padTop + innerH),
                end = Offset(w - padX, padTop + innerH),
                strokeWidth = 1.dp.toPx()
            )

            if (dailyBudget > 0 && dailyBudget <= yMax) {
                val yBudget = yOf(dailyBudget)
                drawLine(
                    color = axis,
                    start = Offset(padX, yBudget),
                    end = Offset(w - padX, yBudget),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }

            val points = buckets.mapIndexed { i, b -> Offset(xOf(i), yOf(b.amount)) }
            if (points.size >= 2) {
                val line = smoothPath(points)
                val fill = Path().apply {
                    addPath(line)
                    lineTo(points.last().x, padTop + innerH)
                    lineTo(points.first().x, padTop + innerH)
                    close()
                }
                drawPath(
                    path = fill,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            SpendLine.copy(alpha = 0.28f * t),
                            SpendLine.copy(alpha = 0.02f)
                        ),
                        startY = padTop,
                        endY = padTop + innerH
                    )
                )
                drawPath(
                    path = line,
                    color = SpendLine,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            } else if (points.size == 1) {
                drawCircle(color = SpendLine, radius = 5.dp.toPx(), center = points[0])
            }

            buckets.forEachIndexed { i, bucket ->
                if (bucket.amount > 0) {
                    drawCircle(
                        color = SpendLine,
                        radius = 3.5.dp.toPx(),
                        center = points[i]
                    )
                }
            }
            selectedIndex?.let { idx ->
                val p = points.getOrNull(idx) ?: return@let
                drawLine(
                    color = SpendLine.copy(alpha = 0.35f),
                    start = Offset(p.x, padTop),
                    end = Offset(p.x, padTop + innerH),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = p)
                drawCircle(color = SpendLine, radius = 4.dp.toPx(), center = p)
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            buckets.forEachIndexed { index, bucket ->
                if (showAxisLabel(range, index, buckets.size)) {
                    Text(
                        text = bucket.label,
                        fontSize = 10.sp,
                        color = muted,
                        maxLines = 1
                    )
                } else {
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        }
    }
}

private fun showAxisLabel(range: SpendRange, index: Int, size: Int): Boolean {
    if (size <= 8) return true
    return when (range) {
        SpendRange.DAY -> index % 6 == 0 || index == size - 1
        SpendRange.WEEK -> true
        SpendRange.MONTH -> index == 0 || (index + 1) % 5 == 0 || index == size - 1
    }
}

private fun smoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    for (i in 1 until points.size) {
        val prev = points[i - 1]
        val cur = points[i]
        val midX = (prev.x + cur.x) / 2f
        path.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
    }
    return path
}

private fun niceMax(value: Double): Double {
    if (value <= 0.0) return 1.0
    val exp = 10.0.pow(floor(log10(value)))
    val n = value / exp
    val nice = when {
        n <= 1.0 -> 1.0
        n <= 2.0 -> 2.0
        n <= 5.0 -> 5.0
        else -> 10.0
    }
    return nice * exp
}

private fun comparisonCaption(
    model: SpendChartModel,
    range: SpendRange,
    symbol: String
): String {
    val previousName = when (range) {
        SpendRange.DAY -> "yesterday"
        SpendRange.WEEK -> "last week"
        SpendRange.MONTH -> "last month"
    }
    if (!model.hasSpend && model.previousTotal <= 0.0) return "Tap the line to inspect a point"
    if (model.previousTotal <= 0.0) return "First $symbol spend in this ${range.label.lowercase()}"
    val ratio = model.deltaRatio ?: return "Vs $previousName"
    val pct = abs((ratio * 100f).roundToInt())
    return when {
        pct == 0 -> "Same as $previousName"
        model.delta > 0 -> "$pct% more than $previousName"
        else -> "$pct% less than $previousName"
    }
}
