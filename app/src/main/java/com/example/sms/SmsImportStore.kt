package com.example.sms

import android.content.Context
import android.util.Log
import com.example.data.AuraDatabase
import com.example.data.Currencies
import com.example.data.PendingSmsTransactionEntity
import com.example.data.UserPreferencesRepository
import com.example.notify.NotificationHelper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single ingest path for bank SMS — used by the live receiver and the inbox
 * scanner so the same payment cannot be queued twice.
 */
object SmsImportStore {

    private const val TAG = "PixiDoSms"
    private const val RECENT_MS = 6L * 60 * 60 * 1000
    private val mutex = Mutex()

    suspend fun ingest(
        context: Context,
        body: String,
        sender: String,
        timestamp: Long,
        notifyIfBackground: Boolean
    ): Boolean = mutex.withLock {
        val appContext = context.applicationContext
        val parsed = SmsTransactionParser.parse(body, sender) ?: return false
        val dao = AuraDatabase.getDatabase(appContext).auraDao()
        val hash = SmsTransactionParser.contentHash(body, sender)
        if (dao.getPendingSmsByHash(hash) != null) return false

        val recent = dao.getRecentSmsTransactions(timestamp - RECENT_MS)
        if (SmsDeduper.matchesExisting(parsed, sender, timestamp, body, recent)) {
            Log.d(TAG, "Skip duplicate SMS txn ${parsed.bankName} ${parsed.amount}")
            return false
        }

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

        if (notifyIfBackground && !AppForegroundState.isResumed) {
            val prefs = UserPreferencesRepository(appContext)
            val currency = prefs.currentProfile().currencyCode.ifBlank { "INR" }
            NotificationHelper.showSmsTransaction(
                context = appContext,
                amountLabel = Currencies.format(parsed.amount, currency),
                isExpense = parsed.isExpense,
                bankName = parsed.bankName,
                merchant = parsed.merchantOrInfo,
                smsHash = hash
            )
        }
        true
    }

    /** Drop leftover pairs already sitting in the pending queue. */
    suspend fun collapsePending(context: Context): Int = mutex.withLock {
        val dao = AuraDatabase.getDatabase(context.applicationContext).auraDao()
        val pending = dao.getPendingSmsTransactionsOnce()
        if (pending.size < 2) return 0
        val dropIds = LinkedHashSet<Int>()
        for (i in pending.indices) {
            if (pending[i].id in dropIds) continue
            for (j in i + 1 until pending.size) {
                if (pending[j].id in dropIds) continue
                val weaker = SmsDeduper.weakerDuplicate(pending[i], pending[j]) ?: continue
                dropIds += weaker.id
            }
        }
        dropIds.forEach { id ->
            dao.setPendingSmsStatus(id, PendingSmsTransactionEntity.STATUS_DISMISSED)
        }
        if (dropIds.isNotEmpty()) {
            Log.d(TAG, "Collapsed ${dropIds.size} duplicate pending SMS txn(s)")
        }
        dropIds.size
    }

    suspend fun dismissSemanticTwins(
        context: Context,
        kept: PendingSmsTransactionEntity
    ) = mutex.withLock {
        val dao = AuraDatabase.getDatabase(context.applicationContext).auraDao()
        val pending = dao.getPendingSmsTransactionsOnce()
        pending.filter { it.id != kept.id }.forEach { other ->
            val weaker = SmsDeduper.weakerDuplicate(kept, other)
            val parsedOther = SmsTransactionParser.parse(other.smsBody, other.smsSender)
            val twin = weaker != null || (
                parsedOther != null && SmsDeduper.matchesExisting(
                    parsed = parsedOther,
                    sender = other.smsSender,
                    timestamp = other.receivedAt,
                    body = other.smsBody,
                    existing = listOf(kept)
                )
            )
            if (twin) {
                dao.setPendingSmsStatus(other.id, PendingSmsTransactionEntity.STATUS_DISMISSED)
                NotificationHelper.cancelSmsNotification(context.applicationContext, other.smsHash)
            }
        }
    }
}
