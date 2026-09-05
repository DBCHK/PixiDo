package com.example

import com.example.data.PendingSmsTransactionEntity
import com.example.sms.SmsDeduper
import com.example.sms.SmsTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsTransactionParserTest {

    @Test
    fun parseHdfcDebit() {
        val body =
            "HDFC Bank: Rs.1,500.00 debited from a/c **1234 on 08-08-24 to VPA merchant@upi. Avl bal Rs.10,000.00"
        val parsed = SmsTransactionParser.parse(body, "VM-HDFCBK")
        assertNotNull(parsed)
        assertEquals(1500.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isExpense)
        assertEquals("HDFC Bank", parsed.bankName)
        assertEquals("1234", parsed.accountLast4)
        assertEquals(SmsTransactionParser.CHANNEL_UPI, parsed.channel)
    }

    @Test
    fun parseSbiDebit() {
        val body =
            "Dear Customer, Rs.500.00 debited from your A/c XX1234 on 08Aug24 transfer to John. UPI Ref no 123456789012"
        val parsed = SmsTransactionParser.parse(body, "AX-SBIINB")
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isExpense)
        assertEquals("SBI", parsed.bankName)
        assertEquals("123456789012", parsed.refId)
    }

    @Test
    fun parseIciciDebitPrefersTxnNotBalance() {
        val body =
            "ICICI Bank Acct XX123 debited for Rs 250.00 on 08-Aug-24; Info: UPI/coffee. Avl Bal Rs 5,000.00"
        val parsed = SmsTransactionParser.parse(body, "VK-ICICIB")
        assertNotNull(parsed)
        assertEquals(250.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isExpense)
        assertEquals("ICICI Bank", parsed.bankName)
    }

    @Test
    fun parseAxisInrDebit() {
        val body =
            "INR 1,200.00 debited from A/c no. XX1234 on 08-08-2024 14:30:00. Info: UPI. Avl Bal:INR 8,000.00"
        val parsed = SmsTransactionParser.parse(body, "VM-AXISBK")
        assertNotNull(parsed)
        assertEquals(1200.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isExpense)
        assertEquals("Axis Bank", parsed.bankName)
    }

    @Test
    fun parseCredit() {
        val body = "Rs.5000.00 credited to a/c **1234 on 08-08-24. Avl bal Rs.15,000.00"
        val parsed = SmsTransactionParser.parse(body, "VM-HDFCBK")
        assertNotNull(parsed)
        assertEquals(5000.0, parsed!!.amount, 0.001)
        assertFalse(parsed.isExpense)
        assertEquals("HDFC Bank", parsed.bankName)
    }

    @Test
    fun parseRupeeSymbol() {
        val body = "₹799.00 spent on your SBI card ending 4321 at AMAZON. Available bal ₹12,000"
        val parsed = SmsTransactionParser.parse(body, "AD-SBICRD")
        assertNotNull(parsed)
        assertEquals(799.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isExpense)
        assertEquals("Shopping", parsed.category)
        assertEquals("4321", parsed.accountLast4)
    }

    @Test
    fun creditCardSpendIsExpenseNotCredit() {
        val body =
            "Rs.2,199.00 spent on your HDFC Bank Credit Card ending 8899 at SWIGGY. Avl limit Rs.40,000"
        val parsed = SmsTransactionParser.parse(body, "VM-HDFCBK")
        assertNotNull(parsed)
        assertTrue(parsed!!.isExpense)
        assertTrue(parsed.isCreditCard)
        assertEquals(2199.0, parsed.amount, 0.001)
        assertEquals("8899", parsed.accountLast4)
        assertEquals("Food & Drink", parsed.category)
    }

    @Test
    fun prefersSpendAmountOverLimitAndDue() {
        val body =
            "Spent Rs.1,299 on AXIS BANK Credit Card XX4321 at FLIPKART on 05-Sep-26. Avl limit Rs.75,000.00. Tot due Rs.8,500"
        val parsed = SmsTransactionParser.parse(body, "VM-AXISBK")
        assertNotNull(parsed)
        assertEquals(1299.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isExpense)
        assertTrue(parsed.isCreditCard)
        assertEquals("4321", parsed.accountLast4)
    }

    @Test
    fun thankYouForUsingCardIsExpenseOnCard() {
        val body =
            "Thank you for using your HDFC Bank Credit Card ending 1234 for Rs.500.00 at MERCHANT on 08-AUG-24. Avl limit Rs.40,000"
        val parsed = SmsTransactionParser.parse(body, "VM-HDFCBK")
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isExpense)
        assertTrue(parsed.isCreditCard)
        assertEquals("1234", parsed.accountLast4)
    }

    @Test
    fun cardBillPaymentIsIncomeOnCard() {
        val body =
            "Payment of Rs.5,000.00 received towards your HDFC Bank Credit Card XX1234. Total outstanding Rs.2,000"
        val parsed = SmsTransactionParser.parse(body, "VM-HDFCBK")
        assertNotNull(parsed)
        assertEquals(5000.0, parsed!!.amount, 0.001)
        assertFalse(parsed.isExpense)
        assertTrue(parsed.isCreditCard)
    }

    @Test
    fun bankUpiIsNotCreditCard() {
        val body =
            "HDFC Bank: Rs.1,500.00 debited from a/c **1234 on 08-08-24 to VPA merchant@upi. Avl bal Rs.10,000.00"
        val parsed = SmsTransactionParser.parse(body, "VM-HDFCBK")
        assertNotNull(parsed)
        assertFalse(parsed!!.isCreditCard)
        assertEquals(1500.0, parsed.amount, 0.001)
    }

    @Test
    fun ignoresCreditCardStatementWithoutSpend() {
        val body =
            "Dear Customer, your HDFC Bank Credit Card XX1234 statement is generated. Total amount due Rs.8,500. Min due Rs.500."
        assertNull(SmsTransactionParser.parse(body, "VM-HDFCBK"))
    }

    @Test
    fun last4MatchesUppercaseXx() {
        val body = "INR 250.00 spent on ICICI Bank Credit Card XX7788 at STARBUCKS. Avl limit INR 12,000"
        val parsed = SmsTransactionParser.parse(body, "VK-ICICIB")
        assertNotNull(parsed)
        assertEquals("7788", parsed!!.accountLast4)
        assertTrue(parsed.isCreditCard)
        assertEquals(250.0, parsed.amount, 0.001)
    }

    @Test
    fun ignoresNonTransactionSms() {
        val body = "Your OTP for login is 123456. Do not share with anyone."
        assertNull(SmsTransactionParser.parse(body, "VM-HDFCBK"))
    }

    @Test
    fun ignoresLoanDueSms() {
        val body = "Dear Customer, your EMI of Rs. 15,000 for Loan XX123 is due on 10-Aug-24."
        assertNull(SmsTransactionParser.parse(body, "VM-HDFCBK"))
    }

    @Test
    fun ignoresFailedUpi() {
        val body = "UPI payment of Rs.500 to merchant@upi failed. Ref 123456789012"
        assertNull(SmsTransactionParser.parse(body, "VM-HDFCBK"))
    }

    @Test
    fun keepsEmiAutoDebit() {
        val body = "Rs.12,500.00 debited from A/c XX7788 towards EMI. Avl bal Rs.8,000"
        val parsed = SmsTransactionParser.parse(body, "VM-HDFCBK")
        assertNotNull(parsed)
        assertEquals(12500.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isExpense)
    }

    @Test
    fun looksLikeTransactionRequiresMoneyHint() {
        assertFalse(SmsTransactionParser.looksLikeTransactionSms("Hello friend", "VM-HDFCBK"))
        assertTrue(
            SmsTransactionParser.looksLikeTransactionSms(
                "Rs.100 debited from a/c",
                "VM-HDFCBK"
            )
        )
    }

    @Test
    fun contentHashIgnoresTimestampAndDltPrefix() {
        val body = "Rs.100.00 debited from a/c **1234 to VPA tea@upi"
        val a = SmsTransactionParser.contentHash(body, "VM-HDFCBK")
        val b = SmsTransactionParser.contentHash("  $body  ", "AD-HDFCBK")
        val old = SmsTransactionParser.smsHash(body, "VM-HDFCBK", 1L)
        val later = SmsTransactionParser.smsHash(body, "VM-HDFCBK", 99L)
        assertEquals(a, b)
        assertEquals(old, later)
        assertEquals(a, old)
    }

    @Test
    fun bankAndWalletSmsAreTreatedAsOnePayment() {
        val bankBody =
            "HDFC Bank: Rs.350.00 debited from a/c **1234 to VPA swiggy@upi. UPI Ref no 987654321012. Avl bal Rs.2,000"
        val walletBody = "Paid Rs.350.00 to Swiggy using PhonePe. UPI Ref 987654321012"
        val bankParsed = SmsTransactionParser.parse(bankBody, "VM-HDFCBK")!!
        val walletParsed = SmsTransactionParser.parse(walletBody, "JD-PHONEPE")!!
        assertTrue(walletParsed.isWallet)
        assertFalse(bankParsed.isWallet)

        val existing = listOf(
            PendingSmsTransactionEntity(
                id = 1,
                amount = bankParsed.amount,
                bankName = bankParsed.bankName,
                isExpense = true,
                merchantOrInfo = bankParsed.merchantOrInfo,
                smsBody = bankBody,
                smsSender = "VM-HDFCBK",
                smsHash = SmsTransactionParser.contentHash(bankBody, "VM-HDFCBK"),
                receivedAt = 1_000_000L,
                status = PendingSmsTransactionEntity.STATUS_PENDING
            )
        )
        assertTrue(
            SmsDeduper.matchesExisting(
                parsed = walletParsed,
                sender = "JD-PHONEPE",
                timestamp = 1_000_000L + 30_000,
                body = walletBody,
                existing = existing
            )
        )
        val weaker = SmsDeduper.weakerDuplicate(
            existing.first(),
            existing.first().copy(
                id = 2,
                bankName = "PhonePe",
                smsBody = walletBody,
                smsSender = "JD-PHONEPE",
                smsHash = SmsTransactionParser.contentHash(walletBody, "JD-PHONEPE")
            )
        )
        assertNotNull(weaker)
        assertEquals("PhonePe", weaker!!.bankName)
    }

    @Test
    fun twoSameAmountBankPaymentsAreNotCollapsedWithoutOverlap() {
        val first =
            "Rs.500.00 debited from a/c **1234 to VPA coffee1@upi. UPI Ref no 111111111111"
        val second =
            "Rs.500.00 debited from a/c **1234 to VPA coffee2@upi. UPI Ref no 222222222222"
        val parsed = SmsTransactionParser.parse(second, "VM-HDFCBK")!!
        val existing = listOf(
            PendingSmsTransactionEntity(
                id = 1,
                amount = 500.0,
                bankName = "HDFC Bank",
                isExpense = true,
                merchantOrInfo = "coffee1@upi",
                smsBody = first,
                smsSender = "VM-HDFCBK",
                smsHash = SmsTransactionParser.contentHash(first, "VM-HDFCBK"),
                receivedAt = 1_000_000L
            )
        )
        assertFalse(
            SmsDeduper.matchesExisting(
                parsed = parsed,
                sender = "VM-HDFCBK",
                timestamp = 1_000_000L + 60_000,
                body = second,
                existing = existing
            )
        )
        assertNotEquals(
            SmsTransactionParser.contentHash(first, "VM-HDFCBK"),
            SmsTransactionParser.contentHash(second, "VM-HDFCBK")
        )
    }
}
