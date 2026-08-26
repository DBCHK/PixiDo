package com.example.data

import androidx.room.Entity

/**
 * Per-goal contribution day (GitHub-style heatmap).
 * [dateKey] format: yyyy-MM-dd
 *
 * Tracks how many progress updates happened on a given day for an individual goal —
 * not task completions.
 */
@Entity(
    tableName = "goal_activity",
    primaryKeys = ["goalId", "dateKey"]
)
data class GoalActivityEntity(
    val goalId: Int,
    val dateKey: String,
    val completedCount: Int = 0,
    val xpEarned: Int = 0
)
