package com.example

import com.example.data.BudgetItemEntity
import com.example.data.ChartMoneyKind
import com.example.data.SpendRange
import com.example.data.SpendSeries
import com.example.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class SpendSeriesTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun expense(id: Int, amount: Double, timestamp: Long, title: String = "Spend") =
        BudgetItemEntity(
            id = id,
            title = title,
            amount = amount,
            isExpense = true,
            category = "Food",
            timestamp = timestamp,
            transactionType = TransactionType.EXPENSE.name
        )

    private fun income(id: Int, amount: Double, timestamp: Long) =
        BudgetItemEntity(
            id = id,
            title = "Pay",
            amount = amount,
            isExpense = false,
            category = "Salary",
            timestamp = timestamp,
            transactionType = TransactionType.INCOME.name
        )

    @Test
    fun dayBucketsAre24HoursAndIgnoreIncome() {
        val now = at(2026, Calendar.AUGUST, 15, 18, 0)
        val items = listOf(
            expense(1, 40.0, at(2026, Calendar.AUGUST, 15, 9, 15)),
            expense(2, 12.5, at(2026, Calendar.AUGUST, 15, 9, 40)),
            income(3, 2000.0, at(2026, Calendar.AUGUST, 15, 10, 0)),
            expense(4, 8.0, at(2026, Calendar.AUGUST, 14, 9, 0))
        )
        val model = SpendSeries.build(items, SpendRange.DAY, offset = 0, now = now)
        assertEquals(24, model.buckets.size)
        assertEquals(52.5, model.total, 0.001)
        assertEquals(52.5, model.buckets[9].amount, 0.001)
        assertEquals(8.0, model.previousTotal, 0.001)
        assertTrue(model.hasSpend)
    }

    @Test
    fun weekUsesLast7DaysAndShiftsWithOffset() {
        val now = at(2026, Calendar.AUGUST, 15, 12, 0)
        val items = listOf(
            expense(1, 10.0, at(2026, Calendar.AUGUST, 15, 8, 0)),
            expense(2, 20.0, at(2026, Calendar.AUGUST, 14, 8, 0)),
            expense(3, 30.0, at(2026, Calendar.AUGUST, 9, 8, 0)),
            expense(4, 40.0, at(2026, Calendar.AUGUST, 8, 8, 0))
        )
        val thisWeek = SpendSeries.build(items, SpendRange.WEEK, offset = 0, now = now)
        assertEquals(7, thisWeek.buckets.size)
        assertEquals(60.0, thisWeek.total, 0.001)

        val lastWeek = SpendSeries.build(items, SpendRange.WEEK, offset = -1, now = now)
        assertEquals(40.0, lastWeek.total, 0.001)
        assertEquals(thisWeek.windowStart, lastWeek.windowEnd)
    }

    @Test
    fun monthCountsCalendarDaysAndIgnoresOtherMonths() {
        val now = at(2026, Calendar.AUGUST, 20, 12, 0)
        val items = listOf(
            expense(1, 100.0, at(2026, Calendar.AUGUST, 1, 10, 0)),
            expense(2, 50.0, at(2026, Calendar.AUGUST, 20, 10, 0)),
            expense(3, 9.0, at(2026, Calendar.JULY, 31, 23, 0)),
            expense(4, 7.0, at(2026, Calendar.SEPTEMBER, 1, 0, 1))
        )
        val model = SpendSeries.build(items, SpendRange.MONTH, offset = 0, now = now)
        assertEquals(31, model.buckets.size)
        assertEquals(150.0, model.total, 0.001)
        assertEquals(100.0, model.buckets.first().amount, 0.001)
        assertEquals(50.0, model.buckets[19].amount, 0.001)
        assertFalse(model.buckets.any { it.amount == 9.0 || it.amount == 7.0 })
    }

    @Test
    fun incomeKindIgnoresExpenses() {
        val now = at(2026, Calendar.AUGUST, 15, 12, 0)
        val items = listOf(
            expense(1, 40.0, at(2026, Calendar.AUGUST, 15, 9, 0)),
            BudgetItemEntity(
                id = 2,
                title = "Pay",
                amount = 200.0,
                isExpense = false,
                category = "Salary",
                timestamp = at(2026, Calendar.AUGUST, 15, 10, 0),
                transactionType = TransactionType.INCOME.name
            )
        )
        val model = SpendSeries.build(items, SpendRange.DAY, now = now, kind = ChartMoneyKind.INCOME)
        assertEquals(200.0, model.total, 0.001)
        assertTrue(model.buckets.any { it.income == 200.0 })
        assertTrue(model.buckets.any { it.spend == 40.0 })
    }

    @Test
    fun emptyItemsStayAtZero() {
        val now = at(2026, Calendar.AUGUST, 15, 12, 0)
        val model = SpendSeries.build(emptyList(), SpendRange.WEEK, now = now)
        assertEquals(0.0, model.total, 0.0)
        assertFalse(model.hasSpend)
        assertEquals(7, model.buckets.size)
    }
}
