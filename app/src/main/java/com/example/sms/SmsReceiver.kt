package com.example.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.AuraDatabase
import com.example.data.PendingSmsTransactionEntity
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

                // Multi-part SMS: merge by originating address
                val bySender = messages.groupBy { it.displayOriginatingAddress.orEmpty() }
                val dao = AuraDatabase.getDatabase(appContext).auraDao()

                bySender.forEach { (sender, parts) ->
                    val body = parts.joinToString(separator = "") { it.displayMessageBody.orEmpty() }
                    val timestamp = parts.minOfOrNull { it.timestampMillis } ?: System.currentTimeMillis()
                    val parsed = SmsTransactionParser.parse(body, sender) ?: return@forEach
                    val hash = SmsTransactionParser.smsHash(body, sender, timestamp)

                    // Unique index will ignore duplicates; still check status rows
                    val existing = dao.getPendingSmsByHash(hash)
                    if (existing != null) return@forEach

                    dao.insertPendingSmsTransaction(
                        PendingSmsTransactionEntity(
                            amount = parsed.amount,
                            bankName = parsed.bankName,
                            isExpense = parsed.isExpense,
                            merchantOrInfo = parsed.merchantOrInfo,
                            smsBody = body.take(500),
                            smsSender = sender,
                            smsHash = hash,
                            receivedAt = timestamp,
                            status = PendingSmsTransactionEntity.STATUS_PENDING
                        )
                    )
                    Log.d(TAG, "Queued SMS txn: ${parsed.bankName} ${parsed.amount} expense=${parsed.isExpense}")
                }
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
