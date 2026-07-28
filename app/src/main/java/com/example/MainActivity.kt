package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AuraViewModel
import com.example.ui.components.AddBudgetDialog
import com.example.ui.components.AddEventDialog
import com.example.ui.components.AddGoalDialog
import com.example.ui.components.AddTaskDialog
import com.example.ui.components.AuraBottomNavigation
import com.example.ui.components.FocusTimerModal
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.AuraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuraTheme {
                AuraApp()
            }
        }
    }
}

@Composable
fun AuraApp(viewModel: AuraViewModel = viewModel()) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val budgetItems by viewModel.budgetItems.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedCalendarDate by viewModel.selectedCalendarDate.collectAsStateWithLifecycle()
    val userXp by viewModel.userXp.collectAsStateWithLifecycle()
    val monthlyAllowance by viewModel.monthlyBudgetAllowance.collectAsStateWithLifecycle()

    val showFocusModal by viewModel.showFocusModal.collectAsStateWithLifecycle()
    val focusSecondsLeft by viewModel.focusSecondsLeft.collectAsStateWithLifecycle()
    val isFocusTimerRunning by viewModel.isFocusTimerRunning.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AuraBottomNavigation(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TasksScreen(
                    tasks = tasks,
                    goals = goals,
                    userXp = userXp,
                    onToggleTask = { viewModel.toggleTaskCompletion(it) },
                    onToggleSubtask = { task, subtask -> viewModel.toggleSubtask(task, subtask) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onOpenAddTask = { showAddTaskDialog = true },
                    onOpenFocusMode = { viewModel.openFocusModal() }
                )

                1 -> BudgetScreen(
                    budgetItems = budgetItems,
                    monthlyAllowance = monthlyAllowance,
                    onAddQuickExpense = { name, amount, cat ->
                        viewModel.addBudgetItem(name, amount, true, cat, "Quick log")
                    },
                    onDeleteBudgetItem = { viewModel.deleteBudgetItem(it) },
                    onOpenAddBudget = { showAddBudgetDialog = true }
                )

                2 -> CalendarScreen(
                    events = calendarEvents,
                    selectedDateMillis = selectedCalendarDate,
                    onSelectDate = { viewModel.setSelectedCalendarDate(it) },
                    onToggleEvent = { viewModel.toggleCalendarEventCompleted(it) },
                    onDeleteEvent = { viewModel.deleteCalendarEvent(it) },
                    onOpenAddEvent = { showAddEventDialog = true }
                )

                3 -> GoalsScreen(
                    goals = goals,
                    onUpdateGoalProgress = { goal, delta -> viewModel.updateGoalProgress(goal, delta) },
                    onDeleteGoal = { viewModel.deleteGoal(it) },
                    onOpenAddGoal = { showAddGoalDialog = true }
                )
            }
        }
    }

    // Modal Dialog Overlays
    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onAddTask = { title, category, priority, dueTimeStr, subtasks, linkedGoalId ->
                viewModel.addTask(title, category, priority, dueTimeStr, subtasks, linkedGoalId)
            }
        )
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            onDismiss = { showAddBudgetDialog = false },
            onAddBudgetItem = { title, amount, isExpense, category, note ->
                viewModel.addBudgetItem(title, amount, isExpense, category, note)
            }
        )
    }

    if (showAddEventDialog) {
        AddEventDialog(
            selectedDateMillis = selectedCalendarDate,
            onDismiss = { showAddEventDialog = false },
            onAddEvent = { title, category, dateMillis, timeSlot, description ->
                viewModel.addCalendarEvent(title, category, dateMillis, timeSlot, description)
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            onDismiss = { showAddGoalDialog = false },
            onAddGoal = { title, category, targetAmount, unit, deadlineStr, colorHex ->
                viewModel.addGoal(title, category, targetAmount, unit, deadlineStr, colorHex)
            }
        )
    }

    if (showFocusModal) {
        FocusTimerModal(
            secondsLeft = focusSecondsLeft,
            isRunning = isFocusTimerRunning,
            onStart = { minutes -> viewModel.startFocusTimer(minutes) },
            onPause = { viewModel.pauseFocusTimer() },
            onReset = { viewModel.resetFocusTimer() },
            onDismiss = { viewModel.closeFocusModal() }
        )
    }
}
