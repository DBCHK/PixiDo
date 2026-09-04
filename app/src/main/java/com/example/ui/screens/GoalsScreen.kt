package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.Currencies
import com.example.data.GoalActivityEntity
import com.example.data.GoalEntity
import com.example.data.UserProfile
import com.example.ui.components.ContributionHeatmap
import com.example.ui.components.GoalMiniHeatmap
import com.example.ui.components.PixiCard
import com.example.ui.components.PixiCloseButton
import com.example.ui.components.PixiEmptyState
import com.example.ui.components.PixiIslandContentInset
import com.example.ui.components.PixiFieldShape
import com.example.ui.components.PixiPrimaryButton
import com.example.ui.components.PulseAvatarSpec
import com.example.ui.components.PulseBlackBanner
import com.example.ui.components.PulseCelebrate
import com.example.ui.components.PulseCircleIcon
import com.example.ui.components.PulseDayRing
import com.example.ui.components.PulseMenuItem
import com.example.ui.components.PulseMoreMenu
import com.example.ui.components.PulseProfileAvatar
import com.example.ui.components.PulseSectionHeader
import com.example.ui.components.PulseSurfaceCard
import com.example.ui.components.PulseTaskCard
import com.example.ui.components.PulseTopRow
import com.example.ui.components.pulseAvatarPalette
import com.example.ui.components.pulseInk
import com.example.ui.components.pulseMuted
import com.example.ui.components.pulsePaper
import com.example.ui.theme.PulseMint
import com.example.ui.theme.rememberPixiDimens

@Composable
fun GoalsScreen(
    goals: List<GoalEntity>,
    currencyCode: String,
    goalActivity: List<GoalActivityEntity> = emptyList(),
    profile: UserProfile = UserProfile(),
    onUpdateGoalProgress: (GoalEntity, Double) -> Unit,
    onCompleteSimpleGoal: (GoalEntity) -> Unit = {},
    onDeleteGoal: (Int) -> Unit,
    onOpenAddGoal: () -> Unit,
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sound = LocalSoundEngine.current
    val d = rememberPixiDimens()
    val ink = pulseInk()
    val completedCount = goals.count { it.isCompleted }
    val totalCount = goals.size
    val currencySymbol = Currencies.symbolOf(currencyCode)

    var amountGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var bannerDismissed by remember { mutableStateOf(false) }
    var celebrateBurst by remember { mutableIntStateOf(0) }

    val activityByGoal = remember(goalActivity) {
        goalActivity.groupBy { it.goalId }
    }
    val overallDayCounts = remember(goalActivity) {
        goalActivity.groupBy { it.dateKey }
            .mapValues { (_, rows) -> rows.sumOf { it.completedCount } }
    }
    val simpleGoals = remember(goals) { goals.filter { it.isSimpleTask } }
    val trackedGoals = remember(goals) { goals.filter { !it.isSimpleTask } }
    val openSimple = remember(simpleGoals) { simpleGoals.filter { !it.isCompleted } }
    val doneSimple = remember(simpleGoals) { simpleGoals.filter { it.isCompleted } }
    val progress = if (totalCount == 0) 0f else completedCount / totalCount.toFloat()
    val nextSimple = openSimple.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(pulsePaper())
            .testTag("goals_summary_card")
    ) {
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
                        )
                    },
                    trailing = {
                        PulseCircleIcon(
                            onClick = {
                                sound.play(Sfx.DIALOG_OPEN)
                                onOpenAddGoal()
                            },
                            contentDescription = "Add goal",
                            icon = Icons.Filled.Add
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
                            text = "Stay On Track",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                            lineHeight = 38.sp,
                            letterSpacing = (-0.6).sp
                        )
                        Text(
                            text = "& Finish Strong",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                            lineHeight = 38.sp,
                            letterSpacing = (-0.6).sp
                        )
                    }
                    PulseDayRing(
                        progress = progress,
                        center = if (totalCount == 0) "0" else "$completedCount/$totalCount",
                        caption = when {
                            totalCount == 0 -> "Start"
                            progress >= 0.999f -> "Clear"
                            else -> "Goals"
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (!bannerDismissed) {
                    PulseBlackBanner(
                        text = when {
                            nextSimple != null -> "Next · ${nextSimple.title}"
                            trackedGoals.any { !it.isCompleted } -> "Log progress on a milestone"
                            totalCount == 0 -> "Add a goal and make it real."
                            else -> "You're on track."
                        },
                        onDismiss = { bannerDismissed = true }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (goals.isNotEmpty()) {
                item {
                    ContributionHeatmap(
                        dayCounts = overallDayCounts,
                        title = "Goals activity",
                        subtitle = "Progress updates across all goals",
                        weeks = 17
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (goals.isEmpty()) {
                item {
                    PulseSectionHeader(title = "Simple tasks", action = "Add", onAction = onOpenAddGoal)
                    Spacer(modifier = Modifier.height(12.dp))
                    PixiEmptyState(
                        title = "Nothing to tap yet",
                        subtitle = "Add a simple task and mark it done when you’ve done it",
                        doodleRes = null,
                        actionLabel = "Add a simple task",
                        onAction = onOpenAddGoal
                    )
                }
            } else {
                if (simpleGoals.isNotEmpty() || trackedGoals.isEmpty()) {
                    item {
                        PulseSectionHeader(
                            title = "Simple tasks",
                            action = "Add",
                            onAction = {
                                sound.play(Sfx.DIALOG_OPEN)
                                onOpenAddGoal()
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (simpleGoals.isEmpty()) {
                        item {
                            PixiEmptyState(
                                title = "No simple tasks",
                                subtitle = "Tap done — no amounts, no tracking",
                                doodleRes = null,
                                actionLabel = "Add a simple task",
                                onAction = onOpenAddGoal
                            )
                        }
                    } else {
                        items(openSimple, key = { "simple_${it.id}" }) { goal ->
                            SimpleGoalCard(
                                goal = goal,
                                onYesDone = {
                                    sound.play(Sfx.GOAL_COMPLETE)
                                    celebrateBurst++
                                    onCompleteSimpleGoal(goal)
                                },
                                onDelete = {
                                    sound.play(Sfx.DELETE)
                                    onDeleteGoal(goal.id)
                                }
                            )
                        }
                        if (doneSimple.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                PulseSectionHeader(title = "Done", onAction = null)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            items(doneSimple, key = { "simple_done_${it.id}" }) { goal ->
                                SimpleGoalCard(
                                    goal = goal,
                                    onYesDone = {
                                        sound.play(Sfx.TASK_UNDO)
                                        onCompleteSimpleGoal(goal)
                                    },
                                    onDelete = {
                                        sound.play(Sfx.DELETE)
                                        onDeleteGoal(goal.id)
                                    }
                                )
                            }
                        }
                    }
                }

                if (trackedGoals.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        PulseSectionHeader(
                            title = "Milestones",
                            action = "Add",
                            onAction = {
                                sound.play(Sfx.DIALOG_OPEN)
                                onOpenAddGoal()
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    items(trackedGoals, key = { it.id }) { goal ->
                        val goalDays = activityByGoal[goal.id]
                            ?.associate { it.dateKey to it.completedCount }
                            .orEmpty()
                        GoalCardItem(
                            goal = goal,
                            currencyCode = currencyCode,
                            currencySymbol = currencySymbol,
                            dayCounts = goalDays,
                            onAddAmount = {
                                sound.play(Sfx.DIALOG_OPEN)
                                amountGoal = goal
                            },
                            onDelete = {
                                sound.play(Sfx.DELETE)
                                onDeleteGoal(goal.id)
                            }
                        )
                    }
                }
            }
        }
        PulseCelebrate(burst = celebrateBurst)
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
fun SimpleGoalCard(
    goal: GoalEntity,
    onYesDone: () -> Unit,
    onDelete: () -> Unit
) {
    val desc = listOf(
        goal.category,
        goal.deadlineStr.takeIf { it.isNotBlank() && it != "Whenever" }
    ).filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")
    PulseTaskCard(
        title = goal.title,
        description = desc,
        progress = if (goal.isCompleted) 1f else 0f,
        avatars = listOf(
            PulseAvatarSpec(
                initial = goal.title.trim().firstOrNull()?.uppercase() ?: "G",
                color = pulseAvatarPalette(goal.id)
            )
        ),
        extraCount = 0,
        done = goal.isCompleted,
        menu = listOf(
            PulseMenuItem(
                label = if (goal.isCompleted) "Undo" else "Yes done",
                onClick = onYesDone,
                testTag = if (goal.isCompleted) "undo_goal_${goal.id}" else "yes_done_goal_${goal.id}"
            ),
            PulseMenuItem(label = "Delete", onClick = onDelete, danger = true)
        ),
        modifier = Modifier
            .padding(bottom = 12.dp)
            .testTag("goal_item_${goal.id}"),
        leadingTestTag = "yes_done_goal_${goal.id}",
        onToggle = onYesDone,
        onClick = onYesDone
    )
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
    val desc = listOf(
        goal.category,
        formatGoalAmount(goal.currentAmount, goal.targetAmount, goal.unit, currencyCode, currencySymbol),
        goal.deadlineStr.takeIf { it.isNotBlank() }
    ).filterNotNull().filter { it.isNotBlank() }.joinToString(" · ")

    PulseSurfaceCard(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .testTag("goal_item_${goal.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = goal.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (goal.isCompleted) pulseMuted() else pulseInk(),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            PulseMoreMenu(
                items = listOf(
                    PulseMenuItem(label = "Delete", onClick = onDelete, danger = true)
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = desc,
            fontSize = 13.sp,
            color = pulseMuted(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFF1F1F3))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(50))
                        .background(if (goal.isCompleted) pulseMuted() else PulseMint)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "${(progressPct * 100).toInt()}%",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pulseMuted()
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = if (activeDays > 0) "Contribution · $activeDays active days"
            else "Log progress to fill the grid",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = pulseMuted()
        )
        Spacer(modifier = Modifier.height(8.dp))
        GoalMiniHeatmap(dayCounts = dayCounts, weeks = 12)
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(PulseMint)
                    .clickable(onClick = onAddAmount)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("add_goal_amount_${goal.id}")
            ) {
                Text(
                    text = if (isMoney) "Add amount" else "Add progress",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
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
