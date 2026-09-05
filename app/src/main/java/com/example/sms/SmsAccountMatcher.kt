package com.example.sms

import com.example.data.AccountEntity

/**
 * Picks the default budget account for a detected bank / card SMS.
 *
 * [preferCreditCard] keeps credit-card SMS on cards and bank/UPI SMS on
 * bank/cash/wallet accounts so a Swiggy card spend does not drain a bank total.
 *
 * Order inside the preferred pool:
 *  1. Last-4 digits in the account name
 *  2. Account whose name matches the bank
 *  3. Last account the user assigned an SMS to (if it is in the pool)
 *  4. Primary account (if it is in the pool)
 *  5. First account in the pool
 */
object SmsAccountMatcher {

    fun defaultAccount(
        accounts: List<AccountEntity>,
        bankName: String,
        lastAccountId: Int? = null,
        accountLast4: String = "",
        preferCreditCard: Boolean? = null
    ): AccountEntity? {
        if (accounts.isEmpty()) return null
        val pool = when (preferCreditCard) {
            true -> accounts.filter { it.isCreditCard }.ifEmpty { accounts }
            false -> accounts.filter { !it.isCreditCard }.ifEmpty { accounts }
            null -> accounts
        }
        return pickFrom(pool, bankName, lastAccountId, accountLast4)
    }

    private fun pickFrom(
        accounts: List<AccountEntity>,
        bankName: String,
        lastAccountId: Int?,
        accountLast4: String
    ): AccountEntity? {
        matchByLast4(accounts, accountLast4)?.let { return it }
        matchByBank(accounts, bankName)?.let { return it }
        val lastId = lastAccountId?.takeIf { it > 0 }
        if (lastId != null) {
            accounts.firstOrNull { it.id == lastId }?.let { return it }
        }
        accounts.firstOrNull { it.isPrimary }?.let { return it }
        return accounts.firstOrNull()
    }

    fun matchByLast4(accounts: List<AccountEntity>, last4: String): AccountEntity? {
        if (last4.length != 4) return null
        val hits = accounts.filter { account ->
            account.lastFour.filter { it.isDigit() }.takeLast(4) == last4 ||
                account.name.contains(last4) ||
                account.notes.contains(last4)
        }
        return hits.singleOrNull() ?: hits.firstOrNull()
    }

    fun matchByBank(accounts: List<AccountEntity>, bankName: String): AccountEntity? {
        if (bankName.isBlank() || bankName.equals("Bank", ignoreCase = true)) return null
        val needle = bankName.lowercase()
        accounts.firstOrNull { it.name.lowercase().contains(needle) }?.let { return it }
        val tokens = needle.split(' ')
            .map { it.trim() }
            .filter { it.length >= 3 && it != "bank" && it != "card" }
        for (token in tokens) {
            accounts.firstOrNull { it.name.lowercase().contains(token) }?.let { return it }
        }
        return null
    }
}
