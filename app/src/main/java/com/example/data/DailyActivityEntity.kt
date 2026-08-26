package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Legacy aggregate daily activity table kept for migration compatibility.
 * Goal contribution heatmaps now use [GoalActivityEntity].
 */
@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val dateKey: String,
    val completedCount: Int = 0,
    val xpEarned: Int = 0
)
