package com.example.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.AuraDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            ACTION_REMIND -> {
                val title = intent.getStringExtra(ReminderScheduler.EXTRA_TITLE) ?: "Reminder"
                val body = intent.getStringExtra(ReminderScheduler.EXTRA_BODY) ?: "Something is due"
                val type = intent.getStringExtra(ReminderScheduler.EXTRA_TYPE).orEmpty()
                val itemId = intent.getIntExtra(ReminderScheduler.EXTRA_ITEM_ID, 0)
                val notifId = ReminderScheduler.requestCode(type, itemId)
                NotificationHelper.showReminder(context, notifId, title, body)
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
                ReminderScheduler.schedule(
                    context = context,
                    type = ReminderScheduler.TYPE_TASK,
                    itemId = task.id,
                    triggerAtMillis = task.dueDateMillis,
                    title = "Task due: ${task.title}",
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
