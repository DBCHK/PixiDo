package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String, // "Work", "Health", "Side Hustle", "Life", "Social"
    val priority: String, // "HIGH_FIRE", "QUICK_WIN", "CORE_GOAL", "BRAINSTORM"
    val dueDateMillis: Long = System.currentTimeMillis(),
    val dueTimeStr: String = "Today",
    val isCompleted: Boolean = false,
    val streakCount: Int = 1,
    val subtasks: String = "", // Semicolon separated list of subtasks
    val completedSubtasks: String = "", // Semicolon separated list of completed subtasks
    val linkedGoalId: Int? = null,
    val xpReward: Int = 20
)
