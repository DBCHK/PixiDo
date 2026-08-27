package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for incoming SMS. When a message looks like an Indian bank / UPI
 * transaction, stores it as a pending Budget prompt for the next app open.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = UserPreferencesRepository(appContext)
                if (!prefs.currentProfile().smsImportEnabled) return@launch

                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                if (messages.isNullOrEmpty()) return@launch

                val bySender = messages.groupBy { it.displayOriginatingAddress.orEmpty() }
                bySender.forEach { (sender, parts) ->
                    val body = parts.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
                    val timestamp = parts.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
                    SmsImportStore.ingest(
                        context = appContext,
                        body = body,
                        sender = sender,
                        timestamp = timestamp,
                        notifyIfBackground = true
                    )
                }
                SmsImportStore.collapsePending(appContext)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to process SMS", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PixiDoSms"
    }
}
