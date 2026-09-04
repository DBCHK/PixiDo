package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DayTime
import com.example.data.GoalEntity
import com.example.data.RepeatRule
import com.example.data.TaskEntity
import com.example.data.TaskPhases
import com.example.notify.ReminderScheduler
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseInk
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
    initialDueDateMillis: Long? = null,
    goals: List<GoalEntity> = emptyList()
) {
    val isEdit = existingTask != null
    val todayStart = remember { DayTime.startOfDay(System.currentTimeMillis()) }

    var title by remember(existingTask?.id) {
        mutableStateOf(existingTask?.title.orEmpty())
    }
    var selectedCategory by remember(existingTask?.id) {
        mutableStateOf(existingTask?.category ?: "Work")
    }
    var selectedPriority by remember(existingTask?.id) {
        mutableStateOf(existingTask?.priority ?: "HIGH_FIRE")
    }
    var selectedHour by remember(existingTask?.id) {
        mutableStateOf(
            existingTask?.let {
                Calendar.getInstance().apply { timeInMillis = it.dueDateMillis }
                    .get(Calendar.HOUR_OF_DAY)
            } ?: 9
        )
    }
    var selectedMinute by remember(existingTask?.id) {
        mutableStateOf(
            existingTask?.let {
                Calendar.getInstance().apply { timeInMillis = it.dueDateMillis }
                    .get(Calendar.MINUTE)
            } ?: 0
        )
    }
    var selectedDayStart by remember(existingTask?.id, initialDueDateMillis) {
        mutableStateOf(
            when {
                existingTask != null -> DayTime.startOfDay(existingTask.dueDateMillis)
                initialDueDateMillis != null && initialDueDateMillis > 0 ->
                    DayTime.startOfDay(initialDueDateMillis)
                else -> todayStart
            }
        )
    }
    var phaseNames by remember(existingTask?.id) {
        mutableStateOf(TaskPhases.names(existingTask?.subtasks.orEmpty()))
    }
    var phaseDraft by remember(existingTask?.id) { mutableStateOf("") }
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
    var showTimePicker by remember { mutableStateOf(false) }

    val categories = listOf("Work", "Personal", "Health", "Learning", "Social", "Other")
    val priorities = listOf(
        "HIGH_FIRE" to "High",
        "QUICK_WIN" to "Quick",
        "CORE_GOAL" to "Core",
        "BRAINSTORM" to "Idea"
    )
    val timePresets = listOf(
        "Morning" to (9 to 0),
        "Noon" to (12 to 0),
        "Afternoon" to (15 to 0),
        "Evening" to (18 to 0),
        "Night" to (20 to 0)
    )
    val upcomingDays = remember(todayStart) {
        (0..13).map { offset ->
            val millis = DayTime.addDays(todayStart, offset)
            UpcomingDay(
                millis = millis,
                number = SimpleDateFormat("d", Locale.getDefault()).format(Date(millis)),
                weekday = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis)),
                isToday = offset == 0
            )
        }
    }
    val dateLabel = remember(selectedDayStart, todayStart) {
        formatDayLabel(selectedDayStart, todayStart)
    }
    val timeLabel = remember(selectedHour, selectedMinute) {
        formatClock(selectedHour, selectedMinute)
    }
    val selectedInStrip = upcomingDays.any { it.millis == selectedDayStart }
    val dateListState = rememberLazyListState()
    LaunchedEffect(selectedDayStart) {
        val idx = upcomingDays.indexOfFirst { it.millis == selectedDayStart }
        if (idx >= 0) dateListState.animateScrollToItem(idx)
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )
    val screenH = LocalConfiguration.current.screenHeightDp.dp

    fun submit() {
        if (title.isBlank()) return
        val timeStr = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
        val dueMillis = ReminderScheduler.combineDateAndTime(selectedDayStart, timeStr)
            ?: (selectedDayStart + selectedHour * 60 * 60 * 1000L + selectedMinute * 60 * 1000L)
        val displayTime = ReminderScheduler.formatTime(dueMillis)
        val dayLabel = formatDayLabel(selectedDayStart, todayStart)
        val dueLabel = "$dayLabel · $displayTime"
        val names = phaseNames.toMutableList()
        val extra = phaseDraft.trim()
        if (extra.isNotBlank() && names.none { it.equals(extra, ignoreCase = true) }) names += extra
        val previous = TaskPhases.parse(existingTask?.subtasks.orEmpty(), dueMillis)
        val encodedSubtasks = TaskPhases.encode(TaskPhases.withNames(previous, names, dueMillis))
        val task = existingTask
        if (task != null) {
            onUpdateTask(
                task.id,
                title.trim(),
                selectedCategory,
                selectedPriority,
                dueLabel,
                dueMillis,
                encodedSubtasks,
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
                encodedSubtasks,
                linkedGoalId,
                selectedRepeat.name,
                isPinned,
                notes.trim()
            )
        }
        onDismiss()
    }

    PixiGlassHost(onDismissRequest = onDismiss) {
        PixiGlass(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .heightIn(max = screenH * 0.92f)
                .testTag(if (isEdit) "edit_task_dialog" else "add_task_dialog"),
            shape = PixiCardShape,
            elevation = 20.dp,
            frost = true,
            liquid = true,
            weight = PixiGlassWeight.Sheet
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val formMax = (maxHeight - 196.dp).coerceAtLeast(120.dp)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = formMax)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = if (isEdit) "Update details — calendar stays in sync"
                            else "What do you want to get done?",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 12.dp)
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

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "When",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            state = dateListState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(end = 8.dp)
                        ) {
                            items(upcomingDays, key = { it.millis }) { day ->
                                DateStripChip(
                                    day = day,
                                    selected = day.millis == selectedDayStart,
                                    onClick = { selectedDayStart = day.millis }
                                )
                            }
                            item {
                                Box(
                                    modifier = Modifier
                                        .size(width = 56.dp, height = 72.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { showDatePicker = true }
                                        .testTag("pick_custom_date"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Filled.CalendarMonth,
                                            contentDescription = "Pick date",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "More",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                        if (!selectedInStrip) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Selected · $dateLabel",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("selected_due_day_label")
                            )
                        } else {
                            Text(
                                text = dateLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .testTag("selected_due_day_label")
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Time",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                                .clickable { showTimePicker = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                                .testTag("input_task_due"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.Schedule,
                                    contentDescription = null,
                                    tint = PulseCoral,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reminder",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = timeLabel,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = "Change",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timePresets.forEach { (label, hm) ->
                                val selected = selectedHour == hm.first && selectedMinute == hm.second
                                PixiChip(
                                    label = label,
                                    selected = selected,
                                    onClick = {
                                        selectedHour = hm.first
                                        selectedMinute = hm.second
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
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
                        PixiChip(
                            label = if (isPinned) "Pinned" else "Pin to top",
                            selected = isPinned,
                            onClick = { isPinned = !isPinned }
                        )

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
                        Text(
                            text = "Phases",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (phaseNames.isNotEmpty()) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                phaseNames.forEach { name ->
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                phaseNames = phaseNames.filterNot { it.equals(name, ignoreCase = true) }
                                            }
                                            .padding(start = 12.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                                            .testTag("phase_chip_$name"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.size(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Remove $name",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        OutlinedTextField(
                            value = phaseDraft,
                            onValueChange = { phaseDraft = it },
                            label = { Text("Add a phase") },
                            placeholder = { Text("e.g. Wireframe") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_task_phase"),
                            singleLine = true,
                            shape = PixiFieldShape,
                            colors = fieldColors,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val n = phaseDraft.trim().replace(";", ",").replace("@", " ")
                                    if (n.isNotBlank() && phaseNames.none { it.equals(n, ignoreCase = true) }) {
                                        phaseNames = phaseNames + n
                                    }
                                    phaseDraft = ""
                                }
                            ),
                            trailingIcon = {
                                Text(
                                    text = "Add",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .clickable {
                                            val n = phaseDraft.trim().replace(";", ",").replace("@", " ")
                                            if (n.isNotBlank() && phaseNames.none { it.equals(n, ignoreCase = true) }) {
                                                phaseNames = phaseNames + n
                                            }
                                            phaseDraft = ""
                                        }
                                        .padding(end = 8.dp)
                                        .testTag("add_phase_chip")
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 20.dp)
                    ) {
                        Text(
                            text = "$dateLabel · $timeLabel",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        PixiPrimaryButton(
                            text = if (isEdit) "Save changes" else "Add Task",
                            onClick = { submit() },
                            enabled = title.isNotBlank(),
                            modifier = Modifier.testTag(
                                if (isEdit) "submit_edit_task_btn" else "submit_add_task_btn"
                            )
                        )
                    }
                }
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

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute,
            is24Hour = false
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = PixiCardShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pick a time",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    )
                    TimePicker(state = timeState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(
                            onClick = {
                                selectedHour = timeState.hour
                                selectedMinute = timeState.minute
                                showTimePicker = false
                            }
                        ) { Text("OK") }
                    }
                }
            }
        }
    }
}

private data class UpcomingDay(
    val millis: Long,
    val number: String,
    val weekday: String,
    val isToday: Boolean
)

@Composable
private fun DateStripChip(
    day: UpcomingDay,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bg = when {
        selected -> PulseCoral
        day.isToday -> PulseInk
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = if (selected || day.isToday) Color.White else MaterialTheme.colorScheme.onSurface
    Column(
        modifier = Modifier
            .size(width = 56.dp, height = 72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (day.isToday) "Today" else day.weekday,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = fg.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = day.number,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = fg
        )
    }
}

private fun formatClock(hour: Int, minute: Int): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
    }
    return ReminderScheduler.formatTime(cal.timeInMillis)
}

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
        in 2..6 -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(dayStart))
        else -> SimpleDateFormat("EEE, MMM d", Locale.getDefault()).format(Date(dayStart))
    }
}
