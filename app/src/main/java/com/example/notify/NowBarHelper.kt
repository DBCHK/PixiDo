package com.example.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.widget.WidgetActions

/**
 * Focus / task Live Updates for Samsung Now Bar.
 *
 * One UI 8.5 maps Android Live Updates → Now Bar when:
 *  - ongoing + requestPromotedOngoing
 *  - valid monochrome small icon
 *  - HIGH channel
 *  - chronometer for timers
 *  - notification is bound via startForeground (Focus)
 *
 * Avoid re-notifying every second (demotes Live Updates).
 */
object NowBarHelper {

    private const val TAG = "PixiDoNowBar"

    const val CHANNEL_NOW_BAR = "pixido_now_bar_v6"
    const val ID_FOCUS = 91_001
    const val ID_TASK_ETA_BASE = 92_000

    const val ACTION_FOCUS_PAUSE = "com.example.pixido.NOWBAR_FOCUS_PAUSE"
    const val ACTION_FOCUS_RESUME = "com.example.pixido.NOWBAR_FOCUS_RESUME"
    const val ACTION_FOCUS_STOP = "com.example.pixido.NOWBAR_FOCUS_STOP"
    const val ACTION_TASK_DONE = "com.example.pixido.NOWBAR_TASK_DONE"
    const val ACTION_TASK_SNOOZE = "com.example.pixido.NOWBAR_TASK_SNOOZE"
    const val ACTION_TASK_DISMISS = "com.example.pixido.NOWBAR_TASK_DISMISS"

    const val EXTRA_TASK_ID = "nowbar_task_id"
    const val EXTRA_TASK_TITLE = "nowbar_task_title"

    private val ACCENT = 0xFF7C3AED.toInt()
    private val DANGER = 0xFFE11D48.toInt()

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val mgr = context.getSystemService(NotificationManager::class.java) ?: return
        listOf(
            "pixido_now_bar_v1", "pixido_now_bar_v2", "pixido_now_bar_v3",
            "pixido_now_bar_v4", "pixido_now_bar_v5"
        ).forEach { runCatching { mgr.deleteNotificationChannel(it) } }

        if (mgr.getNotificationChannel(CHANNEL_NOW_BAR) != null) return
        mgr.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NOW_BAR,
                "Now Bar · Focus",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Focus timer on Samsung Now Bar / Live Updates"
                setShowBadge(true)
                enableVibration(false)
                setSound(null, null)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
        )
        Log.i(TAG, "channel $CHANNEL_NOW_BAR created")
    }

    fun canPostPromoted(context: Context): Boolean = try {
        if (Build.VERSION.SDK_INT < 36) true
        else {
            val mgr = context.getSystemService(NotificationManager::class.java) ?: return true
            val m = mgr.javaClass.methods.firstOrNull {
                it.name == "canPostPromotedNotifications" && it.parameterTypes.isEmpty()
            }
            (m?.invoke(mgr) as? Boolean) ?: true
        }
    } catch (_: Exception) {
        true
    }

    fun buildFocusNotification(
        context: Context,
        secondsLeft: Int,
        totalSeconds: Int,
        isRunning: Boolean,
        endAtMillis: Long
    ): Notification {
        ensureChannel(context)
        val app = context.applicationContext
        val openApp = WidgetActions.openApp(app, WidgetActions.ACTION_OPEN_FOCUS, ID_FOCUS)

        val left = secondsLeft.coerceAtLeast(0)
        val total = totalSeconds.coerceAtLeast(1)
        val mm = (left / 60).toString().padStart(2, '0')
        val ss = (left % 60).toString().padStart(2, '0')
        val timeLabel = "$mm:$ss"
        val elapsed = (total - left).coerceIn(0, total)
        val stateLabel = if (isRunning) "Focusing" else "Paused"

        // Use DEFAULT + progress + chronometer (most compatible Live Update path).
        // ProgressStyle alone was flaky on some One UI 8.5 builds.
        val builder = NotificationCompat.Builder(app, CHANNEL_NOW_BAR)
            .setSmallIcon(R.drawable.ic_stat_pixido)
            .setContentTitle("PixiDo · $stateLabel")
            .setContentText(timeLabel)
            .setSubText(timeLabel)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(timeLabel.take(7))
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setColor(ACCENT)
            .setColorized(false)
            .setShowWhen(true)
            .setUsesChronometer(isRunning)
            .setChronometerCountDown(true)
            .setWhen(if (isRunning) endAtMillis else System.currentTimeMillis())
            .setProgress(total, elapsed, false)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle("PixiDo · $stateLabel")
                    .bigText(timeLabel)
            )

        if (isRunning) {
            builder.addAction(
                android.R.drawable.ic_media_pause, "Pause",
                broadcast(app, ACTION_FOCUS_PAUSE, 11)
            )
        } else {
            builder.addAction(
                android.R.drawable.ic_media_play, "Resume",
                broadcast(app, ACTION_FOCUS_RESUME, 12)
            )
        }
        builder.addAction(
            android.R.drawable.ic_menu_close_clear_cancel, "Stop",
            broadcast(app, ACTION_FOCUS_STOP, 13)
        )

        // Samsung Now Bar extras — timer primary, PixiDo Focusing secondary
        builder.addExtras(
            Bundle().apply {
                putInt("android.ongoingActivityNoti.style", 1)
                putCharSequence("android.ongoingActivityNoti.primaryInfo", timeLabel)
                putCharSequence(
                    "android.ongoingActivityNoti.secondaryInfo",
                    "PixiDo · $stateLabel"
                )
                putCharSequence("android.ongoingActivityNoti.chipPrimaryInfo", timeLabel)
                putCharSequence(
                    "android.ongoingActivityNoti.chipSecondaryInfo",
                    "PixiDo · $stateLabel"
                )
                putCharSequence("android.ongoingActivityNoti.chipExpandedText", timeLabel.take(7))
                putInt("android.ongoingActivityNoti.chipBgColor", ACCENT)
                putInt("android.ongoingActivityNoti.progress", elapsed)
                putInt("android.ongoingActivityNoti.progressMax", total)
                putInt("android.ongoingActivityNoti.actionType", 1)
                putInt("android.ongoingActivityNoti.actionPrimarySet", 1)
                putBoolean(NotificationCompat.EXTRA_REQUEST_PROMOTED_ONGOING, true)
            }
        )
        builder.extras.putBoolean("android.ongoingActivityNoti.isOngoing", true)
        builder.extras.putBoolean(NotificationCompat.EXTRA_REQUEST_PROMOTED_ONGOING, true)

        return builder.build()
    }

    fun showFocus(
        context: Context,
        secondsLeft: Int,
        totalSeconds: Int,
        isRunning: Boolean,
        endAtMillis: Long
    ): Notification {
        val n = buildFocusNotification(context, secondsLeft, totalSeconds, isRunning, endAtMillis)
        logPromotable(context, n)
        notifySafe(context, ID_FOCUS, n)
        return n
    }

    fun clearFocus(context: Context) {
        NotificationManagerCompat.from(context.applicationContext).cancel(ID_FOCUS)
    }

    fun taskNotificationId(taskId: Int): Int = ID_TASK_ETA_BASE + (taskId % 50_000)

    fun showTaskEta(
        context: Context,
        taskId: Int,
        title: String,
        body: String,
        dueAtMillis: Long = System.currentTimeMillis(),
        isDueNow: Boolean = true
    ) {
        ensureChannel(context)
        val app = context.applicationContext
        val notifId = taskNotificationId(taskId)
        val openIntent = Intent(app, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(NotificationHelper.EXTRA_ETA_POPUP, true)
            putExtra(NotificationHelper.EXTRA_ETA_TITLE, title)
            putExtra(NotificationHelper.EXTRA_ETA_BODY, body)
            putExtra(NotificationHelper.EXTRA_ETA_TYPE, ReminderScheduler.TYPE_TASK)
            putExtra(NotificationHelper.EXTRA_ETA_ITEM_ID, taskId)
        }
        val openPi = PendingIntent.getActivity(
            app, notifId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val state = if (isDueNow) "Due now" else "Coming up"
        val primary = title.take(40).ifBlank { "Task" }
        val chip = if (isDueNow) "Due" else "Soon"
        val accent = if (isDueNow) DANGER else ACCENT

        val builder = NotificationCompat.Builder(app, CHANNEL_NOW_BAR)
            .setSmallIcon(R.drawable.ic_stat_pixido)
            .setContentTitle(if (isDueNow) "Task due · $primary" else "Upcoming · $primary")
            .setContentText(body.ifBlank { state })
            .setContentIntent(openPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText(chip)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setColor(accent)
            .setShowWhen(true)
            .setWhen(dueAtMillis)
            .addAction(
                android.R.drawable.checkbox_on_background, "Done",
                taskBroadcast(app, ACTION_TASK_DONE, taskId, title, 21)
            )
            .addAction(
                android.R.drawable.ic_popup_sync, "Snooze",
                taskBroadcast(app, ACTION_TASK_SNOOZE, taskId, title, 22)
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel, "Dismiss",
                taskBroadcast(app, ACTION_TASK_DISMISS, taskId, title, 23)
            )

        builder.addExtras(Bundle().apply {
            putInt("android.ongoingActivityNoti.style", 1)
            putCharSequence("android.ongoingActivityNoti.primaryInfo", primary)
            putCharSequence("android.ongoingActivityNoti.secondaryInfo", "PixiDo · $state")
            putCharSequence("android.ongoingActivityNoti.chipPrimaryInfo", primary.take(18))
            putCharSequence("android.ongoingActivityNoti.chipSecondaryInfo", state)
            putCharSequence("android.ongoingActivityNoti.chipExpandedText", chip)
            putInt("android.ongoingActivityNoti.chipBgColor", accent)
            putBoolean(NotificationCompat.EXTRA_REQUEST_PROMOTED_ONGOING, true)
        })
        builder.extras.putBoolean("android.ongoingActivityNoti.isOngoing", true)
        notifySafe(app, notifId, builder.build())
    }

    fun clearTaskEta(context: Context, taskId: Int) {
        NotificationManagerCompat.from(context.applicationContext)
            .cancel(taskNotificationId(taskId))
    }

    private fun logPromotable(context: Context, notification: Notification) {
        try {
            val can = canPostPromoted(context)
            val has = runCatching {
                notification.javaClass.methods.firstOrNull {
                    it.name == "hasPromotableCharacteristics" && it.parameterTypes.isEmpty()
                }?.invoke(notification) as? Boolean
            }.getOrNull()
            Log.i(TAG, "canPostPromoted=$can hasPromotable=$has")
        } catch (_: Exception) {
        }
    }

    private fun broadcast(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, NowBarActionReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun taskBroadcast(
        context: Context, action: String, taskId: Int, title: String, code: Int
    ): PendingIntent {
        val i = Intent(context, NowBarActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_TASK_ID, taskId)
            putExtra(EXTRA_TASK_TITLE, title)
        }
        return PendingIntent.getBroadcast(
            context, code + taskId, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun notifySafe(context: Context, id: Int, notification: Notification) {
        try {
            NotificationManagerCompat.from(context.applicationContext).notify(id, notification)
            Log.d(TAG, "posted id=$id")
        } catch (e: Exception) {
            Log.e(TAG, "post failed id=$id", e)
        }
    }
}
