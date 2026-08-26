package com.example

import com.example.data.AccountEntity
import com.example.sms.SmsAccountMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsAccountMatcherTest {

    private fun account(
        id: Int,
        name: String,
        isPrimary: Boolean = false
    ) = AccountEntity(
        id = id,
        name = name,
        type = "BANK",
        isPrimary = isPrimary
    )

    @Test
    fun prefersBankNameMatchOverPrimary() {
        val accounts = listOf(
            account(1, "Cash", isPrimary = true),
            account(2, "HDFC Bank")
        )
        val picked = SmsAccountMatcher.defaultAccount(accounts, "HDFC Bank", lastAccountId = 1)
        assertEquals(2, picked?.id)
    }

    @Test
    fun matchesPartialBankToken() {
        val accounts = listOf(account(1, "HDFC"), account(2, "SBI"))
        val picked = SmsAccountMatcher.defaultAccount(accounts, "HDFC Bank")
        assertEquals(1, picked?.id)
    }

    @Test
    fun fallsBackToLastUsedWhenNoBankMatch() {
        val accounts = listOf(
            account(1, "Cash", isPrimary = true),
            account(2, "Wallet")
        )
        val picked = SmsAccountMatcher.defaultAccount(accounts, "Unknown Bank", lastAccountId = 2)
        assertEquals(2, picked?.id)
    }

    @Test
    fun fallsBackToPrimaryThenFirst() {
        val accounts = listOf(
            account(1, "Wallet"),
            account(2, "Cash", isPrimary = true)
        )
        val picked = SmsAccountMatcher.defaultAccount(accounts, "Bank")
        assertEquals(2, picked?.id)
    }

    @Test
    fun emptyAccountsReturnsNull() {
        assertNull(SmsAccountMatcher.defaultAccount(emptyList(), "HDFC Bank"))
    }
}
