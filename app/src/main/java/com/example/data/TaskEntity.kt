package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val category: String,
    val priority: String,
    val dueDateMillis: Long = System.currentTimeMillis(),
    val dueTimeStr: String = "Today",
    val isCompleted: Boolean = false,
    val completedAtMillis: Long? = null,
    val streakCount: Int = 1,
    val subtasks: String = "",
    val completedSubtasks: String = "",
    val linkedGoalId: Int? = null,
    val xpReward: Int = 20,
    /** NONE | DAILY | WEEKDAYS | WEEKLY */
    val repeatRule: String = RepeatRule.NONE.name,
    val isPinned: Boolean = false,
    val notes: String = ""
) {
    val repeat: RepeatRule get() = RepeatRule.from(repeatRule)
    val isRepeating: Boolean get() = repeat != RepeatRule.NONE
}
