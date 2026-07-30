package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.DailyActivityEntity
import com.example.data.GoalEntity
import com.example.data.NoteEntity
import com.example.data.TaskEntity
import com.example.data.UserProfile
import com.example.ui.components.DailyHeaderBanner
import com.example.ui.components.QuickNotesSection
import com.example.ui.components.StatsStrip

@Composable
fun TasksScreen(
    tasks: List<TaskEntity>,
    goals: List<GoalEntity>,
    notes: List<NoteEntity>,
    userXp: Int,
    profile: UserProfile,
    activity: List<DailyActivityEntity>,
    onToggleTask: (TaskEntity) -> Unit,
    onToggleSubtask: (TaskEntity, String) -> Unit,
    onDeleteTask: (Int) -> Unit,
    onOpenAddTask: () -> Unit,
    onOpenFocusMode: () -> Unit,
    onOpenProfile: () -> Unit,
    onAddNote: (String, String) -> Unit,
    onToggleNotePin: (NoteEntity) -> Unit,
    onDeleteNote: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sound = LocalSoundEngine.current
    var selectedFilter by remember { mutableStateOf("ALL") }
    var query by remember { mutableStateOf("") }
    var fabPressed by remember { mutableStateOf(false) }

    val filteredTasks by remember(tasks, selectedFilter, query) {
        derivedStateOf {
            val base = when (selectedFilter) {
                "HIGH_FIRE" -> tasks.filter { it.priority == "HIGH_FIRE" }
                "QUICK_WIN" -> tasks.filter { it.priority == "QUICK_WIN" }
                "COMPLETED" -> tasks.filter { it.isCompleted }
                "PENDING" -> tasks.filter { !it.isCompleted }
                else -> tasks
            }
            if (query.isBlank()) base
            else base.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true)
            }
        }
    }

    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed) 0.9f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "fabScale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 88.dp, top = 8.dp)
        ) {
            item {
                DailyHeaderBanner(
                    userXp = userXp,
                    profile = profile,
                    activity = activity,
                    onOpenFocusMode = {
                        sound.play(Sfx.FOCUS_START)
                        onOpenFocusMode()
                    },
                    onOpenProfile = {
                        sound.play(Sfx.PROFILE_OPEN)
                        onOpenProfile()
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                StatsStrip(tasks = tasks, activity = activity)
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                QuickNotesSection(
                    notes = notes,
                    onAddNote = onAddNote,
                    onTogglePin = onToggleNotePin,
                    onDelete = onDeleteNote
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_search"),
                    placeholder = { Text("Search tasks…") },
                    leadingIcon = {
                        Icon(Icons.Filled.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                sound.play(Sfx.TAP_SOFT)
                                query = ""
                            }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "All",
                        "PENDING" to "To Do",
                        "HIGH_FIRE" to "High",
                        "QUICK_WIN" to "Quick",
                        "COMPLETED" to "Done"
                    )

                    items(filters) { (key, label) ->
                        val isSel = selectedFilter == key
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    sound.play(Sfx.FILTER_SELECT)
                                    selectedFilter = key
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (filteredTasks.isEmpty()) {
                item {
                    EmptyTaskState(
                        filterName = selectedFilter,
                        isFullyEmpty = tasks.isEmpty(),
                        isSearch = query.isNotBlank()
                    )
                }
            } else {
                items(filteredTasks, key = { it.id }) { task ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn(initialScale = 0.96f),
                        exit = fadeOut()
                    ) {
                        TaskCardItem(
                            task = task,
                            linkedGoal = goals.find { it.id == task.linkedGoalId },
                            onToggleTask = {
                                if (!task.isCompleted) sound.play(Sfx.TASK_COMPLETE)
                                else sound.play(Sfx.TASK_UNDO)
                                onToggleTask(task)
                            },
                            onToggleSubtask = { subtask ->
                                sound.play(Sfx.SUBTASK_TOGGLE)
                                onToggleSubtask(task, subtask)
                            },
                            onDeleteTask = {
                                sound.play(Sfx.DELETE)
                                onDeleteTask(task.id)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        FloatingActionButton(
            onClick = {
                sound.play(Sfx.FAB)
                onOpenAddTask()
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
                .scale(fabScale)
                .testTag("add_task_fab")
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Task")
        }
    }
}

@Composable
fun TaskCardItem(
    task: TaskEntity,
    linkedGoal: GoalEntity?,
    onToggleTask: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onDeleteTask: () -> Unit
) {
    var expandedSubtasks by remember { mutableStateOf(false) }
    val sound = LocalSoundEngine.current

    val subtaskList = remember(task.subtasks) {
        task.subtasks.split(";").filter { it.isNotBlank() }
    }
    val completedSubtaskSet = remember(task.completedSubtasks) {
        task.completedSubtasks.split(";").filter { it.isNotBlank() }.toSet()
    }

    val priorityColor = when (task.priority) {
        "HIGH_FIRE" -> Color(0xFFF43F5E)
        "QUICK_WIN" -> Color(0xFF38BDF8)
        "CORE_GOAL" -> Color(0xFFA78BFA)
        else -> Color(0xFF10B981)
    }

    val priorityLabel = when (task.priority) {
        "HIGH_FIRE" -> "High"
        "QUICK_WIN" -> "Quick"
        "CORE_GOAL" -> "Core"
        else -> "Idea"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_${task.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onToggleTask,
                    modifier = Modifier.testTag("checkbox_task_${task.id}")
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = "Complete Task",
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = task.category,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(priorityColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = priorityLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        if (task.streakCount > 1) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${task.streakCount}d",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "+${task.xpReward}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = onDeleteTask,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete Task",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            linkedGoal?.let { goal ->
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
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
                Spacer(modifier = Modifier.height(8.dp))
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
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (expandedSubtasks) "Hide" else "View",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = expandedSubtasks) {
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
                                    fontSize = 12.sp,
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

@Composable
fun EmptyTaskState(
    filterName: String,
    isFullyEmpty: Boolean = false,
    isSearch: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                isSearch -> "🔎"
                isFullyEmpty -> "✨"
                else -> "🔍"
            },
            fontSize = 40.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                isSearch -> "No matches"
                isFullyEmpty -> "No tasks yet"
                else -> "Nothing in “$filterName”"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = when {
                isSearch -> "Try a different search term"
                isFullyEmpty -> "Tap + to create your first task. Notes & the activity grid grow with you."
                else -> "Try another filter or add a new task."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
