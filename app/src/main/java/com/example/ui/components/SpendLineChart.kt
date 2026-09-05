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
import com.example.data.ChartMoneyKind
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
private val IncomeLine = Color(0xFF16A34A)
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
    var kind by remember { mutableStateOf(ChartMoneyKind.SPEND) }
    var offset by remember { mutableIntStateOf(0) }
    val model = remember(items, range, offset, now, kind) {
        SpendSeries.build(items, range, offset, now, kind)
    }
    var selected by remember(model.windowStart, range, kind) {
        mutableStateOf(
            model.buckets.indices.lastOrNull {
                model.buckets[it].amount > 0.0 || model.buckets[it].income > 0.0
            }
        )
    }
    val daysInMonth = remember(now) {
        java.util.Calendar.getInstance().apply { timeInMillis = now }
            .getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
            .coerceAtLeast(1)
    }
    val periodBudget = when (range) {
        SpendRange.DAY -> if (monthlyAllowance > 0) monthlyAllowance / daysInMonth else 0.0
        SpendRange.WEEK -> if (monthlyAllowance > 0) monthlyAllowance / 4.345 else 0.0
        SpendRange.MONTH -> monthlyAllowance.coerceAtLeast(0.0)
    }
    val dailyBudget = if (monthlyAllowance > 0) monthlyAllowance / daysInMonth else 0.0
    val selectedBucket = selected?.let { model.buckets.getOrNull(it) }
    val shownAmount = when {
        selectedBucket == null -> model.total
        kind == ChartMoneyKind.INCOME -> selectedBucket.income
        kind == ChartMoneyKind.BOTH -> selectedBucket.income - selectedBucket.spend
        else -> selectedBucket.amount
    }
    val shownLabel = selectedBucket?.label ?: "Total"
    val symbol = Currencies.symbolOf(currencyCode)
    val title = when (kind) {
        ChartMoneyKind.INCOME -> "Income"
        ChartMoneyKind.BOTH -> "Cashflow"
        ChartMoneyKind.SPEND -> "Spending"
    }
    val axisMax = remember(model.buckets, periodBudget, model.average, kind) {
        val peak = model.buckets.maxOfOrNull { maxOf(it.spend, it.income, it.amount) } ?: 0.0
        niceMax(maxOf(peak, periodBudget, model.average * 1.35, 1.0))
    }

    PixiGlass(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("spend_trend_card"),
        shape = ChartShape,
        role = PixiGlassRole.Content,
        frost = false,
        elevation = 6.dp
    ) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
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
        Spacer(modifier = Modifier.height(8.dp))
        MoneyKindPills(
            selected = kind,
            onSelect = {
                sound.play(Sfx.FILTER_SELECT)
                kind = it
            }
        )
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
            kind = kind,
            selectedIndex = selected,
            yMax = axisMax,
            periodBudget = periodBudget,
            average = model.average,
            currencyCode = currencyCode,
            onSelect = { selected = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp)
                .testTag("spend_line_chart")
        )
        if (!model.hasSpend && kind != ChartMoneyKind.INCOME) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No expenses in this ${range.label.lowercase()}. Log a spend to see the line fill in.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (kind == ChartMoneyKind.INCOME && model.total <= 0.0) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "No income in this ${range.label.lowercase()}.",
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
}

@Composable
private fun MoneyKindPills(
    selected: ChartMoneyKind,
    onSelect: (ChartMoneyKind) -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ChartMoneyKind.entries.forEach { option ->
            val on = option == selected
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (on) MaterialTheme.colorScheme.onSurface else Color.Transparent)
                    .clickable { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("spend_kind_${option.name.lowercase()}"),
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
    kind: ChartMoneyKind,
    selectedIndex: Int?,
    yMax: Double,
    periodBudget: Double,
    average: Double,
    currencyCode: String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(buckets, range, kind) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 720, easing = FastOutSlowInEasing))
    }
    val t = progress.value
    val axis = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val n = buckets.size.coerceAtLeast(1)
    val yLabels = listOf(0.0, yMax / 2.0, yMax)

    BoxWithConstraints(modifier = modifier) {
        val widthPx = constraints.maxWidth.toFloat()
        val padLeft = 40.dp
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(buckets, widthPx) {
                    fun pick(x: Float) {
                        if (buckets.isEmpty() || widthPx <= 0f) return
                        val pad = padLeft.toPx()
                        val inner = (widthPx - pad - 12.dp.toPx()).coerceAtLeast(1f)
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
                            val pad = padLeft.toPx()
                            val inner = (widthPx - pad - 12.dp.toPx()).coerceAtLeast(1f)
                            val idx = if (n == 1) 0
                            else ((pick.x - pad) / inner * (n - 1)).roundToInt().coerceIn(0, n - 1)
                            onSelect(idx)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            if (buckets.isEmpty() || widthPx <= 0f) return@detectHorizontalDragGestures
                            val pad = padLeft.toPx()
                            val inner = (widthPx - pad - 12.dp.toPx()).coerceAtLeast(1f)
                            val idx = if (n == 1) 0
                            else ((change.position.x - pad) / inner * (n - 1))
                                .roundToInt().coerceIn(0, n - 1)
                            onSelect(idx)
                        }
                    )
                }
        ) {
            val padX = padLeft.toPx()
            val padRight = 12.dp.toPx()
            val padTop = 16.dp.toPx()
            val padBottom = 28.dp.toPx()
            val w = size.width
            val h = size.height
            val innerW = (w - padX - padRight).coerceAtLeast(1f)
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
                end = Offset(w - padRight, padTop + innerH),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = axis,
                start = Offset(padX, padTop),
                end = Offset(padX, padTop + innerH),
                strokeWidth = 1.dp.toPx()
            )

            if (periodBudget > 0 && periodBudget <= yMax) {
                val yBudget = yOf(periodBudget)
                drawLine(
                    color = Color(0xFF7B74F6).copy(alpha = 0.55f),
                    start = Offset(padX, yBudget),
                    end = Offset(w - padRight, yBudget),
                    strokeWidth = 1.2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )
            }
            if (average > 0 && average <= yMax) {
                val yAvg = yOf(average)
                drawLine(
                    color = axis,
                    start = Offset(padX, yAvg),
                    end = Offset(w - padRight, yAvg),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                )
            }

            fun drawSeries(values: List<Double>, color: Color) {
                val points = values.mapIndexed { i, v -> Offset(xOf(i), yOf(v)) }
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
                                color.copy(alpha = 0.26f * t),
                                color.copy(alpha = 0.02f)
                            ),
                            startY = padTop,
                            endY = padTop + innerH
                        )
                    )
                    drawPath(
                        path = line,
                        color = color,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else if (points.size == 1) {
                    drawCircle(color = color, radius = 5.dp.toPx(), center = points[0])
                }
                values.forEachIndexed { i, v ->
                    if (v > 0) {
                        drawCircle(color = color, radius = 3.5.dp.toPx(), center = points[i])
                    }
                }
            }

            when (kind) {
                ChartMoneyKind.INCOME -> drawSeries(buckets.map { it.income }, IncomeLine)
                ChartMoneyKind.BOTH -> {
                    drawSeries(buckets.map { it.spend }, SpendLine)
                    drawSeries(buckets.map { it.income }, IncomeLine)
                }
                ChartMoneyKind.SPEND -> drawSeries(buckets.map { it.amount }, SpendLine)
            }

            val markerColor = if (kind == ChartMoneyKind.INCOME) IncomeLine else SpendLine
            selectedIndex?.let { idx ->
                val value = when (kind) {
                    ChartMoneyKind.INCOME -> buckets.getOrNull(idx)?.income ?: 0.0
                    ChartMoneyKind.BOTH -> buckets.getOrNull(idx)?.spend ?: 0.0
                    ChartMoneyKind.SPEND -> buckets.getOrNull(idx)?.amount ?: 0.0
                }
                val p = Offset(xOf(idx), yOf(value))
                drawLine(
                    color = markerColor.copy(alpha = 0.35f),
                    start = Offset(p.x, padTop),
                    end = Offset(p.x, padTop + innerH),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawCircle(color = Color.White, radius = 6.dp.toPx(), center = p)
                drawCircle(color = markerColor, radius = 4.dp.toPx(), center = p)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 8.dp)
                .height(140.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            yLabels.asReversed().forEach { v ->
                Text(
                    text = compactAxis(v, currencyCode),
                    fontSize = 9.sp,
                    color = muted,
                    maxLines = 1
                )
            }
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 36.dp, end = 4.dp),
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

private fun compactAxis(amount: Double, code: String): String {
    val symbol = Currencies.symbolOf(code)
    return when {
        amount >= 1000 -> "$symbol${(amount / 1000.0).let { if (it >= 10) "%.0f".format(it) else "%.1f".format(it) }}k"
        amount <= 0 -> "${symbol}0"
        else -> "$symbol${amount.toInt()}"
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
