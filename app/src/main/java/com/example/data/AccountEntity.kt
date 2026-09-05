package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AccountType {
    BANK,
    CASH,
    CREDIT_CARD,
    SAVINGS,
    WALLET
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String = AccountType.BANK.name,
    /**
     * For bank/cash/savings/wallet: available funds (asset).
     * For credit cards: amount currently owed / utilized (liability) — never a credit limit.
     * Credit limits live only in [creditLimit] and are excluded from overall budget / net worth.
     */
    val balance: Double = 0.0,
    /** Credit limit cap for credit cards only (0 = none). Never counted in net worth. */
    val creditLimit: Double = 0.0,
    /**
     * Credit utilized against the limit (credit cards).
     * Kept in sync with balance for CREDIT_CARD; for others tracks spend vs optional caps.
     */
    val monthlyUsage: Double = 0.0,
    val currencyCode: String = "USD",
    val colorHex: String = "#7C3AED",
    val isPrimary: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    /** VISA / MASTERCARD / RUPAY / OTHER. Blank = infer from [name]. */
    val cardNetwork: String = "",
    /** Last 4 digits printed on the card. */
    val lastFour: String = "",
    val expiryMonth: Int = 0,
    val expiryYear: Int = 0,
    val cardholderName: String = ""
) {
    val accountType: AccountType
        get() = runCatching { AccountType.valueOf(type) }.getOrDefault(AccountType.BANK)

    val isCreditCard: Boolean
        get() = accountType == AccountType.CREDIT_CARD

    /** Amount of credit currently used (credit cards). */
    val creditUtilized: Double
        get() = if (isCreditCard) {
            maxOf(balance, monthlyUsage).coerceAtLeast(0.0)
        } else {
            0.0
        }

    /** Remaining credit headroom (credit cards). */
    val creditAvailable: Double
        get() = if (isCreditCard && creditLimit > 0) {
            (creditLimit - creditUtilized).coerceAtLeast(0.0)
        } else {
            0.0
        }
}
