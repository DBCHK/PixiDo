package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AuraDao {

    // --- Tasks ---
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, dueDateMillis ASC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getTasksOnce(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: Int)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    // --- Budget Items ---
    @Query("SELECT * FROM budget_items ORDER BY timestamp DESC")
    fun getAllBudgetItems(): Flow<List<BudgetItemEntity>>

    @Query("SELECT * FROM budget_items")
    suspend fun getBudgetItemsOnce(): List<BudgetItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetItem(item: BudgetItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetItems(items: List<BudgetItemEntity>)

    @Query("DELETE FROM budget_items WHERE id = :itemId")
    suspend fun deleteBudgetItemById(itemId: Int)

    @Query("DELETE FROM budget_items")
    suspend fun clearBudgetItems()

    // --- Calendar Events ---
    @Query("SELECT * FROM calendar_events ORDER BY dateMillis ASC")
    fun getAllCalendarEvents(): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events")
    suspend fun getCalendarEventsOnce(): List<CalendarEventEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvents(events: List<CalendarEventEntity>)

    @Update
    suspend fun updateCalendarEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :eventId")
    suspend fun deleteCalendarEventById(eventId: Int)

    @Query("DELETE FROM calendar_events")
    suspend fun clearCalendarEvents()

    // --- Life Goals ---
    @Query("SELECT * FROM life_goals ORDER BY isCompleted ASC, id DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM life_goals")
    suspend fun getGoalsOnce(): List<GoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(goals: List<GoalEntity>)

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM life_goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: Int)

    @Query("DELETE FROM life_goals")
    suspend fun clearGoals()

    // --- Accounts ---
    @Query("SELECT * FROM accounts ORDER BY isPrimary DESC, id ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts")
    suspend fun getAccountsOnce(): List<AccountEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("DELETE FROM accounts WHERE id = :accountId")
    suspend fun deleteAccountById(accountId: Int)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    // --- Daily Activity (legacy aggregate; widget fallback) ---
    @Query("SELECT * FROM daily_activity ORDER BY dateKey ASC")
    fun getAllDailyActivity(): Flow<List<DailyActivityEntity>>

    @Query("SELECT * FROM daily_activity")
    suspend fun getDailyActivityOnce(): List<DailyActivityEntity>

    @Query("SELECT * FROM daily_activity WHERE dateKey = :dateKey LIMIT 1")
    suspend fun getActivityForDay(dateKey: String): DailyActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyActivity(activity: DailyActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyActivities(items: List<DailyActivityEntity>)

    @Query("DELETE FROM daily_activity")
    suspend fun clearDailyActivity()

    // --- Goal Activity (per-goal GitHub-style contribution heatmaps) ---
    @Query("SELECT * FROM goal_activity ORDER BY dateKey ASC")
    fun getAllGoalActivity(): Flow<List<GoalActivityEntity>>

    @Query("SELECT * FROM goal_activity")
    suspend fun getGoalActivityOnce(): List<GoalActivityEntity>

    @Query("SELECT * FROM goal_activity WHERE goalId = :goalId ORDER BY dateKey ASC")
    fun getGoalActivityForGoal(goalId: Int): Flow<List<GoalActivityEntity>>

    @Query("SELECT * FROM goal_activity WHERE goalId = :goalId AND dateKey = :dateKey LIMIT 1")
    suspend fun getGoalActivityForDay(goalId: Int, dateKey: String): GoalActivityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGoalActivity(activity: GoalActivityEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalActivities(items: List<GoalActivityEntity>)

    @Query("DELETE FROM goal_activity WHERE goalId = :goalId AND dateKey = :dateKey")
    suspend fun deleteGoalActivityDay(goalId: Int, dateKey: String)

    @Query("DELETE FROM goal_activity WHERE goalId = :goalId")
    suspend fun clearGoalActivityForGoal(goalId: Int)

    @Query("DELETE FROM goal_activity")
    suspend fun clearGoalActivity()

    // --- Quick Notes ---
    @Query("SELECT * FROM quick_notes ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM quick_notes")
    suspend fun getNotesOnce(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM quick_notes WHERE id = :noteId")
    suspend fun deleteNoteById(noteId: Int)

    @Query("DELETE FROM quick_notes")
    suspend fun clearNotes()

    // --- Pending SMS bank transactions ---
    @Query(
        "SELECT * FROM pending_sms_transactions WHERE status = 'PENDING' " +
            "ORDER BY receivedAt DESC"
    )
    fun getPendingSmsTransactions(): Flow<List<PendingSmsTransactionEntity>>

    @Query(
        "SELECT * FROM pending_sms_transactions WHERE status = 'PENDING' " +
            "ORDER BY receivedAt DESC"
    )
    suspend fun getPendingSmsTransactionsOnce(): List<PendingSmsTransactionEntity>

    @Query("SELECT * FROM pending_sms_transactions WHERE smsHash = :hash LIMIT 1")
    suspend fun getPendingSmsByHash(hash: String): PendingSmsTransactionEntity?

    @Query(
        "SELECT * FROM pending_sms_transactions WHERE receivedAt >= :since " +
            "ORDER BY receivedAt DESC"
    )
    suspend fun getRecentSmsTransactions(since: Long): List<PendingSmsTransactionEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingSmsTransaction(item: PendingSmsTransactionEntity): Long

    @Update
    suspend fun updatePendingSmsTransaction(item: PendingSmsTransactionEntity)

    @Query("UPDATE pending_sms_transactions SET status = :status WHERE id = :id")
    suspend fun setPendingSmsStatus(id: Int, status: String)

    @Query(
        "DELETE FROM pending_sms_transactions WHERE status != 'PENDING' " +
            "AND receivedAt < :olderThan"
    )
    suspend fun purgeOldResolvedSms(olderThan: Long)
}
