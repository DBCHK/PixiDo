package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Pure helpers for daily habit streaks and week grids.
 * [dateKey] format matches [AuraRepository.dayKey]: yyyy-MM-dd.
 */
object HabitStats {

    private val fmt: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun dayKey(millis: Long = System.currentTimeMillis()): String =
        fmt.format(Date(millis))

    fun shiftDay(dateKey: String, days: Int): String {
        val cal = parse(dateKey) ?: return dateKey
        cal.add(Calendar.DAY_OF_YEAR, days)
        return fmt.format(cal.time)
    }

    fun doneDays(activity: List<GoalActivityEntity>): Set<String> =
        activity.filter { it.completedCount > 0 }.map { it.dateKey }.toSet()

    fun isDoneOn(doneDays: Set<String>, dateKey: String): Boolean = dateKey in doneDays

    /**
     * Consecutive days ending today, or yesterday if today is still empty.
     */
    fun currentStreak(doneDays: Set<String>, today: String = dayKey()): Int {
        if (doneDays.isEmpty()) return 0
        var key = today
        if (key !in doneDays) {
            key = shiftDay(today, -1)
            if (key !in doneDays) return 0
        }
        var streak = 0
        while (key in doneDays && streak < 400) {
            streak++
            key = shiftDay(key, -1)
        }
        return streak
    }

    /** Sunday → Saturday keys for the week that contains [today]. */
    fun weekKeys(today: String = dayKey()): List<String> {
        val cal = parse(today) ?: return List(7) { today }
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return (0..6).map {
            val key = fmt.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, 1)
            key
        }
    }

    fun isFuture(dateKey: String, today: String = dayKey()): Boolean = dateKey > today

    private fun parse(dateKey: String): Calendar? {
        val parsed = runCatching { fmt.parse(dateKey) }.getOrNull() ?: return null
        return Calendar.getInstance().apply { time = parsed }
    }
}
