package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.R
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.GoalEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.data.UserProfile
import com.example.ui.components.DailyHeaderBanner
import com.example.ui.components.PixiBadge
import com.example.ui.components.PixiCard
import com.example.ui.components.PixiCardShape
import com.example.ui.components.PixiChip
import com.example.ui.components.PixiEmptyState
import com.example.ui.components.PixiPillShape
import com.example.ui.components.PixiPrimaryButton
import com.example.ui.components.PixiSearchField
import com.example.ui.components.PixiSectionLabel
import com.example.ui.components.rememberPopScale
import com.example.ui.theme.rememberPixiDimens
import java.util.Calendar

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
    /** Quick-create a todo with the given title (from search). */
    onQuickAddTask: (title: String) -> Unit = {},
    onOpenFocusMode: () -> Unit,
    onOpenProfile: () -> Unit,
    onAddNote: (String, String) -> Unit,
    onToggleNotePin: (NoteEntity) -> Unit,
    onDeleteNote: (Int) -> Unit,
    onClearCompleted: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sound = LocalSoundEngine.current
    val d = rememberPixiDimens()
    var selectedFilter by remember { mutableStateOf("ALL") }
    var query by remember { mutableStateOf("") }

    val todayStart = remember { startOfDay(System.currentTimeMillis()) }
    val tomorrowStart = todayStart + 24L * 60 * 60 * 1000

    val openCount = remember(tasks) { tasks.count { !it.isCompleted } }
    val doneToday = remember(tasks, todayStart) {
        tasks.count { t ->
            t.isCompleted && t.completedAtMillis != null &&
                t.completedAtMillis!! >= todayStart
        }
    }
    val overdueCount = remember(tasks, todayStart) {
        tasks.count { !it.isCompleted && tDueDay(it) < todayStart }
    }
    val filteredTasks by remember(tasks, selectedFilter, query, todayStart, tomorrowStart) {
        derivedStateOf {
            val base = when (selectedFilter) {
                "HIGH_FIRE" -> tasks.filter { it.priority == "HIGH_FIRE" }
                "QUICK_WIN" -> tasks.filter { it.priority == "QUICK_WIN" }
                "COMPLETED" -> tasks.filter { it.isCompleted }
                "PENDING" -> tasks.filter { !it.isCompleted }
                "OVERDUE" -> tasks.filter { !it.isCompleted && tDueDay(it) < todayStart }
                "TODAY" -> tasks.filter {
                    !it.isCompleted && tDueDay(it) in todayStart until tomorrowStart
                }
                "REPEATING" -> tasks.filter { it.isRepeating && !it.isCompleted }
                "PINNED" -> tasks.filter { it.isPinned && !it.isCompleted }
                else -> tasks
            }
            val searched = if (query.isBlank()) base
            else base.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true) ||
                    it.notes.contains(query, ignoreCase = true)
            }
            searched.sortedWith(
                compareBy<TaskEntity> { it.isCompleted }
                    .thenByDescending { it.isPinned }
                    .thenBy { tDueDay(it) }
                    .thenByDescending {
                        when (it.priority) {
                            "HIGH_FIRE" -> 3
                            "CORE_GOAL" -> 2
                            "QUICK_WIN" -> 1
                            else -> 0
                        }
                    }
            )
        }
    }

    // Smart sections when viewing ALL without search
    val useSections = selectedFilter == "ALL" && query.isBlank()
    val pinnedTasks = remember(filteredTasks, useSections) {
        if (!useSections) emptyList()
        else filteredTasks.filter { it.isPinned && !it.isCompleted }
    }
    val overdueTasks = remember(filteredTasks, todayStart, useSections) {
        if (!useSections) emptyList()
        else filteredTasks.filter { !it.isCompleted && !it.isPinned && tDueDay(it) < todayStart }
    }
    val todayTasks = remember(filteredTasks, todayStart, tomorrowStart, useSections) {
        if (!useSections) emptyList()
        else filteredTasks.filter {
            !it.isCompleted && !it.isPinned && tDueDay(it) in todayStart until tomorrowStart
        }
    }
    val upcomingTasks = remember(filteredTasks, tomorrowStart, useSections) {
        if (!useSections) emptyList()
        else filteredTasks.filter { !it.isCompleted && !it.isPinned && tDueDay(it) >= tomorrowStart }
    }
    val completedTasks = remember(filteredTasks, useSections) {
        if (!useSections) emptyList()
        else filteredTasks.filter { it.isCompleted }.take(12)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = d.screenHorizontal),
            contentPadding = PaddingValues(
                bottom = d.screenVertical + 8.dp,
                top = d.screenVertical / 2
            )
        ) {
            item {
                DailyHeaderBanner(
                    profile = profile,
                    openCount = openCount,
                    doneToday = doneToday,
                    overdueCount = overdueCount,
                    onOpenFocusMode = {
                        sound.play(Sfx.FOCUS_START)
                        onOpenFocusMode()
                    },
                    onOpenProfile = {
                        sound.play(Sfx.PROFILE_OPEN)
                        onOpenProfile()
                    }
                )
                Spacer(modifier = Modifier.height(d.listGap))
            }

            item {
                PixiSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = "Search",
                    modifier = Modifier.testTag("task_search"),
                    leading = {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailing = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    sound.play(Sfx.TAP_SOFT)
                                    query = ""
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All",
                        "TODAY" to "Today",
                        "OVERDUE" to "Overdue",
                        "PINNED" to "Pinned",
                        "REPEATING" to "Repeating",
                        "PENDING" to "To Do",
                        "HIGH_FIRE" to "High",
                        "QUICK_WIN" to "Quick",
                        "COMPLETED" to "Done"
                    )
                    items(filters) { (key, label) ->
                        PixiChip(
                            label = label,
                            selected = selectedFilter == key,
                            onClick = {
                                sound.play(Sfx.FILTER_SELECT)
                                selectedFilter = key
                            }
                        )
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    val trimmed = query.trim()
                    if (trimmed.isNotBlank()) {
                        QuickAddFromSearchCard(
                            query = trimmed,
                            onAdd = {
                                sound.play(Sfx.ADD_TASK)
                                onQuickAddTask(trimmed)
                                query = ""
                                selectedFilter = "ALL"
                            },
                            onDismiss = {
                                sound.play(Sfx.TAP_SOFT)
                                query = ""
                            }
                        )
                    } else {
                        PixiEmptyState(
                            title = when {
                                tasks.isEmpty() -> "No tasks yet"
                                else -> "Nothing here"
                            },
                            subtitle = when {
                                tasks.isEmpty() -> "Tap the yellow + to create your first task"
                                selectedFilter == "OVERDUE" -> "You’re all caught up — no overdue tasks"
                                selectedFilter == "TODAY" -> "Nothing due today. Plan ahead on the calendar."
                                selectedFilter == "REPEATING" -> "No repeating tasks yet. Edit a task and set Repeat."
                                selectedFilter == "PINNED" -> "Pin important tasks to keep them at the top."
                                else -> "Try another filter or add a new task"
                            },
                            doodleRes = if (tasks.isEmpty()) R.drawable.doodle_tasks else null,
                            actionLabel = if (tasks.isEmpty()) "Add a task" else null,
                            onAction = if (tasks.isEmpty()) onOpenAddTask else null
                        )
                    }
                }
            } else if (useSections) {
                if (pinnedTasks.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Pinned", count = pinnedTasks.size)
                    }
                    itemsIndexed(pinnedTasks, key = { _, t -> "pin_${t.id}" }) { index, task ->
                        TaskCardBlock(
                            index = index,
                            task = task,
                            linkedGoal = goals.find { it.id == task.linkedGoalId },
                            isOverdue = tDueDay(task) < todayStart,
                            onToggleTask = { onToggleTask(task) },
                            onToggleSubtask = { sub ->
                                sound.play(Sfx.SUBTASK_TOGGLE)
                                onToggleSubtask(task, sub)
                            },
                            onDeleteTask = {
                                sound.play(Sfx.DELETE)
                                onDeleteTask(task.id)
                            },
                            onEditTask = {
                                sound.play(Sfx.DIALOG_OPEN)
                                onEditTask(task)
                            },
                            onSnoozeTask = {
                                sound.play(Sfx.SNOOZE)
                                onSnoozeTask(task)
                            },
                            onPinTask = { onPinTask(task) },
                            onSkipRepeat = { onSkipRepeat(task) }
                        )
                    }
                }
                if (overdueTasks.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Overdue", count = overdueTasks.size, danger = true)
                    }
                    itemsIndexed(overdueTasks, key = { _, t -> "ov_${t.id}" }) { index, task ->
                        TaskCardBlock(
                            index = index,
                            task = task,
                            linkedGoal = goals.find { it.id == task.linkedGoalId },
                            isOverdue = true,
                            onToggleTask = { onToggleTask(task) },
                            onToggleSubtask = { sub ->
                                sound.play(Sfx.SUBTASK_TOGGLE)
                                onToggleSubtask(task, sub)
                            },
                            onDeleteTask = {
                                sound.play(Sfx.DELETE)
                                onDeleteTask(task.id)
                            },
                            onEditTask = {
                                sound.play(Sfx.DIALOG_OPEN)
                                onEditTask(task)
                            },
                            onSnoozeTask = {
                                sound.play(Sfx.SNOOZE)
                                onSnoozeTask(task)
                            },
                            onPinTask = { onPinTask(task) },
                            onSkipRepeat = { onSkipRepeat(task) }
                        )
                    }
                }
                if (todayTasks.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Today", count = todayTasks.size)
                    }
                    itemsIndexed(todayTasks, key = { _, t -> "td_${t.id}" }) { index, task ->
                        TaskCardBlock(
                            index = index,
                            task = task,
                            linkedGoal = goals.find { it.id == task.linkedGoalId },
                            isOverdue = false,
                            onToggleTask = { onToggleTask(task) },
                            onToggleSubtask = { sub ->
                                sound.play(Sfx.SUBTASK_TOGGLE)
                                onToggleSubtask(task, sub)
                            },
                            onDeleteTask = {
                                sound.play(Sfx.DELETE)
                                onDeleteTask(task.id)
                            },
                            onEditTask = {
                                sound.play(Sfx.DIALOG_OPEN)
                                onEditTask(task)
                            },
                            onSnoozeTask = {
                                sound.play(Sfx.SNOOZE)
                                onSnoozeTask(task)
                            },
                            onPinTask = { onPinTask(task) },
                            onSkipRepeat = { onSkipRepeat(task) }
                        )
                    }
                }
                if (upcomingTasks.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Upcoming", count = upcomingTasks.size)
                    }
                    itemsIndexed(upcomingTasks, key = { _, t -> "up_${t.id}" }) { index, task ->
                        TaskCardBlock(
                            index = index,
                            task = task,
                            linkedGoal = goals.find { it.id == task.linkedGoalId },
                            isOverdue = false,
                            onToggleTask = { onToggleTask(task) },
                            onToggleSubtask = { sub ->
                                sound.play(Sfx.SUBTASK_TOGGLE)
                                onToggleSubtask(task, sub)
                            },
                            onDeleteTask = {
                                sound.play(Sfx.DELETE)
                                onDeleteTask(task.id)
                            },
                            onEditTask = {
                                sound.play(Sfx.DIALOG_OPEN)
                                onEditTask(task)
                            },
                            onSnoozeTask = {
                                sound.play(Sfx.SNOOZE)
                                onSnoozeTask(task)
                            },
                            onPinTask = { onPinTask(task) },
                            onSkipRepeat = { onSkipRepeat(task) }
                        )
                    }
                }
                if (completedTasks.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Completed",
                            count = completedTasks.size,
                            action = "Clear",
                            onAction = {
                                sound.play(Sfx.DELETE)
                                onClearCompleted()
                            }
                        )
                    }
                    itemsIndexed(completedTasks, key = { _, t -> "dn_${t.id}" }) { index, task ->
                        TaskCardBlock(
                            index = index,
                            task = task,
                            linkedGoal = goals.find { it.id == task.linkedGoalId },
                            isOverdue = false,
                            onToggleTask = { onToggleTask(task) },
                            onToggleSubtask = { sub ->
                                sound.play(Sfx.SUBTASK_TOGGLE)
                                onToggleSubtask(task, sub)
                            },
                            onDeleteTask = {
                                sound.play(Sfx.DELETE)
                                onDeleteTask(task.id)
                            },
                            onEditTask = {
                                sound.play(Sfx.DIALOG_OPEN)
                                onEditTask(task)
                            },
                            onSnoozeTask = {
                                sound.play(Sfx.SNOOZE)
                                onSnoozeTask(task)
                            },
                            onPinTask = { onPinTask(task) },
                            onSkipRepeat = { onSkipRepeat(task) }
                        )
                    }
                }
            } else {
                itemsIndexed(filteredTasks, key = { _, t -> t.id }) { index, task ->
                    TaskCardBlock(
                        index = index,
                        task = task,
                        linkedGoal = goals.find { it.id == task.linkedGoalId },
                        isOverdue = !task.isCompleted && tDueDay(task) < todayStart,
                        onToggleTask = { onToggleTask(task) },
                        onToggleSubtask = { subtask ->
                            sound.play(Sfx.SUBTASK_TOGGLE)
                            onToggleSubtask(task, subtask)
                        },
                        onDeleteTask = {
                            sound.play(Sfx.DELETE)
                            onDeleteTask(task.id)
                        },
                        onEditTask = {
                            sound.play(Sfx.DIALOG_OPEN)
                            onEditTask(task)
                        },
                        onSnoozeTask = {
                            sound.play(Sfx.SNOOZE)
                            onSnoozeTask(task)
                        },
                        onPinTask = { onPinTask(task) },
                        onSkipRepeat = { onSkipRepeat(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
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
            color = if (danger) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant
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
            PixiBadge(
                text = count.toString(),
                containerColor = if (danger) MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (danger) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LazyItemScope.TaskCardBlock(
    index: Int,
    task: TaskEntity,
    linkedGoal: GoalEntity?,
    isOverdue: Boolean,
    onToggleTask: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onDeleteTask: () -> Unit,
    onEditTask: () -> Unit,
    onSnoozeTask: () -> Unit,
    onPinTask: () -> Unit = {},
    onSkipRepeat: () -> Unit = {}
) {
    Column {
        TaskCardItem(
            task = task,
            linkedGoal = linkedGoal,
            isOverdue = isOverdue,
            onToggleTask = onToggleTask,
            onToggleSubtask = onToggleSubtask,
            onDeleteTask = onDeleteTask,
            onEditTask = onEditTask,
            onSnoozeTask = onSnoozeTask,
            onPinTask = onPinTask,
            onSkipRepeat = onSkipRepeat
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}

/**
 * Shown when search has text but zero matches — ask to create a quick todo.
 */
@Composable
private fun QuickAddFromSearchCard(
    query: String,
    onAdd: () -> Unit,
    onDismiss: () -> Unit
) {
    PixiCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quick_add_from_search")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No matches",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Nothing found for “$query”",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(16.dp))
            PixiPrimaryButton(
                text = "Add “$query” as a todo",
                onClick = onAdd,
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
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    isOverdue: Boolean = false
) {
    var expandedSubtasks by remember { mutableStateOf(false) }
    val sound = LocalSoundEngine.current
    val scope = rememberCoroutineScope()
    var isSlashing by remember(task.id) { mutableStateOf(false) }
    val slashProgress = remember(task.id) { Animatable(if (task.isCompleted) 1f else 0f) }
    val showAsDone = task.isCompleted || isSlashing
    val checkPop = rememberPopScale(showAsDone)

    val contentAlpha by animateFloatAsState(
        targetValue = if (showAsDone) 0.58f else 1f,
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "taskDoneAlpha"
    )

    LaunchedEffect(task.isCompleted, task.id) {
        if (task.isCompleted && !isSlashing) {
            slashProgress.snapTo(1f)
        } else if (!task.isCompleted && !isSlashing) {
            slashProgress.snapTo(0f)
        }
    }

    fun handleToggle() {
        if (isSlashing) return
        if (task.isCompleted) {
            sound.play(Sfx.TASK_UNDO)
            onToggleTask()
            return
        }
        // Slow slash-out, then commit complete so list can settle it at the bottom
        isSlashing = true
        scope.launch {
            sound.play(Sfx.TASK_COMPLETE)
            slashProgress.snapTo(0f)
            slashProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing)
            )
            delay(200)
            onToggleTask()
            isSlashing = false
        }
    }

    val subtaskList = remember(task.subtasks) {
        task.subtasks.split(";").filter { it.isNotBlank() }
    }
    val completedSubtaskSet = remember(task.completedSubtasks) {
        task.completedSubtasks.split(";").filter { it.isNotBlank() }.toSet()
    }

    val priorityColor = when (task.priority) {
        "HIGH_FIRE" -> Color(0xFFFF6BA8)
        "QUICK_WIN" -> Color(0xFF67D4E8)
        "CORE_GOAL" -> Color(0xFFC4A8F5)
        else -> Color(0xFF34D399)
    }

    val priorityLabel = when (task.priority) {
        "HIGH_FIRE" -> "High"
        "QUICK_WIN" -> "Quick"
        "CORE_GOAL" -> "Core"
        else -> "Idea"
    }

    val dueLabel = remember(task.dueTimeStr) { formatDueLabel(task.dueTimeStr) }
    val slashColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { it * 0.45f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    handleToggle()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDeleteTask()
                    true
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val towardEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 2.dp)
                    .clip(PixiCardShape)
                    .background(
                        if (towardEnd) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                    )
                    .padding(horizontal = 22.dp),
                contentAlignment = if (towardEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = if (towardEnd) Icons.Filled.CheckCircle else Icons.Filled.DeleteOutline,
                    contentDescription = if (towardEnd) "Complete" else "Delete",
                    tint = if (towardEnd) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }
    ) {
    PixiCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}")
            .graphicsLayer { alpha = contentAlpha }
            .clickable(enabled = !isSlashing, onClick = onEditTask),
        containerColor = if (showAsDone) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                IconButton(
                    onClick = { handleToggle() },
                    enabled = !isSlashing,
                    modifier = Modifier
                        .testTag("checkbox_task_${task.id}")
                        .graphicsLayer {
                            scaleX = checkPop
                            scaleY = checkPop
                        }
                ) {
                    Icon(
                        imageVector = if (showAsDone) Icons.Filled.CheckCircle
                        else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = "Complete Task",
                        tint = if (showAsDone) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(2.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = task.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (showAsDone) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                            // Soft system line-through after animation settles; live slash draws below
                            textDecoration = if (task.isCompleted && !isSlashing) {
                                TextDecoration.LineThrough
                            } else {
                                TextDecoration.None
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Animated slash that slowly draws across the title
                        if (slashProgress.value > 0.01f) {
                            Canvas(
                                modifier = Modifier
                                    .matchParentSize()
                                    .padding(vertical = 2.dp)
                            ) {
                                val y = size.height * 0.52f
                                val endX = size.width * slashProgress.value.coerceIn(0f, 1f)
                                drawLine(
                                    color = slashColor,
                                    start = Offset(0f, y),
                                    end = Offset(endX, y),
                                    strokeWidth = 2.4.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PixiBadge(
                            text = task.category,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        PixiBadge(
                            text = priorityLabel,
                            containerColor = priorityColor.copy(alpha = 0.15f),
                            contentColor = priorityColor
                        )
                        if (task.isRepeating) {
                            PixiBadge(
                                text = task.repeat.shortLabel,
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (task.isPinned) {
                            PixiBadge(
                                text = "Pinned",
                                containerColor = Color(0xFFFBBF24).copy(alpha = 0.18f),
                                contentColor = Color(0xFFB45309)
                            )
                        }
                        if (isOverdue && !showAsDone) {
                            PixiBadge(
                                text = "Overdue",
                                containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        }
                        if (task.streakCount > 1) {
                            PixiBadge(
                                text = "🔥 ${task.streakCount}d",
                                containerColor = Color(0xFFFBBF24).copy(alpha = 0.18f),
                                contentColor = Color(0xFFB45309)
                            )
                        }
                    }

                    if (task.notes.isNotBlank()) {
                        Text(
                            text = task.notes,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    if (dueLabel.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isOverdue && !task.isCompleted)
                                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                                    else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                )
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = "Due",
                                tint = if (isOverdue && !task.isCompleted)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = dueLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isOverdue && !task.isCompleted)
                                    MaterialTheme.colorScheme.onErrorContainer
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(
                        onClick = onPinTask,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("pin_task_${task.id}")
                    ) {
                        Icon(
                            imageVector = if (task.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (task.isPinned) "Unpin" else "Pin",
                            tint = if (task.isPinned) Color(0xFFD97706)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Row {
                        if (!task.isCompleted && task.isRepeating) {
                            IconButton(
                                onClick = onSkipRepeat,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(top = 4.dp)
                                    .testTag("skip_task_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Repeat,
                                    contentDescription = "Skip to next",
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (!task.isCompleted) {
                            IconButton(
                                onClick = onSnoozeTask,
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(top = 4.dp)
                                    .testTag("snooze_task_${task.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Snooze,
                                    contentDescription = "Snooze 1 day",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            linkedGoal?.let { goal ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Goal Linked",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = goal.title,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (subtaskList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            sound.play(Sfx.TAP_SOFT)
                            expandedSubtasks = !expandedSubtasks
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Subtasks (${completedSubtaskSet.size}/${subtaskList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (expandedSubtasks) "Hide" else "View",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(
                    visible = expandedSubtasks,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column(modifier = Modifier.padding(top = 6.dp)) {
                        subtaskList.forEach { subtask ->
                            val isSubDone = completedSubtaskSet.contains(subtask)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onToggleSubtask(subtask) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Checkbox(
                                    checked = isSubDone,
                                    onCheckedChange = { onToggleSubtask(subtask) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = subtask,
                                    fontSize = 13.sp,
                                    color = if (isSubDone) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface,
                                    textDecoration = if (isSubDone) TextDecoration.LineThrough
                                    else TextDecoration.None
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

private fun startOfDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun tDueDay(task: TaskEntity): Long = startOfDay(task.dueDateMillis)

/**
 * Normalize due strings like "Today · 12:00 AM" / "Tomorrow · 9:00 PM"
 * into a compact, single-line friendly label.
 */
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
