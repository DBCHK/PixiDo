package com.example.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AuraDatabase
import com.example.data.UserPreferencesRepository
import com.example.widget.WidgetActions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles Pause / Resume / Stop on Focus Now Bar cards, and
 * Done / Snooze / Dismiss on task ETA Now Bar cards.
 */
class NowBarActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val app = context.applicationContext

        when (action) {
            NowBarHelper.ACTION_FOCUS_PAUSE -> {
                FocusTimerService.pause(app)
            }
            NowBarHelper.ACTION_FOCUS_RESUME -> {
                FocusTimerService.resume(app, 0, 0)
            }
            NowBarHelper.ACTION_FOCUS_STOP -> {
                FocusTimerService.stop(app)
            }
            NowBarHelper.ACTION_TASK_DONE -> {
                val taskId = intent.getIntExtra(NowBarHelper.EXTRA_TASK_ID, 0)
                if (taskId <= 0) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        completeTask(app, taskId)
                        NowBarHelper.clearTaskEta(app, taskId)
                        WidgetActions.refreshAll(app)
                    } finally {
                        pending.finish()
                    }
                }
            }
            NowBarHelper.ACTION_TASK_SNOOZE -> {
                val taskId = intent.getIntExtra(NowBarHelper.EXTRA_TASK_ID, 0)
                if (taskId <= 0) return
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        snoozeTaskMinutes(app, taskId, 10)
                        NowBarHelper.clearTaskEta(app, taskId)
                        WidgetActions.refreshAll(app)
                    } finally {
                        pending.finish()
                    }
                }
            }
            NowBarHelper.ACTION_TASK_DISMISS -> {
                val taskId = intent.getIntExtra(NowBarHelper.EXTRA_TASK_ID, 0)
                if (taskId > 0) NowBarHelper.clearTaskEta(app, taskId)
            }
        }
    }

    private suspend fun completeTask(context: Context, taskId: Int) {
        val dao = AuraDatabase.getDatabase(context).auraDao()
        val tasks = dao.getTasksOnce()
        val task = tasks.find { it.id == taskId } ?: return
        if (task.isCompleted) return
        val now = System.currentTimeMillis()
        if (task.isRepeating) {
            val rolled = com.example.data.TaskRepeat.rollForward(task, now)
            dao.updateTask(rolled)
            ReminderScheduler.cancelTaskReminders(context, taskId)
            if (rolled.dueDateMillis > now) {
                ReminderScheduler.scheduleTaskReminders(
                    context = context,
                    itemId = taskId,
                    dueAtMillis = rolled.dueDateMillis,
                    title = rolled.title,
                    body = "It's time for “${rolled.title}” (${rolled.dueTimeStr})"
                )
            }
        } else {
            dao.updateTask(
                task.copy(
                    isCompleted = true,
                    streakCount = task.streakCount + 1,
                    completedAtMillis = now
                )
            )
            ReminderScheduler.cancelTaskReminders(context, taskId)
        }
        runCatching {
            UserPreferencesRepository(context).addXp(task.xpReward)
        }
    }

    private suspend fun snoozeTaskMinutes(context: Context, taskId: Int, minutes: Int) {
        val dao = AuraDatabase.getDatabase(context).auraDao()
        val task = dao.getTasksOnce().find { it.id == taskId } ?: return
        if (task.isCompleted) return
        val newDue = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        val timePart = ReminderScheduler.formatTime(newDue)
        val updated = task.copy(
            dueDateMillis = newDue,
            dueTimeStr = "Snoozed · $timePart"
        )
        dao.updateTask(updated)
        ReminderScheduler.cancelTaskReminders(context, taskId)
        ReminderScheduler.scheduleTaskReminders(
            context = context,
            itemId = task.id,
            dueAtMillis = newDue,
            title = updated.title,
            body = "It's time for “${updated.title}” (${updated.dueTimeStr})"
        )
    }
}
