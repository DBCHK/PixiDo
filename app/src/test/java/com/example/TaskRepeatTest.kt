package com.example

import com.example.data.RepeatRule
import com.example.data.TaskRepeat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class TaskRepeatTest {

    @Test
    fun dailySkipsToNextFutureDay() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -3)
        }
        val now = System.currentTimeMillis()
        val next = TaskRepeat.nextDue(cal.timeInMillis, RepeatRule.DAILY, now)
        assertTrue(next > now)
        val nextCal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(9, nextCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun weekdaysNeverLandOnWeekend() {
        val saturday = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val next = TaskRepeat.nextDue(
            saturday.timeInMillis,
            RepeatRule.WEEKDAYS,
            saturday.timeInMillis
        )
        val day = Calendar.getInstance().apply { timeInMillis = next }
            .get(Calendar.DAY_OF_WEEK)
        assertTrue(day != Calendar.SATURDAY && day != Calendar.SUNDAY)
    }

    @Test
    fun weeklyKeepsWeekday() {
        val start = System.currentTimeMillis()
        val next = TaskRepeat.nextDue(start, RepeatRule.WEEKLY, start)
        val startDay = Calendar.getInstance().apply { timeInMillis = start }
            .get(Calendar.DAY_OF_WEEK)
        val nextDay = Calendar.getInstance().apply { timeInMillis = next }
            .get(Calendar.DAY_OF_WEEK)
        assertTrue(next > start)
        assertEquals(startDay, nextDay)
    }
}
