package com.example.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AuraRepository(private val dao: AuraDao) {

    val allTasks = dao.getAllTasks()
    val allBudgetItems = dao.getAllBudgetItems()
    val allCalendarEvents = dao.getAllCalendarEvents()
    val allGoals = dao.getAllGoals()
    val allAccounts = dao.getAllAccounts()
    val allDailyActivity = dao.getAllDailyActivity()
    val allNotes = dao.getAllNotes()

    suspend fun addTask(task: TaskEntity) = dao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(taskId: Int) = dao.deleteTaskById(taskId)

    suspend fun addBudgetItem(item: BudgetItemEntity) = dao.insertBudgetItem(item)
    suspend fun deleteBudgetItem(itemId: Int) = dao.deleteBudgetItemById(itemId)

    suspend fun addCalendarEvent(event: CalendarEventEntity) = dao.insertCalendarEvent(event)
    suspend fun updateCalendarEvent(event: CalendarEventEntity) = dao.updateCalendarEvent(event)
    suspend fun deleteCalendarEvent(eventId: Int) = dao.deleteCalendarEventById(eventId)

    suspend fun addGoal(goal: GoalEntity) = dao.insertGoal(goal)
    suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(goalId: Int) = dao.deleteGoalById(goalId)

    suspend fun addAccount(account: AccountEntity) = dao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = dao.updateAccount(account)
    suspend fun deleteAccount(accountId: Int) = dao.deleteAccountById(accountId)

    suspend fun addNote(note: NoteEntity) = dao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = dao.updateNote(note)
    suspend fun deleteNote(noteId: Int) = dao.deleteNoteById(noteId)

    /**
     * Record a completed task toward the GitHub-style contribution heatmap.
     */
    suspend fun recordTaskCompletion(xpEarned: Int, timestamp: Long = System.currentTimeMillis()) {
        val dateKey = dayKey(timestamp)
        val existing = dao.getActivityForDay(dateKey)
        if (existing == null) {
            dao.upsertDailyActivity(
                DailyActivityEntity(
                    dateKey = dateKey,
                    completedCount = 1,
                    xpEarned = xpEarned
                )
            )
        } else {
            dao.upsertDailyActivity(
                existing.copy(
                    completedCount = existing.completedCount + 1,
                    xpEarned = existing.xpEarned + xpEarned
                )
            )
        }
    }

    companion object {
        fun dayKey(millis: Long = System.currentTimeMillis()): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
        }
    }
}
