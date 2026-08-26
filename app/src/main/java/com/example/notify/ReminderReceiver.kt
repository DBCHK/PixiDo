package com.example.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AuraDatabase
import com.example.data.NotificationSoundOption
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
// NowBarHelper used for approaching task cards

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_REMIND -> {
                val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Reminder"
                val body = intent.getStringExtra(ReminderScheduler.EXTRA_BODY) ?: "Something is due"
                val type = intent.getStringExtra(ReminderScheduler.EXTRA_TYPE).orEmpty()
                val itemId = intent.getIntExtra(ReminderScheduler.EXTRA_ITEM_ID, 0)
                val notifId = ReminderScheduler.requestCode(type, itemId)
                val sound = runCatching {
                    runBlocking {
                        UserPreferencesRepository(context.applicationContext)
                            .currentProfile()
                            .notificationSound
                    }
                }.getOrDefault(NotificationSoundOption.SOFT)
                when (type) {
                    ReminderScheduler.TYPE_TASK_APPROACHING -> {
                        // Soft Now Bar “coming up” card — no loud ringtone / full popup
                        NowBarHelper.showTaskEta(
                            context = context,
                            taskId = itemId,
                            title = title.removePrefix("Task due: ").trim().ifBlank { title },
                            body = body,
                            dueAtMillis = System.currentTimeMillis() + 5 * 60_000L,
                            isDueNow = false
                        )
                    }
                    else -> {
                        NotificationHelper.showReminder(
                            context = context,
                            notificationId = notifId,
                            title = title,
                            body = body,
                            soundOption = sound,
                            type = type,
                            itemId = itemId,
                            showEtaPopup = type == ReminderScheduler.TYPE_TASK ||
                                type == ReminderScheduler.TYPE_EVENT
                        )
                    }
                }
            }
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                // Reschedule all future reminders after reboot / update
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        rescheduleAll(context.applicationContext)
                    } catch (e: Exception) {
                        Log.e(TAG, "Reschedule failed", e)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private suspend fun rescheduleAll(context: Context) {
        val dao = AuraDatabase.getDatabase(context).auraDao()
        val now = System.currentTimeMillis()

        dao.getAllTasks().first().forEach { task ->
            if (!task.isCompleted && task.dueDateMillis > now) {
                ReminderScheduler.scheduleTaskReminders(
                    context = context,
                    itemId = task.id,
                    dueAtMillis = task.dueDateMillis,
                    title = task.title,
                    body = "It's time for “${task.title}” (${task.dueTimeStr})"
                )
            }
        }

        dao.getAllCalendarEvents().first().forEach { event ->
            if (!event.isCompleted && event.startMillis > now) {
                ReminderScheduler.schedule(
                    context = context,
                    type = ReminderScheduler.TYPE_EVENT,
                    itemId = event.id,
                    triggerAtMillis = event.startMillis,
                    title = "Event: ${event.title}",
                    body = "Starting now · ${event.timeSlot}"
                )
            }
        }
    }

    companion object {
        const val ACTION_REMIND = "com.example.pixido.ACTION_REMIND"
        private const val TAG = "ReminderReceiver"
    }
}
