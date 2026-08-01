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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.notify.ReminderScheduler
import java.util.Calendar

@OptIn(ExperimentalLayoutApi::class)
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
        linkedGoalId: Int?
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Work") }
    var selectedPriority by remember { mutableStateOf("HIGH_FIRE") }
    var selectedTime by remember { mutableStateOf("09:00") }
    var dayOffset by remember { mutableStateOf(0) } // 0 = today, 1 = tomorrow, …
    var subtasks by remember { mutableStateOf("") }

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
                .testTag("add_task_dialog")
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
                        text = "New Task",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_add_task")
                    )
                }

                PixiDoodle3D(
                    resId = R.drawable.doodle_tasks,
                    size = 110.dp,
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .align(Alignment.CenterHorizontally),
                    orbitSeconds = 8
                )

                Text(
                    text = "What do you want to get done?",
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
                            onClick = { dayOffset = offset }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Due time (you’ll get a notification)",
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
                            selected = selectedTime == t,
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
                    text = "Add Task",
                    onClick = {
                        if (title.isNotBlank()) {
                            val dayStart = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 0)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                                add(Calendar.DAY_OF_YEAR, dayOffset)
                            }.timeInMillis

                            val dueMillis = ReminderScheduler.combineDateAndTime(dayStart, selectedTime)
                                ?: (dayStart + 9 * 60 * 60 * 1000L) // fallback 9:00

                            val displayTime = ReminderScheduler.formatTime(dueMillis)
                            val dayLabel = dayPresets.find { it.first == dayOffset }?.second ?: "Today"
                            // Keep a short, single-line friendly label for task cards
                            val dueLabel = "$dayLabel · $displayTime"

                            onAddTask(
                                title,
                                selectedCategory,
                                selectedPriority,
                                dueLabel,
                                dueMillis,
                                subtasks,
                                null
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.testTag("submit_add_task_btn")
                )
            }
        }
    }
}
