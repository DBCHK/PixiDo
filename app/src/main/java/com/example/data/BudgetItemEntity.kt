package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_items")
data class BudgetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val isExpense: Boolean, // true = expense, false = income
    val category: String, // "Food & Coffee ☕", "Subscriptions 🎵", "Vibes & Fun 🎉", "Savings & Wealth 💰", "Shopping 🛍️", "Hustle Income 💸"
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = ""
)
