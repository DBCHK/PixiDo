package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.Currencies
import com.example.data.GoalActivityEntity
import com.example.data.GoalEntity
import com.example.ui.components.ContributionHeatmap
import com.example.ui.components.GoalMiniHeatmap
import com.example.ui.components.PixiBadge
import com.example.ui.components.PixiCard
import com.example.ui.components.PixiCardShapeSm
import com.example.ui.components.PixiCloseButton
import com.example.ui.components.PixiEmptyState
import com.example.ui.components.PixiFieldShape
import com.example.ui.components.PixiPillShape
import com.example.ui.components.PixiPrimaryButton
import com.example.ui.components.PixiScreenHeader
import com.example.ui.components.PixiSectionLabel
import com.example.ui.theme.rememberPixiDimens
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun GoalsScreen(
    goals: List<GoalEntity>,
    currencyCode: String,
    goalActivity: List<GoalActivityEntity> = emptyList(),
    onUpdateGoalProgress: (GoalEntity, Double) -> Unit,
    onDeleteGoal: (Int) -> Unit,
    onOpenAddGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val d = rememberPixiDimens()
    val completedCount = goals.count { it.isCompleted }
    val totalCount = goals.size
    val currencySymbol = Currencies.symbolOf(currencyCode)

    var amountGoal by remember { mutableStateOf<GoalEntity?>(null) }

    val activityByGoal = remember(goalActivity) {
        goalActivity.groupBy { it.goalId }
    }
    val overallDayCounts = remember(goalActivity) {
        goalActivity.groupBy { it.dateKey }
            .mapValues { (_, rows) -> rows.sumOf { it.completedCount } }
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
                top = d.screenVertical
            )
        ) {
            item {
                PixiScreenHeader(
                    title = "Goals",
                    subtitle = if (totalCount == 0) "Start with your first milestone"
                    else "$completedCount of $totalCount completed",
                    modifier = Modifier.testTag("goals_summary_card")
                )
                Spacer(modifier = Modifier.height(d.sectionGap))
            }
            item {
                PixiCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (totalCount == 0) "No goals yet"
                                else "$completedCount of $totalCount",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (totalCount == 0) "Add a milestone to track"
                                else "completed · $currencyCode",
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(if (d.isCompact) 48.dp else 52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = "Trophy",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(if (d.isCompact) 22.dp else 24.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(d.sectionGap))
            }

            // Overall goals contribution (GitHub-style) — lives here, not on Tasks
            if (goals.isNotEmpty()) {
                item {
                    ContributionHeatmap(
                        dayCounts = overallDayCounts,
                        title = "Goals activity",
                        subtitle = "Progress updates across all goals",
                        weeks = 17
                    )
                    Spacer(modifier = Modifier.height(d.sectionGap))
                }
            }

            item {
                PixiSectionLabel(text = "Milestones")
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (goals.isEmpty()) {
                item {
                    PixiEmptyState(
                        title = "No goals yet",
                        subtitle = "Tap the yellow + to set your first milestone",
                        doodleRes = null,
                        actionLabel = "Add a goal",
                        onAction = onOpenAddGoal
                    )
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    val goalDays = activityByGoal[goal.id]
                        ?.associate { it.dateKey to it.completedCount }
                        .orEmpty()
                    GoalCardItem(
                        goal = goal,
                        currencyCode = currencyCode,
                        currencySymbol = currencySymbol,
                        dayCounts = goalDays,
                        onAddAmount = { amountGoal = goal },
                        onDelete = { onDeleteGoal(goal.id) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    amountGoal?.let { goal ->
        AddGoalAmountDialog(
            goal = goal,
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            onDismiss = { amountGoal = null },
            onConfirm = { amount ->
                if (amount != 0.0) {
                    onUpdateGoalProgress(goal, amount)
                }
                amountGoal = null
            }
        )
    }
}

@Composable
fun GoalCardItem(
    goal: GoalEntity,
    currencyCode: String,
    currencySymbol: String,
    dayCounts: Map<String, Int> = emptyMap(),
    onAddAmount: () -> Unit,
    onDelete: () -> Unit
) {
    val progressPct = (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(targetValue = progressPct, label = "goalProgress")
    val isMoney = isMoneyUnit(goal.unit, currencySymbol)
    val activeDays = dayCounts.count { it.value > 0 }

    PixiCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_item_${goal.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixiBadge(text = goal.category)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (goal.isCompleted) {
                        PixiBadge(
                            text = "Done",
                            containerColor = Color(0xFF34D399).copy(alpha = 0.2f),
                            contentColor = Color(0xFF34D399)
                        )
                    }
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Goal",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = goal.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatGoalAmount(goal.currentAmount, goal.targetAmount, goal.unit, currencyCode, currencySymbol),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${(progressPct * 100).toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(PixiCardShapeSm),
                color = if (goal.isCompleted) Color(0xFF34D399) else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Per-goal GitHub-style contribution grid
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (activeDays > 0) "Contribution · $activeDays active days"
                else "Contribution · log progress to fill the grid",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            GoalMiniHeatmap(dayCounts = dayCounts, weeks = 12)

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Deadline: ${goal.deadlineStr}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onAddAmount,
                    shape = PixiPillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .height(38.dp)
                        .testTag("add_goal_amount_${goal.id}")
                ) {
                    Text(
                        text = if (isMoney) "Add amount" else "Add progress",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AddGoalAmountDialog(
    goal: GoalEntity,
    currencyCode: String,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    val isMoney = isMoneyUnit(goal.unit, currencySymbol)
    val unitLabel = if (isMoney) currencySymbol else goal.unit

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )

    Dialog(onDismissRequest = onDismiss) {
        PixiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isMoney) "Add amount" else "Add progress",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(onClick = onDismiss)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = goal.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Current: ${formatSingle(goal.currentAmount, goal.unit, currencyCode, currencySymbol)} · Target: ${formatSingle(goal.targetAmount, goal.unit, currencyCode, currencySymbol)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it.filter { ch -> ch.isDigit() || ch == '.' || ch == '-' } },
                    label = { Text(if (isMoney) "Amount ($unitLabel)" else "Amount ($unitLabel)") },
                    placeholder = { Text(if (isMoney) "e.g. 25.50" else "e.g. 1") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_goal_amount"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Tip: use a negative value to subtract progress",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                PixiPrimaryButton(
                    text = "Save",
                    onClick = {
                        val amount = amountStr.toDoubleOrNull() ?: 0.0
                        onConfirm(amount)
                    },
                    modifier = Modifier.testTag("confirm_goal_amount_btn")
                )
            }
        }
    }
}

private fun isMoneyUnit(unit: String, currencySymbol: String): Boolean {
    return unit == "$" || unit == currencySymbol || unit.equals("money", ignoreCase = true)
}

private fun formatGoalAmount(
    current: Double,
    target: Double,
    unit: String,
    currencyCode: String,
    currencySymbol: String
): String {
    return if (isMoneyUnit(unit, currencySymbol)) {
        "${Currencies.format(current, currencyCode)} / ${Currencies.format(target, currencyCode)}"
    } else {
        "${current.toInt()} / ${target.toInt()} $unit"
    }
}

private fun formatSingle(
    amount: Double,
    unit: String,
    currencyCode: String,
    currencySymbol: String
): String {
    return if (isMoneyUnit(unit, currencySymbol)) {
        Currencies.format(amount, currencyCode)
    } else {
        "${amount.toInt()} $unit"
    }
}
