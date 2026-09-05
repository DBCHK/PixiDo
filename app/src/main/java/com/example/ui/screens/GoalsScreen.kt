package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.HabitStats
import com.example.data.UserProfile
import com.example.ui.components.ContributionHeatmap
import com.example.ui.components.GoalMiniHeatmap
import com.example.ui.components.PixiCard
import com.example.ui.components.PixiCloseButton
import com.example.ui.components.PixiEmptyState
import com.example.ui.components.PixiIslandContentInset
import com.example.ui.components.PixiFieldShape
import com.example.ui.components.PixiPrimaryButton
import com.example.ui.components.PulseCelebrate
import com.example.ui.components.PulseCircleIcon
import com.example.ui.components.PulseMenuItem
import com.example.ui.components.PulseMoreMenu
import com.example.ui.components.PulseProfileAvatar
import com.example.ui.components.PulseSectionHeader
import com.example.ui.components.PulseSurfaceCard
import com.example.ui.components.isPulseDark
import com.example.ui.components.pulseInk
import com.example.ui.components.pulseMuted
import com.example.ui.components.pulsePaper
import com.example.ui.theme.GoalsMintWash
import com.example.ui.theme.GoalsMintWashDark
import com.example.ui.theme.PulseMint
import com.example.ui.theme.PulseMintDeep
import com.example.ui.theme.rememberPixiDimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun GoalsScreen(
    goals: List<GoalEntity>,
    currencyCode: String,
    goalActivity: List<GoalActivityEntity> = emptyList(),
    profile: UserProfile = UserProfile(),
    onUpdateGoalProgress: (GoalEntity, Double) -> Unit,
    onToggleHabit: (GoalEntity, String) -> Unit = { _, _ -> },
    onDeleteGoal: (Int) -> Unit,
    onOpenAddGoal: () -> Unit,
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sound = LocalSoundEngine.current
    val d = rememberPixiDimens()
    val ink = pulseInk()
    val currencySymbol = Currencies.symbolOf(currencyCode)

    var amountGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var celebrateBurst by remember { mutableIntStateOf(0) }

    val todayKey = remember { HabitStats.dayKey() }
    val weekKeys = remember(todayKey) { HabitStats.weekKeys(todayKey) }
    val activityByGoal = remember(goalActivity) {
        goalActivity.groupBy { it.goalId }
    }
    val overallDayCounts = remember(goalActivity) {
        goalActivity.groupBy { it.dateKey }
            .mapValues { (_, rows) -> rows.sumOf { it.completedCount } }
    }
    val habits = remember(goals) { goals.filter { it.isDailyHabit } }
    val trackedGoals = remember(goals) { goals.filter { it.isMilestone } }
    val habitDoneToday = remember(habits, activityByGoal, todayKey) {
        habits.count { habit ->
            HabitStats.isDoneOn(HabitStats.doneDays(activityByGoal[habit.id].orEmpty()), todayKey)
        }
    }
    val habitTotal = habits.size
    val longestStreak = remember(habits, activityByGoal, todayKey) {
        habits.maxOfOrNull { habit ->
            HabitStats.currentStreak(HabitStats.doneDays(activityByGoal[habit.id].orEmpty()), todayKey)
        } ?: 0
    }
    val completedMilestones = trackedGoals.count { it.isCompleted }
    val progress = when {
        habitTotal > 0 -> habitDoneToday / habitTotal.toFloat()
        trackedGoals.isNotEmpty() -> completedMilestones / trackedGoals.size.toFloat()
        else -> 0f
    }

    val greeting = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Good morning!"
            hour < 17 -> "Good afternoon!"
            else -> "Good evening!"
        }
    }
    val displayName = profile.displayName.trim().ifBlank { "there" }
    val weekStrip = remember(weekKeys, todayKey, overallDayCounts) {
        weekKeys.map { key ->
            val cal = runCatching {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(key)
            }.getOrNull()?.let { date ->
                Calendar.getInstance().apply { time = date }
            }
            Triple(
                key,
                cal?.get(Calendar.DAY_OF_MONTH) ?: 0,
                (overallDayCounts[key] ?: 0) > 0
            )
        }
    }
    val monthTitle = remember(todayKey) {
        SimpleDateFormat("MMMM yyyy", Locale.US).format(Calendar.getInstance().time)
    }
    val weekLabels = listOf("S", "M", "T", "W", "T", "F", "S")

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulseProfileAvatar(profile = profile, onClick = onOpenProfile, size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = greeting,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = pulseMuted()
                        )
                        Text(
                            text = displayName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = ink,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    PulseCircleIcon(
                        onClick = {
                            sound.play(Sfx.DIALOG_OPEN)
                            onOpenAddGoal()
                        },
                        contentDescription = "Add habit or goal",
                        icon = Icons.Filled.Add
                    )
                }
                Spacer(modifier = Modifier.height(18.dp))
                GoalsHeroCard(
                    progress = progress,
                    streakDays = longestStreak,
                    habitsTotal = habitTotal
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GoalsMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.DirectionsWalk,
                        iconTint = Color(0xFFFF8A4C),
                        label = "Habits today",
                        value = if (habitTotal > 0) "$habitDoneToday/$habitTotal" else "0",
                        unit = if (habitTotal == 1) "habit" else "habits"
                    )
                    GoalsMetricTile(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.WaterDrop,
                        iconTint = Color(0xFF4DA3FF),
                        label = "Current streak",
                        value = longestStreak.toString(),
                        unit = if (longestStreak == 1) "day" else "days"
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                GoalsWeekStrip(
                    monthTitle = monthTitle,
                    labels = weekLabels,
                    days = weekStrip,
                    todayKey = todayKey
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (goals.isNotEmpty()) {
                item {
                    ContributionHeatmap(
                        dayCounts = overallDayCounts,
                        title = "Activity",
                        weeks = 17
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (goals.isEmpty()) {
                item {
                    PulseSectionHeader(title = "Habits", action = "Add", onAction = onOpenAddGoal)
                    Spacer(modifier = Modifier.height(12.dp))
                    PixiEmptyState(
                        title = "No habits yet",
                        subtitle = "Add a daily habit and check it off the days you keep it",
                        doodleRes = null,
                        actionLabel = "Add a habit",
                        onAction = onOpenAddGoal
                    )
                }
            } else {
                item {
                    PulseSectionHeader(
                        title = "Habits",
                        action = "Add",
                        onAction = {
                            sound.play(Sfx.DIALOG_OPEN)
                            onOpenAddGoal()
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (habits.isEmpty()) {
                    item {
                        PixiEmptyState(
                            title = "No daily habits",
                            subtitle = "Habits reset every morning — tap a day to log it",
                            doodleRes = null,
                            actionLabel = "Add a habit",
                            onAction = onOpenAddGoal
                        )
                    }
                } else {
                    items(habits, key = { "habit_${it.id}" }) { habit ->
                        val days = HabitStats.doneDays(activityByGoal[habit.id].orEmpty())
                        HabitCard(
                            habit = habit,
                            doneDays = days,
                            todayKey = todayKey,
                            weekKeys = weekKeys,
                            onToggleDay = { dateKey ->
                                val nowDone = !HabitStats.isDoneOn(days, dateKey)
                                if (nowDone) {
                                    sound.play(Sfx.GOAL_COMPLETE)
                                    if (dateKey == todayKey) celebrateBurst++
                                } else {
                                    sound.play(Sfx.TASK_UNDO)
                                }
                                onToggleHabit(habit, dateKey)
                            },
                            onDelete = {
                                sound.play(Sfx.DELETE)
                                onDeleteGoal(habit.id)
                            }
                        )
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
private fun GoalsHeroCard(
    progress: Float,
    streakDays: Int,
    habitsTotal: Int
) {
    val wash = if (isPulseDark()) GoalsMintWashDark else GoalsMintWash
    val t by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "heroRing"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(wash)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = null,
                            tint = PulseMintDeep,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Daily streak",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pulseInk().copy(alpha = 0.72f)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Your Weekly\nProgress",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = pulseInk(),
                    lineHeight = 30.sp,
                    letterSpacing = (-0.4).sp
                )
            }
            Box(
                modifier = Modifier.size(84.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val stroke = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                    drawArc(
                        color = Color.White.copy(alpha = 0.45f),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = stroke
                    )
                    val sweep = if (habitsTotal > 0) 360f * t
                    else if (streakDays > 0) (streakDays.coerceAtMost(7) / 7f) * 360f
                    else 0f
                    if (sweep > 0f) {
                        drawArc(
                            color = PulseMintDeep,
                            startAngle = -90f,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = stroke
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = streakDays.toString(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = pulseInk()
                    )
                    Text(
                        text = if (streakDays == 1) "day" else "days",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = pulseMuted()
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalsMetricTile(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    PulseSurfaceCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = pulseInk(),
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = pulseInk()
        )
        Text(
            text = unit,
            fontSize = 12.sp,
            color = pulseMuted()
        )
    }
}

@Composable
private fun GoalsWeekStrip(
    monthTitle: String,
    labels: List<String>,
    days: List<Triple<String, Int, Boolean>>,
    todayKey: String
) {
    PulseSurfaceCard {
        Text(
            text = monthTitle,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = pulseInk()
        )
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEachIndexed { index, (key, number, active) ->
                val today = key == todayKey
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = labels.getOrElse(index) { "" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = pulseMuted()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    today -> PulseMint
                                    active -> PulseMint.copy(alpha = 0.16f)
                                    else -> Color.Transparent
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = number.toString().padStart(2, '0'),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                today -> Color.White
                                active -> pulseInk()
                                else -> pulseMuted()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalGlyph(
    goal: GoalEntity,
    modifier: Modifier = Modifier
) {
    val (icon, tint) = goalGlyph(goal)
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}

private fun goalGlyph(goal: GoalEntity): Pair<ImageVector, Color> {
    val hay = (goal.category + " " + goal.title).lowercase(Locale.US)
    return when {
        "water" in hay || "drink" in hay -> Icons.Filled.WaterDrop to Color(0xFF4DA3FF)
        "walk" in hay || "step" in hay -> Icons.AutoMirrored.Filled.DirectionsWalk to Color(0xFFFF8A4C)
        "save" in hay || "fund" in hay || "money" in hay || hay.contains("$") ->
            Icons.Filled.Savings to PulseMintDeep
        else -> Icons.Filled.Flag to Color(0xFF7B74F6)
    }
}

@Composable
fun HabitCard(
    habit: GoalEntity,
    doneDays: Set<String>,
    todayKey: String,
    weekKeys: List<String>,
    onToggleDay: (String) -> Unit,
    onDelete: () -> Unit
) {
    val doneToday = HabitStats.isDoneOn(doneDays, todayKey)
    val streak = HabitStats.currentStreak(doneDays, todayKey)
    val cue = habit.deadlineStr.takeIf { raw ->
        val it = raw.trim()
        it.isNotBlank() &&
            !it.equals("Whenever", ignoreCase = true) &&
            !it.equals("Daily", ignoreCase = true) &&
            !it.contains("end of", ignoreCase = true) &&
            !it.matches(Regex("\\d{4}"))
    }
    val desc = listOfNotNull(
        habit.category.takeIf { it.isNotBlank() },
        cue,
        if (streak > 0) "$streak-day streak" else "Start a streak"
    ).joinToString(" · ")

    PulseSurfaceCard(
        modifier = Modifier
            .padding(bottom = 12.dp)
            .testTag("goal_item_${habit.id}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoalGlyph(goal = habit)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = pulseInk(),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = desc,
                    fontSize = 13.sp,
                    color = pulseMuted(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HabitCheck(
                done = doneToday,
                onClick = { onToggleDay(todayKey) },
                modifier = Modifier.testTag("habit_check_${habit.id}")
            )
            Spacer(modifier = Modifier.width(4.dp))
            PulseMoreMenu(
                items = listOf(
                    PulseMenuItem(
                        label = if (doneToday) "Undo today" else "Done today",
                        onClick = { onToggleDay(todayKey) },
                        testTag = if (doneToday) "undo_goal_${habit.id}" else "yes_done_goal_${habit.id}"
                    ),
                    PulseMenuItem(label = "Delete", onClick = onDelete, danger = true)
                )
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        HabitWeekDots(
            weekKeys = weekKeys,
            doneDays = doneDays,
            todayKey = todayKey,
            onToggleDay = onToggleDay,
            habitId = habit.id
        )
    }
}

@Composable
private fun HabitCheck(
    done: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (done) PulseMint else Color(0xFFF1F1F3))
            .then(
                if (done) Modifier
                else Modifier.border(1.5.dp, Color(0xFFE4E4E7), CircleShape)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (done) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Done today",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun HabitWeekDots(
    weekKeys: List<String>,
    doneDays: Set<String>,
    todayKey: String,
    onToggleDay: (String) -> Unit,
    habitId: Int
) {
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekKeys.forEachIndexed { index, key ->
            val done = HabitStats.isDoneOn(doneDays, key)
            val today = key == todayKey
            val future = HabitStats.isFuture(key, todayKey)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = labels.getOrElse(index) { "" },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = pulseMuted()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                done -> PulseMint
                                today -> PulseMint.copy(alpha = 0.16f)
                                else -> Color(0xFFF1F1F3)
                            }
                        )
                        .then(
                            if (today && !done) Modifier.border(1.5.dp, PulseMint, CircleShape)
                            else Modifier
                        )
                        .clickable(enabled = !future) { onToggleDay(key) }
                        .testTag("habit_dot_${habitId}_$key"),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
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
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoalGlyph(goal = goal)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = goal.title,
                fontSize = 16.sp,
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
