package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val category: String,
    val dateMillis: Long, // Day timestamp
    val timeSlot: String, // Display string e.g. "10:00 AM"
    val startMillis: Long = 0L, // Exact start for notifications
    val isCompleted: Boolean = false
)
