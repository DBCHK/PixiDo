package com.example

import com.example.data.GoalActivityEntity
import com.example.data.HabitStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitStatsTest {

    @Test
    fun streakCountsBackFromToday() {
        val today = "2026-09-05"
        val done = setOf("2026-09-05", "2026-09-04", "2026-09-03")
        assertEquals(3, HabitStats.currentStreak(done, today))
    }

    @Test
    fun streakAllowsEmptyTodayAndUsesYesterday() {
        val today = "2026-09-05"
        val done = setOf("2026-09-04", "2026-09-03")
        assertEquals(2, HabitStats.currentStreak(done, today))
    }

    @Test
    fun streakBreaksOnGap() {
        val today = "2026-09-05"
        val done = setOf("2026-09-05", "2026-09-03")
        assertEquals(1, HabitStats.currentStreak(done, today))
    }

    @Test
    fun emptyDaysIsZeroStreak() {
        assertEquals(0, HabitStats.currentStreak(emptySet(), "2026-09-05"))
    }

    @Test
    fun weekKeysStartOnSundayAndContainToday() {
        val today = "2026-09-05"
        val week = HabitStats.weekKeys(today)
        assertEquals(7, week.size)
        assertTrue(today in week)
        val parsed = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(week.first())
        val cal = java.util.Calendar.getInstance().apply { time = parsed!! }
        assertEquals(java.util.Calendar.SUNDAY, cal.get(java.util.Calendar.DAY_OF_WEEK))
    }

    @Test
    fun doneDaysIgnoresZeroCounts() {
        val days = HabitStats.doneDays(
            listOf(
                GoalActivityEntity(1, "2026-09-05", completedCount = 1),
                GoalActivityEntity(1, "2026-09-04", completedCount = 0)
            )
        )
        assertTrue("2026-09-05" in days)
        assertFalse("2026-09-04" in days)
    }

    @Test
    fun futureDayIsAfterToday() {
        assertTrue(HabitStats.isFuture("2026-09-06", "2026-09-05"))
        assertFalse(HabitStats.isFuture("2026-09-05", "2026-09-05"))
        assertFalse(HabitStats.isFuture("2026-09-04", "2026-09-05"))
    }

    @Test
    fun shiftDayMovesCalendar() {
        assertEquals("2026-09-04", HabitStats.shiftDay("2026-09-05", -1))
        assertEquals("2026-09-01", HabitStats.shiftDay("2026-08-31", 1))
    }
}
