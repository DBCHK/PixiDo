package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.AppThemeOption
import com.example.data.AuraDatabase
import com.example.data.AuraRepository
import com.example.data.BudgetItemEntity
import com.example.data.CalendarEventEntity
import com.example.data.Currencies
import com.example.data.DailyActivityEntity
import com.example.data.GoalEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.data.UserPreferencesRepository
import com.example.data.UserProfile
import com.example.notify.NotificationHelper
import com.example.notify.ReminderScheduler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AuraRepository
    private val preferences: UserPreferencesRepository

    val tasks: StateFlow<List<TaskEntity>>
    val budgetItems: StateFlow<List<BudgetItemEntity>>
    val calendarEvents: StateFlow<List<CalendarEventEntity>>
    val goals: StateFlow<List<GoalEntity>>
    val accounts: StateFlow<List<AccountEntity>>
    val dailyActivity: StateFlow<List<DailyActivityEntity>>
    val notes: StateFlow<List<NoteEntity>>
    val userProfile: StateFlow<UserProfile>

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _selectedCalendarDate = MutableStateFlow(System.currentTimeMillis())
    val selectedCalendarDate: StateFlow<Long> = _selectedCalendarDate.asStateFlow()

    private val _focusSecondsLeft = MutableStateFlow(25 * 60)
    val focusSecondsLeft: StateFlow<Int> = _focusSecondsLeft.asStateFlow()

    private val _isFocusTimerRunning = MutableStateFlow(false)
    val isFocusTimerRunning: StateFlow<Boolean> = _isFocusTimerRunning.asStateFlow()

    private val _showFocusModal = MutableStateFlow(false)
    val showFocusModal: StateFlow<Boolean> = _showFocusModal.asStateFlow()

    private val _showProfile = MutableStateFlow(false)
    val showProfile: StateFlow<Boolean> = _showProfile.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var timerJob: Job? = null
    private var lastDeletedTask: TaskEntity? = null

    init {
        val dao = AuraDatabase.getDatabase(application).auraDao()
        repository = AuraRepository(dao)
        preferences = UserPreferencesRepository(application)

        tasks = repository.allTasks.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        budgetItems = repository.allBudgetItems.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        calendarEvents = repository.allCalendarEvents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        goals = repository.allGoals.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        accounts = repository.allAccounts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        dailyActivity = repository.allDailyActivity.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        notes = repository.allNotes.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        userProfile = preferences.userProfile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserProfile()
        )

        // Normalize selected calendar date to local midnight
        _selectedCalendarDate.value = startOfDay(System.currentTimeMillis())

        NotificationHelper.ensureChannels(application)
    }

    private fun startOfDay(millis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun appContext() = getApplication<Application>()

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(msg: String) {
        _snackbarMessage.value = msg
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setSelectedCalendarDate(dateMillis: Long) {
        _selectedCalendarDate.value = startOfDay(dateMillis)
    }

    fun openProfile() {
        _showProfile.value = true
    }

    fun closeProfile() {
        _showProfile.value = false
    }

    // --- Profile / Preferences ---
    fun updateProfile(
        displayName: String,
        bio: String,
        email: String,
        location: String
    ) {
        viewModelScope.launch {
            preferences.updateProfile(
                displayName = displayName,
                bio = bio,
                email = email,
                location = location
            )
        }
    }

    fun setAvatarUri(uri: String) {
        viewModelScope.launch {
            preferences.updateProfile(avatarUri = uri)
        }
    }

    fun setTheme(option: AppThemeOption) {
        viewModelScope.launch {
            preferences.setTheme(option)
        }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch {
            preferences.setCurrency(code)
        }
    }

    fun setMonthlyBudgetLimit(limit: Double) {
        viewModelScope.launch {
            preferences.setMonthlyBudgetLimit(limit)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setSoundEnabled(enabled) }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch { preferences.setHapticsEnabled(enabled) }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch { preferences.setReduceMotion(enabled) }
    }

    // --- Notes ---
    fun addNote(content: String, colorHex: String = "#7C3AED") {
        viewModelScope.launch {
            if (content.isBlank()) return@launch
            repository.addNote(
                NoteEntity(content = content.trim(), colorHex = colorHex)
            )
        }
    }

    fun toggleNotePin(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(
                note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis())
            )
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch { repository.deleteNote(noteId) }
    }

    // --- Task Actions ---
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            val now = System.currentTimeMillis()
            val newStreak = if (newCompleted) task.streakCount + 1 else maxOf(1, task.streakCount - 1)
            val updated = task.copy(
                isCompleted = newCompleted,
                streakCount = newStreak,
                completedAtMillis = if (newCompleted) now else null
            )
            repository.updateTask(updated)

            if (newCompleted) {
                // No more reminder once completed
                ReminderScheduler.cancel(appContext(), ReminderScheduler.TYPE_TASK, task.id)
                preferences.addXp(task.xpReward)
                repository.recordTaskCompletion(task.xpReward, now)
                task.linkedGoalId?.let { goalId ->
                    goals.value.find { it.id == goalId }?.let { targetGoal ->
                        val updatedGoal = targetGoal.copy(
                            currentAmount = targetGoal.currentAmount + 1.0,
                            isCompleted = (targetGoal.currentAmount + 1.0) >= targetGoal.targetAmount
                        )
                        repository.updateGoal(updatedGoal)
                    }
                }
            }
        }
    }

    fun toggleSubtask(task: TaskEntity, subtask: String) {
        viewModelScope.launch {
            val currentCompleted = task.completedSubtasks
                .split(";")
                .filter { it.isNotBlank() }
                .toMutableSet()

            if (currentCompleted.contains(subtask)) {
                currentCompleted.remove(subtask)
            } else {
                currentCompleted.add(subtask)
            }

            repository.updateTask(task.copy(completedSubtasks = currentCompleted.joinToString(";")))
        }
    }

    fun addTask(
        title: String,
        category: String,
        priority: String,
        dueTimeStr: String,
        dueDateMillis: Long,
        subtasks: String,
        linkedGoalId: Int?
    ) {
        viewModelScope.launch {
            val task = TaskEntity(
                title = title.ifBlank { "Untitled Task" },
                category = category,
                priority = priority,
                dueTimeStr = dueTimeStr.ifBlank { "Today" },
                dueDateMillis = if (dueDateMillis > 0) dueDateMillis else System.currentTimeMillis(),
                subtasks = subtasks,
                linkedGoalId = linkedGoalId,
                xpReward = when (priority) {
                    "HIGH_FIRE" -> 40
                    "CORE_GOAL" -> 30
                    else -> 20
                }
            )
            val newId = repository.addTask(task).toInt()
            if (task.dueDateMillis > System.currentTimeMillis()) {
                ReminderScheduler.schedule(
                    context = appContext(),
                    type = ReminderScheduler.TYPE_TASK,
                    itemId = newId,
                    triggerAtMillis = task.dueDateMillis,
                    title = "Task due: ${task.title}",
                    body = "It's time for “${task.title}” (${task.dueTimeStr})"
                )
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            lastDeletedTask = tasks.value.find { it.id == taskId }
            ReminderScheduler.cancel(appContext(), ReminderScheduler.TYPE_TASK, taskId)
            repository.deleteTask(taskId)
            _snackbarMessage.value = "Task deleted · undo available"
        }
    }

    fun undoDeleteTask() {
        viewModelScope.launch {
            lastDeletedTask?.let { task ->
                val restored = task.copy(id = 0)
                val newId = repository.addTask(restored).toInt()
                if (!restored.isCompleted && restored.dueDateMillis > System.currentTimeMillis()) {
                    ReminderScheduler.schedule(
                        context = appContext(),
                        type = ReminderScheduler.TYPE_TASK,
                        itemId = newId,
                        triggerAtMillis = restored.dueDateMillis,
                        title = "Task due: ${restored.title}",
                        body = "It's time for “${restored.title}” (${restored.dueTimeStr})"
                    )
                }
                lastDeletedTask = null
                _snackbarMessage.value = "Task restored"
            }
        }
    }

    // --- Budget Actions ---
    fun addBudgetItem(
        title: String,
        amount: Double,
        isExpense: Boolean,
        category: String,
        note: String,
        accountId: Int? = null
    ) {
        viewModelScope.launch {
            repository.addBudgetItem(
                BudgetItemEntity(
                    title = title.ifBlank { if (isExpense) "Expense" else "Income" },
                    amount = amount,
                    isExpense = isExpense,
                    category = category,
                    note = note,
                    accountId = accountId
                )
            )

            // Update linked account balance & usage
            if (accountId != null) {
                accounts.value.find { it.id == accountId }?.let { account ->
                    val newBalance = if (isExpense) account.balance - amount else account.balance + amount
                    val newUsage = if (isExpense) account.monthlyUsage + amount else account.monthlyUsage
                    repository.updateAccount(
                        account.copy(
                            balance = newBalance,
                            monthlyUsage = newUsage.coerceAtLeast(0.0)
                        )
                    )
                }
            }

            if (isExpense && category.contains("Savings", ignoreCase = true)) {
                goals.value.firstOrNull {
                    it.category.contains("Savings", ignoreCase = true) ||
                        it.category.contains("Travel", ignoreCase = true)
                }?.let { goal ->
                    val newAmt = goal.currentAmount + amount
                    repository.updateGoal(
                        goal.copy(
                            currentAmount = newAmt,
                            isCompleted = newAmt >= goal.targetAmount
                        )
                    )
                }
            }
        }
    }

    fun deleteBudgetItem(itemId: Int) {
        viewModelScope.launch {
            val item = budgetItems.value.find { it.id == itemId }
            if (item != null && item.accountId != null) {
                accounts.value.find { it.id == item.accountId }?.let { account ->
                    val restoredBalance =
                        if (item.isExpense) account.balance + item.amount else account.balance - item.amount
                    val restoredUsage =
                        if (item.isExpense) (account.monthlyUsage - item.amount).coerceAtLeast(0.0)
                        else account.monthlyUsage
                    repository.updateAccount(
                        account.copy(balance = restoredBalance, monthlyUsage = restoredUsage)
                    )
                }
            }
            repository.deleteBudgetItem(itemId)
        }
    }

    // --- Account Actions ---
    fun addAccount(
        name: String,
        type: AccountType,
        balance: Double,
        creditLimit: Double,
        colorHex: String,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val currency = userProfile.value.currencyCode
            val isFirst = accounts.value.isEmpty()
            repository.addAccount(
                AccountEntity(
                    name = name.ifBlank { type.name.replace('_', ' ') },
                    type = type.name,
                    balance = balance,
                    creditLimit = creditLimit,
                    monthlyUsage = 0.0,
                    currencyCode = currency,
                    colorHex = colorHex,
                    isPrimary = isFirst,
                    notes = notes
                )
            )
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(accountId: Int) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
        }
    }

    fun setPrimaryAccount(accountId: Int) {
        viewModelScope.launch {
            accounts.value.forEach { acc ->
                repository.updateAccount(acc.copy(isPrimary = acc.id == accountId))
            }
        }
    }

    // --- Calendar Event Actions ---
    fun addCalendarEvent(
        title: String,
        category: String,
        dateMillis: Long,
        timeSlot: String,
        startMillis: Long,
        description: String
    ) {
        viewModelScope.launch {
            val day = startOfDay(dateMillis)
            val event = CalendarEventEntity(
                title = title.ifBlank { "Event" },
                category = category,
                dateMillis = day,
                timeSlot = timeSlot.ifBlank { "All Day" },
                startMillis = if (startMillis > 0) startMillis else day,
                description = description
            )
            val newId = repository.addCalendarEvent(event).toInt()
            if (event.startMillis > System.currentTimeMillis()) {
                ReminderScheduler.schedule(
                    context = appContext(),
                    type = ReminderScheduler.TYPE_EVENT,
                    itemId = newId,
                    triggerAtMillis = event.startMillis,
                    title = "Event: ${event.title}",
                    body = "Starting now · ${event.timeSlot}"
                )
            }
        }
    }

    fun toggleCalendarEventCompleted(event: CalendarEventEntity) {
        viewModelScope.launch {
            val done = !event.isCompleted
            repository.updateCalendarEvent(event.copy(isCompleted = done))
            if (done) {
                ReminderScheduler.cancel(appContext(), ReminderScheduler.TYPE_EVENT, event.id)
            }
        }
    }

    fun deleteCalendarEvent(eventId: Int) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext(), ReminderScheduler.TYPE_EVENT, eventId)
            repository.deleteCalendarEvent(eventId)
        }
    }

    // --- Goal Actions ---
    fun addGoal(
        title: String,
        category: String,
        targetAmount: Double,
        unit: String,
        deadlineStr: String,
        colorHex: String
    ) {
        viewModelScope.launch {
            // Money goals use the same currency as Budget settings
            val currencyUnit = if (unit == "$" || unit.equals("money", ignoreCase = true)) {
                Currencies.symbolOf(userProfile.value.currencyCode)
            } else {
                unit.ifBlank { Currencies.symbolOf(userProfile.value.currencyCode) }
            }
            repository.addGoal(
                GoalEntity(
                    title = title.ifBlank { "New Goal" },
                    category = category,
                    targetAmount = maxOf(1.0, targetAmount),
                    unit = currencyUnit,
                    deadlineStr = deadlineStr.ifBlank { "2027" },
                    colorHex = colorHex
                )
            )
        }
    }

    fun updateGoalProgress(goal: GoalEntity, delta: Double) {
        viewModelScope.launch {
            val newAmt = maxOf(0.0, goal.currentAmount + delta)
            repository.updateGoal(
                goal.copy(
                    currentAmount = newAmt,
                    isCompleted = newAmt >= goal.targetAmount
                )
            )
            if (delta > 0) {
                preferences.addXp(15)
            }
        }
    }

    fun deleteGoal(goalId: Int) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
        }
    }

    // --- Focus Mode Timer ---
    fun openFocusModal() {
        _showFocusModal.value = true
    }

    fun closeFocusModal() {
        _showFocusModal.value = false
    }

    fun startFocusTimer(minutes: Int = 25) {
        timerJob?.cancel()
        _focusSecondsLeft.value = minutes * 60
        _isFocusTimerRunning.value = true

        timerJob = viewModelScope.launch {
            while (_focusSecondsLeft.value > 0 && _isFocusTimerRunning.value) {
                delay(1000)
                _focusSecondsLeft.value -= 1
            }
            if (_focusSecondsLeft.value == 0) {
                _isFocusTimerRunning.value = false
                preferences.addXp(50)
                repository.recordTaskCompletion(50)
                _snackbarMessage.value = "Focus complete · +50 XP"
            }
        }
    }

    fun pauseFocusTimer() {
        _isFocusTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetFocusTimer(minutes: Int = 25) {
        pauseFocusTimer()
        _focusSecondsLeft.value = minutes * 60
    }
}
