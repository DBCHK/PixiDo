package com.example.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * Schedules exact local alarms that fire [ReminderReceiver] even when the app is closed.
 */
object ReminderScheduler {

    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_BODY = "extra_body"
    const val EXTRA_TYPE = "extra_type"
    const val EXTRA_ITEM_ID = "extra_item_id"

    const val TYPE_TASK = "task"
    const val TYPE_EVENT = "event"
    /** Pre-alert ~5 min before due — surfaces an “Upcoming” Now Bar card. */
    const val TYPE_TASK_APPROACHING = "task_approaching"

    private const val TAG = "ReminderScheduler"

    fun schedule(
        context: Context,
        type: String,
        itemId: Int,
        triggerAtMillis: Long,
        title: String,
        body: String
    ) {
        if (triggerAtMillis <= System.currentTimeMillis() + 5_000L) {
            Log.d(TAG, "Skip schedule: time already passed for $type#$itemId")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, type, itemId, title, body)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pending
                    )
                } else {
                    // Fallback — still fire around the time even without exact permission
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pending
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending
                )
            }
            Log.d(TAG, "Scheduled $type#$itemId at $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.e(TAG, "Exact alarm not permitted", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pending
                )
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to schedule alarm", e2)
            }
        }
    }

    fun cancel(context: Context, type: String, itemId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, type, itemId, "", "")
        alarmManager.cancel(pending)
        pending.cancel()
    }

    private fun pendingIntent(
        context: Context,
        type: String,
        itemId: Int,
        title: String,
        body: String
    ): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_REMIND
            putExtra(EXTRA_TYPE, type)
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
        }
        val requestCode = requestCode(type, itemId)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun requestCode(type: String, itemId: Int): Int {
        return when (type) {
            TYPE_TASK -> 10_000 + itemId
            TYPE_EVENT -> 20_000 + itemId
            TYPE_TASK_APPROACHING -> 40_000 + itemId
            else -> 30_000 + itemId
        }
    }

    /**
     * Schedule due-time reminder + optional “coming up” Now Bar ping
     * [leadMinutes] before the ETA.
     */
    fun scheduleTaskReminders(
        context: Context,
        itemId: Int,
        dueAtMillis: Long,
        title: String,
        body: String,
        leadMinutes: Int = 5
    ) {
        schedule(
            context = context,
            type = TYPE_TASK,
            itemId = itemId,
            triggerAtMillis = dueAtMillis,
            title = "Task due: $title",
            body = body
        )
        val approachAt = dueAtMillis - leadMinutes.coerceAtLeast(1) * 60_000L
        if (approachAt > System.currentTimeMillis() + 15_000L) {
            schedule(
                context = context,
                type = TYPE_TASK_APPROACHING,
                itemId = itemId,
                triggerAtMillis = approachAt,
                title = title,
                body = "Coming up in $leadMinutes min · $body"
            )
        }
    }

    fun cancelTaskReminders(context: Context, itemId: Int) {
        cancel(context, TYPE_TASK, itemId)
        cancel(context, TYPE_TASK_APPROACHING, itemId)
    }

    /**
     * Combines a day timestamp with "HH:mm" or "h:mm a" time into epoch millis.
     */
    fun combineDateAndTime(dateMillis: Long, timeStr: String): Long? {
        val parsed = parseTimeOfDay(timeStr) ?: return null
        val cal = Calendar.getInstance().apply {
            timeInMillis = dateMillis
            set(Calendar.HOUR_OF_DAY, parsed.first)
            set(Calendar.MINUTE, parsed.second)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /**
     * Accepts "14:30", "2:30 PM", "2:30pm", "14.30".
     */
    fun parseTimeOfDay(raw: String): Pair<Int, Int>? {
        val s = raw.trim()
        if (s.isBlank()) return null

        // 12h with AM/PM
        val amPm = Regex("""(?i)^(\d{1,2}):(\d{2})\s*(AM|PM)$""").matchEntire(s)
        if (amPm != null) {
            var hour = amPm.groupValues[1].toInt()
            val minute = amPm.groupValues[2].toInt()
            val period = amPm.groupValues[3].uppercase()
            if (period == "PM" && hour < 12) hour += 12
            if (period == "AM" && hour == 12) hour = 0
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour to minute
        }

        // 24h
        val h24 = Regex("""^(\d{1,2})[:.](\d{2})$""").matchEntire(s)
        if (h24 != null) {
            val hour = h24.groupValues[1].toInt()
            val minute = h24.groupValues[2].toInt()
            if (hour !in 0..23 || minute !in 0..59) return null
            return hour to minute
        }

        return null
    }

    /** Formats millis as "h:mm a" for display. */
    fun formatTime(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        val hour = cal.get(Calendar.HOUR)
        val displayHour = if (hour == 0) 12 else hour
        val minute = cal.get(Calendar.MINUTE)
        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        return String.format("%d:%02d %s", displayHour, minute, amPm)
    }
}
