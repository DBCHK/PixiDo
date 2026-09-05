package com.example.ui

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import com.example.data.DeviceCalendarRepository
import com.example.data.DeviceCalendarSource
import com.example.data.DeviceCalendars
import com.example.data.GoalActivityEntity
import com.example.data.GoalEntity
import com.example.data.NoteEntity
import com.example.data.NotificationSoundOption
import com.example.data.PendingSmsTransactionEntity
import com.example.data.RepeatRule
import com.example.data.DayTime
import com.example.data.TaskEntity
import com.example.data.TaskPhases
import com.example.data.TaskRepeat
import com.example.data.TransactionType
import com.example.data.UserPreferencesRepository
import com.example.data.UserProfile
import com.example.notify.FocusTimerService
import com.example.notify.NotificationHelper
import com.example.notify.NowBarHelper
import com.example.notify.ReminderScheduler
import com.example.sms.SmsAccountMatcher
import com.example.sms.SmsImportStore
import com.example.sms.SmsInboxScanner
import com.example.sms.SmsTransactionParser
import com.example.widget.WidgetActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class AuraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AuraRepository
    private val preferences: UserPreferencesRepository
    private val cloudBackup: CloudBackupRepository
    private val googleAuth: GoogleAuthRepository
    private val deviceCalendar: DeviceCalendarRepository
    private val deviceEvents = MutableStateFlow<List<CalendarEventEntity>>(emptyList())
    private val _deviceCalendars = MutableStateFlow<List<DeviceCalendarSource>>(emptyList())
    val deviceCalendars: StateFlow<List<DeviceCalendarSource>> = _deviceCalendars.asStateFlow()

    val tasks: StateFlow<List<TaskEntity>>
    val budgetItems: StateFlow<List<BudgetItemEntity>>
    val calendarEvents: StateFlow<List<CalendarEventEntity>>
    val goals: StateFlow<List<GoalEntity>>
    val accounts: StateFlow<List<AccountEntity>>
    val goalActivity: StateFlow<List<GoalActivityEntity>>
    val notes: StateFlow<List<NoteEntity>>
    val userProfile: StateFlow<UserProfile>
    val pendingSmsTransactions: StateFlow<List<PendingSmsTransactionEntity>>

    /** Current SMS import prompt (head of queue); null when nothing to show. */
    private val _activeSmsPrompt = MutableStateFlow<PendingSmsTransactionEntity?>(null)
    val activeSmsPrompt: StateFlow<PendingSmsTransactionEntity?> = _activeSmsPrompt.asStateFlow()

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
    private var focusTotalSeconds: Int = 25 * 60

    /** Mirrors FocusTimerService ticks so UI + Now Bar stay in sync. */
    private val focusStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != FocusTimerService.ACTION_STATE) return
            val left = intent.getIntExtra(FocusTimerService.EXTRA_SECONDS_LEFT, _focusSecondsLeft.value)
            val running = intent.getBooleanExtra(FocusTimerService.EXTRA_RUNNING, false)
            val completed = intent.getBooleanExtra(FocusTimerService.EXTRA_COMPLETED, false)
            val total = intent.getIntExtra(FocusTimerService.EXTRA_TOTAL_SECONDS, focusTotalSeconds)
            focusTotalSeconds = total
            _focusSecondsLeft.value = left
            _isFocusTimerRunning.value = running
            if (completed) {
                viewModelScope.launch {
                    preferences.addXp(50)
                    _snackbarMessage.value = "Focus complete"
                    refreshWidgets()
                }
            }
        }
    }

    init {
        val dao = AuraDatabase.getDatabase(application).auraDao()
        repository = AuraRepository(dao)
        preferences = UserPreferencesRepository(application)
        cloudBackup = CloudBackupRepository(application, repository, preferences)
        googleAuth = GoogleAuthRepository(application)
        deviceCalendar = DeviceCalendarRepository(application)

        // Listen for Focus service ticks (Now Bar actions / background countdown)
        val filter = IntentFilter(FocusTimerService.ACTION_STATE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(focusStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            application.registerReceiver(focusStateReceiver, filter)
        }
        NowBarHelper.ensureChannel(application)

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

        calendarEvents = combine(
            repository.allCalendarEvents,
            deviceEvents
        ) { local, device ->
            local + device
        }.stateIn(
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

        goalActivity = repository.allGoalActivity.stateIn(
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

        pendingSmsTransactions = repository.pendingSmsTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Normalize selected calendar date to local midnight
        _selectedCalendarDate.value = startOfDay(System.currentTimeMillis())

        // Re-apply backup schedule from saved prefs + hydrate Google session + notif channels
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
            NotificationHelper.ensureChannels(application, profile.notificationSound)
            NowBarHelper.ensureChannel(application)
            BackupScheduler.apply(
                application,
                profile.backupFrequency,
                profile.isSignedIn || googleAuth.isSignedIn
            )
            repository.purgeOldResolvedSms()
        }

        // Keep active prompt in sync with pending queue
        viewModelScope.launch {
            pendingSmsTransactions.collect { list ->
                val current = _activeSmsPrompt.value
                if (current != null && list.any { it.id == current.id }) return@collect
                _activeSmsPrompt.value = list.firstOrNull()
            }
        }
    }

    /**
     * Scan recent inbox SMS (if permitted) and surface the next pending import prompt.
     * Safe to call on every resume / app open.
     */
    fun refreshSmsImports() {
        viewModelScope.launch {
            val profile = preferences.currentProfile()
            if (!profile.smsImportEnabled) {
                _activeSmsPrompt.value = null
                return@launch
            }
            if (SmsInboxScanner.hasReadPermission(getApplication())) {
                SmsInboxScanner.scanAndQueue(getApplication())
            }
            SmsImportStore.collapsePending(getApplication())
            val pending = repository.getPendingSmsOnce()
            if (_activeSmsPrompt.value == null) {
                _activeSmsPrompt.value = pending.firstOrNull()
            }
        }
    }

    fun refreshDeviceCalendar() {
        viewModelScope.launch {
            val profile = preferences.currentProfile()
            val permitted = deviceCalendar.hasPermission()
            val sources = if (permitted) {
                withContext(Dispatchers.IO) { deviceCalendar.listCalendars() }
            } else emptyList()
            _deviceCalendars.value = sources
            if (!profile.calendarSyncEnabled || !permitted || !profile.calendarSourcesPicked) {
                deviceEvents.value = emptyList()
                return@launch
            }
            val ids = profile.selectedCalendarIdSet.intersect(sources.map { it.id }.toSet())
            val now = System.currentTimeMillis()
            val from = now - 60L * 24 * 60 * 60 * 1000
            val to = now + 400L * 24 * 60 * 60 * 1000
            deviceEvents.value = withContext(Dispatchers.IO) {
                deviceCalendar.queryVisibleEvents(from, to, ids)
            }
        }
    }

    fun setCalendarSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setCalendarSyncEnabled(enabled)
            if (enabled) refreshDeviceCalendar()
            else deviceEvents.value = emptyList()
        }
    }

    fun setSelectedCalendarSources(ids: Set<Long>, notify: Boolean = true) {
        viewModelScope.launch {
            preferences.setSelectedCalendarSources(ids, picked = true)
            if (notify) {
                _snackbarMessage.value = when (ids.size) {
                    0 -> "Phone events hidden"
                    1 -> "1 calendar synced"
                    else -> "${ids.size} calendars synced"
                }
            }
            refreshDeviceCalendar()
        }
    }

    fun toggleCalendarSource(id: Long, enabled: Boolean) {
        viewModelScope.launch {
            val profile = preferences.currentProfile()
            val sources = _deviceCalendars.value.ifEmpty {
                if (deviceCalendar.hasPermission()) {
                    withContext(Dispatchers.IO) { deviceCalendar.listCalendars() }
                } else emptyList()
            }
            val base = if (profile.calendarSourcesPicked) {
                profile.selectedCalendarIdSet
            } else {
                DeviceCalendars.suggestedIds(sources)
            }
            val next = base.toMutableSet()
            if (enabled) next += id else next -= id
            setSelectedCalendarSources(next, notify = false)
        }
    }

    fun setSmsImportEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setSmsImportEnabled(enabled)
            if (enabled) {
                refreshSmsImports()
            } else {
                _activeSmsPrompt.value = null
            }
        }
    }

    /** Accept detected SMS → add Budget line (expense or income) and match bank account if possible. */
    fun acceptSmsTransaction(item: PendingSmsTransactionEntity, manualAccountId: Int? = null) {
        viewModelScope.launch {
            val parsed = SmsTransactionParser.parse(item.smsBody, item.smsSender)
            val type = if (item.isExpense) TransactionType.EXPENSE else TransactionType.INCOME
            val category = parsed?.category?.takeIf { it != "Other" }
                ?: if (item.isExpense) "Other" else "Other"
            val merchant = item.merchantOrInfo.ifBlank { parsed?.merchantOrInfo.orEmpty() }
            val title = buildString {
                append(item.bankName)
                if (merchant.isNotBlank()) {
                    append(" · ")
                    append(merchant.take(28))
                }
            }
            val note = buildString {
                append("From SMS")
                if (item.smsSender.isNotBlank()) append(" · ${item.smsSender}")
                parsed?.refId?.takeIf { it.isNotBlank() }?.let { append(" · Ref $it") }
            }
            val matchedAccountId = manualAccountId ?: matchAccountForBank(
                bankName = item.bankName,
                accountLast4 = parsed?.accountLast4.orEmpty(),
                preferCreditCard = parsed?.isCreditCard
            )
            if (matchedAccountId != null && matchedAccountId > 0) {
                preferences.setLastSmsAccountId(matchedAccountId)
            }
            NotificationHelper.cancelSmsNotification(appContext(), item.smsHash)

            addBudgetItem(
                title = title,
                amount = item.amount,
                isExpense = item.isExpense,
                category = category,
                note = note,
                accountId = matchedAccountId,
                transactionType = type
            )
            repository.markSmsAccepted(item.id)
            SmsImportStore.dismissSemanticTwins(getApplication(), item)
            advanceSmsPrompt(item.id)
            val kind = if (item.isExpense) "expense" else "income"
            _snackbarMessage.value =
                "Added $kind · ${Currencies.format(item.amount, userProfile.value.currencyCode)} (${item.bankName})"
            selectTab(1) // Budget
        }
    }

    fun dismissSmsTransaction(item: PendingSmsTransactionEntity) {
        viewModelScope.launch {
            NotificationHelper.cancelSmsNotification(appContext(), item.smsHash)
            repository.markSmsDismissed(item.id)
            advanceSmsPrompt(item.id)
        }
    }

    private suspend fun advanceSmsPrompt(resolvedId: Int) {
        val next = repository.getPendingSmsOnce().firstOrNull { it.id != resolvedId }
        _activeSmsPrompt.value = next
    }

    /** Link SMS to a PixiDo account: card vs bank first, then last-4 / name / last used. */
    private fun matchAccountForBank(
        bankName: String,
        accountLast4: String = "",
        preferCreditCard: Boolean? = null
    ): Int? {
        return SmsAccountMatcher.defaultAccount(
            accounts = accounts.value,
            bankName = bankName,
            lastAccountId = userProfile.value.lastSmsAccountId,
            accountLast4 = accountLast4,
            preferCreditCard = preferCreditCard
        )?.id
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

    fun setGlassEffectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setGlassEffectEnabled(enabled)
            refreshWidgets()
        }
    }

    fun setNotificationSound(option: NotificationSoundOption) {
        viewModelScope.launch {
            preferences.setNotificationSound(option)
            NotificationHelper.applySoundOption(appContext(), option)
            _snackbarMessage.value = "Reminder sound · ${option.name.lowercase().replaceFirstChar { it.titlecase() }}"
        }
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

            if (newCompleted && task.isRepeating) {
                val rolled = TaskRepeat.rollForward(task, now)
                repository.updateTask(rolled)
                ReminderScheduler.cancelTaskReminders(appContext(), task.id)
                NowBarHelper.clearTaskEta(appContext(), task.id)
                if (rolled.dueDateMillis > now) {
                    ReminderScheduler.scheduleTaskReminders(
                        context = appContext(),
                        itemId = task.id,
                        dueAtMillis = rolled.dueDateMillis,
                        title = rolled.title,
                        body = "It's time for “${rolled.title}” (${rolled.dueTimeStr})"
                    )
                }
                preferences.addXp(task.xpReward)
                bumpLinkedGoal(task, now)
                _snackbarMessage.value = "Done · next ${rolled.dueTimeStr}"
                refreshWidgets()
                return@launch
            }

            val newStreak = if (newCompleted) task.streakCount + 1 else maxOf(1, task.streakCount - 1)
            val updated = task.copy(
                isCompleted = newCompleted,
                streakCount = newStreak,
                completedAtMillis = if (newCompleted) now else null
            )
            repository.updateTask(updated)

            if (newCompleted) {
                ReminderScheduler.cancelTaskReminders(appContext(), task.id)
                NowBarHelper.clearTaskEta(appContext(), task.id)
                preferences.addXp(task.xpReward)
                bumpLinkedGoal(task, now)
            }
            refreshWidgets()
        }
    }

    private suspend fun bumpLinkedGoal(task: TaskEntity, now: Long) {
        task.linkedGoalId?.let { goalId ->
            goals.value.find { it.id == goalId }?.let { targetGoal ->
                if (targetGoal.isCompleted) return
                if (targetGoal.isDailyHabit) {
                    repository.recordGoalProgress(goalId, xpEarned = 10, timestamp = now)
                    refreshWidgets()
                    return
                }
                val next = targetGoal.currentAmount + 1.0
                repository.updateGoal(
                    targetGoal.copy(
                        currentAmount = next,
                        isCompleted = next >= targetGoal.targetAmount
                    )
                )
                repository.recordGoalProgress(goalId, xpEarned = 10, timestamp = now)
            }
        }
    }

    fun toggleTaskPinned(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isPinned = !task.isPinned))
            refreshWidgets()
        }
    }

    /** Skip this occurrence of a repeating task (no streak / XP). */
    fun skipRepeatOccurrence(task: TaskEntity) {
        viewModelScope.launch {
            if (!task.isRepeating || task.isCompleted) return@launch
            val next = TaskRepeat.nextDue(
                task.dueDateMillis,
                task.repeat,
                System.currentTimeMillis()
            )
            val updated = task.copy(
                dueDateMillis = next,
                dueTimeStr = TaskRepeat.dueLabel(next)
            )
            repository.updateTask(updated)
            ReminderScheduler.cancelTaskReminders(appContext(), task.id)
            NowBarHelper.clearTaskEta(appContext(), task.id)
            if (next > System.currentTimeMillis()) {
                ReminderScheduler.scheduleTaskReminders(
                    context = appContext(),
                    itemId = task.id,
                    dueAtMillis = next,
                    title = updated.title,
                    body = "It's time for “${updated.title}” (${updated.dueTimeStr})"
                )
            }
            _snackbarMessage.value = "Skipped · next ${updated.dueTimeStr}"
            refreshWidgets()
        }
    }

    fun toggleSubtask(task: TaskEntity, subtask: String) {
        viewModelScope.launch {
            val name = TaskPhases.nameOf(subtask)
            if (name.isBlank()) return@launch
            val currentCompleted = TaskPhases.names(task.completedSubtasks).toMutableSet()
            if (!currentCompleted.add(name)) {
                currentCompleted.remove(name)
            }
            repository.updateTask(task.copy(completedSubtasks = currentCompleted.joinToString(";")))
        }
    }

    /** Move the due day, keeping the existing time of day. */
    fun rescheduleTask(task: TaskEntity, dayStartMillis: Long) {
        viewModelScope.launch {
            if (task.isCompleted) return@launch
            val newDue = DayTime.withTimeFrom(dayStartMillis, task.dueDateMillis)
            val todayStart = DayTime.startOfDay(System.currentTimeMillis())
            val offset = DayTime.daysBetween(todayStart, DayTime.startOfDay(newDue))
            val dayLabel = when (offset) {
                0 -> "Today"
                1 -> "Tomorrow"
                -1 -> "Yesterday"
                else -> java.text.SimpleDateFormat("EEE, MMM d", java.util.Locale.getDefault())
                    .format(java.util.Date(newDue))
            }
            val updated = task.copy(
                dueDateMillis = newDue,
                dueTimeStr = "$dayLabel · ${ReminderScheduler.formatTime(newDue)}"
            )
            repository.updateTask(updated)
            ReminderScheduler.cancelTaskReminders(appContext(), task.id)
            NowBarHelper.clearTaskEta(appContext(), task.id)
            if (newDue > System.currentTimeMillis()) {
                ReminderScheduler.scheduleTaskReminders(
                    context = appContext(),
                    itemId = task.id,
                    dueAtMillis = newDue,
                    title = updated.title,
                    body = "It's time for “${updated.title}” (${updated.dueTimeStr})"
                )
            }
            _snackbarMessage.value = "Due $dayLabel"
            refreshWidgets()
        }
    }

    fun rewriteSubtasks(task: TaskEntity, subtasks: String) {
        viewModelScope.launch {
            val names = TaskPhases.names(subtasks).toSet()
            val completed = TaskPhases.names(task.completedSubtasks).filter { it in names }
            repository.updateTask(
                task.copy(
                    subtasks = subtasks,
                    completedSubtasks = completed.joinToString(";")
                )
            )
        }
    }

    fun addTask(
        title: String,
        category: String,
        priority: String,
        dueTimeStr: String,
        dueDateMillis: Long,
        subtasks: String,
        linkedGoalId: Int?,
        repeatRule: String = RepeatRule.NONE.name,
        isPinned: Boolean = false,
        notes: String = ""
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
                },
                repeatRule = RepeatRule.from(repeatRule).name,
                isPinned = isPinned,
                notes = notes.trim()
            )
            val newId = repository.addTask(task).toInt()
            if (task.dueDateMillis > System.currentTimeMillis()) {
                ReminderScheduler.scheduleTaskReminders(
                    context = appContext(),
                    itemId = newId,
                    dueAtMillis = task.dueDateMillis,
                    title = task.title,
                    body = "It's time for “${task.title}” (${task.dueTimeStr})"
                )
            }
            _snackbarMessage.value = "Task saved · shows on calendar"
            refreshWidgets()
        }
    }

    fun updateTask(
        taskId: Int,
        title: String,
        category: String,
        priority: String,
        dueTimeStr: String,
        dueDateMillis: Long,
        subtasks: String,
        linkedGoalId: Int?,
        repeatRule: String = RepeatRule.NONE.name,
        isPinned: Boolean = false,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val existing = tasks.value.find { it.id == taskId } ?: return@launch
            val updated = existing.copy(
                title = title.ifBlank { existing.title },
                category = category,
                priority = priority,
                dueTimeStr = dueTimeStr.ifBlank { existing.dueTimeStr },
                dueDateMillis = if (dueDateMillis > 0) dueDateMillis else existing.dueDateMillis,
                subtasks = subtasks,
                linkedGoalId = linkedGoalId,
                xpReward = when (priority) {
                    "HIGH_FIRE" -> 40
                    "CORE_GOAL" -> 30
                    else -> 20
                },
                repeatRule = RepeatRule.from(repeatRule).name,
                isPinned = isPinned,
                notes = notes.trim()
            )
            repository.updateTask(updated)
            ReminderScheduler.cancelTaskReminders(appContext(), taskId)
            NowBarHelper.clearTaskEta(appContext(), taskId)
            if (!updated.isCompleted && updated.dueDateMillis > System.currentTimeMillis()) {
                ReminderScheduler.scheduleTaskReminders(
                    context = appContext(),
                    itemId = taskId,
                    dueAtMillis = updated.dueDateMillis,
                    title = updated.title,
                    body = "It's time for “${updated.title}” (${updated.dueTimeStr})"
                )
            }
            _snackbarMessage.value = "Task updated"
            refreshWidgets()
        }
    }

    /** Push due date by one day and reschedule the reminder. */
    fun snoozeTask(task: TaskEntity, days: Int = 1) {
        viewModelScope.launch {
            if (task.isCompleted) return@launch
            val newDue = task.dueDateMillis + days * 24L * 60 * 60 * 1000
            val label = when (days) {
                1 -> "Tomorrow"
                else -> "In $days days"
            }
            val timePart = ReminderScheduler.formatTime(newDue)
            val updated = task.copy(
                dueDateMillis = newDue,
                dueTimeStr = "$label · $timePart"
            )
            repository.updateTask(updated)
            ReminderScheduler.cancelTaskReminders(appContext(), task.id)
            NowBarHelper.clearTaskEta(appContext(), task.id)
            if (newDue > System.currentTimeMillis()) {
                ReminderScheduler.scheduleTaskReminders(
                    context = appContext(),
                    itemId = task.id,
                    dueAtMillis = newDue,
                    title = updated.title,
                    body = "It's time for “${updated.title}” (${updated.dueTimeStr})"
                )
            }
            _snackbarMessage.value = "Snoozed · $label"
            refreshWidgets()
        }
    }

    /** Snooze a due task by a few minutes (ETA popup). */
    fun snoozeTaskMinutes(task: TaskEntity, minutes: Int = 10) {
        viewModelScope.launch {
            if (task.isCompleted) return@launch
            val newDue = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
            val timePart = ReminderScheduler.formatTime(newDue)
            val updated = task.copy(
                dueDateMillis = newDue,
                dueTimeStr = "Snoozed · $timePart"
            )
            repository.updateTask(updated)
            ReminderScheduler.cancelTaskReminders(appContext(), task.id)
            NowBarHelper.clearTaskEta(appContext(), task.id)
            ReminderScheduler.scheduleTaskReminders(
                context = appContext(),
                itemId = task.id,
                dueAtMillis = newDue,
                title = updated.title,
                body = "It's time for “${updated.title}” (${updated.dueTimeStr})"
            )
            _snackbarMessage.value = "Snoozed · $minutes min"
            refreshWidgets()
        }
    }

    fun clearCompletedTasks() {
        viewModelScope.launch {
            val done = tasks.value.filter { it.isCompleted }
            if (done.isEmpty()) return@launch
            done.forEach { task ->
                ReminderScheduler.cancelTaskReminders(appContext(), task.id)
                NowBarHelper.clearTaskEta(appContext(), task.id)
                repository.deleteTask(task.id)
            }
            _snackbarMessage.value = "Cleared ${done.size} completed"
            refreshWidgets()
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            lastDeletedTask = tasks.value.find { it.id == taskId }
            ReminderScheduler.cancelTaskReminders(appContext(), taskId)
            NowBarHelper.clearTaskEta(appContext(), taskId)
            repository.deleteTask(taskId)
            _snackbarMessage.value = "Task deleted · undo"
            refreshWidgets()
        }
    }

    fun expireDeletedTask() {
        lastDeletedTask = null
    }

    fun undoDeleteTask() {
        viewModelScope.launch {
            lastDeletedTask?.let { task ->
                val restored = task.copy(id = 0)
                val newId = repository.addTask(restored).toInt()
                if (!restored.isCompleted && restored.dueDateMillis > System.currentTimeMillis()) {
                    ReminderScheduler.scheduleTaskReminders(
                        context = appContext(),
                        itemId = newId,
                        dueAtMillis = restored.dueDateMillis,
                        title = restored.title,
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
        notes: String = "",
        cardNetwork: String = "",
        lastFour: String = "",
        expiryMonth: Int = 0,
        expiryYear: Int = 0,
        cardholderName: String = ""
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
                    notes = notes,
                    cardNetwork = if (isCard) cardNetwork else "",
                    lastFour = if (isCard) lastFour.filter { it.isDigit() }.takeLast(4) else "",
                    expiryMonth = if (isCard) expiryMonth else 0,
                    expiryYear = if (isCard) expiryYear else 0,
                    cardholderName = if (isCard) cardholderName else ""
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
        if (DeviceCalendarRepository.isDeviceEvent(event.id)) return
        viewModelScope.launch {
            val done = !event.isCompleted
            repository.updateCalendarEvent(event.copy(isCompleted = done))
            if (done) {
                ReminderScheduler.cancel(appContext(), ReminderScheduler.TYPE_EVENT, event.id)
            }
        }
    }

    fun deleteCalendarEvent(eventId: Int) {
        if (DeviceCalendarRepository.isDeviceEvent(eventId)) return
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
        colorHex: String,
        isSimple: Boolean = false,
        isHabit: Boolean = false
    ) {
        viewModelScope.launch {
            val habit = isHabit ||
                unit.equals("habit", ignoreCase = true) ||
                isSimple ||
                unit.equals("done", ignoreCase = true) ||
                unit.equals("yes", ignoreCase = true)
            val simple = isSimple || habit
            val currencyUnit = when {
                habit -> "habit"
                unit == "$" || unit.equals("money", ignoreCase = true) ->
                    Currencies.symbolOf(userProfile.value.currencyCode)
                else -> unit.ifBlank { Currencies.symbolOf(userProfile.value.currencyCode) }
            }
            repository.addGoal(
                GoalEntity(
                    title = title.ifBlank { if (habit) "New habit" else "New Goal" },
                    category = category,
                    targetAmount = if (habit) 1.0 else maxOf(1.0, targetAmount),
                    unit = currencyUnit,
                    deadlineStr = deadlineStr.ifBlank { if (habit) "Daily" else "2027" },
                    colorHex = colorHex,
                    isSimple = simple,
                    isHabit = habit
                )
            )
        }
    }

    /** Check or uncheck a habit for a calendar day (yyyy-MM-dd). Future days are ignored. */
    fun toggleHabitDay(goal: GoalEntity, dateKey: String = AuraRepository.dayKey()) {
        viewModelScope.launch {
            val today = AuraRepository.dayKey()
            if (dateKey > today) return@launch
            val marked = repository.toggleHabitDay(goal.id, dateKey)
            if (marked) preferences.addXp(12)
            refreshWidgets()
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
                repository.recordGoalProgress(goal.id, xpEarned = 15)
                refreshWidgets()
            }
        }
    }

    /** One-tap complete / undo for simple YES DONE goals. */
    fun completeSimpleGoal(goal: GoalEntity) {
        viewModelScope.launch {
            if (goal.isCompleted) {
                repository.updateGoal(
                    goal.copy(currentAmount = 0.0, isCompleted = false)
                )
            } else {
                repository.updateGoal(
                    goal.copy(
                        currentAmount = goal.targetAmount.coerceAtLeast(1.0),
                        isCompleted = true
                    )
                )
                preferences.addXp(20)
                repository.recordGoalProgress(goal.id, xpEarned = 20)
                refreshWidgets()
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
        val total = minutes * 60
        // Resume from remaining time if already mid-session and not zero
        val left = if (_focusSecondsLeft.value in 1 until total && !_isFocusTimerRunning.value) {
            _focusSecondsLeft.value
        } else {
            total
        }
        focusTotalSeconds = total
        _focusSecondsLeft.value = left
        _isFocusTimerRunning.value = true

        // Immediate Live Update / Now Bar post from UI process (don't wait for FGS)
        val endAt = System.currentTimeMillis() + left * 1000L
        NowBarHelper.ensureChannel(appContext())
        NowBarHelper.showFocus(
            context = appContext(),
            secondsLeft = left,
            totalSeconds = total,
            isRunning = true,
            endAtMillis = endAt
        )
        if (!NowBarHelper.canPostPromoted(appContext())) {
            _snackbarMessage.value =
                "Enable Live updates for PixiDo in notification settings for Now Bar"
        }

        // Service keeps countdown alive in background + refreshes the card
        FocusTimerService.start(appContext(), left, total)
    }

    fun pauseFocusTimer() {
        _isFocusTimerRunning.value = false
        timerJob?.cancel()
        FocusTimerService.pause(appContext())
    }

    fun resetFocusTimer(minutes: Int = 25) {
        timerJob?.cancel()
        FocusTimerService.stop(appContext())
        _isFocusTimerRunning.value = false
        focusTotalSeconds = minutes * 60
        _focusSecondsLeft.value = minutes * 60
        NowBarHelper.clearFocus(appContext())
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(focusStateReceiver) }
        super.onCleared()
    }
}
