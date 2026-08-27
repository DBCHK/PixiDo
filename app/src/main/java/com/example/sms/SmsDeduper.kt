package com.example.sms

import com.example.data.PendingSmsTransactionEntity
import kotlin.math.abs

/**
 * Stops the same real-world payment from becoming two PixiDo prompts.
 *
 * Typical doubles:
 *  - SMS_RECEIVED + inbox rescan of the same message (timestamps differ)
 *  - Bank debit SMS + PhonePe / GPay / Paytm "you paid" SMS
 */
object SmsDeduper {

    /** Bank + wallet alerts for one UPI usually arrive within a few minutes. */
    const val PAIR_WINDOW_MS = 20L * 60 * 1000

    fun matchesExisting(
        parsed: SmsTransactionParser.ParsedTransaction,
        sender: String,
        timestamp: Long,
        body: String,
        existing: List<PendingSmsTransactionEntity>
    ): Boolean {
        val hash = SmsTransactionParser.contentHash(body, sender)
        if (existing.any { it.smsHash == hash }) return true
        if (existing.any { sameNormalizedBody(body, it.smsBody) && sameSenderFamily(sender, it.smsSender) }) {
            return true
        }
        if (parsed.refId.isNotBlank()) {
            for (row in existing) {
                val otherRef = SmsTransactionParser.extractRefId(row.smsBody)
                if (otherRef.isNotBlank() && otherRef == parsed.refId) return true
            }
        }
        for (row in existing) {
            if (!sameMoney(parsed.amount, row.amount, parsed.isExpense, row.isExpense)) continue
            if (abs(row.receivedAt - timestamp) > PAIR_WINDOW_MS) continue
            if (isPairedAlert(parsed, sender, row)) return true
        }
        return false
    }

    fun isPairedAlert(
        parsed: SmsTransactionParser.ParsedTransaction,
        sender: String,
        row: PendingSmsTransactionEntity
    ): Boolean {
        val other = SmsTransactionParser.parse(row.smsBody, row.smsSender)
        val otherWallet = other?.isWallet ?: SmsTransactionParser.isWalletBank(row.bankName)
        if (parsed.refId.isNotBlank() && other?.refId == parsed.refId) return true
        if (parsed.isWallet != otherWallet) return true
        val otherMerchant = other?.merchantOrInfo?.ifBlank { row.merchantOrInfo } ?: row.merchantOrInfo
        if (merchantOverlap(parsed.merchantOrInfo, otherMerchant)) return true
        val otherLast4 = other?.accountLast4.orEmpty()
        if (parsed.accountLast4.isNotBlank() && parsed.accountLast4 == otherLast4) {
            return sameSenderFamily(sender, row.smsSender).not()
        }
        return false
    }

    /**
     * When two pending rows describe one payment, keep the bank SMS
     * (better account match) and drop the wallet copy.
     */
    fun weakerDuplicate(
        a: PendingSmsTransactionEntity,
        b: PendingSmsTransactionEntity
    ): PendingSmsTransactionEntity? {
        if (!sameMoney(a.amount, b.amount, a.isExpense, b.isExpense)) return null
        if (abs(a.receivedAt - b.receivedAt) > PAIR_WINDOW_MS) return null
        val parsedA = SmsTransactionParser.parse(a.smsBody, a.smsSender)
        val parsedB = SmsTransactionParser.parse(b.smsBody, b.smsSender)
        val pair = when {
            parsedA != null -> isPairedAlert(parsedA, a.smsSender, b)
            parsedB != null -> isPairedAlert(parsedB, b.smsSender, a)
            else -> false
        } || (
            SmsTransactionParser.contentHash(a.smsBody, a.smsSender) ==
                SmsTransactionParser.contentHash(b.smsBody, b.smsSender)
            )
        if (!pair) return null
        val aWallet = parsedA?.isWallet ?: SmsTransactionParser.isWalletBank(a.bankName)
        val bWallet = parsedB?.isWallet ?: SmsTransactionParser.isWalletBank(b.bankName)
        return when {
            aWallet && !bWallet -> a
            bWallet && !aWallet -> b
            a.receivedAt >= b.receivedAt -> a
            else -> b
        }
    }

    fun sameMoney(amountA: Double, amountB: Double, expenseA: Boolean, expenseB: Boolean): Boolean {
        if (expenseA != expenseB) return false
        return abs(amountA - amountB) < 0.01
    }

    fun merchantOverlap(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        val ta = SmsTransactionParser.merchantTokens(a)
        val tb = SmsTransactionParser.merchantTokens(b)
        if (ta.isEmpty() || tb.isEmpty()) return false
        return ta.intersect(tb).isNotEmpty()
    }

    private fun sameNormalizedBody(a: String, b: String): Boolean {
        if (a.isBlank() || b.isBlank()) return false
        return SmsTransactionParser.normalizeBody(a) == SmsTransactionParser.normalizeBody(b)
    }

    private fun sameSenderFamily(a: String, b: String): Boolean {
        val na = SmsTransactionParser.normalizeSender(a)
        val nb = SmsTransactionParser.normalizeSender(b)
        if (na.isBlank() || nb.isBlank()) return false
        return na == nb || na.contains(nb) || nb.contains(na)
    }
}
