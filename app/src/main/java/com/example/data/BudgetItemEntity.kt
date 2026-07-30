package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "budget_items")
data class BudgetItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val isExpense: Boolean,
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String = "",
    val accountId: Int? = null
)
