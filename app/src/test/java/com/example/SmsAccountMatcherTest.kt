package com.example

import com.example.data.AccountEntity
import com.example.data.AccountType
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

    @Test
    fun creditCardSmsPicksCardNotBankWithSameBrand() {
        val accounts = listOf(
            account(1, "HDFC Bank", isPrimary = true),
            AccountEntity(
                id = 2,
                name = "HDFC Millennia",
                type = AccountType.CREDIT_CARD.name
            )
        )
        val picked = SmsAccountMatcher.defaultAccount(
            accounts = accounts,
            bankName = "HDFC Bank",
            lastAccountId = 1,
            preferCreditCard = true
        )
        assertEquals(2, picked?.id)
    }

    @Test
    fun bankSmsDoesNotFallBackToCreditCard() {
        val accounts = listOf(
            AccountEntity(
                id = 1,
                name = "HDFC Millennia",
                type = AccountType.CREDIT_CARD.name,
                isPrimary = true
            ),
            account(2, "HDFC Bank")
        )
        val picked = SmsAccountMatcher.defaultAccount(
            accounts = accounts,
            bankName = "HDFC Bank",
            lastAccountId = 1,
            preferCreditCard = false
        )
        assertEquals(2, picked?.id)
    }

    @Test
    fun last4OnCardWinsForCardSms() {
        val accounts = listOf(
            AccountEntity(
                id = 1,
                name = "HDFC Millennia 8899",
                type = AccountType.CREDIT_CARD.name
            ),
            AccountEntity(
                id = 2,
                name = "HDFC Regalia 4321",
                type = AccountType.CREDIT_CARD.name
            ),
            account(3, "HDFC Bank")
        )
        val picked = SmsAccountMatcher.defaultAccount(
            accounts = accounts,
            bankName = "HDFC Bank",
            accountLast4 = "4321",
            preferCreditCard = true
        )
        assertEquals(2, picked?.id)
    }

    @Test
    fun last4FieldWinsOverName() {
        val accounts = listOf(
            AccountEntity(
                id = 1,
                name = "HDFC Millennia",
                type = AccountType.CREDIT_CARD.name,
                lastFour = "4321"
            ),
            AccountEntity(
                id = 2,
                name = "HDFC Regalia",
                type = AccountType.CREDIT_CARD.name,
                lastFour = "8899"
            )
        )
        val picked = SmsAccountMatcher.defaultAccount(
            accounts = accounts,
            bankName = "HDFC Bank",
            accountLast4 = "4321",
            preferCreditCard = true
        )
        assertEquals(1, picked?.id)
    }
}
