package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ActivityInfo
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
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.LocalSoundEngine
import com.example.audio.ProvideSoundEngine
import com.example.audio.Sfx
import com.example.audio.SoundEngine
import com.example.notify.NotificationHelper
import com.example.notify.ReminderScheduler
import com.example.sms.AppForegroundState
import com.example.sms.SmsInboxScanner
import com.example.ui.AuraViewModel
import com.example.ui.BudgetPagerPage
import com.example.ui.CalendarPagerPage
import com.example.ui.GoalsPagerPage
import com.example.ui.TasksPagerPage
import com.example.data.DeviceCalendars
import com.example.ui.components.AddBudgetDialog
import com.example.ui.components.AddEventDialog
import com.example.ui.components.AddGoalDialog
import com.example.ui.components.AddTaskDialog
import com.example.ui.components.CalendarSourcesDialog
import com.example.ui.components.AutoHideBottomNavigation
import com.example.ui.components.FocusTimerModal
import com.example.ui.components.LocalGlassEnabled
import com.example.ui.components.LocalHazeState
import com.example.ui.components.LocalReduceMotion
import com.example.ui.components.PixiGlass
import com.example.ui.components.PixiGlassRole
import com.example.ui.components.PixiPillShape
import com.example.ui.components.ProvideGlassLight
import com.example.ui.components.SmsImportDialog
import com.example.ui.components.StartupSplash
import com.example.ui.components.TaskEtaDialog
import com.example.ui.components.rememberScrollHideBarState
import com.example.ui.components.scrollHideNestedConnection
import com.example.ui.screens.ProfileDialog
import com.example.ui.theme.PixiDoTheme
import com.example.ui.theme.WalletInk
import com.example.widget.WidgetActions
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

data class PendingEtaAlert(
    val title: String,
    val body: String,
    val type: String,
    val itemId: Int
)

class MainActivity : ComponentActivity() {

    private var pendingWidgetAction by mutableStateOf<String?>(null)
    private var pendingEtaAlert by mutableStateOf<PendingEtaAlert?>(null)
    private var pendingSmsOpen by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        preferHighRefreshRate()
        NotificationHelper.ensureChannels(this)
        pendingWidgetAction = intent?.getStringExtra(com.example.widget.WidgetActions.EXTRA_ACTION)
        pendingEtaAlert = intent?.toEtaAlert()
        pendingSmsOpen = intent?.getBooleanExtra(NotificationHelper.EXTRA_SMS_PROMPT, false) == true
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
                        onWidgetActionConsumed = { pendingWidgetAction = null },
                        pendingEtaAlert = pendingEtaAlert,
                        onEtaAlertConsumed = { pendingEtaAlert = null },
                        pendingSmsOpen = pendingSmsOpen,
                        onSmsOpenConsumed = { pendingSmsOpen = false }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        AppForegroundState.isResumed = true
    }

    override fun onStop() {
        AppForegroundState.isResumed = false
        super.onStop()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingWidgetAction = intent.getStringExtra(com.example.widget.WidgetActions.EXTRA_ACTION)
        intent.toEtaAlert()?.let { pendingEtaAlert = it }
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_SMS_PROMPT, false)) {
            pendingSmsOpen = true
        }
    }

    private fun Intent.toEtaAlert(): PendingEtaAlert? {
        if (!getBooleanExtra(NotificationHelper.EXTRA_ETA_POPUP, false)) return null
        return PendingEtaAlert(
            title = getStringExtra(NotificationHelper.EXTRA_ETA_TITLE).orEmpty(),
            body = getStringExtra(NotificationHelper.EXTRA_ETA_BODY).orEmpty(),
            type = getStringExtra(NotificationHelper.EXTRA_ETA_TYPE).orEmpty(),
            itemId = getIntExtra(NotificationHelper.EXTRA_ETA_ITEM_ID, 0)
        )
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

            val current = d.mode
            // Stay at the panel's native resolution; only bump refresh rate.
            // Picking a higher-res mode at 120 Hz with blur is a common stutter source.
            val best = d.supportedModes
                .filter {
                    it.physicalWidth == current.physicalWidth &&
                        it.physicalHeight == current.physicalHeight
                }
                .maxByOrNull { it.refreshRate }
                ?: current
            if (best.refreshRate >= 89f && best.modeId != current.modeId) {
                val lp = window.attributes
                lp.preferredDisplayModeId = best.modeId
                window.attributes = lp
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
    onWidgetActionConsumed: () -> Unit = {},
    pendingEtaAlert: PendingEtaAlert? = null,
    onEtaAlertConsumed: () -> Unit = {},
    pendingSmsOpen: Boolean = false,
    onSmsOpenConsumed: () -> Unit = {}
) {
    val sound = LocalSoundEngine.current
    val context = LocalContext.current

    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val deviceCalendars by viewModel.deviceCalendars.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val showFocusModal by viewModel.showFocusModal.collectAsStateWithLifecycle()
    val showProfile by viewModel.showProfile.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val activeSmsPrompt by viewModel.activeSmsPrompt.collectAsStateWithLifecycle()
    val pendingSmsTransactions by viewModel.pendingSmsTransactions.collectAsStateWithLifecycle()

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<com.example.data.TaskEntity?>(null) }
    var addTaskForDate by remember { mutableStateOf<Long?>(null) }
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
                editingTask = null
                addTaskForDate = null
                showAddTaskDialog = true
            }
            WidgetActions.ACTION_OPEN_FOCUS -> {
                viewModel.selectTab(0)
                viewModel.openFocusModal()
            }
            WidgetActions.ACTION_OPEN_TASKS -> viewModel.selectTab(0)
            WidgetActions.ACTION_OPEN_BUDGET -> viewModel.selectTab(1)
            WidgetActions.ACTION_OPEN_GOALS -> viewModel.selectTab(3)
            else -> { /* open app only */ }
        }
        onWidgetActionConsumed()
    }

    LaunchedEffect(pendingSmsOpen) {
        if (!pendingSmsOpen) return@LaunchedEffect
        showSplash = false
        viewModel.refreshSmsImports()
        onSmsOpenConsumed()
    }

    // Task ETA full-screen / notification deep link → show popup immediately
    var activeEta by remember { mutableStateOf<PendingEtaAlert?>(null) }
    LaunchedEffect(pendingEtaAlert) {
        val eta = pendingEtaAlert ?: return@LaunchedEffect
        showSplash = false
        viewModel.selectTab(0)
        activeEta = eta
        onEtaAlertConsumed()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val hazeState = remember { HazeState() }
    val reduceMotion = profile.reduceMotion
    val scope = rememberCoroutineScope()

    // Bottom nav hide — isolated state so scroll never recomposes the whole app tree
    val scrollHideBar = rememberScrollHideBarState()
    val nestedScrollConnection = remember(scrollHideBar) {
        scrollHideNestedConnection(scrollHideBar)
    }
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

    // Swipeable tabs (left / right) — simple pager, no custom pop transitions
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

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.READ_SMS] == true ||
            result[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            viewModel.refreshSmsImports()
        }
    }

    var smsPermissionAsked by remember { mutableStateOf(false) }
    var calendarPermissionAsked by remember { mutableStateOf(false) }
    var showCalendarPicker by remember { mutableStateOf(false) }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.refreshDeviceCalendar()
        }
    }

    fun requestCalendarPermissionIfNeeded(forceAsk: Boolean = false) {
        if (!forceAsk && !profile.calendarSyncEnabled) return
        val need = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) != PackageManager.PERMISSION_GRANTED
        if (need) {
            if (forceAsk || !calendarPermissionAsked) {
                calendarPermissionAsked = true
                calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR)
            }
        } else {
            viewModel.refreshDeviceCalendar()
        }
    }

    fun requestSmsPermissionsIfNeeded(forceAsk: Boolean = false) {
        // forceAsk: user just turned the toggle on (profile Flow may not have updated yet)
        if (!forceAsk && !profile.smsImportEnabled) return
        val needRead = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_SMS
        ) != PackageManager.PERMISSION_GRANTED
        val needReceive = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECEIVE_SMS
        ) != PackageManager.PERMISSION_GRANTED
        if (needRead || needReceive) {
            if (forceAsk || !smsPermissionAsked) {
                smsPermissionAsked = true
                smsPermissionLauncher.launch(
                    arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                )
            }
        } else {
            viewModel.refreshSmsImports()
        }
    }

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

    // After splash: request SMS (for bank import) once, then scan inbox
    LaunchedEffect(showSplash, profile.smsImportEnabled) {
        if (showSplash) return@LaunchedEffect
        if (profile.smsImportEnabled) {
            requestSmsPermissionsIfNeeded(forceAsk = false)
        }
    }

    LaunchedEffect(showSplash, profile.calendarSyncEnabled) {
        if (showSplash) return@LaunchedEffect
        if (profile.calendarSyncEnabled) {
            requestCalendarPermissionIfNeeded(forceAsk = false)
        }
    }

    LaunchedEffect(
        showSplash,
        profile.calendarSyncEnabled,
        profile.calendarSourcesPicked,
        deviceCalendars.size
    ) {
        if (showSplash) return@LaunchedEffect
        if (profile.calendarSyncEnabled &&
            !profile.calendarSourcesPicked &&
            deviceCalendars.isNotEmpty()
        ) {
            showCalendarPicker = true
        }
    }

    // Re-scan when app returns to foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, profile.smsImportEnabled, profile.calendarSyncEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event != Lifecycle.Event.ON_RESUME) return@LifecycleEventObserver
            if (profile.smsImportEnabled) {
                if (SmsInboxScanner.hasReadPermission(context) ||
                    SmsInboxScanner.hasReceivePermission(context)
                ) {
                    viewModel.refreshSmsImports()
                }
            }
            if (profile.calendarSyncEnabled) {
                viewModel.refreshDeviceCalendar()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openAddForCurrentTab() {
        sound.play(Sfx.FAB)
        sound.play(Sfx.DIALOG_OPEN)
        when (selectedTab) {
            0 -> {
                editingTask = null
                addTaskForDate = null
                showAddTaskDialog = true
            }
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
        val canUndo = msg.contains("undo", ignoreCase = true)
        val display = if (canUndo) "Task deleted" else msg
        val autoDismiss = if (canUndo) {
            launch {
                delay(3_000)
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        } else {
            null
        }
        val result = snackbarHostState.showSnackbar(
            message = display,
            actionLabel = if (canUndo) "Undo" else null,
            duration = if (canUndo) SnackbarDuration.Indefinite else SnackbarDuration.Short
        )
        autoDismiss?.cancel()
        if (result == SnackbarResult.ActionPerformed && canUndo) {
            sound.play(Sfx.SUCCESS)
            viewModel.undoDeleteTask()
        } else if (canUndo) {
            viewModel.expireDeletedTask()
        }
        viewModel.clearSnackbar()
    }

    val glassOn = profile.glassEffectEnabled
    val onPulseTab = selectedTab == 0 || selectedTab == 2
    val schemeBg = MaterialTheme.colorScheme.background
    val canvasColor = if (onPulseTab) schemeBg else WalletInk
    val view = LocalView.current
    val lightStatusBars = onPulseTab && schemeBg.luminance() > 0.5f
    LaunchedEffect(lightStatusBars) {
        val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = lightStatusBars
    }
    val onEditTask = remember(sound) {
        { task: com.example.data.TaskEntity ->
            sound.play(Sfx.DIALOG_OPEN)
            editingTask = task
            addTaskForDate = null
            showAddTaskDialog = true
        }
    }
    val onAddTask = remember(sound) {
        { due: Long? ->
            sound.play(Sfx.DIALOG_OPEN)
            editingTask = null
            addTaskForDate = due
            showAddTaskDialog = true
        }
    }
    val onAddBudget = remember(sound) {
        {
            sound.play(Sfx.FAB)
            sound.play(Sfx.DIALOG_OPEN)
            showAddBudgetDialog = true
        }
    }
    val onAddEvent = remember(sound) {
        {
            sound.play(Sfx.FAB)
            sound.play(Sfx.DIALOG_OPEN)
            showAddEventDialog = true
        }
    }
    val onAddGoal = remember(sound) {
        {
            sound.play(Sfx.FAB)
            sound.play(Sfx.DIALOG_OPEN)
            showAddGoalDialog = true
        }
    }

    CompositionLocalProvider(
        LocalHazeState provides hazeState.takeIf { glassOn },
        LocalGlassEnabled provides glassOn,
        LocalReduceMotion provides reduceMotion
    ) {
    ProvideGlassLight(enabled = glassOn, reduceMotion = reduceMotion) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showSplash) {
                        Modifier.graphicsLayer {
                            alpha = contentAlpha
                            scaleX = contentScale
                            scaleY = contentScale
                        }
                    } else {
                        Modifier
                    }
                )
                .background(canvasColor),
            containerColor = canvasColor,
            // Bottom bar is overlaid so it can slide away without leaving a gap
            bottomBar = {},
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 118.dp)
                ) { data ->
                    PixiGlass(
                        shape = PixiPillShape,
                        role = PixiGlassRole.Chrome,
                        elevation = 12.dp
                    ) {
                        Snackbar(
                            snackbarData = data,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            actionColor = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(50)
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .background(canvasColor)
                    .then(if (glassOn) Modifier.hazeSource(state = hazeState) else Modifier)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection),
                    beyondViewportPageCount = 0,
                    userScrollEnabled = !showSplash
                ) { page ->
                    when (page) {
                        0 -> TasksPagerPage(
                            viewModel = viewModel,
                            onEditTask = onEditTask,
                            onAddTask = onAddTask
                        )
                        1 -> BudgetPagerPage(
                            viewModel = viewModel,
                            onAddBudget = onAddBudget
                        )
                        2 -> CalendarPagerPage(
                            viewModel = viewModel,
                            onEditTask = onEditTask,
                            onAddTask = onAddTask,
                            onAddEvent = onAddEvent
                        )
                        3 -> GoalsPagerPage(
                            viewModel = viewModel,
                            onAddGoal = onAddGoal
                        )
                    }
                }
            }
        }

        if (glassOn) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                canvasColor.copy(alpha = 0.18f)
                            )
                        )
                    )
            )
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

        // In-app iOS-style banner for debit/credit SMS — closeable, non-blocking
        val smsPrompt = activeSmsPrompt
        if (!showSplash && smsPrompt != null && !showProfile) {
            val remaining = (pendingSmsTransactions.size - 1).coerceAtLeast(0)
            val accounts by viewModel.accounts.collectAsStateWithLifecycle()
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                SmsImportDialog(
                    item = smsPrompt,
                    accounts = accounts,
                    currencyCode = profile.currencyCode.ifBlank { "INR" },
                    remainingCount = remaining,
                    lastAccountId = profile.lastSmsAccountId.takeIf { it > 0 },
                    onAccept = { manualId ->
                        sound.play(Sfx.IMPORT)
                        viewModel.acceptSmsTransaction(smsPrompt, manualId)
                    },
                    onDismiss = {
                        sound.play(Sfx.DIALOG_CLOSE)
                        viewModel.dismissSmsTransaction(smsPrompt)
                    }
                )
            }
        }
    }

    if (showAddTaskDialog) {
        val goals by viewModel.goals.collectAsStateWithLifecycle()
        AddTaskDialog(
            onDismiss = {
                sound.play(Sfx.DIALOG_CLOSE)
                showAddTaskDialog = false
                editingTask = null
                addTaskForDate = null
            },
            existingTask = editingTask,
            initialDueDateMillis = addTaskForDate,
            goals = goals,
            onAddTask = { title, category, priority, dueTimeStr, dueDateMillis, subtasks, linkedGoalId, repeatRule, isPinned, notes ->
                sound.play(Sfx.ADD_TASK)
                viewModel.addTask(
                    title, category, priority, dueTimeStr, dueDateMillis, subtasks, linkedGoalId,
                    repeatRule, isPinned, notes
                )
            },
            onUpdateTask = { id, title, category, priority, dueTimeStr, dueDateMillis, subtasks, linkedGoalId, repeatRule, isPinned, notes ->
                sound.play(Sfx.ADD_TASK)
                viewModel.updateTask(
                    id, title, category, priority, dueTimeStr, dueDateMillis, subtasks, linkedGoalId,
                    repeatRule, isPinned, notes
                )
            }
        )
    }

    if (showAddBudgetDialog) {
        val accounts by viewModel.accounts.collectAsStateWithLifecycle()
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
        val selectedCalendarDate by viewModel.selectedCalendarDate.collectAsStateWithLifecycle()
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
            onAddGoal = { title, category, targetAmount, unit, deadlineStr, colorHex, isSimple, isHabit ->
                sound.play(Sfx.ADD_GOAL)
                viewModel.addGoal(
                    title, category, targetAmount, unit, deadlineStr, colorHex, isSimple, isHabit
                )
            }
        )
    }

    if (showFocusModal) {
        FocusTimerHost(viewModel = viewModel, sound = sound)
    }

    // Task ETA popup + custom calm ringtone
    activeEta?.let { eta ->
        val tasks by viewModel.tasks.collectAsStateWithLifecycle()
        val calendarEvents by viewModel.calendarEvents.collectAsStateWithLifecycle()
        TaskEtaDialog(
            title = eta.title.removePrefix("Task due: ").removePrefix("Event: ").trim()
                .ifBlank { eta.title },
            body = eta.body,
            onMarkDone = {
                sound.play(Sfx.TASK_COMPLETE)
                if (eta.type == ReminderScheduler.TYPE_TASK && eta.itemId > 0) {
                    tasks.find { it.id == eta.itemId }?.let { viewModel.toggleTaskCompletion(it) }
                } else if (eta.type == ReminderScheduler.TYPE_EVENT && eta.itemId > 0) {
                    calendarEvents.find { it.id == eta.itemId }?.let {
                        viewModel.toggleCalendarEventCompleted(it)
                    }
                }
                activeEta = null
            },
            onSnooze = {
                sound.play(Sfx.SNOOZE)
                if (eta.type == ReminderScheduler.TYPE_TASK && eta.itemId > 0) {
                    tasks.find { it.id == eta.itemId }?.let { viewModel.snoozeTaskMinutes(it, 10) }
                }
                activeEta = null
            },
            onDismiss = {
                sound.play(Sfx.DIALOG_CLOSE)
                activeEta = null
            }
        )
    }

    if (showCalendarPicker) {
        CalendarSourcesDialog(
            calendars = deviceCalendars,
            selectedIds = if (profile.calendarSourcesPicked) {
                profile.selectedCalendarIdSet
            } else {
                DeviceCalendars.suggestedIds(deviceCalendars)
            },
            onConfirm = { ids ->
                sound.play(Sfx.TAP_CONFIRM)
                viewModel.setSelectedCalendarSources(ids)
                showCalendarPicker = false
            },
            onDismiss = {
                sound.play(Sfx.DIALOG_CLOSE)
                if (!profile.calendarSourcesPicked) {
                    viewModel.setSelectedCalendarSources(
                        DeviceCalendars.suggestedIds(deviceCalendars)
                    )
                }
                showCalendarPicker = false
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
            onGlassEffectToggle = {
                sound.play(Sfx.SETTINGS_CHANGE)
                viewModel.setGlassEffectEnabled(it)
            },
            onSmsImportToggle = { enabled ->
                sound.play(Sfx.SETTINGS_CHANGE)
                viewModel.setSmsImportEnabled(enabled)
                if (enabled) {
                    requestSmsPermissionsIfNeeded(forceAsk = true)
                }
            },
            onCalendarSyncToggle = { enabled ->
                sound.play(Sfx.SETTINGS_CHANGE)
                viewModel.setCalendarSyncEnabled(enabled)
                if (enabled) {
                    requestCalendarPermissionIfNeeded(forceAsk = true)
                }
            },
            deviceCalendars = deviceCalendars,
            onCalendarSourceToggle = { id, enabled ->
                sound.play(Sfx.SETTINGS_CHANGE)
                viewModel.toggleCalendarSource(id, enabled)
            },
            onNotificationSoundSelected = { option ->
                sound.play(Sfx.SETTINGS_CHANGE)
                viewModel.setNotificationSound(option)
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
    }
}

@Composable
private fun FocusTimerHost(
    viewModel: AuraViewModel,
    sound: SoundEngine
) {
    val secondsLeft by viewModel.focusSecondsLeft.collectAsStateWithLifecycle()
    val running by viewModel.isFocusTimerRunning.collectAsStateWithLifecycle()
    FocusTimerModal(
        secondsLeft = secondsLeft,
        isRunning = running,
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

/** Backward-compatible alias. */
@Composable
fun AuraApp(viewModel: AuraViewModel = viewModel()) {
    PixiDoApp(viewModel)
}
