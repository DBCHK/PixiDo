package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bank SMS detected offline — shown once when the user opens PixiDo so they can
 * accept (add to Budget) or skip.
 */
@Entity(
    tableName = "pending_sms_transactions",
    indices = [Index(value = ["smsHash"], unique = true)]
)
data class PendingSmsTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val bankName: String,
    /** true = debit / expense, false = credit / income */
    val isExpense: Boolean,
    val merchantOrInfo: String = "",
    val smsBody: String = "",
    val smsSender: String = "",
    /** Dedup key — body + sender (+ optional timestamp). */
    val smsHash: String,
    val receivedAt: Long = System.currentTimeMillis(),
    /** PENDING | ACCEPTED | DISMISSED */
    val status: String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_ACCEPTED = "ACCEPTED"
        const val STATUS_DISMISSED = "DISMISSED"
    }
}
