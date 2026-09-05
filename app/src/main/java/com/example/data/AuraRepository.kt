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
    val allGoalActivity = dao.getAllGoalActivity()
    val allNotes = dao.getAllNotes()
    val pendingSmsTransactions = dao.getPendingSmsTransactions()

    suspend fun addTask(task: TaskEntity): Long = dao.insertTask(task)
    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)
    suspend fun deleteTask(taskId: Int) = dao.deleteTaskById(taskId)

    suspend fun addBudgetItem(item: BudgetItemEntity) = dao.insertBudgetItem(item)
    suspend fun deleteBudgetItem(itemId: Int) = dao.deleteBudgetItemById(itemId)

    suspend fun addCalendarEvent(event: CalendarEventEntity): Long = dao.insertCalendarEvent(event)
    suspend fun updateCalendarEvent(event: CalendarEventEntity) = dao.updateCalendarEvent(event)
    suspend fun deleteCalendarEvent(eventId: Int) = dao.deleteCalendarEventById(eventId)

    suspend fun addGoal(goal: GoalEntity) = dao.insertGoal(goal)
    suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)
    suspend fun deleteGoal(goalId: Int) {
        dao.clearGoalActivityForGoal(goalId)
        dao.deleteGoalById(goalId)
    }

    suspend fun addAccount(account: AccountEntity) = dao.insertAccount(account)
    suspend fun updateAccount(account: AccountEntity) = dao.updateAccount(account)
    suspend fun deleteAccount(accountId: Int) = dao.deleteAccountById(accountId)

    suspend fun addNote(note: NoteEntity) = dao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = dao.updateNote(note)
    suspend fun deleteNote(noteId: Int) = dao.deleteNoteById(noteId)

    suspend fun getPendingSmsOnce(): List<PendingSmsTransactionEntity> =
        dao.getPendingSmsTransactionsOnce()

    suspend fun getRecentSms(since: Long): List<PendingSmsTransactionEntity> =
        dao.getRecentSmsTransactions(since)

    suspend fun markSmsAccepted(id: Int) =
        dao.setPendingSmsStatus(id, PendingSmsTransactionEntity.STATUS_ACCEPTED)

    suspend fun markSmsDismissed(id: Int) =
        dao.setPendingSmsStatus(id, PendingSmsTransactionEntity.STATUS_DISMISSED)

    suspend fun purgeOldResolvedSms(olderThan: Long = System.currentTimeMillis() - 14L * 24 * 60 * 60 * 1000) {
        dao.purgeOldResolvedSms(olderThan)
    }

    /** Snapshot of all Room tables for cloud backup. */
    suspend fun exportSnapshot(): AppDataSnapshot = AppDataSnapshot(
        tasks = dao.getTasksOnce(),
        budgetItems = dao.getBudgetItemsOnce(),
        calendarEvents = dao.getCalendarEventsOnce(),
        goals = dao.getGoalsOnce(),
        accounts = dao.getAccountsOnce(),
        dailyActivity = dao.getDailyActivityOnce(),
        goalActivity = dao.getGoalActivityOnce(),
        notes = dao.getNotesOnce()
    )

    /** Replace local Room data with a cloud snapshot (full restore). */
    suspend fun importSnapshot(snapshot: AppDataSnapshot) {
        dao.clearTasks()
        dao.clearBudgetItems()
        dao.clearCalendarEvents()
        dao.clearGoals()
        dao.clearAccounts()
        dao.clearDailyActivity()
        dao.clearGoalActivity()
        dao.clearNotes()

        if (snapshot.tasks.isNotEmpty()) dao.insertTasks(snapshot.tasks)
        if (snapshot.budgetItems.isNotEmpty()) dao.insertBudgetItems(snapshot.budgetItems)
        if (snapshot.calendarEvents.isNotEmpty()) dao.insertCalendarEvents(snapshot.calendarEvents)
        if (snapshot.goals.isNotEmpty()) dao.insertGoals(snapshot.goals)
        if (snapshot.accounts.isNotEmpty()) dao.insertAccounts(snapshot.accounts)
        if (snapshot.dailyActivity.isNotEmpty()) dao.insertDailyActivities(snapshot.dailyActivity)
        if (snapshot.goalActivity.isNotEmpty()) dao.insertGoalActivities(snapshot.goalActivity)
        if (snapshot.notes.isNotEmpty()) dao.insertNotes(snapshot.notes)
    }

    /**
     * Toggle a daily habit check-in. Returns true if the day is now marked done.
     */
    suspend fun toggleHabitDay(
        goalId: Int,
        dateKey: String = dayKey()
    ): Boolean {
        val existing = dao.getGoalActivityForDay(goalId, dateKey)
        val alreadyDone = existing != null && existing.completedCount > 0
        return if (alreadyDone) {
            dao.deleteGoalActivityDay(goalId, dateKey)
            false
        } else {
            dao.upsertGoalActivity(
                GoalActivityEntity(
                    goalId = goalId,
                    dateKey = dateKey,
                    completedCount = 1,
                    xpEarned = 12
                )
            )
            true
        }
    }

    /**
     * Record progress on an individual goal for its contribution heatmap.
     */
    suspend fun recordGoalProgress(
        goalId: Int,
        xpEarned: Int = 15,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val dateKey = dayKey(timestamp)
        val existing = dao.getGoalActivityForDay(goalId, dateKey)
        if (existing == null) {
            dao.upsertGoalActivity(
                GoalActivityEntity(
                    goalId = goalId,
                    dateKey = dateKey,
                    completedCount = 1,
                    xpEarned = xpEarned
                )
            )
        } else {
            dao.upsertGoalActivity(
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

/** Portable dump of all user-generated Room data. */
data class AppDataSnapshot(
    val tasks: List<TaskEntity> = emptyList(),
    val budgetItems: List<BudgetItemEntity> = emptyList(),
    val calendarEvents: List<CalendarEventEntity> = emptyList(),
    val goals: List<GoalEntity> = emptyList(),
    val accounts: List<AccountEntity> = emptyList(),
    val dailyActivity: List<DailyActivityEntity> = emptyList(),
    val goalActivity: List<GoalActivityEntity> = emptyList(),
    val notes: List<NoteEntity> = emptyList()
)
