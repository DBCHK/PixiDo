package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * GitHub-style contribution day: how many tasks were completed on a given date.
 * [dateKey] format: yyyy-MM-dd
 */
@Entity(tableName = "daily_activity")
data class DailyActivityEntity(
    @PrimaryKey val dateKey: String,
    val completedCount: Int = 0,
    val xpEarned: Int = 0
)
