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
    val balance: Double = 0.0,
    /** Credit limit / spending cap for this account (0 = no limit). */
    val creditLimit: Double = 0.0,
    /** Monthly usage / spent amount tracked against limit. */
    val monthlyUsage: Double = 0.0,
    val currencyCode: String = "USD",
    val colorHex: String = "#7C3AED",
    val isPrimary: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
