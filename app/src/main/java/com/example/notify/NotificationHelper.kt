package com.example.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.NotificationSoundOption

object NotificationHelper {

    private const val CHANNEL_BASE = "pixido_reminders"
    /** High-priority channel for task ETA with custom calm ringtone. */
    private const val CHANNEL_ETA = "pixido_eta_ringtone_v4"

    const val EXTRA_ETA_POPUP = "pixido_eta_popup"
    const val EXTRA_ETA_TITLE = "pixido_eta_title"
    const val EXTRA_ETA_BODY = "pixido_eta_body"
    const val EXTRA_ETA_TYPE = "pixido_eta_type"
    const val EXTRA_ETA_ITEM_ID = "pixido_eta_item_id"

    const val EXTRA_SMS_PROMPT = "pixido_sms_prompt"
    private const val CHANNEL_SMS = "pixido_sms_transactions"
    private const val SMS_NOTIFICATION_BASE = 71_000

    fun channelIdFor(option: NotificationSoundOption): String =
        "${CHANNEL_BASE}_${option.name.lowercase()}_v4"

    fun ensureChannels(context: Context, option: NotificationSoundOption = NotificationSoundOption.SOFT) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Create (or no-op) the preferred channel with its sound
        val preferredId = channelIdFor(option)
        if (manager.getNotificationChannel(preferredId) == null) {
            manager.createNotificationChannel(buildChannel(context, option))
        }

        // Ensure all known variants exist so switching sound never fails
        NotificationSoundOption.entries.forEach { opt ->
            val id = channelIdFor(opt)
            if (manager.getNotificationChannel(id) == null) {
                manager.createNotificationChannel(buildChannel(context, opt))
            }
        }

        if (manager.getNotificationChannel(CHANNEL_ETA) == null) {
            manager.createNotificationChannel(buildEtaChannel(context))
        }

        if (manager.getNotificationChannel(CHANNEL_SMS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SMS,
                    "Bank SMS transactions",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Debit and credit alerts detected from bank SMS"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 40, 80, 40)
                }
            )
        }

        // Clean up legacy channel ids from earlier builds
        manager.deleteNotificationChannel(CHANNEL_BASE)
        manager.deleteNotificationChannel("pixido_eta_ringtone")
        manager.deleteNotificationChannel("pixido_eta_ringtone_v2")
        manager.deleteNotificationChannel("pixido_eta_ringtone_v3")
        NotificationSoundOption.entries.forEach { opt ->
            val key = opt.name.lowercase()
            manager.deleteNotificationChannel("${CHANNEL_BASE}_$key")
            manager.deleteNotificationChannel("${CHANNEL_BASE}_${key}_v2")
            manager.deleteNotificationChannel("${CHANNEL_BASE}_${key}_v3")
        }
    }

    /**
     * Recreate the channel for [option] so a sound change is applied.
     * Android freezes channel sound after first create — delete + recreate.
     */
    fun applySoundOption(context: Context, option: NotificationSoundOption) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val id = channelIdFor(option)
        manager.deleteNotificationChannel(id)
        manager.createNotificationChannel(buildChannel(context, option))
        ensureChannels(context, option)
    }

    private fun buildChannel(context: Context, option: NotificationSoundOption): NotificationChannel {
        val channel = NotificationChannel(
            channelIdFor(option),
            "Task & Event reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Sweet calm alerts when a task or event is due · ${option.displayName()}"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 40, 80, 40)
            val soundUri = soundUri(context, option)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(soundUri, attrs)
        }
        return channel
    }

    private fun buildEtaChannel(context: Context): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ETA,
            "Task ETA · PixiDo ringtone",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Full popup + calm custom ringtone when a task is due"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 60, 120, 60, 120, 60)
            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(EtaRingtonePlayer.ringtoneUri(context), attrs)
            setBypassDnd(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
    }

    fun soundUri(context: Context, option: NotificationSoundOption): Uri {
        return when (option) {
            NotificationSoundOption.SOFT ->
                Uri.parse("android.resource://${context.packageName}/${R.raw.sfx_task}")
            NotificationSoundOption.BRIGHT ->
                Uri.parse("android.resource://${context.packageName}/${R.raw.sfx_goal}")
            NotificationSoundOption.CALM ->
                Uri.parse("android.resource://${context.packageName}/${R.raw.sfx_transaction}")
            NotificationSoundOption.SYSTEM ->
                Settings.System.DEFAULT_NOTIFICATION_URI
        }
    }

    /**
     * Task / event due alert: notification with custom calm ringtone + full-screen
     * intent so a popup can appear even when the device is locked.
     */
    fun showReminder(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        soundOption: NotificationSoundOption = NotificationSoundOption.SOFT,
        type: String = "",
        itemId: Int = 0,
        showEtaPopup: Boolean = true
    ) {
        ensureChannels(context, soundOption)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (showEtaPopup) {
                putExtra(EXTRA_ETA_POPUP, true)
                putExtra(EXTRA_ETA_TITLE, title)
                putExtra(EXTRA_ETA_BODY, body)
                putExtra(EXTRA_ETA_TYPE, type)
                putExtra(EXTRA_ETA_ITEM_ID, itemId)
            }
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Separate full-screen pending intent (must be distinct request code)
        val fullScreenPending = PendingIntent.getActivity(
            context,
            notificationId + 50_000,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (showEtaPopup) CHANNEL_ETA else channelIdFor(soundOption)
        val sound = if (showEtaPopup) {
            EtaRingtonePlayer.ringtoneUri(context)
        } else {
            soundUri(context, soundOption)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_pixido)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .setSound(sound)
            .setVibrate(longArrayOf(0, 60, 120, 60))
            .apply {
                if (showEtaPopup) {
                    setFullScreenIntent(fullScreenPending, true)
                }
            }
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on API 33+
        }

        // Samsung Now Bar / Live Update ongoing card for task ETAs
        if (showEtaPopup && type == ReminderScheduler.TYPE_TASK && itemId > 0) {
            NowBarHelper.showTaskEta(
                context = context,
                taskId = itemId,
                title = title.removePrefix("Task due: ").trim().ifBlank { title },
                body = body,
                dueAtMillis = System.currentTimeMillis(),
                isDueNow = true
            )
        }

        // Best-effort launch popup when app process can start activities
        if (showEtaPopup) {
            try {
                context.startActivity(openIntent)
            } catch (_: Exception) {
                // Full-screen intent / notification will cover locked / restricted cases
            }
        }
    }

    fun smsNotificationId(smsHash: String): Int =
        SMS_NOTIFICATION_BASE + (smsHash.hashCode() and 0x7FFF)

    /**
     * System tray alert when a debit/credit SMS is detected while PixiDo is
     * in the background. Tapping opens the in-app assignment banner.
     */
    fun showSmsTransaction(
        context: Context,
        amountLabel: String,
        isExpense: Boolean,
        bankName: String,
        merchant: String,
        smsHash: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ensureChannels(context)
        }
        val kind = if (isExpense) "Deducted" else "Credited"
        val title = "$kind $amountLabel"
        val body = buildString {
            append(bankName)
            if (merchant.isNotBlank()) {
                append(" · ")
                append(merchant)
            }
            append(" · Tap to choose an account")
        }
        val notificationId = smsNotificationId(smsHash)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SMS_PROMPT, true)
        }
        val contentPending = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SMS)
            .setSmallIcon(R.drawable.ic_stat_pixido)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(contentPending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted on API 33+
        }
    }

    fun cancelSmsNotification(context: Context, smsHash: String) {
        try {
            NotificationManagerCompat.from(context).cancel(smsNotificationId(smsHash))
        } catch (_: Exception) {
            // ignore
        }
    }
}

fun NotificationSoundOption.displayName(): String = when (this) {
    NotificationSoundOption.SOFT -> "Task reminder"
    NotificationSoundOption.BRIGHT -> "Goal reminder"
    NotificationSoundOption.CALM -> "Transaction reminder"
    NotificationSoundOption.SYSTEM -> "System default"
}
