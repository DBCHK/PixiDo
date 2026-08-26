package com.example.data

import com.example.notify.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class RepeatRule {
    NONE,
    DAILY,
    WEEKDAYS,
    WEEKLY;

    val displayName: String
        get() = when (this) {
            NONE -> "Never"
            DAILY -> "Every day"
            WEEKDAYS -> "Weekdays"
            WEEKLY -> "Weekly"
        }

    val shortLabel: String
        get() = when (this) {
            NONE -> ""
            DAILY -> "Daily"
            WEEKDAYS -> "Weekdays"
            WEEKLY -> "Weekly"
        }

    companion object {
        fun from(raw: String?): RepeatRule =
            entries.find { it.name.equals(raw, ignoreCase = true) } ?: NONE
    }
}

object TaskRepeat {

    fun isRepeating(task: TaskEntity): Boolean =
        RepeatRule.from(task.repeatRule) != RepeatRule.NONE

    fun nextDue(fromMillis: Long, rule: RepeatRule, afterMillis: Long = System.currentTimeMillis()): Long {
        if (rule == RepeatRule.NONE) return fromMillis
        val cal = Calendar.getInstance().apply { timeInMillis = fromMillis }
        var guard = 0
        while (cal.timeInMillis <= afterMillis && guard < 400) {
            when (rule) {
                RepeatRule.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                RepeatRule.WEEKLY -> cal.add(Calendar.DAY_OF_YEAR, 7)
                RepeatRule.WEEKDAYS -> {
                    do {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    } while (
                        cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ||
                            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                    )
                }
                RepeatRule.NONE -> return fromMillis
            }
            guard++
        }
        return cal.timeInMillis
    }

    fun dueLabel(millis: Long, now: Long = System.currentTimeMillis()): String {
        val dayStart = startOfDay(millis)
        val today = startOfDay(now)
        val days = TimeUnit.MILLISECONDS.toDays(dayStart - today)
        val day = when (days) {
            0L -> "Today"
            1L -> "Tomorrow"
            -1L -> "Yesterday"
            else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(millis))
        }
        return "$day · ${ReminderScheduler.formatTime(millis)}"
    }

    /** Completing a repeating task rolls it to the next due, still open. */
    fun rollForward(task: TaskEntity, now: Long = System.currentTimeMillis()): TaskEntity {
        val rule = RepeatRule.from(task.repeatRule)
        val next = nextDue(task.dueDateMillis, rule, now)
        return task.copy(
            isCompleted = false,
            completedAtMillis = now,
            streakCount = task.streakCount + 1,
            dueDateMillis = next,
            dueTimeStr = dueLabel(next, now),
            completedSubtasks = ""
        )
    }

    private fun startOfDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
}
