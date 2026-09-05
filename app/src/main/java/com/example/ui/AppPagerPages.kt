package com.example.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.TaskEntity
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.TasksScreen
import java.util.Calendar

/**
 * Each tab collects only the data it shows so a timer tick, snackbar, or
 * wallet edit cannot recompose the tasks list (and vice versa).
 */
@Composable
internal fun TasksPagerPage(
    viewModel: AuraViewModel,
    onEditTask: (TaskEntity) -> Unit,
    onAddTask: (dueMillis: Long?) -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val sound = LocalSoundEngine.current
    TasksScreen(
        tasks = tasks,
        goals = goals,
        notes = notes,
        profile = profile,
        onToggleTask = { viewModel.toggleTaskCompletion(it) },
        onToggleSubtask = { task, subtask -> viewModel.toggleSubtask(task, subtask) },
        onDeleteTask = { viewModel.deleteTask(it) },
        onEditTask = onEditTask,
        onSnoozeTask = { viewModel.snoozeTask(it) },
        onPinTask = { viewModel.toggleTaskPinned(it) },
        onSkipRepeat = { viewModel.skipRepeatOccurrence(it) },
        onRescheduleTask = { task, day -> viewModel.rescheduleTask(task, day) },
        onRewriteSubtasks = { task, encoded -> viewModel.rewriteSubtasks(task, encoded) },
        onOpenAddTask = { onAddTask(null) },
        onCompleteSimpleGoal = { goal ->
            if (goal.isCompleted) sound.play(Sfx.TASK_UNDO)
            else sound.play(Sfx.GOAL_COMPLETE)
            viewModel.completeSimpleGoal(goal)
        },
        onDeleteSimpleGoal = {
            sound.play(Sfx.DELETE)
            viewModel.deleteGoal(it)
        },
        onQuickAddTask = { title, dueMillis ->
            val due = if (dueMillis > 0) dueMillis
            else Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 18)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            viewModel.addTask(
                title = title,
                category = "Personal",
                priority = "QUICK_WIN",
                dueTimeStr = "Quick add",
                dueDateMillis = due,
                subtasks = "",
                linkedGoalId = null
            )
        },
        onOpenFocusMode = { viewModel.openFocusModal() },
        onOpenProfile = { viewModel.openProfile() },
        onAddNote = { content, color -> viewModel.addNote(content, color) },
        onToggleNotePin = { viewModel.toggleNotePin(it) },
        onDeleteNote = { viewModel.deleteNote(it) },
        onClearCompleted = { viewModel.clearCompletedTasks() }
    )
}

@Composable
internal fun BudgetPagerPage(
    viewModel: AuraViewModel,
    onAddBudget: () -> Unit
) {
    val budgetItems by viewModel.budgetItems.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val sound = LocalSoundEngine.current
    BudgetScreen(
        budgetItems = budgetItems,
        accounts = accounts,
        currencyCode = profile.currencyCode,
        monthlyAllowance = profile.monthlyBudgetLimit,
        profile = profile,
        onOpenProfile = { viewModel.openProfile() },
        onDeleteBudgetItem = {
            sound.play(Sfx.DELETE)
            viewModel.deleteBudgetItem(it)
        },
        onOpenAddBudget = onAddBudget,
        onAddAccount = { form ->
            sound.play(Sfx.ADD_ACCOUNT)
            viewModel.addAccount(
                name = form.name,
                type = form.type,
                balance = form.balance,
                creditLimit = form.creditLimit,
                colorHex = form.colorHex,
                cardNetwork = form.cardNetwork,
                lastFour = form.lastFour,
                expiryMonth = form.expiryMonth,
                expiryYear = form.expiryYear,
                cardholderName = form.cardholderName
            )
        },
        onEditAccount = { account ->
            sound.play(Sfx.SETTINGS_CHANGE)
            viewModel.updateAccount(account)
        },
        onDeleteAccount = {
            sound.play(Sfx.DELETE)
            viewModel.deleteAccount(it)
        },
        onTransfer = { from, to, amount, note ->
            viewModel.transferBetweenAccounts(from, to, amount, note)
        },
        onSetCurrency = {
            sound.play(Sfx.SETTINGS_CHANGE)
            viewModel.setCurrency(it)
        },
        onSetMonthlyLimit = {
            sound.play(Sfx.SETTINGS_CHANGE)
            viewModel.setMonthlyBudgetLimit(it)
        }
    )
}

@Composable
internal fun CalendarPagerPage(
    viewModel: AuraViewModel,
    onEditTask: (TaskEntity) -> Unit,
    onAddTask: (dueMillis: Long?) -> Unit,
    onAddEvent: () -> Unit
) {
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val selectedCalendarDate by viewModel.selectedCalendarDate.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val sound = LocalSoundEngine.current
    CalendarScreen(
        events = calendarEvents,
        tasks = tasks,
        selectedDateMillis = selectedCalendarDate,
        profile = profile,
        onOpenProfile = { viewModel.openProfile() },
        onSelectDate = {
            sound.play(Sfx.DAY_SELECT)
            viewModel.setSelectedCalendarDate(it)
        },
        onToggleEvent = {
            sound.play(Sfx.EVENT_TOGGLE)
            viewModel.toggleCalendarEventCompleted(it)
        },
        onDeleteEvent = {
            sound.play(Sfx.DELETE)
            viewModel.deleteCalendarEvent(it)
        },
        onOpenAddEvent = onAddEvent,
        onOpenAddTask = { onAddTask(selectedCalendarDate) },
        onToggleTask = { viewModel.toggleTaskCompletion(it) },
        onEditTask = onEditTask
    )
}

@Composable
internal fun GoalsPagerPage(
    viewModel: AuraViewModel,
    onAddGoal: () -> Unit
) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val goalActivity by viewModel.goalActivity.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val sound = LocalSoundEngine.current
    GoalsScreen(
        goals = goals,
        currencyCode = profile.currencyCode,
        goalActivity = goalActivity,
        profile = profile,
        onOpenProfile = { viewModel.openProfile() },
        onUpdateGoalProgress = { goal, delta ->
            val willComplete = delta > 0 && goal.currentAmount + delta >= goal.targetAmount
            if (willComplete) sound.play(Sfx.GOAL_COMPLETE)
            else sound.play(Sfx.GOAL_PROGRESS)
            viewModel.updateGoalProgress(goal, delta)
        },
        onToggleHabit = { goal, dateKey ->
            viewModel.toggleHabitDay(goal, dateKey)
        },
        onDeleteGoal = {
            sound.play(Sfx.DELETE)
            viewModel.deleteGoal(it)
        },
        onOpenAddGoal = onAddGoal
    )
}
