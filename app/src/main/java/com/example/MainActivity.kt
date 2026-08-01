package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.LocalSoundEngine
import com.example.audio.ProvideSoundEngine
import com.example.audio.Sfx
import com.example.notify.NotificationHelper
import com.example.ui.AuraViewModel
import com.example.ui.components.AddBudgetDialog
import com.example.ui.components.AddEventDialog
import com.example.ui.components.AddGoalDialog
import com.example.ui.components.AddTaskDialog
import com.example.ui.components.AutoHideBottomNavigation
import com.example.ui.components.FocusTimerModal
import com.example.ui.components.StartupSplash
import com.example.ui.components.rememberScrollHideBarState
import com.example.ui.components.scrollHideNestedConnection
import com.example.ui.screens.BudgetScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.ProfileDialog
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.PixiDoTheme
import com.example.widget.WidgetActions
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var pendingWidgetAction by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferHighRefreshRate()
        NotificationHelper.ensureChannels(this)
        pendingWidgetAction = intent?.getStringExtra(com.example.widget.WidgetActions.EXTRA_ACTION)
        setContent {
            val viewModel: AuraViewModel = viewModel()
            val profile by viewModel.userProfile.collectAsStateWithLifecycle()
            PixiDoTheme(
                themeOption = profile.themeOption,
                accentColorHex = profile.accentColorHex
            ) {
                ProvideSoundEngine(
                    enabled = profile.soundEnabled,
                    hapticsEnabled = profile.hapticsEnabled
                ) {
                    PixiDoApp(
                        viewModel = viewModel,
                        pendingWidgetAction = pendingWidgetAction,
                        onWidgetActionConsumed = { pendingWidgetAction = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetAction = intent.getStringExtra(com.example.widget.WidgetActions.EXTRA_ACTION)
    }

    /**
     * Prefer the highest supported refresh rate (90 / 120 Hz when available)
     * so Compose animations and scroll stay buttery.
     */
    private fun preferHighRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val d = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                display
            } else {
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val modes = d.supportedModes
                // Pick highest refresh rate mode with reasonable resolution match
                val best = modes.maxWithOrNull(
                    compareBy<android.view.Display.Mode> { it.refreshRate }
                        .thenBy { it.physicalWidth * it.physicalHeight }
                ) ?: return
                if (best.refreshRate >= 89f) {
                    val lp = window.attributes
                    lp.preferredDisplayModeId = best.modeId
                    window.attributes = lp
                }
            }
        } catch (_: Exception) {
            // Devices without multi-mode displays are fine at 60 Hz
        }
    }
}

@Composable
fun PixiDoApp(
    viewModel: AuraViewModel = viewModel(),
    pendingWidgetAction: String? = null,
    onWidgetActionConsumed: () -> Unit = {}
) {
    val sound = LocalSoundEngine.current
    val context = LocalContext.current

    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val budgetItems by viewModel.budgetItems.collectAsStateWithLifecycle()
    val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val dailyActivity by viewModel.dailyActivity.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val selectedCalendarDate by viewModel.selectedCalendarDate.collectAsStateWithLifecycle()

    val showFocusModal by viewModel.showFocusModal.collectAsStateWithLifecycle()
    val focusSecondsLeft by viewModel.focusSecondsLeft.collectAsStateWithLifecycle()
    val isFocusTimerRunning by viewModel.isFocusTimerRunning.collectAsStateWithLifecycle()
    val showProfile by viewModel.showProfile.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showAddBudgetDialog by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }

    // Deep links from home-screen widgets
    LaunchedEffect(pendingWidgetAction) {
        val action = pendingWidgetAction ?: return@LaunchedEffect
        // Skip splash so dialogs appear immediately
        showSplash = false
        when (action) {
            WidgetActions.ACTION_ADD_TASK -> {
                viewModel.selectTab(0)
                showAddTaskDialog = true
            }
            WidgetActions.ACTION_OPEN_FOCUS -> {
                viewModel.selectTab(0)
                viewModel.openFocusModal()
            }
            WidgetActions.ACTION_OPEN_TASKS -> viewModel.selectTab(0)
            WidgetActions.ACTION_OPEN_BUDGET -> viewModel.selectTab(1)
            else -> { /* open app only */ }
        }
        onWidgetActionConsumed()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val reduceMotion = profile.reduceMotion
    val scope = rememberCoroutineScope()

    // Bottom nav hide — isolated state so scroll never recomposes the whole app tree
    val scrollHideBar = rememberScrollHideBarState()
    val nestedScrollConnection = remember(scrollHideBar) {
        scrollHideNestedConnection(scrollHideBar)
    }
    // Fixed bottom inset: no layout thrash while the bar slides (GPU-only transform)
    val contentBottomPad = 88.dp

    // Soft scale-in of main content after splash (splash only)
    val contentAlpha by animateFloatAsState(
        targetValue = if (showSplash) 0.0f else 1f,
        animationSpec = tween(durationMillis = 320),
        label = "contentAlpha"
    )
    val contentScale by animateFloatAsState(
        targetValue = if (showSplash) 0.97f else 1f,
        animationSpec = tween(durationMillis = 360),
        label = "contentScale"
    )

    // Swipeable tabs (left / right)
    val pagerState = rememberPagerState(
        initialPage = selectedTab,
        pageCount = { 4 }
    )

    // Bottom nav / code → pager
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) {
            if (reduceMotion) pagerState.scrollToPage(selectedTab)
            else pagerState.animateScrollToPage(selectedTab)
        }
    }

    // Swipe → selected tab
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                if (page != selectedTab) {
                    sound.playTab(page)
                    viewModel.selectTab(page)
                }
            }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* granted or not — scheduling still works; display needs grant */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    fun openAddForCurrentTab() {
        sound.play(Sfx.FAB)
        sound.play(Sfx.DIALOG_OPEN)
        when (selectedTab) {
            0 -> showAddTaskDialog = true
            1 -> showAddBudgetDialog = true
            2 -> showAddEventDialog = true
            3 -> showAddGoalDialog = true
        }
    }

    fun selectTab(index: Int) {
        scrollHideBar.snapShow()
        if (index != selectedTab) {
            sound.playTab(index)
            viewModel.selectTab(index)
            scope.launch {
                if (reduceMotion) pagerState.scrollToPage(index)
                else pagerState.animateScrollToPage(index)
            }
        } else {
            sound.play(Sfx.TAP_SOFT)
        }
    }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        if (msg.contains("Focus complete", ignoreCase = true)) {
            sound.play(Sfx.FOCUS_COMPLETE)
        }
        val result = snackbarHostState.showSnackbar(
            message = msg,
            actionLabel = if (msg.contains("undo", ignoreCase = true)) "Undo" else null,
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            sound.play(Sfx.SUCCESS)
            viewModel.undoDeleteTask()
        }
        viewModel.clearSnackbar()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = contentAlpha
                    scaleX = contentScale
                    scaleY = contentScale
                }
                .background(MaterialTheme.colorScheme.background),
            containerColor = MaterialTheme.colorScheme.background,
            // Bottom bar is overlaid so it can slide away without leaving a gap
            bottomBar = {},
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        actionColor = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    )
                }
            }
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(nestedScrollConnection)
                    .padding(top = innerPadding.calculateTopPadding())
                    .padding(bottom = contentBottomPad)
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(MaterialTheme.colorScheme.background),
                // Keep only the current offscreen page to cut composition cost
                beyondViewportPageCount = 0,
                userScrollEnabled = !showSplash
            ) { page ->
                when (page) {
                    0 -> TasksScreen(
                        tasks = tasks,
                        goals = goals,
                        notes = notes,
                        userXp = profile.userXp,
                        profile = profile,
                        activity = dailyActivity,
                        onToggleTask = { viewModel.toggleTaskCompletion(it) },
                        onToggleSubtask = { task, subtask -> viewModel.toggleSubtask(task, subtask) },
                        onDeleteTask = { viewModel.deleteTask(it) },
                        onOpenAddTask = {
                            sound.play(Sfx.DIALOG_OPEN)
                            showAddTaskDialog = true
                        },
                        onQuickAddTask = { title ->
                            val dayStart = java.util.Calendar.getInstance().apply {
                                set(java.util.Calendar.HOUR_OF_DAY, 0)
                                set(java.util.Calendar.MINUTE, 0)
                                set(java.util.Calendar.SECOND, 0)
                                set(java.util.Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            // Due later today so it lands on "today"
                            val due = dayStart + 18L * 60 * 60 * 1000
                            viewModel.addTask(
                                title = title,
                                category = "Personal",
                                priority = "QUICK_WIN",
                                dueTimeStr = "Today · quick add",
                                dueDateMillis = due,
                                subtasks = "",
                                linkedGoalId = null
                            )
                        },
                        onOpenFocusMode = { viewModel.openFocusModal() },
                        onOpenProfile = { viewModel.openProfile() },
                        onAddNote = { content, color -> viewModel.addNote(content, color) },
                        onToggleNotePin = { viewModel.toggleNotePin(it) },
                        onDeleteNote = { viewModel.deleteNote(it) }
                    )

                    1 -> BudgetScreen(
                        budgetItems = budgetItems,
                        accounts = accounts,
                        currencyCode = profile.currencyCode,
                        monthlyAllowance = profile.monthlyBudgetLimit,
                        onDeleteBudgetItem = {
                            sound.play(Sfx.DELETE)
                            viewModel.deleteBudgetItem(it)
                        },
                        onOpenAddBudget = {
                            sound.play(Sfx.FAB)
                            sound.play(Sfx.DIALOG_OPEN)
                            showAddBudgetDialog = true
                        },
                        onAddAccount = { name, type, balance, limit, color ->
                            sound.play(Sfx.ADD_ACCOUNT)
                            viewModel.addAccount(name, type, balance, limit, color)
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

                    2 -> CalendarScreen(
                        events = calendarEvents,
                        selectedDateMillis = selectedCalendarDate,
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
                        onOpenAddEvent = {
                            sound.play(Sfx.FAB)
                            sound.play(Sfx.DIALOG_OPEN)
                            showAddEventDialog = true
                        }
                    )

                    3 -> GoalsScreen(
                        goals = goals,
                        currencyCode = profile.currencyCode,
                        onUpdateGoalProgress = { goal, delta ->
                            val willComplete =
                                delta > 0 && goal.currentAmount + delta >= goal.targetAmount
                            if (willComplete) sound.play(Sfx.GOAL_COMPLETE)
                            else sound.play(Sfx.GOAL_PROGRESS)
                            viewModel.updateGoalProgress(goal, delta)
                        },
                        onDeleteGoal = {
                            sound.play(Sfx.DELETE)
                            viewModel.deleteGoal(it)
                        },
                        onOpenAddGoal = {
                            sound.play(Sfx.FAB)
                            sound.play(Sfx.DIALOG_OPEN)
                            showAddGoalDialog = true
                        }
                    )
                }
            }
        }

        // Isolated auto-hide bar — only this branch recomposes on scroll
        AutoHideBottomNavigation(
            state = scrollHideBar,
            selectedTab = selectedTab,
            onTabSelected = { selectTab(it) },
            onCenterAdd = { openAddForCurrentTab() },
            contentAlpha = contentAlpha,
            reduceMotion = reduceMotion
        )

        if (showSplash) {
            StartupSplash(onFinished = { showSplash = false })
        }
    }

    if (showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = {
                sound.play(Sfx.DIALOG_CLOSE)
                showAddTaskDialog = false
            },
            onAddTask = { title, category, priority, dueTimeStr, dueDateMillis, subtasks, linkedGoalId ->
                sound.play(Sfx.ADD_TASK)
                viewModel.addTask(
                    title, category, priority, dueTimeStr, dueDateMillis, subtasks, linkedGoalId
                )
            }
        )
    }

    if (showAddBudgetDialog) {
        AddBudgetDialog(
            currencyCode = profile.currencyCode,
            accounts = accounts,
            onDismiss = {
                sound.play(Sfx.DIALOG_CLOSE)
                showAddBudgetDialog = false
            },
            onAddBudgetItem = { title, amount, isExpense, category, note, accountId, transactionType ->
                sound.play(Sfx.ADD_BUDGET)
                viewModel.addBudgetItem(
                    title, amount, isExpense, category, note, accountId, transactionType
                )
            }
        )
    }

    if (showAddEventDialog) {
        AddEventDialog(
            selectedDateMillis = selectedCalendarDate,
            onDismiss = {
                sound.play(Sfx.DIALOG_CLOSE)
                showAddEventDialog = false
            },
            onAddEvent = { title, category, dateMillis, timeSlot, startMillis, description ->
                sound.play(Sfx.ADD_EVENT)
                viewModel.addCalendarEvent(
                    title, category, dateMillis, timeSlot, startMillis, description
                )
            }
        )
    }

    if (showAddGoalDialog) {
        AddGoalDialog(
            currencyCode = profile.currencyCode,
            onDismiss = {
                sound.play(Sfx.DIALOG_CLOSE)
                showAddGoalDialog = false
            },
            onAddGoal = { title, category, targetAmount, unit, deadlineStr, colorHex ->
                sound.play(Sfx.ADD_GOAL)
                viewModel.addGoal(title, category, targetAmount, unit, deadlineStr, colorHex)
            }
        )
    }

    if (showFocusModal) {
        FocusTimerModal(
            secondsLeft = focusSecondsLeft,
            isRunning = isFocusTimerRunning,
            onStart = { minutes ->
                sound.play(Sfx.FOCUS_START)
                viewModel.startFocusTimer(minutes)
            },
            onPause = {
                sound.play(Sfx.FOCUS_PAUSE)
                viewModel.pauseFocusTimer()
            },
            onReset = {
                sound.play(Sfx.FOCUS_RESET)
                viewModel.resetFocusTimer()
            },
            onDismiss = {
                sound.play(Sfx.DIALOG_CLOSE)
                viewModel.closeFocusModal()
            }
        )
    }

    if (showProfile) {
        val authBusy by viewModel.authBusy.collectAsStateWithLifecycle()
        val backupBusy by viewModel.backupBusy.collectAsStateWithLifecycle()
        ProfileDialog(
            profile = profile,
            authBusy = authBusy,
            backupBusy = backupBusy,
            onDismiss = {
                sound.play(Sfx.DIALOG_CLOSE)
                viewModel.closeProfile()
            },
            onSaveName = { name ->
                viewModel.updateProfile(name)
            },
            onAvatarPicked = { uri ->
                sound.play(Sfx.TAP_CONFIRM)
                viewModel.setAvatarUri(uri)
            },
            onThemeSelected = { viewModel.setTheme(it) },
            onAccentSelected = { hex ->
                viewModel.setAccentColor(hex)
            },
            onSoundToggle = {
                sound.play(Sfx.SETTINGS_CHANGE)
                viewModel.setSoundEnabled(it)
            },
            onHapticsToggle = {
                sound.play(Sfx.SETTINGS_CHANGE)
                viewModel.setHapticsEnabled(it)
            },
            onGoogleSignIn = {
                viewModel.signInWithGoogle(context)
            },
            onGoogleSignOut = {
                viewModel.signOutGoogle()
            },
            onBackupFrequencyChange = { frequency ->
                viewModel.setBackupFrequency(frequency)
            },
            onBackupNow = {
                viewModel.backupNow()
            },
            onRestoreNow = {
                viewModel.restoreFromCloud()
            }
        )
    }
}

/** Backward-compatible alias. */
@Composable
fun AuraApp(viewModel: AuraViewModel = viewModel()) {
    PixiDoApp(viewModel)
}
