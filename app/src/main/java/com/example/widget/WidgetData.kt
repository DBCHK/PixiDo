package com.example.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.example.data.AuraDatabase
import com.example.data.AuraRepository
import com.example.data.Currencies
import com.example.data.DailyActivityEntity
import com.example.data.TaskEntity
import com.example.data.TransactionType
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HeatmapWidgetData(
    val totalCompletions: Int,
    val activeDays: Int,
    val streak: Int,
    val todayCount: Int,
    val bestDay: Int,
    val dayCounts: Map<String, Int>
)

data class TodosWidgetData(
    val todayLabel: String,
    val openCount: Int,
    val doneCount: Int,
    val totalCount: Int,
    val titles: List<Pair<String, Boolean>>, // title, completed
    val userXp: Int
)

data class FocusWidgetData(
    val label: String,
    val minutesDefault: Int,
    val todayCompletions: Int,
    val userXp: Int
)

data class TransactionsWidgetData(
    val currencyCode: String,
    val income: Double,
    val spent: Double,
    val net: Double,
    val txnCount: Int,
    val topCategory: String,
    val periodLabel: String
)

data class GoalsWidgetData(
    val habitsDone: Int,
    val habitsTotal: Int,
    val streak: Int,
    val progress: Float,
    val habitNames: List<Pair<String, Boolean>>
)

data class SpendCurveWidgetData(
    val currencyCode: String,
    val total: Double,
    val periodLabel: String,
    val normalized: List<Float>,
    val labels: List<String>
)

object WidgetDataLoader {

    suspend fun loadHeatmap(context: Context): HeatmapWidgetData = withContext(Dispatchers.IO) {
        val dao = AuraDatabase.getDatabase(context).auraDao()
        // Aggregate per-goal contribution days into a single GitHub-style grid
        val goalActivity = dao.getGoalActivityOnce()
        val byDay = goalActivity.groupBy { it.dateKey }.mapValues { (_, rows) ->
            DailyActivityEntity(
                dateKey = rows.first().dateKey,
                completedCount = rows.sumOf { it.completedCount },
                xpEarned = rows.sumOf { it.xpEarned }
            )
        }
        val activity = byDay.values.toList()
        val todayKey = AuraRepository.dayKey()
        val todayCount = byDay[todayKey]?.completedCount ?: 0
        val total = activity.sumOf { it.completedCount }
        val active = activity.count { it.completedCount > 0 }
        val best = activity.maxOfOrNull { it.completedCount } ?: 0
        val streak = computeStreak(activity)
        val counts = byDay.mapValues { it.value.completedCount }
        HeatmapWidgetData(total, active, streak, todayCount, best, counts)
    }

    suspend fun loadGoals(context: Context): GoalsWidgetData = withContext(Dispatchers.IO) {
        val dao = AuraDatabase.getDatabase(context).auraDao()
        val goals = dao.getGoalsOnce()
        val activity = dao.getGoalActivityOnce()
        val today = com.example.data.HabitStats.dayKey()
        val habits = goals.filter { it.isDailyHabit }
        val byGoal = activity.groupBy { it.goalId }
        val names = habits.take(4).map { habit ->
            val done = com.example.data.HabitStats.isDoneOn(
                com.example.data.HabitStats.doneDays(byGoal[habit.id].orEmpty()),
                today
            )
            habit.title to done
        }
        val doneToday = names.count { it.second }
        val streak = habits.maxOfOrNull { habit ->
            com.example.data.HabitStats.currentStreak(
                com.example.data.HabitStats.doneDays(byGoal[habit.id].orEmpty()),
                today
            )
        } ?: 0
        val total = habits.size
        val progress = when {
            total > 0 -> doneToday / total.toFloat()
            else -> 0f
        }
        GoalsWidgetData(doneToday, total, streak, progress, names)
    }

    suspend fun loadSpendCurve(context: Context): SpendCurveWidgetData = withContext(Dispatchers.IO) {
        val dao = AuraDatabase.getDatabase(context).auraDao()
        val prefs = UserPreferencesRepository(context)
        val currency = prefs.currentProfile().currencyCode
        val items = dao.getBudgetItemsOnce()
        val model = com.example.data.SpendSeries.build(
            items,
            com.example.data.SpendRange.WEEK,
            0,
            System.currentTimeMillis()
        )
        val peak = model.buckets.maxOfOrNull { it.amount }?.coerceAtLeast(1.0) ?: 1.0
        SpendCurveWidgetData(
            currencyCode = currency,
            total = model.total,
            periodLabel = model.windowLabel,
            normalized = model.buckets.map { (it.amount / peak).toFloat() },
            labels = model.buckets.map { it.label }
        )
    }

    suspend fun loadTodos(context: Context): TodosWidgetData = withContext(Dispatchers.IO) {
        val dao = AuraDatabase.getDatabase(context).auraDao()
        val prefs = UserPreferencesRepository(context)
        val profile = prefs.currentProfile()
        val tasks = dao.getTasksOnce()
        val dayStart = startOfDay(System.currentTimeMillis())
        val dayEnd = dayStart + 24L * 60 * 60 * 1000 - 1
        val today = tasks.filter { t ->
            t.dueDateMillis in dayStart..dayEnd ||
                t.dueTimeStr.contains("Today", ignoreCase = true) ||
                isSameDay(t.dueDateMillis, System.currentTimeMillis())
        }.ifEmpty {
            // Fallback: incomplete tasks + completed today
            tasks.filter {
                !it.isCompleted ||
                    (it.completedAtMillis != null && isSameDay(it.completedAtMillis, System.currentTimeMillis()))
            }.take(12)
        }
        val open = today.count { !it.isCompleted }
        val done = today.count { it.isCompleted }
        val lines = today
            .sortedWith(compareBy<TaskEntity> { it.isCompleted }.thenBy { it.dueDateMillis })
            .take(5)
            .map { it.title to it.isCompleted }
        val label = SimpleDateFormat("EEE · MMM d", Locale.getDefault()).format(Date())
        TodosWidgetData(label, open, done, today.size, lines, profile.userXp)
    }

    suspend fun loadFocus(context: Context): FocusWidgetData = withContext(Dispatchers.IO) {
        val dao = AuraDatabase.getDatabase(context).auraDao()
        val prefs = UserPreferencesRepository(context)
        val profile = prefs.currentProfile()
        val todayKey = AuraRepository.dayKey()
        val today = dao.getGoalActivityOnce()
            .filter { it.dateKey == todayKey }
            .sumOf { it.completedCount }
        FocusWidgetData(
            label = "Focus · 25 min",
            minutesDefault = 25,
            todayCompletions = today,
            userXp = profile.userXp
        )
    }

    suspend fun loadTransactions(context: Context): TransactionsWidgetData = withContext(Dispatchers.IO) {
        val dao = AuraDatabase.getDatabase(context).auraDao()
        val prefs = UserPreferencesRepository(context)
        val currency = prefs.currentProfile().currencyCode
        val items = dao.getBudgetItemsOnce()
        val cal = Calendar.getInstance()
        val month = cal.get(Calendar.MONTH)
        val year = cal.get(Calendar.YEAR)
        val monthItems = items.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.timestamp }
            c.get(Calendar.MONTH) == month && c.get(Calendar.YEAR) == year
        }
        val income = monthItems.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val spent = monthItems.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val top = monthItems
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .maxByOrNull { (_, v) -> v.sumOf { it.amount } }
            ?.key ?: "—"
        val period = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date())
        TransactionsWidgetData(
            currencyCode = currency,
            income = income,
            spent = spent,
            net = income - spent,
            txnCount = monthItems.size,
            topCategory = top,
            periodLabel = period
        )
    }

    fun formatMoney(amount: Double, code: String): String = Currencies.format(amount, code)

    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun isSameDay(a: Long, b: Long): Boolean {
        val ca = Calendar.getInstance().apply { timeInMillis = a }
        val cb = Calendar.getInstance().apply { timeInMillis = b }
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
    }

    private fun computeStreak(activity: List<DailyActivityEntity>): Int {
        if (activity.isEmpty()) return 0
        val keys = activity.filter { it.completedCount > 0 }.map { it.dateKey }.toSet()
        var streak = 0
        val cal = Calendar.getInstance()
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        // Walk back from today
        repeat(400) {
            val key = fmt.format(cal.time)
            if (keys.contains(key)) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                if (streak == 0 && key == AuraRepository.dayKey()) {
                    // Today empty — allow streak from yesterday
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                } else return streak
            }
        }
        return streak
    }

    /**
     * Compact GitHub-style contribution grid as a bitmap (tiny cells).
     */
    fun renderHeatmapBitmap(
        activityMap: Map<String, DailyActivityEntity>,
        weeks: Int = 12,
        cell: Int = 9,
        gap: Int = 2
    ): Bitmap {
        val days = 7
        val w = weeks * cell + (weeks - 1) * gap
        val h = days * cell + (days - 1) * gap
        val bmp = Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val round = cell * 0.28f

        val empty = 0xFFEDE4FF.toInt()
        val l1 = 0xFFD4C4F5.toInt()
        val l2 = 0xFFB8A0F0.toInt()
        val l3 = 0xFF9B7AE8.toInt()
        val l4 = 0xFF7C5CD6.toInt()

        val grid = contributionGrid(weeks)
        grid.forEachIndexed { wi, week ->
            week.forEachIndexed { di, dayKey ->
                val count = activityMap[dayKey]?.completedCount ?: 0
                paint.color = when {
                    count <= 0 -> empty
                    count == 1 -> l1
                    count == 2 -> l2
                    count <= 4 -> l3
                    else -> l4
                }
                val left = wi * (cell + gap).toFloat()
                val top = di * (cell + gap).toFloat()
                canvas.drawRoundRect(
                    RectF(left, top, left + cell, top + cell),
                    round,
                    round,
                    paint
                )
            }
        }
        return bmp
    }

    fun contributionGrid(weeks: Int): List<List<String>> {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        // End of this week (today)
        val end = cal.clone() as Calendar
        // Align to Saturday end of week column like GitHub (Sun start)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun
        val columns = mutableListOf<List<String>>()
        // Start from (weeks-1) weeks ago Sunday
        cal.add(Calendar.WEEK_OF_YEAR, -(weeks - 1))
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        repeat(weeks) {
            val week = mutableListOf<String>()
            repeat(7) {
                week.add(fmt.format(cal.time))
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            columns.add(week)
        }
        return columns
    }
}
