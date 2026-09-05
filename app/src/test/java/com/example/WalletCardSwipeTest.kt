package com.example

import com.example.data.BudgetItemEntity
import com.example.data.TransactionType
import com.example.ui.components.WalletSwipeDir
import com.example.ui.components.walletRubberX
import com.example.ui.components.walletSwipeDecision
import com.example.ui.components.wrapWalletCardIndex
import com.example.ui.screens.txnAccountLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletCardSwipeTest {

    @Test
    fun wrapsForwardAndBack() {
        assertEquals(0, wrapWalletCardIndex(3, 3))
        assertEquals(2, wrapWalletCardIndex(-1, 3))
        assertEquals(1, wrapWalletCardIndex(4, 3))
        assertEquals(0, wrapWalletCardIndex(0, 3))
    }

    @Test
    fun emptyDeckIsZero() {
        assertEquals(0, wrapWalletCardIndex(2, 0))
    }

    @Test
    fun swipeLeftSelectsNextAndRightSelectsPrevious() {
        val max = 200f
        assertEquals(WalletSwipeDir.Next, walletSwipeDecision(-80f, 0f, max, 3))
        assertEquals(WalletSwipeDir.Previous, walletSwipeDecision(80f, 0f, max, 3))
        assertEquals(WalletSwipeDir.Next, walletSwipeDecision(-10f, -900f, max, 3))
        assertEquals(WalletSwipeDir.Previous, walletSwipeDecision(10f, 900f, max, 3))
        assertNull(walletSwipeDecision(-10f, 0f, max, 3))
        assertNull(walletSwipeDecision(-80f, 0f, max, 1))
    }

    @Test
    fun rubberBandsPastLimit() {
        assertEquals(-40f, walletRubberX(-40f, 100f), 0.01f)
        val extra = walletRubberX(150f, 100f)
        assertTrue(extra > 100f && extra < 150f)
    }

    @Test
    fun txnLineShowsFromAndTo() {
        val transfer = BudgetItemEntity(
            id = 1,
            title = "Move",
            amount = 10.0,
            isExpense = true,
            category = "Transfer",
            transactionType = TransactionType.TRANSFER.name
        )
        assertEquals("Everyday → Visa", txnAccountLine(transfer, "Everyday", "Visa"))
        val spend = transfer.copy(transactionType = TransactionType.EXPENSE.name)
        assertEquals("Everyday", txnAccountLine(spend, "Everyday", null))
        val income = transfer.copy(transactionType = TransactionType.INCOME.name)
        assertEquals("wallet", txnAccountLine(income, null, null))
    }
}
