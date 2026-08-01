package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kind of money movement.
 * - EXPENSE / INCOME: normal cashflow (expense counts toward monthly budget)
 * - LENT: you lent money out (not a budget expense)
 * - BORROW: you borrowed money (not income)
 * - TRANSFER: move between own accounts (e.g. bank → credit card repayment)
 */
enum class TransactionType {
    EXPENSE,
    INCOME,
    LENT,
    BORROW,
    TRANSFER;

    val displayName: String
        get() = when (this) {
            EXPENSE -> "Expense"
            INCOME -> "Income"
            LENT -> "Lent"
            BORROW -> "Borrow"
            TRANSFER -> "Transfer"
        }

    /** Leaves an asset account (cash out). */
    val decreasesAsset: Boolean
        get() = this == EXPENSE || this == LENT || this == TRANSFER

    /** Counts against the monthly spending budget. */
    val countsTowardBudget: Boolean
        get() = this == EXPENSE

    companion object {
        fun fromStorage(raw: String?, isExpense: Boolean): TransactionType {
            if (!raw.isNullOrBlank()) {
                runCatching { valueOf(raw) }.getOrNull()?.let { return it }
            }
            return if (isExpense) EXPENSE else INCOME
        }
    }
}

@Entity(tableName = "budget_items")
data class BudgetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    /** Legacy flag: true for EXPENSE, LENT, TRANSFER. Prefer [transactionType]. */
    val isExpense: Boolean,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    /** Source account (or only account for expense/income). */
    val accountId: Int? = null,
    /** EXPENSE | INCOME | LENT | BORROW | TRANSFER */
    val transactionType: String = TransactionType.EXPENSE.name,
    /** Destination account for TRANSFER (e.g. credit card being paid). */
    val relatedAccountId: Int? = null
) {
    val type: TransactionType
        get() = TransactionType.fromStorage(transactionType, isExpense)
}
