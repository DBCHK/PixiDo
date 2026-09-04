package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.DayTime
import com.example.data.GoalEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.data.TaskPhases
import com.example.data.UserProfile
import com.example.ui.components.PixiEmptyState
import com.example.ui.components.PixiIslandContentInset
import com.example.ui.components.PixiPillShape
import com.example.ui.components.PixiPrimaryButton
import com.example.ui.components.PulseAvatarSpec
import com.example.ui.components.PulseCircleIcon
import com.example.ui.components.PulseDayStat
import com.example.ui.components.PulseMenuItem
import com.example.ui.components.PulseProfileAvatar
import com.example.ui.components.PulseProgressCard
import com.example.ui.components.PulseSectionHeader
import com.example.ui.components.PulseSurfaceCard
import com.example.ui.components.PulseTaskCard
import com.example.ui.components.PulsePhaseItem
import com.example.ui.components.PulseTimelineBar
import com.example.ui.components.PulseTimelineCard
import com.example.ui.components.PulseTimelineColors
import com.example.ui.components.PulseTimelineDay
import com.example.ui.components.PulseTopRow
import com.example.ui.components.PulseActionRow
import com.example.ui.components.PulseBlackBanner
import com.example.ui.components.PulseCelebrate
import com.example.ui.components.PulseDayRing
import com.example.ui.components.PulseQuickCapture
import com.example.ui.components.PulseWeekStrip
import com.example.ui.components.InkSheetSearchField
import com.example.ui.components.pulseAvatarPalette
import com.example.ui.components.pulseCard
import com.example.ui.components.pulseInk
import com.example.ui.components.pulseMuted
import com.example.ui.components.pulsePaper
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseMint
import com.example.ui.theme.rememberPixiDimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    goals: List<GoalEntity>,
    notes: List<NoteEntity>,
    profile: UserProfile,
    onToggleTask: (TaskEntity) -> Unit,
    onToggleSubtask: (TaskEntity, String) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onSnoozeTask: (TaskEntity) -> Unit = {},
    onPinTask: (TaskEntity) -> Unit = {},
    onSkipRepeat: (TaskEntity) -> Unit = {},
    onOpenAddTask: () -> Unit,
    onQuickAddTask: (title: String, dueMillis: Long) -> Unit = { _, _ -> },
    onCompleteSimpleGoal: (GoalEntity) -> Unit = {},
    onDeleteSimpleGoal: (Int) -> Unit = {},
    onOpenFocusMode: () -> Unit,
    onOpenProfile: () -> Unit,
    onAddNote: (String, String) -> Unit,
    onToggleNotePin: (NoteEntity) -> Unit,
    onDeleteNote: (Int) -> Unit,
    onClearCompleted: () -> Unit = {},
    onRescheduleTask: (TaskEntity, Long) -> Unit = { _, _ -> },
    onRewriteSubtasks: (TaskEntity, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val sound = LocalSoundEngine.current
    var selectedTaskId by remember { mutableStateOf<Int?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var seeAll by remember { mutableStateOf(false) }
    var bannerDismissed by remember { mutableStateOf(false) }
    var detailBannerDismissed by remember { mutableStateOf(false) }
    var celebrateBurst by remember { mutableStateOf(0) }

    val selectedTask = tasks.firstOrNull { it.id == selectedTaskId }
    BackHandler(enabled = selectedTask != null) { selectedTaskId = null }

    val todayStart = remember { startOfDay(System.currentTimeMillis()) }
    val tomorrowStart = todayStart + 24L * 60 * 60 * 1000
    val openToday = remember(tasks, todayStart, tomorrowStart) {
        tasks.count { !it.isCompleted && tDueDay(it) in todayStart until tomorrowStart }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pulsePaper())
    ) {
        AnimatedContent(
            targetState = selectedTask,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 5 } + fadeOut())
                } else {
                    (slideInHorizontally { -it / 5 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            label = "taskPulsePage"
        ) { task ->
            if (task == null) {
                TasksHome(
                    tasks = tasks,
                    goals = goals,
                    profile = profile,
                    query = query,
                    showSearch = showSearch,
                    seeAll = seeAll,
                    bannerDismissed = bannerDismissed,
                    openToday = openToday,
                    todayStart = todayStart,
                    tomorrowStart = tomorrowStart,
                    onQueryChange = { query = it },
                    onToggleSearch = {
                        sound.play(Sfx.TAP_SOFT)
                        showSearch = !showSearch
                        if (!showSearch) query = ""
                    },
                    onSeeAll = { seeAll = !seeAll },
                    onDismissBanner = { bannerDismissed = true },
                    onOpenProfile = onOpenProfile,
                    onOpenFocusMode = {
                        sound.play(Sfx.FOCUS_START)
                        onOpenFocusMode()
                    },
                    onOpenAddTask = onOpenAddTask,
                    onQuickAddTask = { title, due ->
                        sound.play(Sfx.ADD_TASK)
                        onQuickAddTask(title, due)
                    },
                    onCelebrate = { celebrateBurst++ },
                    onOpenTask = { selectedTaskId = it.id },
                    onToggleTask = onToggleTask,
                    onDeleteTask = onDeleteTask,
                    onEditTask = onEditTask,
                    onSnoozeTask = onSnoozeTask,
                    onPinTask = onPinTask,
                    onSkipRepeat = onSkipRepeat,
                    onCompleteSimpleGoal = onCompleteSimpleGoal,
                    onDeleteSimpleGoal = onDeleteSimpleGoal,
                    onClearCompleted = onClearCompleted
                )
            } else {
                TaskPulseDetail(
                    task = task,
                    tasks = tasks,
                    profile = profile,
                    bannerDismissed = detailBannerDismissed,
                    onBack = { selectedTaskId = null },
                    onDismissBanner = { detailBannerDismissed = true },
                    onToggleTask = { onToggleTask(task) },
                    onToggleSubtask = { onToggleSubtask(task, it) },
                    onDeleteTask = {
                        selectedTaskId = null
                        onDeleteTask(task.id)
                    },
                    onEditTask = { onEditTask(task) },
                    onSnoozeTask = { onSnoozeTask(task) },
                    onPinTask = { onPinTask(task) },
                    onSkipRepeat = { onSkipRepeat(task) },
                    onRescheduleTask = { day -> onRescheduleTask(task, day) },
                    onRewriteSubtasks = { encoded -> onRewriteSubtasks(task, encoded) },
                    onOpenFocusMode = {
                        sound.play(Sfx.FOCUS_START)
                        onOpenFocusMode()
                    },
                    onCelebrate = { celebrateBurst++ }
                )
            }
        }
        PulseCelebrate(burst = celebrateBurst)
    }
}

@Composable
private fun TasksHome(
    tasks: List<TaskEntity>,
    goals: List<GoalEntity>,
    profile: UserProfile,
    query: String,
    showSearch: Boolean,
    seeAll: Boolean,
    bannerDismissed: Boolean,
    openToday: Int,
    todayStart: Long,
    tomorrowStart: Long,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onSeeAll: () -> Unit,
    onDismissBanner: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenFocusMode: () -> Unit,
    onOpenAddTask: () -> Unit,
    onQuickAddTask: (String, Long) -> Unit,
    onCelebrate: () -> Unit,
    onOpenTask: (TaskEntity) -> Unit,
    onToggleTask: (TaskEntity) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onSnoozeTask: (TaskEntity) -> Unit,
    onPinTask: (TaskEntity) -> Unit,
    onSkipRepeat: (TaskEntity) -> Unit,
    onCompleteSimpleGoal: (GoalEntity) -> Unit,
    onDeleteSimpleGoal: (Int) -> Unit,
    onClearCompleted: () -> Unit
) {
    val sound = LocalSoundEngine.current
    val d = rememberPixiDimens()
    val paper = pulsePaper()
    val ink = pulseInk()
    val muted = pulseMuted()
    var focusDay by remember { mutableStateOf(todayStart) }
    var capture by remember { mutableStateOf("") }
    val weekDays = remember(todayStart) {
        val start = DayTime.startOfWeekSunday(todayStart)
        (0..6).map { DayTime.addDays(start, it) }
    }
    val busyDays = remember(tasks) {
        tasks.filter { !it.isCompleted }.map { tDueDay(it) }.toSet()
    }
    val doneToday = remember(tasks, todayStart) {
        tasks.count { t ->
            t.isCompleted && t.completedAtMillis != null && t.completedAtMillis!! >= todayStart
        }
    }
    val ringTotal = (openToday + doneToday).coerceAtLeast(0)
    val ringProgress = if (ringTotal == 0) 0f else doneToday / ringTotal.toFloat()

    val filtered by remember(tasks, query, seeAll, todayStart, tomorrowStart, focusDay) {
        derivedStateOf {
            val searched = if (query.isBlank()) tasks
            else tasks.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true) ||
                    it.notes.contains(query, ignoreCase = true)
            }
            if (query.isNotBlank() || seeAll) searched
            else searched.filter { !it.isCompleted && tDueDay(it) == focusDay }
                .sortedWith(
                    compareByDescending<TaskEntity> { it.isPinned }.thenBy { tDueDay(it) }
                )
        }
    }

    val overdueTasks = remember(tasks, todayStart, seeAll, query, focusDay) {
        if (query.isNotBlank()) emptyList()
        else if (seeAll || focusDay == todayStart) {
            tasks.filter { !it.isCompleted && tDueDay(it) < todayStart }
        } else emptyList()
    }
    val todayTasks = remember(filtered, todayStart, tomorrowStart, seeAll, query, focusDay) {
        if (query.isNotBlank() || seeAll) {
            filtered.filter {
                !it.isCompleted && tDueDay(it) in todayStart until tomorrowStart
            }
        } else filtered
    }
    val upcomingTasks = remember(filtered, tomorrowStart, seeAll, query) {
        if (!seeAll && query.isBlank()) emptyList()
        else filtered.filter { !it.isCompleted && tDueDay(it) >= tomorrowStart }
    }
    val completedTasks = remember(filtered, seeAll, query) {
        if (!seeAll && query.isBlank()) emptyList()
        else filtered.filter { it.isCompleted }.take(12)
    }
    val openSimpleGoals = remember(goals, seeAll, query) {
        if (query.isNotBlank()) emptyList()
        else goals.filter { it.isSimpleTask && !it.isCompleted }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = d.screenHorizontal,
            end = d.screenHorizontal,
            top = 8.dp,
            bottom = PixiIslandContentInset + 16.dp
        )
    ) {
        item {
            PulseTopRow(
                leading = {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Profile",
                        tint = ink,
                        modifier = Modifier
                            .size(28.dp)
                            .clickable(onClick = onOpenProfile)
                            .testTag("profile_menu_button")
                    )
                },
                trailing = {
                    PulseCircleIcon(
                        onClick = onToggleSearch,
                        modifier = Modifier.testTag("task_search_toggle"),
                        contentDescription = if (showSearch) "Close search" else "Search",
                        icon = if (showSearch) Icons.Filled.Close else Icons.Filled.Search
                    )
                    PulseCircleIcon(
                        onClick = onOpenFocusMode,
                        modifier = Modifier.testTag("focus_mode_button"),
                        contentDescription = "Focus",
                        icon = Icons.Filled.NotificationsNone
                    )
                    PulseProfileAvatar(profile = profile, onClick = onOpenProfile)
                }
            )
            Spacer(modifier = Modifier.height(22.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Start Your Day",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = ink,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.6).sp
                    )
                    Text(
                        text = "& Be Productive ✌️",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = ink,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.6).sp
                    )
                }
                PulseDayRing(
                    progress = ringProgress,
                    center = if (ringTotal == 0) "0" else "$doneToday/$ringTotal",
                    caption = when {
                        ringTotal == 0 -> "Plan"
                        ringProgress >= 0.999f -> "Clear"
                        else -> "Today"
                    },
                    onClick = {
                        sound.play(Sfx.FILTER_SELECT)
                        focusDay = todayStart
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (!bannerDismissed) {
                PulseBlackBanner(
                    text = when (openToday) {
                        0 -> "You're clear for today."
                        1 -> "You have 1 task today."
                        else -> "You have $openToday tasks today."
                    },
                    onDismiss = onDismissBanner
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            PulseWeekStrip(
                days = weekDays,
                selectedMillis = focusDay,
                todayMillis = todayStart,
                busyDays = busyDays,
                onSelect = {
                    sound.play(Sfx.DAY_SELECT)
                    focusDay = it
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
            PulseQuickCapture(
                value = capture,
                onValueChange = { capture = it },
                onSubmit = {
                    val title = capture.trim()
                    if (title.isNotBlank()) {
                        val due = DayTime.withTimeFrom(
                            focusDay,
                            todayStart + 18L * 60 * 60 * 1000
                        )
                        onQuickAddTask(title, due)
                        capture = ""
                    }
                },
                placeholder = "Quick add for ${
                    if (focusDay == todayStart) "today" else SimpleDateFormat("EEE", Locale.getDefault()).format(Date(focusDay))
                }…"
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (showSearch) {
                InkSheetSearchField(
                    value = query,
                    onValueChange = onQueryChange,
                    placeholder = "Search tasks",
                    modifier = Modifier
                        .testTag("task_search")
                        .padding(horizontal = 0.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            PulseSectionHeader(
                title = when {
                    seeAll || query.isNotBlank() -> "All Tasks"
                    focusDay == todayStart -> "Today Tasks"
                    else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(focusDay))
                },
                action = if (seeAll) "Today" else "See All",
                onAction = {
                    sound.play(Sfx.FILTER_SELECT)
                    onSeeAll()
                    if (seeAll) focusDay = todayStart
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        val trimmed = query.trim()
        val showEmpty = todayTasks.isEmpty() && overdueTasks.isEmpty() &&
            upcomingTasks.isEmpty() && completedTasks.isEmpty() &&
            (openSimpleGoals.isEmpty() || query.isNotBlank())

        if (showEmpty) {
            item {
                if (trimmed.isNotBlank()) {
                    QuickAddFromSearchCard(
                        query = trimmed,
                        onAdd = {
                            onQuickAddTask(it, todayStart + 18L * 60 * 60 * 1000)
                        },
                        onDismiss = {
                            sound.play(Sfx.TAP_SOFT)
                            onQueryChange("")
                        }
                    )
                } else {
                    PixiEmptyState(
                        title = if (tasks.isEmpty()) "No tasks yet" else "Nothing here",
                        subtitle = if (tasks.isEmpty()) "Tap + to add one"
                        else "You're all caught up — enjoy the quiet.",
                        actionLabel = if (tasks.isEmpty()) "Add a task" else null,
                        onAction = if (tasks.isEmpty()) onOpenAddTask else null
                    )
                }
            }
        } else {
            if (seeAll && overdueTasks.isNotEmpty()) {
                item { SectionLabel("Overdue", overdueTasks.size, danger = true) }
                itemsIndexed(overdueTasks, key = { _, t -> "ov_${t.id}" }) { _, task ->
                    HomeTaskRow(
                        task = task,
                        isOverdue = true,
                        onOpen = { onOpenTask(task) },
                        onToggle = {
                            if (!task.isCompleted) onCelebrate()
                            onToggleTask(task)
                        },
                        onDelete = { onDeleteTask(task.id) },
                        onEdit = { onEditTask(task) },
                        onSnooze = { onSnoozeTask(task) },
                        onPin = { onPinTask(task) },
                        onSkip = { onSkipRepeat(task) }
                    )
                }
            }
            if (todayTasks.isNotEmpty()) {
                if (seeAll) item { SectionLabel("Today", todayTasks.size) }
                itemsIndexed(todayTasks, key = { _, t -> "td_${t.id}" }) { _, task ->
                    HomeTaskRow(
                        task = task,
                        isOverdue = tDueDay(task) < todayStart,
                        onOpen = { onOpenTask(task) },
                        onToggle = {
                            if (!task.isCompleted) onCelebrate()
                            onToggleTask(task)
                        },
                        onDelete = { onDeleteTask(task.id) },
                        onEdit = { onEditTask(task) },
                        onSnooze = { onSnoozeTask(task) },
                        onPin = { onPinTask(task) },
                        onSkip = { onSkipRepeat(task) }
                    )
                }
            }
            if (upcomingTasks.isNotEmpty()) {
                item { SectionLabel("Upcoming", upcomingTasks.size) }
                itemsIndexed(upcomingTasks, key = { _, t -> "up_${t.id}" }) { _, task ->
                    HomeTaskRow(
                        task = task,
                        isOverdue = false,
                        onOpen = { onOpenTask(task) },
                        onToggle = {
                            if (!task.isCompleted) onCelebrate()
                            onToggleTask(task)
                        },
                        onDelete = { onDeleteTask(task.id) },
                        onEdit = { onEditTask(task) },
                        onSnooze = { onSnoozeTask(task) },
                        onPin = { onPinTask(task) },
                        onSkip = { onSkipRepeat(task) }
                    )
                }
            }
            if (completedTasks.isNotEmpty()) {
                item {
                    SectionLabel(
                        title = "Completed",
                        count = completedTasks.size,
                        action = "Clear",
                        onAction = {
                            sound.play(Sfx.DELETE)
                            onClearCompleted()
                        }
                    )
                }
                itemsIndexed(completedTasks, key = { _, t -> "dn_${t.id}" }) { _, task ->
                    HomeTaskRow(
                        task = task,
                        isOverdue = false,
                        onOpen = { onOpenTask(task) },
                        onToggle = {
                            if (!task.isCompleted) onCelebrate()
                            onToggleTask(task)
                        },
                        onDelete = { onDeleteTask(task.id) },
                        onEdit = { onEditTask(task) },
                        onSnooze = { onSnoozeTask(task) },
                        onPin = { onPinTask(task) },
                        onSkip = { onSkipRepeat(task) }
                    )
                }
            }
        }

        if (openSimpleGoals.isNotEmpty() && query.isBlank() && seeAll) {
            item { SectionLabel("Simple tasks", openSimpleGoals.size) }
            itemsIndexed(openSimpleGoals, key = { _, g -> "simple_goal_${g.id}" }) { _, goal ->
                PulseTaskCard(
                    title = goal.title,
                    description = listOf(goal.category, goal.deadlineStr)
                        .filter { it.isNotBlank() }
                        .joinToString(" · "),
                    progress = if (goal.isCompleted) 1f else 0f,
                    avatars = listOf(
                        PulseAvatarSpec(
                            initial = goalInitial(goal.title),
                            color = pulseAvatarPalette(goal.id)
                        )
                    ),
                    extraCount = 0,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .testTag("goal_item_${goal.id}"),
                    menu = listOf(
                        PulseMenuItem(
                            label = "Yes done",
                            onClick = {
                                sound.play(Sfx.GOAL_COMPLETE)
                                onCompleteSimpleGoal(goal)
                            },
                            testTag = "yes_done_goal_${goal.id}"
                        ),
                        PulseMenuItem(
                            label = "Delete",
                            onClick = {
                                sound.play(Sfx.DELETE)
                                onDeleteSimpleGoal(goal.id)
                            },
                            danger = true
                        )
                    ),
                    onClick = {
                        sound.play(Sfx.GOAL_COMPLETE)
                        onCompleteSimpleGoal(goal)
                    }
                )
            }
        }

    }
}

@Composable
private fun TaskPulseDetail(
    task: TaskEntity,
    tasks: List<TaskEntity>,
    profile: UserProfile,
    bannerDismissed: Boolean,
    onBack: () -> Unit,
    onDismissBanner: () -> Unit,
    onToggleTask: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onDeleteTask: () -> Unit,
    onEditTask: () -> Unit,
    onSnoozeTask: () -> Unit,
    onPinTask: () -> Unit,
    onSkipRepeat: () -> Unit,
    onRescheduleTask: (Long) -> Unit = {},
    onRewriteSubtasks: (String) -> Unit = {},
    onOpenFocusMode: () -> Unit = {},
    onCelebrate: () -> Unit = {}
) {
    val sound = LocalSoundEngine.current
    val d = rememberPixiDimens()
    val ink = pulseInk()
    val progress = taskProgress(task)
    val today = remember { DayTime.startOfDay(System.currentTimeMillis()) }
    var weekStart by remember(task.id) {
        mutableStateOf(DayTime.startOfWeekSunday(task.dueDateMillis.coerceAtLeast(today)))
    }
    val dueDay = DayTime.startOfDay(task.dueDateMillis)
    val weekDays = remember(weekStart, dueDay, today) {
        (0..6).map { i ->
            val millis = DayTime.addDays(weekStart, i)
            PulseTimelineDay(
                label = SimpleDateFormat("d", Locale.getDefault()).format(Date(millis)),
                weekday = SimpleDateFormat("EEE", Locale.US).format(Date(millis)).take(1),
                isToday = millis == today,
                isDue = millis == dueDay
            )
        }
    }
    val weekLabel = remember(weekStart) {
        val end = DayTime.addDays(weekStart, 6)
        val fmt = SimpleDateFormat("MMM d", Locale.getDefault())
        "${fmt.format(Date(weekStart))} – ${fmt.format(Date(end))}"
    }
    val phases = remember(task.subtasks, task.dueDateMillis) {
        TaskPhases.parse(task.subtasks, task.dueDateMillis)
    }
    LaunchedEffect(task.id, task.subtasks, task.dueDateMillis) {
        if (phases.isEmpty()) return@LaunchedEffect
        val encoded = TaskPhases.encode(phases)
        if (encoded != task.subtasks) onRewriteSubtasks(encoded)
    }
    val doneNames = remember(task.completedSubtasks) {
        TaskPhases.names(task.completedSubtasks).toSet()
    }
    val days = remember(phases, doneNames, weekStart, task.isCompleted, task.completedAtMillis) {
        val fmt = SimpleDateFormat("d", Locale.getDefault())
        val percents = TaskPhases.dayPercents(
            phases = phases,
            doneNames = doneNames,
            weekStart = weekStart,
            dayCount = 7,
            taskCompleted = task.isCompleted,
            completedAtMillis = task.completedAtMillis
        )
        percents.mapIndexed { i, pct ->
            PulseDayStat(
                label = fmt.format(Date(DayTime.addDays(weekStart, i))),
                percent = pct
            )
        }
    }
    val bars = remember(phases, doneNames, weekStart, dueDay) {
        val phaseBars = phases.mapIndexed { index, phase ->
            PulseTimelineBar(
                id = "phase_${phase.name}",
                label = phase.name,
                color = PulseTimelineColors[index % PulseTimelineColors.size],
                startDay = DayTime.daysBetween(weekStart, phase.dayMillis),
                spanDays = phase.spanDays.coerceIn(1, 3),
                done = phase.name in doneNames
            )
        }
        val dueBar = PulseTimelineBar(
            id = "due",
            label = "Due",
            color = PulseCoral,
            startDay = DayTime.daysBetween(weekStart, dueDay),
            spanDays = 1,
            isDue = true
        )
        phaseBars + dueBar
    }
    val phaseItems = remember(phases, doneNames) {
        val dayFmt = SimpleDateFormat("EEE d", Locale.getDefault())
        phases.mapIndexed { index, phase ->
            PulsePhaseItem(
                name = phase.name,
                dayLabel = dayFmt.format(Date(phase.dayMillis)),
                done = phase.name in doneNames,
                color = PulseTimelineColors[index % PulseTimelineColors.size]
            )
        }
    }
    val phasesDone = phases.count { it.name in doneNames }
    val good = progress >= 0.5f || task.isCompleted
    val bannerText = when {
        task.isCompleted -> "Task complete"
        phases.isEmpty() -> "Add phases to track progress"
        progress >= 0.999f -> "All phases done — tap Done"
        good -> "You have a good progress"
        else -> "Let's pick up the pace"
    }
    val progressSubtitle = when {
        task.isCompleted -> "Completed"
        phases.isEmpty() -> "No phases yet"
        else -> "$phasesDone of ${phases.size} phases · ${(progress * 100f).toInt()}%"
    }
    val menu = taskMenu(
        task = task,
        onToggle = {
            sound.play(if (task.isCompleted) Sfx.TASK_UNDO else Sfx.TASK_COMPLETE)
            onToggleTask()
        },
        onEdit = {
            sound.play(Sfx.DIALOG_OPEN)
            onEditTask()
        },
        onPin = onPinTask,
        onSnooze = {
            sound.play(Sfx.SNOOZE)
            onSnoozeTask()
        },
        onSkip = onSkipRepeat,
        onDelete = {
            sound.play(Sfx.DELETE)
            onDeleteTask()
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = d.screenHorizontal,
            end = d.screenHorizontal,
            top = 8.dp,
            bottom = PixiIslandContentInset + 16.dp
        )
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                PulseCircleIcon(
                    onClick = onBack,
                    contentDescription = "Back",
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
                Text(
                    text = task.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(horizontal = 56.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            PulseActionRow(
                done = task.isCompleted,
                onToggle = {
                    if (!task.isCompleted) onCelebrate()
                    sound.play(if (task.isCompleted) Sfx.TASK_UNDO else Sfx.TASK_COMPLETE)
                    onToggleTask()
                },
                onFocus = onOpenFocusMode,
                onSnooze = {
                    sound.play(Sfx.SNOOZE)
                    onSnoozeTask()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            PulseProgressCard(
                days = days,
                bannerText = bannerText,
                subtitle = progressSubtitle,
                menu = menu,
                phases = phaseItems,
                onTogglePhase = { name ->
                    sound.play(Sfx.SUBTASK_TOGGLE)
                    onToggleSubtask(name)
                },
                onRemovePhase = { name ->
                    sound.play(Sfx.DELETE)
                    onRewriteSubtasks(TaskPhases.encode(TaskPhases.remove(phases, name)))
                },
                onDismissBanner = if (bannerDismissed) null else onDismissBanner,
                onDayClick = { index ->
                    val day = DayTime.addDays(weekStart, index)
                    val onDay = phases.filter { TaskPhases.covers(it, day) }
                    val target = onDay.firstOrNull { it.name !in doneNames } ?: onDay.lastOrNull()
                    if (target != null) {
                        sound.play(Sfx.SUBTASK_TOGGLE)
                        onToggleSubtask(target.name)
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            PulseTimelineCard(
                days = weekDays,
                bars = bars,
                weekLabel = weekLabel,
                menu = menu,
                onPrevWeek = {
                    sound.play(Sfx.TAP_SOFT)
                    weekStart = DayTime.addDays(weekStart, -7)
                },
                onNextWeek = {
                    sound.play(Sfx.TAP_SOFT)
                    weekStart = DayTime.addDays(weekStart, 7)
                },
                onThisWeek = {
                    sound.play(Sfx.TAP_SOFT)
                    weekStart = DayTime.startOfWeekSunday(today)
                },
                onBarClick = { bar ->
                    if (bar.isDue) {
                        sound.play(Sfx.DIALOG_OPEN)
                        onEditTask()
                    } else {
                        sound.play(Sfx.SUBTASK_TOGGLE)
                        onToggleSubtask(bar.label)
                    }
                },
                onBarMoved = { bar, dayIndex ->
                    val newDay = DayTime.addDays(weekStart, dayIndex)
                    sound.play(Sfx.SNOOZE)
                    if (bar.isDue) {
                        onRescheduleTask(newDay)
                    } else {
                        val moved = TaskPhases.move(phases, bar.label, newDay)
                        onRewriteSubtasks(TaskPhases.encode(moved))
                    }
                },
                onAddPhase = { name, dayIndex ->
                    sound.play(Sfx.ADD_TASK)
                    val day = DayTime.addDays(weekStart, dayIndex)
                    val added = TaskPhases.add(phases, name, day)
                    onRewriteSubtasks(TaskPhases.encode(added))
                },
                onRemovePhase = { bar ->
                    if (!bar.isDue) {
                        sound.play(Sfx.DELETE)
                        onRewriteSubtasks(TaskPhases.encode(TaskPhases.remove(phases, bar.label)))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTaskRow(
    task: TaskEntity,
    isOverdue: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    onSnooze: () -> Unit,
    onPin: () -> Unit,
    onSkip: () -> Unit
) {
    val sound = LocalSoundEngine.current
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.45f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    sound.play(if (task.isCompleted) Sfx.TASK_UNDO else Sfx.TASK_COMPLETE)
                    onToggle()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    sound.play(Sfx.DELETE)
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )
    val desc = buildString {
        append(taskDescription(task))
        if (isOverdue && !task.isCompleted) {
            if (isNotEmpty()) append(" · ")
            append("Overdue")
        }
    }
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val towardEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 12.dp)
                    .clip(com.example.ui.components.PulseCardShape)
                    .background(
                        if (towardEnd) PulseMint.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                    )
            )
        },
        modifier = Modifier.padding(bottom = 0.dp)
    ) {
        TaskCardItem(
            task = task,
            linkedGoal = null,
            onToggleTask = onToggle,
            onToggleSubtask = {},
            onDeleteTask = onDelete,
            onEditTask = onEdit,
            onSnoozeTask = onSnooze,
            onPinTask = onPin,
            onSkipRepeat = onSkip,
            isOverdue = isOverdue,
            onOpen = onOpen,
            descriptionOverride = desc
        )
    }
}

@Composable
fun TaskCardItem(
    task: TaskEntity,
    linkedGoal: GoalEntity?,
    onToggleTask: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onDeleteTask: () -> Unit,
    onEditTask: () -> Unit = {},
    onSnoozeTask: () -> Unit = {},
    onPinTask: () -> Unit = {},
    onSkipRepeat: () -> Unit = {},
    isOverdue: Boolean = false,
    onOpen: (() -> Unit)? = null,
    descriptionOverride: String? = null
) {
    val sound = LocalSoundEngine.current
    val desc = descriptionOverride ?: buildString {
        append(taskDescription(task, linkedGoal))
        if (isOverdue && !task.isCompleted) {
            if (isNotEmpty()) append(" · ")
            append("Overdue")
        }
    }
    val menu = taskMenu(
        task = task,
        onToggle = {
            sound.play(if (task.isCompleted) Sfx.TASK_UNDO else Sfx.TASK_COMPLETE)
            onToggleTask()
        },
        onEdit = {
            sound.play(Sfx.DIALOG_OPEN)
            onEditTask()
        },
        onPin = onPinTask,
        onSnooze = {
            sound.play(Sfx.SNOOZE)
            onSnoozeTask()
        },
        onSkip = onSkipRepeat,
        onDelete = {
            sound.play(Sfx.DELETE)
            onDeleteTask()
        }
    )
    PulseTaskCard(
        title = task.title,
        description = desc,
        progress = taskProgress(task),
        avatars = taskAvatars(task, UserProfile()),
        extraCount = extraFor(task),
        done = task.isCompleted,
        menu = menu,
        modifier = Modifier
            .padding(bottom = 12.dp)
            .testTag("task_item_${task.id}"),
        leadingTestTag = "checkbox_task_${task.id}",
        onToggle = {
            sound.play(if (task.isCompleted) Sfx.TASK_UNDO else Sfx.TASK_COMPLETE)
            onToggleTask()
        },
        onClick = {
            if (onOpen != null) onOpen()
            else {
                sound.play(Sfx.DIALOG_OPEN)
                onEditTask()
            }
        }
    )
}

@Composable
private fun QuickAddFromSearchCard(
    query: String,
    onAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    PulseSurfaceCard(modifier = Modifier.testTag("quick_add_from_search")) {
        Text(
            text = "No matches",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = pulseInk()
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Nothing found for “$query”",
            fontSize = 13.sp,
            color = pulseMuted(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(16.dp))
        PixiPrimaryButton(
            text = "Add “$query” as a todo",
            onClick = { onAdd(query) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("btn_quick_add_search_todo")
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Clear search",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clip(PixiPillShape)
                .clickable(onClick = onDismiss)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("btn_clear_search_no_match")
        )
    }
}

@Composable
private fun SectionLabel(
    title: String,
    count: Int,
    danger: Boolean = false,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (danger) MaterialTheme.colorScheme.error else pulseMuted()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (action != null && onAction != null) {
                Text(
                    text = action,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable(onClick = onAction)
                        .padding(end = 10.dp)
                )
            }
            Text(
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = pulseMuted()
            )
        }
    }
}

private fun taskMenu(
    task: TaskEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onPin: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit,
    onDelete: () -> Unit
): List<PulseMenuItem> {
    val items = mutableListOf(
        PulseMenuItem(
            label = if (task.isCompleted) "Undo" else "Mark done",
            onClick = onToggle,
            testTag = "yes_done_task_${task.id}"
        ),
        PulseMenuItem(label = "Edit", onClick = onEdit),
        PulseMenuItem(label = if (task.isPinned) "Unpin" else "Pin", onClick = onPin, testTag = "pin_task_${task.id}")
    )
    if (!task.isCompleted) {
        items += PulseMenuItem(label = "Snooze 1 day", onClick = onSnooze, testTag = "snooze_task_${task.id}")
    }
    if (!task.isCompleted && task.isRepeating) {
        items += PulseMenuItem(label = "Skip to next", onClick = onSkip, testTag = "skip_task_${task.id}")
    }
    items += PulseMenuItem(label = "Delete", onClick = onDelete, danger = true)
    return items
}

private fun taskProgress(task: TaskEntity): Float =
    TaskPhases.completionRatio(task.subtasks, task.completedSubtasks, task.isCompleted)

private fun taskDescription(task: TaskEntity, linkedGoal: GoalEntity? = null): String {
    val parts = mutableListOf<String>()
    if (task.notes.isNotBlank()) parts += task.notes
    else if (task.category.isNotBlank()) parts += task.category
    val due = formatDueLabel(task.dueTimeStr)
    if (due.isNotBlank() && task.notes.isBlank()) parts += due
    if (task.isRepeating) parts += task.repeat.shortLabel
    linkedGoal?.let { parts += it.title }
    return parts.joinToString(" · ")
}

private fun extraFor(task: TaskEntity): Int {
    val people = 1 + TaskPhases.names(task.subtasks).size
    return (people - 2).coerceAtLeast(0)
}

private fun taskAvatars(task: TaskEntity, profile: UserProfile): List<PulseAvatarSpec> {
    val list = mutableListOf<PulseAvatarSpec>()
    val photo = profile.avatarUri.ifBlank { profile.googlePhotoUrl }
    if (photo.isNotBlank() || profile.displayName.isNotBlank()) {
        list += PulseAvatarSpec(
            initial = profile.displayName.trim().firstOrNull()?.uppercase() ?: "P",
            color = pulseAvatarPalette(0),
            photoUrl = photo
        )
    }
    val subs = TaskPhases.names(task.subtasks)
    subs.take(2).forEachIndexed { index, sub ->
        list += PulseAvatarSpec(
            initial = sub.take(1).uppercase(),
            color = pulseAvatarPalette(task.id + index + 1)
        )
    }
    if (list.isEmpty()) {
        list += PulseAvatarSpec(
            initial = task.title.take(1).uppercase(),
            color = pulseAvatarPalette(task.id)
        )
    }
    return list.take(2)
}

private fun goalInitial(title: String): String =
    title.trim().firstOrNull()?.uppercase() ?: "G"

private fun startOfDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun tDueDay(task: TaskEntity): Long = startOfDay(task.dueDateMillis)

private fun formatDueLabel(raw: String): String {
    val cleaned = raw
        .replace('\u00A0', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
    if (cleaned.isEmpty()) return ""
    val parts = cleaned.split("·", "•", " - ", "–").map { it.trim() }.filter { it.isNotEmpty() }
    return if (parts.size >= 2) {
        val day = parts[0]
        val time = parts.drop(1).joinToString(" ")
            .replace(Regex("(?i)\\s*(a\\.?m\\.?)"), " AM")
            .replace(Regex("(?i)\\s*(p\\.?m\\.?)"), " PM")
            .replace(Regex("\\s+"), " ")
            .trim()
        "$day · $time"
    } else {
        cleaned
            .replace(Regex("(?i)\\s*(a\\.?m\\.?)"), " AM")
            .replace(Regex("(?i)\\s*(p\\.?m\\.?)"), " PM")
    }
}
