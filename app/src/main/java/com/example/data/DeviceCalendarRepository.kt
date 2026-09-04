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

data class DeviceCalendarSource(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
    val isPrimary: Boolean
)

object DeviceCalendars {
    fun parseIds(raw: String): Set<Long> =
        raw.split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()

    fun encodeIds(ids: Set<Long>): String =
        ids.sorted().joinToString(",")

    fun looksLikeHolidayOrBirthday(name: String): Boolean {
        val n = name.lowercase(Locale.getDefault())
        return n.contains("holiday") ||
            n.contains("birthday") ||
            n.contains("birthdays") ||
            n.contains("contacts")
    }

    /**
     * One primary calendar per account. Skips Holidays / Birthdays so the
     * same public events aren't imported from every signed-in Google account.
     */
    fun suggestedIds(calendars: List<DeviceCalendarSource>): Set<Long> {
        if (calendars.isEmpty()) return emptySet()
        val out = mutableSetOf<Long>()
        calendars.groupBy { it.accountName.ifBlank { it.displayName } }.values.forEach { group ->
            val usable = group.filterNot { looksLikeHolidayOrBirthday(it.displayName) }
            val pick = usable.firstOrNull { it.isPrimary }
                ?: usable.firstOrNull()
                ?: group.firstOrNull { it.isPrimary }
            if (pick != null) out += pick.id
        }
        if (out.isEmpty()) out += calendars.first().id
        return out
    }
}

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

    fun listCalendars(): List<DeviceCalendarSource> {
        if (!hasPermission()) return emptyList()
        val cr = context.contentResolver
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.OWNER_ACCOUNT
        )
        val out = ArrayList<DeviceCalendarSource>()
        runCatching {
            cr.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                null,
                null,
                "${CalendarContract.Calendars.ACCOUNT_NAME} ASC"
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountIdx = cursor.getColumnIndex(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                val visibleIdx = cursor.getColumnIndex(CalendarContract.Calendars.VISIBLE)
                val primaryIdx = cursor.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)
                val ownerIdx = cursor.getColumnIndex(CalendarContract.Calendars.OWNER_ACCOUNT)
                if (idIdx < 0) return@use
                while (cursor.moveToNext()) {
                    if (visibleIdx >= 0 && cursor.getInt(visibleIdx) == 0) continue
                    val id = cursor.getLong(idIdx)
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx)?.trim().orEmpty() else ""
                    val account = if (accountIdx >= 0) cursor.getString(accountIdx)?.trim().orEmpty() else ""
                    val owner = if (ownerIdx >= 0) cursor.getString(ownerIdx)?.trim().orEmpty() else ""
                    val color = if (colorIdx >= 0) cursor.getInt(colorIdx) else 0
                    val isPrimary = when {
                        primaryIdx >= 0 -> cursor.getInt(primaryIdx) == 1
                        owner.isNotBlank() && account.isNotBlank() ->
                            owner.equals(account, ignoreCase = true)
                        else -> false
                    }
                    out += DeviceCalendarSource(
                        id = id,
                        displayName = name.ifBlank { "Calendar" },
                        accountName = account.ifBlank { "This device" },
                        color = color,
                        isPrimary = isPrimary
                    )
                }
            }
        }
        return out.distinctBy { it.id }
    }

    fun queryVisibleEvents(
        fromMillis: Long,
        toMillis: Long,
        calendarIds: Set<Long>
    ): List<CalendarEventEntity> {
        if (!hasPermission()) return emptyList()
        if (toMillis <= fromMillis) return emptyList()
        if (calendarIds.isEmpty()) return emptyList()

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
            CalendarContract.Instances.CALENDAR_DISPLAY_NAME,
            CalendarContract.Instances.CALENDAR_ID
        )

        val idList = calendarIds.joinToString(",") { it.toString() }
        val selection = "${CalendarContract.Instances.CALENDAR_ID} IN ($idList)"

        val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        val out = ArrayList<CalendarEventEntity>()
        val seen = HashSet<String>()

        cr.query(
            uri,
            projection,
            selection,
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
                val dedupeKey = "${title.lowercase(Locale.getDefault())}|$begin|$allDay"
                if (!seen.add(dedupeKey)) continue
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
