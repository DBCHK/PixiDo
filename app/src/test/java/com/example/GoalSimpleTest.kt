package com.example

import com.example.data.BudgetItemEntity
import com.example.data.Currencies
import com.example.data.GoalEntity
import com.example.data.TransactionType
import com.example.ui.screens.formatListAmount
import com.example.ui.screens.txnInitials
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalSimpleTest {

    @Test
    fun simpleFlagMarksChecklistGoal() {
        val goal = GoalEntity(
            title = "Drink water",
            category = "Health",
            targetAmount = 1.0,
            unit = "done",
            isSimple = true
        )
        assertTrue(goal.isSimpleTask)
        assertTrue(goal.isDailyHabit)
        assertFalse(goal.isMilestone)
        assertFalse(goal.isCompleted)
    }

    @Test
    fun doneUnitCountsAsSimpleWithoutFlag() {
        val goal = GoalEntity(
            title = "Walk the dog",
            category = "Personal",
            targetAmount = 1.0,
            unit = "done"
        )
        assertTrue(goal.isSimpleTask)
        assertTrue(goal.isDailyHabit)
    }

    @Test
    fun moneyGoalIsNotSimple() {
        val goal = GoalEntity(
            title = "Emergency fund",
            category = "Savings",
            targetAmount = 2000.0,
            unit = "$"
        )
        assertFalse(goal.isSimpleTask)
        assertFalse(goal.isDailyHabit)
        assertTrue(goal.isMilestone)
    }

    @Test
    fun habitFlagIsDailyHabit() {
        val goal = GoalEntity(
            title = "Meditate",
            category = "Mindset",
            targetAmount = 1.0,
            unit = "habit",
            isHabit = true
        )
        assertTrue(goal.isDailyHabit)
        assertTrue(goal.isMilestone.not())
    }
}

class CurrenciesSplitTest {

    @Test
    fun splitsHeroAmountWithGrouping() {
        val split = Currencies.split(7854.43, "USD")
        assertEquals("$", split.symbol)
        assertEquals("7,854", split.whole)
        assertEquals("43", split.cents)
        assertFalse(split.negative)
    }

    @Test
    fun roundsUpToNextWhole() {
        val split = Currencies.split(10.996, "USD")
        assertEquals("11", split.whole)
        assertEquals("00", split.cents)
    }

    @Test
    fun negativeKeepsSignFlag() {
        val split = Currencies.split(-12.5, "INR")
        assertTrue(split.negative)
        assertEquals("₹", split.symbol)
        assertEquals("12", split.whole)
        assertEquals("50", split.cents)
    }
}

class WalletTxnFormatTest {

    @Test
    fun wholeAmountsDropCentsAndPutSymbolLast() {
        assertEquals("60 $", formatListAmount(60.0, "USD"))
    }

    @Test
    fun fractionalKeepsCents() {
        assertEquals("12.50 $", formatListAmount(12.5, "USD"))
    }

    @Test
    fun initialsFromTwoWords() {
        val item = BudgetItemEntity(
            title = "Apple Store",
            amount = 60.0,
            isExpense = true,
            category = "Shopping",
            transactionType = TransactionType.EXPENSE.name
        )
        assertEquals("AS", txnInitials(item))
    }
}
