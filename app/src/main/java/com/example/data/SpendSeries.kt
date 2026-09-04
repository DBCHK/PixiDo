package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class SpendRange {
    DAY,
    WEEK,
    MONTH;

    val label: String
        get() = when (this) {
            DAY -> "Day"
            WEEK -> "Week"
            MONTH -> "Month"
        }
}

data class SpendBucket(
    val startMillis: Long,
    val endMillis: Long,
    val label: String,
    val amount: Double
)

data class SpendChartModel(
    val range: SpendRange,
    val offset: Int,
    val windowStart: Long,
    val windowEnd: Long,
    val windowLabel: String,
    val buckets: List<SpendBucket>,
    val total: Double,
    val previousTotal: Double,
    val peak: SpendBucket?
) {
    val hasSpend: Boolean get() = total > 0.0
    val average: Double
        get() = if (buckets.isEmpty()) 0.0 else total / buckets.size
    val delta: Double get() = total - previousTotal
    val deltaRatio: Float?
        get() = if (previousTotal <= 0.0) null else ((total - previousTotal) / previousTotal).toFloat()
}

/**
 * Builds expense time series for the wallet chart.
 * Only [TransactionType.EXPENSE] counts — income, transfers, lent and borrow are ignored.
 */
object SpendSeries {

    fun build(
        items: List<BudgetItemEntity>,
        range: SpendRange,
        offset: Int = 0,
        now: Long = System.currentTimeMillis()
    ): SpendChartModel {
        val clampedOffset = offset.coerceAtMost(0)
        val window = windowOf(range, clampedOffset, now)
        val previous = previousWindow(range, window.start, window.end)
        val expenses = items.filter { it.type == TransactionType.EXPENSE }
        val buckets = bucketsFor(range, window.start, window.end, expenses)
        val total = buckets.sumOf { it.amount }
        val previousTotal = expenses
            .filter { it.timestamp >= previous.start && it.timestamp < previous.end }
            .sumOf { it.amount }
        val peak = buckets.maxByOrNull { it.amount }?.takeIf { it.amount > 0.0 }
        return SpendChartModel(
            range = range,
            offset = clampedOffset,
            windowStart = window.start,
            windowEnd = window.end,
            windowLabel = window.label,
            buckets = buckets,
            total = total,
            previousTotal = previousTotal,
            peak = peak
        )
    }

    internal fun windowOf(range: SpendRange, offset: Int, now: Long): Window {
        return when (range) {
            SpendRange.DAY -> {
                val start = DayTime.addDays(DayTime.startOfDay(now), offset)
                val end = DayTime.addDays(start, 1)
                Window(start, end, dayLabel(start, now))
            }
            SpendRange.WEEK -> {
                val today = DayTime.startOfDay(now)
                val end = DayTime.addDays(today, 1 + offset * 7)
                val start = DayTime.addDays(end, -7)
                val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
                Window(start, end, "${fmt.format(Date(start))} – ${fmt.format(Date(DayTime.addDays(end, -1)))}")
            }
            SpendRange.MONTH -> {
                val start = addMonths(startOfMonth(now), offset)
                val end = addMonths(start, 1)
                Window(start, end, SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(start)))
            }
        }
    }

    private fun previousWindow(range: SpendRange, start: Long, end: Long): Window {
        return when (range) {
            SpendRange.DAY -> Window(DayTime.addDays(start, -1), start, "")
            SpendRange.WEEK -> Window(DayTime.addDays(start, -7), start, "")
            SpendRange.MONTH -> Window(addMonths(start, -1), start, "")
        }.also { check(end > start) }
    }

    private fun bucketsFor(
        range: SpendRange,
        start: Long,
        end: Long,
        expenses: List<BudgetItemEntity>
    ): List<SpendBucket> {
        return when (range) {
            SpendRange.DAY -> (0 until 24).map { hour ->
                val cal = calendar(start).apply { add(Calendar.HOUR_OF_DAY, hour) }
                val bStart = cal.timeInMillis
                val bEnd = calendar(bStart).apply { add(Calendar.HOUR_OF_DAY, 1) }.timeInMillis
                val label = SimpleDateFormat("h a", Locale.getDefault()).format(Date(bStart))
                SpendBucket(bStart, bEnd, label, sumIn(expenses, bStart, bEnd))
            }
            SpendRange.WEEK -> (0 until 7).map { day ->
                val bStart = DayTime.addDays(start, day)
                val bEnd = DayTime.addDays(bStart, 1)
                val label = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(bStart))
                SpendBucket(bStart, bEnd, label, sumIn(expenses, bStart, bEnd))
            }
            SpendRange.MONTH -> {
                val days = DayTime.daysBetween(start, end).coerceAtLeast(1)
                (0 until days).map { day ->
                    val bStart = DayTime.addDays(start, day)
                    val bEnd = DayTime.addDays(bStart, 1)
                    val label = SimpleDateFormat("d", Locale.getDefault()).format(Date(bStart))
                    SpendBucket(bStart, bEnd, label, sumIn(expenses, bStart, bEnd))
                }
            }
        }
    }

    private fun sumIn(expenses: List<BudgetItemEntity>, start: Long, end: Long): Double =
        expenses.filter { it.timestamp >= start && it.timestamp < end }.sumOf { it.amount }

    private fun dayLabel(start: Long, now: Long): String {
        val today = DayTime.startOfDay(now)
        return when (DayTime.daysBetween(today, start)) {
            0 -> "Today"
            -1 -> "Yesterday"
            1 -> "Tomorrow"
            else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(start))
        }
    }

    internal fun startOfMonth(millis: Long): Long =
        calendar(millis).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    internal fun addMonths(millis: Long, months: Int): Long =
        calendar(millis).apply { add(Calendar.MONTH, months) }.timeInMillis

    private fun calendar(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }

    data class Window(val start: Long, val end: Long, val label: String)
}
