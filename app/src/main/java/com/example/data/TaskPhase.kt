package com.example.data

import java.util.Calendar

/**
 * A named phase on a task timeline.
 *
 * Stored inside [TaskEntity.subtasks] as `Name` or `Name@dayMillis` or
 * `Name@dayMillis:spanDays`. Older tasks with bare names stay readable;
 * dates are filled in from the due day when first needed.
 */
data class TaskPhase(
    val name: String,
    val dayMillis: Long,
    val spanDays: Int = 1
)

object DayTime {
    fun startOfDay(millis: Long): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    fun addDays(dayStart: Long, days: Int): Long =
        Calendar.getInstance().apply {
            timeInMillis = dayStart
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis

    fun startOfWeekSunday(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = startOfDay(millis)
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                add(Calendar.DAY_OF_YEAR, -1)
            }
        }
        return cal.timeInMillis
    }

    fun daysBetween(fromDay: Long, toDay: Long): Int {
        val a = startOfDay(fromDay)
        val b = startOfDay(toDay)
        if (a == b) return 0
        var n = 0
        var t = a
        if (b > a) {
            while (t < b && n < 400) {
                t = addDays(t, 1)
                n++
            }
        } else {
            while (t > b && n > -400) {
                t = addDays(t, -1)
                n--
            }
        }
        return n
    }

    fun withTimeFrom(dayStart: Long, timeSource: Long): Long {
        val src = Calendar.getInstance().apply { timeInMillis = timeSource }
        return Calendar.getInstance().apply {
            timeInMillis = dayStart
            set(Calendar.HOUR_OF_DAY, src.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, src.get(Calendar.MINUTE))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

object TaskPhases {
    fun nameOf(token: String): String = token.substringBefore("@").trim()

    fun names(subtasks: String): List<String> =
        subtasks.split(";").map { nameOf(it) }.filter { it.isNotBlank() }

    fun parse(subtasks: String, dueMillis: Long): List<TaskPhase> {
        val dueDay = DayTime.startOfDay(dueMillis)
        val tokens = subtasks.split(";").map { it.trim() }.filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()
        val dated = mutableListOf<TaskPhase>()
        val undated = mutableListOf<String>()
        tokens.forEach { token ->
            val name = nameOf(token)
            if (name.isBlank()) return@forEach
            val meta = token.substringAfter("@", missingDelimiterValue = "")
            if (meta.isBlank()) {
                undated += name
            } else {
                val millisPart = meta.substringBefore(":")
                val spanPart = meta.substringAfter(":", missingDelimiterValue = "1")
                val millis = millisPart.toLongOrNull()
                val span = spanPart.toIntOrNull()?.coerceIn(1, 14) ?: 1
                if (millis != null && millis > 0L) {
                    dated += TaskPhase(name, DayTime.startOfDay(millis), span)
                } else {
                    undated += name
                }
            }
        }
        undated.forEachIndexed { i, name ->
            val day = DayTime.addDays(dueDay, -(undated.size - 1 - i))
            dated += TaskPhase(name, day, 1)
        }
        return dated
    }

    fun encode(phases: List<TaskPhase>): String =
        phases.joinToString(";") { phase ->
            val span = phase.spanDays.coerceIn(1, 14)
            if (span == 1) "${phase.name}@${phase.dayMillis}"
            else "${phase.name}@${phase.dayMillis}:$span"
        }

    fun move(phases: List<TaskPhase>, name: String, newDayMillis: Long): List<TaskPhase> =
        phases.map {
            if (it.name.equals(name, ignoreCase = true)) {
                it.copy(dayMillis = DayTime.startOfDay(newDayMillis))
            } else it
        }

    fun add(phases: List<TaskPhase>, name: String, dayMillis: Long): List<TaskPhase> {
        val clean = name.trim().replace(";", ",").replace("@", " ")
        if (clean.isBlank()) return phases
        if (phases.any { it.name.equals(clean, ignoreCase = true) }) return phases
        return phases + TaskPhase(clean, DayTime.startOfDay(dayMillis), 1)
    }

    fun remove(phases: List<TaskPhase>, name: String): List<TaskPhase> =
        phases.filterNot { it.name.equals(name, ignoreCase = true) }

    fun covers(phase: TaskPhase, dayMillis: Long): Boolean {
        val start = DayTime.startOfDay(phase.dayMillis)
        val day = DayTime.startOfDay(dayMillis)
        if (day < start) return false
        val span = phase.spanDays.coerceIn(1, 14)
        return DayTime.daysBetween(start, day) < span
    }

    fun completionRatio(subtasks: String, completedSubtasks: String, isCompleted: Boolean): Float {
        if (isCompleted) return 1f
        val subs = names(subtasks)
        if (subs.isEmpty()) return 0f
        val done = names(completedSubtasks).toSet()
        return (subs.count { it in done }.toFloat() / subs.size.toFloat()).coerceIn(0f, 1f)
    }

    fun dayPercents(
        phases: List<TaskPhase>,
        doneNames: Set<String>,
        weekStart: Long,
        dayCount: Int = 7,
        taskCompleted: Boolean = false,
        completedAtMillis: Long? = null
    ): List<Int> {
        val completedDay = completedAtMillis?.let { DayTime.startOfDay(it) }
        return (0 until dayCount).map { i ->
            val day = DayTime.addDays(weekStart, i)
            val onDay = phases.filter { covers(it, day) }
            when {
                taskCompleted && completedDay != null && completedDay == day -> 100
                onDay.isEmpty() -> 0
                else -> ((onDay.count { it.name in doneNames } * 100f) / onDay.size).toInt().coerceIn(0, 100)
            }
        }
    }

    /**
     * Keep dates for names that already exist; spread brand-new names back from the due day.
     */
    fun withNames(existing: List<TaskPhase>, names: List<String>, dueMillis: Long): List<TaskPhase> {
        val dueDay = DayTime.startOfDay(dueMillis)
        val byLower = existing.associateBy { it.name.lowercase() }
        val clean = names
            .map { it.trim().replace(";", ",").replace("@", " ") }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        if (clean.isEmpty()) return emptyList()
        val unknown = clean.filter { it.lowercase() !in byLower }
        return clean.map { name ->
            val prev = byLower[name.lowercase()]
            if (prev != null) prev.copy(name = name)
            else {
                val slot = unknown.indexOf(name)
                val day = DayTime.addDays(dueDay, -(unknown.size - 1 - slot).coerceAtLeast(0))
                TaskPhase(name, day, 1)
            }
        }
    }
}
