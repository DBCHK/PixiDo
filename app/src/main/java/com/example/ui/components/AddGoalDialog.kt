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
import com.example.data.Currencies

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddGoalDialog(
    currencyCode: String = "USD",
    onDismiss: () -> Unit,
    onAddGoal: (
        title: String,
        category: String,
        targetAmount: Double,
        unit: String,
        deadlineStr: String,
        colorHex: String,
        isSimple: Boolean,
        isHabit: Boolean
    ) -> Unit
) {
    val moneySymbol = Currencies.symbolOf(currencyCode)
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Personal") }
    var targetAmountStr by remember { mutableStateOf("") }
    // "$" means money — ViewModel maps it to budget currency symbol
    var unit by remember { mutableStateOf("$") }
    var deadlineStr by remember { mutableStateOf("") }
    var isHabit by remember { mutableStateOf(true) }
    val isSimple = false

    val categories = listOf("Health", "Personal", "Fitness", "Learning", "Career", "Savings", "Travel")
    val units = listOf(
        "$" to "Money ($moneySymbol · $currencyCode)",
        "tasks" to "tasks",
        "books" to "books",
        "kms" to "kms",
        "%" to "%"
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
                .testTag("add_goal_dialog")
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
                        text = if (isHabit) "New habit" else "New goal",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(onClick = onDismiss)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isHabit) "Check it off each day you do it. Streaks live on this tab."
                    else "Money goals use your budget currency ($currencyCode)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Type",
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
                        label = "Daily habit",
                        selected = isHabit,
                        onClick = { isHabit = true },
                        modifier = Modifier.testTag("goal_type_habit")
                    )
                    PixiChip(
                        label = "Milestone",
                        selected = !isHabit,
                        onClick = { isHabit = false },
                        modifier = Modifier.testTag("goal_type_tracked")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(if (isHabit) "What do you want to do every day?" else "Goal name") },
                    placeholder = {
                        Text(if (isHabit) "e.g. Drink 2L of water" else "e.g. Save for Summer Trip")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_goal_title"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isHabit) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = targetAmountStr,
                            onValueChange = { targetAmountStr = it },
                            label = {
                                Text(
                                    if (unit == "$") "Target ($moneySymbol)"
                                    else "Target ($unit)"
                                )
                            },
                            placeholder = { Text("2000") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("input_goal_target"),
                            singleLine = true,
                            shape = PixiFieldShape,
                            colors = fieldColors
                        )
                        Spacer(modifier = Modifier.padding(4.dp))
                        OutlinedTextField(
                            value = deadlineStr,
                            onValueChange = { deadlineStr = it },
                            label = { Text("Deadline") },
                            placeholder = { Text("Dec 2027") },
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            singleLine = true,
                            shape = PixiFieldShape,
                            colors = fieldColors
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Unit",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        units.forEach { (key, label) ->
                            PixiChip(
                                label = label,
                                selected = key == unit,
                                onClick = { unit = key }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    OutlinedTextField(
                        value = deadlineStr,
                        onValueChange = { deadlineStr = it },
                        label = { Text("Cue (optional)") },
                        placeholder = { Text("Morning · after coffee") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_goal_deadline_simple"),
                        singleLine = true,
                        shape = PixiFieldShape,
                        colors = fieldColors
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

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
                            selected = cat == category,
                            onClick = { category = cat }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                PixiPrimaryButton(
                    text = if (isHabit) "Add habit" else "Create goal",
                    onClick = {
                        val parsedAmt = if (isHabit) 1.0 else (targetAmountStr.toDoubleOrNull() ?: 1.0)
                        if (title.isNotBlank()) {
                            onAddGoal(
                                title,
                                category,
                                parsedAmt,
                                if (isHabit) "habit" else unit,
                                deadlineStr,
                                "#C4A8F5",
                                false,
                                isHabit
                            )
                            onDismiss()
                        }
                    },
                    modifier = Modifier.testTag("submit_add_goal_btn")
                )
            }
        }
    }
}
