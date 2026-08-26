package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.GoalEntity
import com.example.data.RepeatRule
import com.example.data.TaskEntity
import com.example.notify.ReminderScheduler
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onAddTask: (
        title: String,
        category: String,
        priority: String,
        dueTimeStr: String,
        dueDateMillis: Long,
        subtasks: String,
        linkedGoalId: Int?,
        repeatRule: String,
        isPinned: Boolean,
        notes: String
    ) -> Unit,
    /** When non-null, dialog is in edit mode. */
    existingTask: TaskEntity? = null,
    onUpdateTask: (
        taskId: Int,
        title: String,
        category: String,
        priority: String,
        dueTimeStr: String,
        dueDateMillis: Long,
        subtasks: String,
        linkedGoalId: Int?,
        repeatRule: String,
        isPinned: Boolean,
        notes: String
    ) -> Unit = { _, _, _, _, _, _, _, _, _, _, _ -> },
    /** Pre-select a due day (e.g. from calendar). Ignored when editing. */
    initialDueDateMillis: Long? = null,
    goals: List<GoalEntity> = emptyList()
) {
    val isEdit = existingTask != null
    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var title by remember(existingTask?.id) {
        mutableStateOf(existingTask?.title.orEmpty())
    }
    var selectedCategory by remember(existingTask?.id) {
        mutableStateOf(existingTask?.category ?: "Work")
    }
    var selectedPriority by remember(existingTask?.id) {
        mutableStateOf(existingTask?.priority ?: "HIGH_FIRE")
    }
    var selectedTime by remember(existingTask?.id) {
        mutableStateOf(
            existingTask?.let { ReminderScheduler.formatTime(it.dueDateMillis) }
                ?: "09:00"
        )
    }
    var selectedDayStart by remember(existingTask?.id, initialDueDateMillis) {
        mutableStateOf(
            when {
                existingTask != null -> startOfDay(existingTask.dueDateMillis)
                initialDueDateMillis != null && initialDueDateMillis > 0 ->
                    startOfDay(initialDueDateMillis)
                else -> todayStart
            }
        )
    }
    var subtasks by remember(existingTask?.id) {
        mutableStateOf(existingTask?.subtasks.orEmpty())
    }
    var linkedGoalId by remember(existingTask?.id) {
        mutableStateOf(existingTask?.linkedGoalId)
    }
    var selectedRepeat by remember(existingTask?.id) {
        mutableStateOf(existingTask?.repeat ?: RepeatRule.NONE)
    }
    var isPinned by remember(existingTask?.id) {
        mutableStateOf(existingTask?.isPinned ?: false)
    }
    var notes by remember(existingTask?.id) {
        mutableStateOf(existingTask?.notes.orEmpty())
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val categories = listOf("Work", "Personal", "Health", "Learning", "Social", "Other")
    val priorities = listOf(
        "HIGH_FIRE" to "High",
        "QUICK_WIN" to "Quick",
        "CORE_GOAL" to "Core",
        "BRAINSTORM" to "Idea"
    )
    val timePresets = listOf(
        "09:00", "10:00", "12:00", "14:00", "15:00", "17:00", "18:00", "20:00"
    )
    val dayPresets = listOf(
        0 to "Today",
        1 to "Tomorrow",
        2 to "In 2 days",
        7 to "In a week"
    )

    val dayOffset = remember(selectedDayStart, todayStart) {
        val diff = selectedDayStart - todayStart
        TimeUnit.MILLISECONDS.toDays(diff).toInt()
    }
    val dateLabel = remember(selectedDayStart) {
        formatDayLabel(selectedDayStart, todayStart)
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )

    Dialog(onDismissRequest = onDismiss) {
        PixiCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(if (isEdit) "edit_task_dialog" else "add_task_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "Edit Task" else "New Task",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_task")
                    )
                }

                if (!isEdit) {
                    PixiDoodle3D(
                        resId = R.drawable.doodle_tasks,
                        size = 110.dp,
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .align(Alignment.CenterHorizontally),
                        orbitSeconds = 8
                    )
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = if (isEdit) "Update details — calendar stays in sync"
                    else "What do you want to get done?",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .align(Alignment.CenterHorizontally)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    placeholder = { Text("e.g. Design App Prototype V2") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_task_title"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Due day",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    dayPresets.forEach { (offset, label) ->
                        PixiChip(
                            label = label,
                            selected = dayOffset == offset,
                            onClick = {
                                selectedDayStart = Calendar.getInstance().apply {
                                    timeInMillis = todayStart
                                    add(Calendar.DAY_OF_YEAR, offset)
                                }.timeInMillis
                            }
                        )
                    }
                    PixiChip(
                        label = if (dayPresets.none { it.first == dayOffset }) dateLabel else "Pick date",
                        selected = dayPresets.none { it.first == dayOffset },
                        onClick = { showDatePicker = true }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Selected · $dateLabel",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("selected_due_day_label")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Due time (custom notification)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timePresets.forEach { t ->
                        PixiChip(
                            label = t,
                            selected = selectedTime == t ||
                                normalizeTimeChip(selectedTime) == t,
                            onClick = { selectedTime = t }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = selectedTime,
                    onValueChange = { selectedTime = it },
                    label = { Text("Custom time (HH:mm or h:mm AM)") },
                    placeholder = { Text("14:30") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_task_due"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Category",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        PixiChip(
                            label = cat,
                            selected = cat == selectedCategory,
                            onClick = { selectedCategory = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Priority Level",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorities.forEach { (key, label) ->
                        PixiChip(
                            label = label,
                            selected = key == selectedPriority,
                            onClick = { selectedPriority = key }
                        )
                    }
                }

                if (goals.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Link goal (optional)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PixiChip(
                            label = "None",
                            selected = linkedGoalId == null,
                            onClick = { linkedGoalId = null }
                        )
                        goals.filter { !it.isCompleted }.take(8).forEach { goal ->
                            PixiChip(
                                label = goal.title.take(18),
                                selected = linkedGoalId == goal.id,
                                onClick = { linkedGoalId = goal.id }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Repeat",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatRule.entries.forEach { rule ->
                        PixiChip(
                            label = rule.displayName,
                            selected = selectedRepeat == rule,
                            onClick = { selectedRepeat = rule },
                            modifier = Modifier.testTag("repeat_${rule.name}")
                        )
                    }
                }
                if (selectedRepeat != RepeatRule.NONE) {
                    Text(
                        text = when (selectedRepeat) {
                            RepeatRule.DAILY -> "Completing it schedules the next day at the same time."
                            RepeatRule.WEEKDAYS -> "Skips Saturday and Sunday."
                            RepeatRule.WEEKLY -> "Comes back on the same weekday."
                            RepeatRule.NONE -> ""
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixiChip(
                        label = if (isPinned) "Pinned" else "Pin to top",
                        selected = isPinned,
                        onClick = { isPinned = !isPinned }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    placeholder = { Text("Optional details") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_task_notes"),
                    minLines = 2,
                    maxLines = 4,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = subtasks,
                    onValueChange = { subtasks = it },
                    label = { Text("Subtasks (separate with ';')") },
                    placeholder = { Text("Research; Draft; Export") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(22.dp))

                PixiPrimaryButton(
                    text = if (isEdit) "Save changes" else "Add Task",
                    onClick = {
                        if (title.isNotBlank()) {
                            val dueMillis = ReminderScheduler.combineDateAndTime(
                                selectedDayStart,
                                selectedTime
                            ) ?: (selectedDayStart + 9 * 60 * 60 * 1000L)

                            val displayTime = ReminderScheduler.formatTime(dueMillis)
                            val dayLabel = formatDayLabel(selectedDayStart, todayStart)
                            val dueLabel = "$dayLabel · $displayTime"

                            val task = existingTask
                            if (task != null) {
                                onUpdateTask(
                                    task.id,
                                    title.trim(),
                                    selectedCategory,
                                    selectedPriority,
                                    dueLabel,
                                    dueMillis,
                                    subtasks,
                                    linkedGoalId,
                                    selectedRepeat.name,
                                    isPinned,
                                    notes.trim()
                                )
                            } else {
                                onAddTask(
                                    title.trim(),
                                    selectedCategory,
                                    selectedPriority,
                                    dueLabel,
                                    dueMillis,
                                    subtasks,
                                    linkedGoalId,
                                    selectedRepeat.name,
                                    isPinned,
                                    notes.trim()
                                )
                            }
                            onDismiss()
                        }
                    },
                    modifier = Modifier.testTag(
                        if (isEdit) "submit_edit_task_btn" else "submit_add_task_btn"
                    )
                )
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDayStart
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { utcMillis ->
                            // DatePicker returns UTC midnight — convert to local start-of-day
                            selectedDayStart = utcMillisToLocalDayStart(utcMillis)
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
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

/**
 * Material DatePicker yields UTC midnight for the picked civil date.
 * Rebuild as local calendar day-start so tasks land on the intended day.
 */
private fun utcMillisToLocalDayStart(utcMillis: Long): Long {
    val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcMillis
    }
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, utc.get(Calendar.YEAR))
        set(Calendar.MONTH, utc.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

private fun formatDayLabel(dayStart: Long, todayStart: Long): String {
    val days = TimeUnit.MILLISECONDS.toDays(dayStart - todayStart).toInt()
    return when (days) {
        0 -> "Today"
        1 -> "Tomorrow"
        -1 -> "Yesterday"
        in 2..6 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(dayStart))
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(dayStart))
    }
}

private fun normalizeTimeChip(raw: String): String? {
    val parsed = ReminderScheduler.parseTimeOfDay(raw) ?: return null
    return String.format("%02d:%02d", parsed.first, parsed.second)
}
