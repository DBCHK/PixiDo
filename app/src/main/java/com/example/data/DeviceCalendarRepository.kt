package com.example.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

/**
 * Reads the device calendar (Google, Samsung, etc.) so those events can
 * be shown alongside PixiDo events. Read-only — we never write back.
 */
class DeviceCalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

    fun queryVisibleEvents(
        fromMillis: Long,
        toMillis: Long
    ): List<CalendarEventEntity> {
        if (!hasPermission()) return emptyList()
        if (toMillis <= fromMillis) return emptyList()

        val cr = context.contentResolver
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().let { builder ->
            android.content.ContentUris.appendId(builder, fromMillis)
            android.content.ContentUris.appendId(builder, toMillis)
            builder.build()
        }

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME
        )

        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        val out = ArrayList<CalendarEventEntity>()

        cr.query(
            uri,
            projection,
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val descIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
            val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val allDayIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val calIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val eventId = cursor.getLong(idIdx)
                val title = cursor.getString(titleIdx)?.trim().orEmpty()
                if (title.isEmpty()) continue
                val begin = cursor.getLong(beginIdx)
                val end = cursor.getLong(endIdx)
                val allDay = cursor.getInt(allDayIdx) == 1
                val calendarName = cursor.getString(calIdx)?.trim().orEmpty().ifBlank { "Phone" }
                val description = cursor.getString(descIdx).orEmpty()

                val dayStart = if (allDay) utcMidnightToLocalDay(begin) else startOfDay(begin)
                val timeSlot = if (allDay) {
                    "All day"
                } else {
                    val startLabel = timeFmt.format(Date(begin))
                    val sameDay = startOfDay(end) == dayStart
                    if (end > begin && sameDay) {
                        "$startLabel–${timeFmt.format(Date(end))}"
                    } else {
                        startLabel
                    }
                }

                out += CalendarEventEntity(
                    id = syntheticId(eventId, begin),
                    title = title,
                    description = description,
                    category = calendarName,
                    dateMillis = dayStart,
                    timeSlot = timeSlot,
                    startMillis = if (allDay) dayStart else begin,
                    isCompleted = false
                )
            }
        }

        return out
    }

    companion object {
        /** Device events use negative ids so they never collide with Room rows. */
        fun isDeviceEvent(id: Int): Boolean = id < 0

        private fun syntheticId(eventId: Long, begin: Long): Int {
            val mixed = eventId * 31L + begin
            val mag = (abs(mixed.hashCode()) % 1_000_000_000).coerceAtLeast(1)
            return -mag
        }

        private fun startOfDay(millis: Long): Long =
            Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

        /** All-day instances are stored as UTC midnight. */
        private fun utcMidnightToLocalDay(utcMillis: Long): Long {
            val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                timeInMillis = utcMillis
            }
            return Calendar.getInstance().apply {
                set(Calendar.YEAR, utc.get(Calendar.YEAR))
                set(Calendar.MONTH, utc.get(Calendar.MONTH))
                set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }
}
