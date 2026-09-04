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
    val unit: String = "$", // "$", "tasks", "books", "kms", "%", "done"
    val deadlineStr: String = "End of Year",
    val colorHex: String = "#A78BFA",
    val isCompleted: Boolean = false,
    /** Checklist goal: one tap on YES DONE marks it complete. No amount tracking. */
    val isSimple: Boolean = false
) {
    val isSimpleTask: Boolean
        get() = isSimple ||
            unit.equals("done", ignoreCase = true) ||
            unit.equals("yes", ignoreCase = true)
}
