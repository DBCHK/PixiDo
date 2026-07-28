package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val category: String, // "Deep Work", "Social & Hangouts", "Fitness & Wellness", "Bill Payment", "Personal Growth"
    val dateMillis: Long, // Day timestamp at 00:00 UTC
    val timeSlot: String, // e.g. "10:00 AM - 11:30 AM"
    val isCompleted: Boolean = false
)
