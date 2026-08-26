package com.example.sms

import com.example.data.AccountEntity

/**
 * Picks the default budget account for a detected bank SMS.
 *
 * Order:
 *  1. Account whose name matches the bank
 *  2. Last account the user assigned an SMS to
 *  3. Primary account
 *  4. First account
 */
object SmsAccountMatcher {

    fun defaultAccount(
        accounts: List<AccountEntity>,
        bankName: String,
        lastAccountId: Int? = null
    ): AccountEntity? {
        if (accounts.isEmpty()) return null
        matchByBank(accounts, bankName)?.let { return it }
        val lastId = lastAccountId?.takeIf { it > 0 }
        if (lastId != null) {
            accounts.firstOrNull { it.id == lastId }?.let { return it }
        }
        accounts.firstOrNull { it.isPrimary }?.let { return it }
        return accounts.firstOrNull()
    }

    fun matchByBank(accounts: List<AccountEntity>, bankName: String): AccountEntity? {
        if (bankName.isBlank() || bankName.equals("Bank", ignoreCase = true)) return null
        val needle = bankName.lowercase()
        accounts.firstOrNull { it.name.lowercase().contains(needle) }?.let { return it }
        val tokens = needle.split(' ').filter { it.length >= 3 && it != "bank" }
        for (token in tokens) {
            accounts.firstOrNull { it.name.lowercase().contains(token) }?.let { return it }
        }
        return null
    }
}
