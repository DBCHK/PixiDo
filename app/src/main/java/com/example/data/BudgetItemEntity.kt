package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kind of money movement.
 * - EXPENSE / INCOME: normal cashflow (expense counts toward monthly budget)
 * - LENT: you lent money out (asset claim; leaves your account; not a budget expense)
 * - BORROW: you borrowed money (liability; enters your account; not income)
 */
enum class TransactionType {
    EXPENSE,
    INCOME,
    LENT,
    BORROW;

    val displayName: String
        get() = when (this) {
            EXPENSE -> "Expense"
            INCOME -> "Income"
            LENT -> "Lent"
            BORROW -> "Borrow"
        }

    /** Leaves the account (reduces cash / increases credit debt). */
    val decreasesAsset: Boolean
        get() = this == EXPENSE || this == LENT

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
    /** Legacy flag: true for EXPENSE and LENT. Prefer [transactionType]. */
    val isExpense: Boolean,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val accountId: Int? = null,
    /** EXPENSE | INCOME | LENT | BORROW */
    val transactionType: String = TransactionType.EXPENSE.name
) {
    val type: TransactionType
        get() = TransactionType.fromStorage(transactionType, isExpense)
}
