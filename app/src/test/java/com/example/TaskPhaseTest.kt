package com.example

import com.example.data.DayTime
import com.example.data.TaskPhases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskPhaseTest {

    @Test
    fun bareNamesSpreadBackFromDueDay() {
        val due = DayTime.startOfDay(System.currentTimeMillis())
        val phases = TaskPhases.parse("Interview;Ideate;Wireframe", due)
        assertEquals(3, phases.size)
        assertEquals("Interview", phases[0].name)
        assertEquals("Wireframe", phases[2].name)
        assertEquals(due, phases[2].dayMillis)
        assertEquals(DayTime.addDays(due, -1), phases[1].dayMillis)
        assertEquals(DayTime.addDays(due, -2), phases[0].dayMillis)
    }

    @Test
    fun encodeRoundTripKeepsDates() {
        val due = DayTime.startOfDay(System.currentTimeMillis())
        val original = TaskPhases.parse("Interview;Ideate", due)
        val encoded = TaskPhases.encode(original)
        val again = TaskPhases.parse(encoded, due)
        assertEquals(original.map { it.name to it.dayMillis }, again.map { it.name to it.dayMillis })
        assertTrue(encoded.contains("@"))
    }

    @Test
    fun moveUpdatesOnlyNamedPhase() {
        val due = DayTime.startOfDay(System.currentTimeMillis())
        val phases = TaskPhases.parse("Interview;Ideate", due)
        val shifted = DayTime.addDays(due, 3)
        val moved = TaskPhases.move(phases, "Interview", shifted)
        assertEquals(shifted, moved.first { it.name == "Interview" }.dayMillis)
        assertEquals(phases.first { it.name == "Ideate" }.dayMillis, moved.first { it.name == "Ideate" }.dayMillis)
    }

    @Test
    fun completionRatioUsesRealPhases() {
        assertEquals(0f, TaskPhases.completionRatio("", "", false), 0.0f)
        assertEquals(1f, TaskPhases.completionRatio("", "", true), 0.0f)
        val ratio = TaskPhases.completionRatio("Interview;Ideate;Wireframe", "Interview;Ideate", false)
        assertEquals(2f / 3f, ratio, 0.001f)
    }

    @Test
    fun dayPercentsFollowScheduledPhases() {
        val due = DayTime.startOfDay(System.currentTimeMillis())
        val phases = TaskPhases.parse("Interview;Ideate;Wireframe", due)
        val done = setOf("Interview", "Ideate")
        val weekStart = DayTime.startOfWeekSunday(due)
        val percents = TaskPhases.dayPercents(
            phases = phases,
            doneNames = done,
            weekStart = weekStart,
            dayCount = 7
        )
        phases.forEach { phase ->
            val idx = DayTime.daysBetween(weekStart, phase.dayMillis)
            if (idx in percents.indices) {
                val expected = if (phase.name in done) 100 else 0
                assertEquals(expected, percents[idx])
            }
        }
        assertTrue(TaskPhases.covers(phases[2], due))
        assertTrue(phases[0].name in done)
    }

    @Test
    fun withNamesKeepsExistingDates() {
        val due = DayTime.startOfDay(System.currentTimeMillis())
        val original = TaskPhases.parse("Interview;Ideate", due)
        val interviewDay = original.first { it.name == "Interview" }.dayMillis
        val merged = TaskPhases.withNames(original, listOf("Interview", "Ship"), due)
        assertEquals(interviewDay, merged.first { it.name == "Interview" }.dayMillis)
        assertEquals(due, merged.first { it.name == "Ship" }.dayMillis)
        assertEquals(2, merged.size)
    }
}
