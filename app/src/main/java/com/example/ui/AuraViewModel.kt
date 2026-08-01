package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.AuthResult
import com.example.auth.GoogleAuthRepository
import com.example.backup.BackupScheduler
import com.example.data.AccountEntity
import com.example.data.AccountType
import com.example.data.AppThemeOption
import com.example.data.AuraDatabase
import com.example.data.AuraRepository
import com.example.data.BackupFrequency
import com.example.data.BudgetItemEntity
import com.example.data.CalendarEventEntity
import com.example.data.CloudBackupRepository
import com.example.data.Currencies
import com.example.data.DailyActivityEntity
import com.example.data.GoalEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.data.TransactionType
import com.example.data.UserPreferencesRepository
import com.example.data.UserProfile
import com.example.notify.NotificationHelper
import com.example.notify.ReminderScheduler
import com.example.widget.WidgetActions
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
    private val cloudBackup: CloudBackupRepository
    private val googleAuth: GoogleAuthRepository

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

    private val _authBusy = MutableStateFlow(false)
    val authBusy: StateFlow<Boolean> = _authBusy.asStateFlow()

    private val _backupBusy = MutableStateFlow(false)
    val backupBusy: StateFlow<Boolean> = _backupBusy.asStateFlow()

    private var timerJob: Job? = null
    private var lastDeletedTask: TaskEntity? = null

    init {
        val dao = AuraDatabase.getDatabase(application).auraDao()
        repository = AuraRepository(dao)
        preferences = UserPreferencesRepository(application)
        cloudBackup = CloudBackupRepository(application, repository, preferences)
        googleAuth = GoogleAuthRepository(application)

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

        // Re-apply backup schedule from saved prefs + hydrate Google session
        viewModelScope.launch {
            googleAuth.currentGoogleUser()?.let { user ->
                preferences.setGoogleAccount(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName,
                    photoUrl = user.photoUrl
                )
            }
            val profile = preferences.currentProfile()
            BackupScheduler.apply(
                application,
                profile.backupFrequency,
                profile.isSignedIn || googleAuth.isSignedIn
            )
        }
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

    /** Push latest Room / prefs into home-screen widgets. */
    private fun refreshWidgets() {
        runCatching { WidgetActions.refreshAll(appContext()) }
    }

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
        bio: String = "",
        email: String = "",
        location: String = ""
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

    fun setAccentColor(hex: String) {
        viewModelScope.launch {
            preferences.setAccentColorHex(hex)
        }
    }

    // --- Google SSO + cloud backup ---

    /**
     * @param activityContext must be an Activity context for Credential Manager UI.
     */
    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            if (_authBusy.value) return@launch
            _authBusy.value = true
            try {
                // Use activity-scoped auth for the credential UI
                val auth = GoogleAuthRepository(activityContext)
                when (val result = auth.signIn()) {
                    is AuthResult.Success -> {
                        preferences.setGoogleAccount(
                            uid = result.user.uid,
                            email = result.user.email,
                            displayName = result.user.displayName,
                            photoUrl = result.user.photoUrl
                        )
                        val sync = cloudBackup.syncOnSignIn()
                        sync.onSuccess { msg ->
                            _snackbarMessage.value = msg
                        }.onFailure { e ->
                            _snackbarMessage.value =
                                "Signed in, but sync failed: ${e.message ?: "unknown error"}"
                        }
                        val profile = preferences.currentProfile()
                        BackupScheduler.apply(
                            appContext(),
                            profile.backupFrequency,
                            signedIn = true
                        )
                    }
                    is AuthResult.Cancelled -> {
                        // User dismissed the sheet — stay quiet
                    }
                    is AuthResult.Error -> {
                        _snackbarMessage.value = result.message
                    }
                }
            } finally {
                _authBusy.value = false
            }
        }
    }

    fun signOutGoogle() {
        viewModelScope.launch {
            _authBusy.value = true
            try {
                googleAuth.signOut()
                preferences.clearGoogleAccount()
                BackupScheduler.apply(appContext(), BackupFrequency.NEVER, signedIn = false)
                _snackbarMessage.value = "Signed out. Local data stays on this device."
            } finally {
                _authBusy.value = false
            }
        }
    }

    fun setBackupFrequency(frequency: BackupFrequency) {
        viewModelScope.launch {
            preferences.setBackupFrequency(frequency)
            val profile = preferences.currentProfile()
            BackupScheduler.apply(
                appContext(),
                frequency,
                signedIn = profile.isSignedIn || googleAuth.isSignedIn
            )
            _snackbarMessage.value = when (frequency) {
                BackupFrequency.EVERY_24_HOURS -> "Auto-backup every 24 hours enabled"
                BackupFrequency.NEVER -> "Cloud backup turned off"
            }
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            if (_backupBusy.value) return@launch
            _backupBusy.value = true
            try {
                cloudBackup.backupNow().fold(
                    onSuccess = {
                        _snackbarMessage.value = "Backup saved to your Google account"
                    },
                    onFailure = { e ->
                        _snackbarMessage.value = e.message ?: "Backup failed"
                    }
                )
            } finally {
                _backupBusy.value = false
            }
        }
    }

    fun restoreFromCloud() {
        viewModelScope.launch {
            if (_backupBusy.value) return@launch
            _backupBusy.value = true
            try {
                cloudBackup.restoreIfAvailable().fold(
                    onSuccess = { restored ->
                        _snackbarMessage.value = if (restored) {
                            "Data restored from Google account"
                        } else {
                            "No cloud backup found yet"
                        }
                    },
                    onFailure = { e ->
                        _snackbarMessage.value = e.message ?: "Restore failed"
                    }
                )
            } finally {
                _backupBusy.value = false
            }
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
            refreshWidgets()
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
            refreshWidgets()
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            lastDeletedTask = tasks.value.find { it.id == taskId }
            ReminderScheduler.cancel(appContext(), ReminderScheduler.TYPE_TASK, taskId)
            repository.deleteTask(taskId)
            _snackbarMessage.value = "Task deleted · undo available"
            refreshWidgets()
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
                refreshWidgets()
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
        accountId: Int? = null,
        transactionType: TransactionType = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
    ) {
        viewModelScope.launch {
            val type = transactionType
            val defaultTitle = type.displayName
            repository.addBudgetItem(
                BudgetItemEntity(
                    title = title.ifBlank { defaultTitle },
                    amount = amount,
                    isExpense = type.decreasesAsset,
                    category = category,
                    note = note,
                    accountId = accountId,
                    transactionType = type.name
                )
            )

            // Update linked account balance & credit utilization
            if (accountId != null) {
                accounts.value.find { it.id == accountId }?.let { account ->
                    val updated = applyTransactionToAccount(account, type, amount, reverse = false)
                    repository.updateAccount(updated)
                }
            }

            // Savings expenses can bump savings goals
            if (type == TransactionType.EXPENSE && category.contains("Savings", ignoreCase = true)) {
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
            refreshWidgets()
        }
    }

    fun deleteBudgetItem(itemId: Int) {
        viewModelScope.launch {
            val item = budgetItems.value.find { it.id == itemId }
            if (item != null) {
                if (item.type == TransactionType.TRANSFER) {
                    reverseTransfer(item)
                } else if (item.accountId != null) {
                    accounts.value.find { it.id == item.accountId }?.let { account ->
                        val updated = applyTransactionToAccount(
                            account,
                            item.type,
                            item.amount,
                            reverse = true
                        )
                        repository.updateAccount(updated)
                    }
                }
            }
            repository.deleteBudgetItem(itemId)
            refreshWidgets()
        }
    }

    /**
     * Move money between accounts (bank → bank, bank → credit card repayment, etc.).
     * Does not count as monthly spend. Paying a credit card reduces its debt.
     */
    fun transferBetweenAccounts(
        fromAccountId: Int,
        toAccountId: Int,
        amount: Double,
        note: String = ""
    ) {
        if (amount <= 0 || fromAccountId == toAccountId) return
        viewModelScope.launch {
            val from = accounts.value.find { it.id == fromAccountId } ?: return@launch
            val to = accounts.value.find { it.id == toAccountId } ?: return@launch
            if (from.isCreditCard) {
                _snackbarMessage.value = "Transfer from a credit card isn’t supported — use expense"
                return@launch
            }

            // Debit source (asset)
            repository.updateAccount(
                from.copy(balance = from.balance - amount)
            )
            // Credit destination
            if (to.isCreditCard) {
                val newDebt = (to.balance - amount).coerceAtLeast(0.0)
                repository.updateAccount(
                    to.copy(balance = newDebt, monthlyUsage = newDebt)
                )
            } else {
                repository.updateAccount(
                    to.copy(balance = to.balance + amount)
                )
            }

            val title = if (to.isCreditCard) {
                "Card payment · ${to.name}"
            } else {
                "Transfer · ${from.name} → ${to.name}"
            }
            repository.addBudgetItem(
                BudgetItemEntity(
                    title = title,
                    amount = amount,
                    isExpense = true,
                    category = if (to.isCreditCard) "Credit payment" else "Transfer",
                    note = note.ifBlank {
                        "${from.name} → ${to.name}"
                    },
                    accountId = fromAccountId,
                    transactionType = TransactionType.TRANSFER.name,
                    relatedAccountId = toAccountId
                )
            )
            _snackbarMessage.value = if (to.isCreditCard) {
                "Paid ${Currencies.format(amount, userProfile.value.currencyCode)} on ${to.name}"
            } else {
                "Transferred ${Currencies.format(amount, userProfile.value.currencyCode)}"
            }
            refreshWidgets()
        }
    }

    private suspend fun reverseTransfer(item: BudgetItemEntity) {
        val amount = item.amount
        val fromId = item.accountId ?: return
        val toId = item.relatedAccountId ?: return
        accounts.value.find { it.id == fromId }?.let { from ->
            repository.updateAccount(from.copy(balance = from.balance + amount))
        }
        accounts.value.find { it.id == toId }?.let { to ->
            if (to.isCreditCard) {
                val newDebt = to.balance + amount
                repository.updateAccount(to.copy(balance = newDebt, monthlyUsage = newDebt))
            } else {
                repository.updateAccount(to.copy(balance = to.balance - amount))
            }
        }
    }

    /**
     * Apply or reverse a transaction against an account.
     *
     * Credit cards: [AccountEntity.balance] = amount owed (debt). Credit limits never change.
     * Asset accounts: balance = available funds.
     */
    private fun applyTransactionToAccount(
        account: AccountEntity,
        type: TransactionType,
        amount: Double,
        reverse: Boolean
    ): AccountEntity {
        val signed = if (reverse) -amount else amount
        return if (account.isCreditCard) {
            // Debt goes up on EXPENSE/LENT; down on INCOME / payment-style
            val debtDelta = when (type) {
                TransactionType.EXPENSE, TransactionType.LENT -> signed
                TransactionType.INCOME, TransactionType.BORROW, TransactionType.TRANSFER -> -signed
            }
            val newDebt = (account.balance + debtDelta).coerceAtLeast(0.0)
            account.copy(
                balance = newDebt,
                monthlyUsage = newDebt
            )
        } else {
            val cashDelta = when (type) {
                TransactionType.EXPENSE, TransactionType.LENT, TransactionType.TRANSFER -> -signed
                TransactionType.INCOME, TransactionType.BORROW -> signed
            }
            val newBalance = account.balance + cashDelta
            val usageDelta = if (type == TransactionType.EXPENSE) signed else 0.0
            account.copy(
                balance = newBalance,
                monthlyUsage = (account.monthlyUsage + usageDelta).coerceAtLeast(0.0)
            )
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
            // Credit cards: balance = amount owed; limit is separate and never part of net worth
            val isCard = type == AccountType.CREDIT_CARD
            val owed = balance.coerceAtLeast(0.0)
            repository.addAccount(
                AccountEntity(
                    name = name.ifBlank { type.name.replace('_', ' ') },
                    type = type.name,
                    balance = owed,
                    creditLimit = if (isCard) creditLimit.coerceAtLeast(0.0) else 0.0,
                    monthlyUsage = if (isCard) owed else 0.0,
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
