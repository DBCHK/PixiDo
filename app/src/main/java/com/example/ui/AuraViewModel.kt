package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuraDatabase
import com.example.data.AuraRepository
import com.example.data.BudgetItemEntity
import com.example.data.CalendarEventEntity
import com.example.data.GoalEntity
import com.example.data.TaskEntity
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

    val tasks: StateFlow<List<TaskEntity>>
    val budgetItems: StateFlow<List<BudgetItemEntity>>
    val calendarEvents: StateFlow<List<CalendarEventEntity>>
    val goals: StateFlow<List<GoalEntity>>

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _monthlyBudgetAllowance = MutableStateFlow(1200.0)
    val monthlyBudgetAllowance: StateFlow<Double> = _monthlyBudgetAllowance.asStateFlow()

    private val _userXp = MutableStateFlow(180)
    val userXp: StateFlow<Int> = _userXp.asStateFlow()

    private val _selectedCalendarDate = MutableStateFlow(System.currentTimeMillis())
    val selectedCalendarDate: StateFlow<Long> = _selectedCalendarDate.asStateFlow()

    // Focus Mode Timer State
    private val _focusSecondsLeft = MutableStateFlow(25 * 60)
    val focusSecondsLeft: StateFlow<Int> = _focusSecondsLeft.asStateFlow()

    private val _isFocusTimerRunning = MutableStateFlow(false)
    val isFocusTimerRunning: StateFlow<Boolean> = _isFocusTimerRunning.asStateFlow()

    private val _showFocusModal = MutableStateFlow(false)
    val showFocusModal: StateFlow<Boolean> = _showFocusModal.asStateFlow()

    private var timerJob: Job? = null

    init {
        val dao = AuraDatabase.getDatabase(application).auraDao()
        repository = AuraRepository(dao)

        viewModelScope.launch {
            repository.prepopulateIfEmpty()
        }

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
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setSelectedCalendarDate(dateMillis: Long) {
        _selectedCalendarDate.value = dateMillis
    }

    fun updateMonthlyBudget(newLimit: Double) {
        _monthlyBudgetAllowance.value = newLimit
    }

    // --- Task Actions ---
    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            val newCompleted = !task.isCompleted
            val newStreak = if (newCompleted) task.streakCount + 1 else maxOf(1, task.streakCount - 1)
            val updated = task.copy(isCompleted = newCompleted, streakCount = newStreak)
            repository.updateTask(updated)

            if (newCompleted) {
                _userXp.value += task.xpReward
                // If linked goal exists, bump its currentAmount
                task.linkedGoalId?.let { goalId ->
                    val goalList = goals.value
                    goalList.find { it.id == goalId }?.let { targetGoal ->
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

            val updatedTask = task.copy(completedSubtasks = currentCompleted.joinToString(";"))
            repository.updateTask(updatedTask)
        }
    }

    fun addTask(
        title: String,
        category: String,
        priority: String,
        dueTimeStr: String,
        subtasks: String,
        linkedGoalId: Int?
    ) {
        viewModelScope.launch {
            val newTask = TaskEntity(
                title = title.ifBlank { "Untitled Task" },
                category = category,
                priority = priority,
                dueTimeStr = dueTimeStr.ifBlank { "Today" },
                subtasks = subtasks,
                linkedGoalId = linkedGoalId,
                xpReward = when (priority) {
                    "HIGH_FIRE" -> 40
                    "CORE_GOAL" -> 30
                    else -> 20
                }
            )
            repository.addTask(newTask)
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    // --- Budget Actions ---
    fun addBudgetItem(
        title: String,
        amount: Double,
        isExpense: Boolean,
        category: String,
        note: String
    ) {
        viewModelScope.launch {
            val newItem = BudgetItemEntity(
                title = title.ifBlank { if (isExpense) "Quick Expense" else "Income" },
                amount = amount,
                isExpense = isExpense,
                category = category,
                note = note
            )
            repository.addBudgetItem(newItem)

            // If expense in "Savings & Wealth 💰", check if we can update savings goals
            if (isExpense && category.contains("Savings")) {
                goals.value.firstOrNull { it.category.contains("Savings") || it.category.contains("Travel") }?.let { goal ->
                    val newAmt = goal.currentAmount + amount
                    repository.updateGoal(goal.copy(currentAmount = newAmt, isCompleted = newAmt >= goal.targetAmount))
                }
            }
        }
    }

    fun deleteBudgetItem(itemId: Int) {
        viewModelScope.launch {
            repository.deleteBudgetItem(itemId)
        }
    }

    // --- Calendar Event Actions ---
    fun addCalendarEvent(
        title: String,
        category: String,
        dateMillis: Long,
        timeSlot: String,
        description: String
    ) {
        viewModelScope.launch {
            val newEvent = CalendarEventEntity(
                title = title.ifBlank { "Event" },
                category = category,
                dateMillis = dateMillis,
                timeSlot = timeSlot.ifBlank { "All Day" },
                description = description
            )
            repository.addCalendarEvent(newEvent)
        }
    }

    fun toggleCalendarEventCompleted(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.updateCalendarEvent(event.copy(isCompleted = !event.isCompleted))
        }
    }

    fun deleteCalendarEvent(eventId: Int) {
        viewModelScope.launch {
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
            val newGoal = GoalEntity(
                title = title.ifBlank { "New Vision Goal" },
                category = category,
                targetAmount = maxOf(1.0, targetAmount),
                unit = unit.ifBlank { "$" },
                deadlineStr = deadlineStr.ifBlank { "2027" },
                colorHex = colorHex
            )
            repository.addGoal(newGoal)
        }
    }

    fun updateGoalProgress(goal: GoalEntity, delta: Double) {
        viewModelScope.launch {
            val newAmt = maxOf(0.0, goal.currentAmount + delta)
            val updated = goal.copy(
                currentAmount = newAmt,
                isCompleted = newAmt >= goal.targetAmount
            )
            repository.updateGoal(updated)
            if (delta > 0) {
                _userXp.value += 15
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
                _userXp.value += 50 // Focus Session Complete reward!
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
