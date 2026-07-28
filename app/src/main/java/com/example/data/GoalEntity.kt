package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "life_goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "Travel ✈️", "Savings 💰", "Fitness 💪", "Career 🚀", "Mindset 🧠"
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val unit: String = "$", // "$", "tasks", "books", "kms", "%"
    val deadlineStr: String = "End of Year",
    val colorHex: String = "#A78BFA",
    val isCompleted: Boolean = false
)
