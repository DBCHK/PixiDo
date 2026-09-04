package com.example

import com.example.data.DeviceCalendarSource
import com.example.data.DeviceCalendars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceCalendarsTest {

    @Test
    fun holidayNamesAreFiltered() {
        assertTrue(DeviceCalendars.looksLikeHolidayOrBirthday("Holidays in India"))
        assertTrue(DeviceCalendars.looksLikeHolidayOrBirthday("Birthdays"))
        assertTrue(DeviceCalendars.looksLikeHolidayOrBirthday("Contacts"))
        assertFalse(DeviceCalendars.looksLikeHolidayOrBirthday("Personal"))
        assertFalse(DeviceCalendars.looksLikeHolidayOrBirthday("Work"))
    }

    @Test
    fun suggestedIdsKeepsOnePrimaryPerAccountAndSkipsHolidays() {
        val calendars = listOf(
            source(1, "Personal", "a@gmail.com", primary = true),
            source(2, "Holidays in India", "a@gmail.com"),
            source(3, "Birthdays", "a@gmail.com"),
            source(4, "Work", "work@company.com", primary = true),
            source(5, "Holidays in India", "work@company.com")
        )
        val ids = DeviceCalendars.suggestedIds(calendars)
        assertEquals(setOf(1L, 4L), ids)
    }

    @Test
    fun encodeRoundTrip() {
        val raw = DeviceCalendars.encodeIds(setOf(9, 3, 3, 1))
        assertEquals("1,3,9", raw)
        assertEquals(setOf(1L, 3L, 9L), DeviceCalendars.parseIds(raw))
        assertTrue(DeviceCalendars.parseIds("").isEmpty())
    }

    private fun source(
        id: Long,
        name: String,
        account: String,
        primary: Boolean = false
    ) = DeviceCalendarSource(
        id = id,
        displayName = name,
        accountName = account,
        color = 0,
        isPrimary = primary
    )
}
