package com.example

import com.example.sms.SmsTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    }

    @Test
    fun parseSbiDebit() {
        val body =
            "Dear Customer, Rs.500.00 debited from your A/c XX1234 on 08Aug24 transfer to John. UPI Ref no 123456"
        val parsed = SmsTransactionParser.parse(body, "AX-SBIINB")
        assertNotNull(parsed)
        assertEquals(500.0, parsed!!.amount, 0.001)
        assertTrue(parsed.isExpense)
        assertEquals("SBI", parsed.bankName)
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
    }

    @Test
    fun ignoresNonTransactionSms() {
        val body = "Your OTP for login is 123456. Do not share with anyone."
        assertNull(SmsTransactionParser.parse(body, "VM-HDFCBK"))
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
}
