package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        BudgetItemEntity::class,
        CalendarEventEntity::class,
        GoalEntity::class,
        AccountEntity::class,
        DailyActivityEntity::class,
        NoteEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AuraDatabase : RoomDatabase() {
    abstract fun auraDao(): AuraDao

    companion object {
        @Volatile
        private var INSTANCE: AuraDatabase? = null

        fun getDatabase(context: Context): AuraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AuraDatabase::class.java,
                    "pixido_organizer.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
