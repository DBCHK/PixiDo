package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalendarEventEntity
import com.example.data.DeviceCalendarRepository
import com.example.data.TaskEntity
import com.example.data.UserProfile
import com.example.ui.components.PixiEmptyState
import com.example.ui.components.PixiIslandContentInset
import com.example.ui.components.PixiOutlineButton
import com.example.ui.components.PixiPrimaryButton
import com.example.ui.components.PulseAvatarSpec
import com.example.ui.components.PulseMenuItem
import com.example.ui.components.PulseLegendDot
import com.example.ui.components.PulseSectionHeader
import com.example.ui.components.PulseStripes
import com.example.ui.components.PulseTaskCard
import com.example.ui.components.PulseTopRow
import com.example.ui.components.pulseAvatarPalette
import com.example.ui.components.pulseCard
import com.example.ui.components.pulseInk
import com.example.ui.components.pulseMuted
import com.example.ui.components.pulsePaper
import com.example.ui.theme.PulseCoral
import com.example.ui.theme.PulseInk
import com.example.ui.theme.PulseMint
import com.example.ui.theme.rememberPixiDimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * idea5 calendar — month grid of mint / ink / coral day chips
 * plus the same Today Tasks project cards as the Tasks tab.
 */
@Composable
fun CalendarScreen(
    events: List<CalendarEventEntity>,
    tasks: List<TaskEntity> = emptyList(),
    selectedDateMillis: Long,
    profile: UserProfile = UserProfile(),
    onSelectDate: (Long) -> Unit,
    onToggleEvent: (CalendarEventEntity) -> Unit,
    onDeleteEvent: (Int) -> Unit,
    onOpenAddEvent: () -> Unit,
    onOpenAddTask: () -> Unit = {},
    onToggleTask: (TaskEntity) -> Unit = {},
    onEditTask: (TaskEntity) -> Unit = {},
    onOpenProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val d = rememberPixiDimens()
    val paper = pulsePaper()
    val ink = pulseInk()
    val todayStart = remember { startOfDay(System.currentTimeMillis()) }

    var visibleMonth by remember(selectedDateMillis) {
        mutableStateOf(startOfMonth(selectedDateMillis))
    }
    var seeAll by remember { mutableStateOf(false) }
    var monthMenu by remember { mutableStateOf(false) }

    val dayEvents = remember(events, selectedDateMillis) {
        events
            .filter { isSameDay(it.dateMillis, selectedDateMillis) }
            .sortedBy { if (it.startMillis > 0) it.startMillis else it.dateMillis }
    }
    val dayTasks = remember(tasks, selectedDateMillis, seeAll, visibleMonth) {
        val source = if (seeAll) {
            tasks.filter {
                val c = Calendar.getInstance().apply { timeInMillis = it.dueDateMillis }
                val m = Calendar.getInstance().apply { timeInMillis = visibleMonth }
                c.get(Calendar.YEAR) == m.get(Calendar.YEAR) &&
                    c.get(Calendar.MONTH) == m.get(Calendar.MONTH)
            }
        } else {
            tasks.filter { isSameDay(it.dueDateMillis, selectedDateMillis) }
        }
        source.sortedWith(compareBy<TaskEntity> { it.isCompleted }.thenBy { it.dueDateMillis })
    }
    val dayEventsShown = remember(dayEvents, events, seeAll, visibleMonth) {
        if (!seeAll) dayEvents
        else events.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            val m = Calendar.getInstance().apply { timeInMillis = visibleMonth }
            c.get(Calendar.YEAR) == m.get(Calendar.YEAR) &&
                c.get(Calendar.MONTH) == m.get(Calendar.MONTH)
        }
    }

    val monthTitle = remember(visibleMonth) {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(visibleMonth))
    }
    val monthCells = remember(visibleMonth, events, tasks) {
        buildMonthGrid(visibleMonth, events, tasks)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(paper),
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
                    Box {
                        Row(
                            modifier = Modifier
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(50),
                                    ambientColor = Color.Black.copy(alpha = 0.04f),
                                    spotColor = Color.Black.copy(alpha = 0.08f)
                                )
                                .clip(RoundedCornerShape(50))
                                .background(pulseCard())
                                .clickable { monthMenu = true }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("calendar_month_chip"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = ink,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = monthTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ink
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Change month",
                                tint = pulseMuted(),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = monthMenu,
                            onDismissRequest = { monthMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Today") },
                                onClick = {
                                    monthMenu = false
                                    visibleMonth = startOfMonth(todayStart)
                                    onSelectDate(todayStart)
                                },
                                modifier = Modifier.testTag("calendar_today_btn")
                            )
                            DropdownMenuItem(
                                text = { Text("Previous month") },
                                onClick = {
                                    monthMenu = false
                                    visibleMonth = shiftMonth(visibleMonth, -1)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Next month") },
                                onClick = {
                                    monthMenu = false
                                    visibleMonth = shiftMonth(visibleMonth, 1)
                                }
                            )
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(18.dp))
            PulseCalendarGrid(
                cells = monthCells,
                selectedMillis = selectedDateMillis,
                todayMillis = todayStart,
                onSelect = { millis -> onSelectDate(millis) },
                onLongPress = { millis ->
                    onSelectDate(millis)
                    onOpenAddTask()
                },
                onSwipeMonth = { delta ->
                    visibleMonth = shiftMonth(visibleMonth, delta)
                }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PulseLegendDot(color = PulseMint, label = "Planned")
                PulseLegendDot(color = PulseInk, label = "Done")
                PulseLegendDot(color = PulseCoral, label = "Selected")
            }
            Spacer(modifier = Modifier.height(16.dp))
            PulseSectionHeader(
                title = if (seeAll) "Month Tasks" else "Today Tasks",
                action = if (seeAll) "Today" else "See All",
                onAction = { seeAll = !seeAll }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (dayTasks.isEmpty() && dayEventsShown.isEmpty()) {
            item {
                PixiEmptyState(
                    title = "Free day",
                    subtitle = "Add a task or event for this date — we’ll remind you",
                    actionLabel = "Add a task",
                    onAction = onOpenAddTask
                )
                Spacer(modifier = Modifier.height(8.dp))
                PixiOutlineButton(
                    text = "Add event",
                    onClick = onOpenAddEvent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_empty_add")
                )
            }
        } else {
            items(dayTasks, key = { "task_${it.id}" }) { task ->
                val subs = task.subtasks.split(";").filter { it.isNotBlank() }
                val done = task.completedSubtasks.split(";").filter { it.isNotBlank() }.toSet()
                val progress = when {
                    task.isCompleted -> 1f
                    subs.isEmpty() -> 0.08f
                    else -> done.size.toFloat() / subs.size
                }
                PulseTaskCard(
                    title = task.title,
                    description = task.notes.ifBlank {
                        listOf(task.category, task.dueTimeStr.ifBlank { "All day" })
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                    },
                    progress = progress,
                    avatars = listOf(
                        PulseAvatarSpec(
                            initial = task.title.take(1).uppercase(),
                            color = pulseAvatarPalette(task.id)
                        )
                    ) + subs.take(2).mapIndexed { i, s ->
                        PulseAvatarSpec(s.take(1).uppercase(), pulseAvatarPalette(task.id + i + 3))
                    },
                    extraCount = (subs.size - 2).coerceAtLeast(0),
                    done = task.isCompleted,
                    menu = listOf(
                        PulseMenuItem(
                            label = if (task.isCompleted) "Undo" else "Mark done",
                            onClick = { onToggleTask(task) }
                        ),
                        PulseMenuItem(label = "Edit", onClick = { onEditTask(task) })
                    ),
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .testTag("calendar_task_${task.id}"),
                    onClick = { onEditTask(task) },
                    leadingTestTag = "checkbox_task_${task.id}",
                    onToggle = { onToggleTask(task) }
                )
            }
            items(dayEventsShown, key = { "event_${it.id}_${it.startMillis}" }) { event ->
                val fromDevice = DeviceCalendarRepository.isDeviceEvent(event.id)
                PulseTaskCard(
                    title = event.title,
                    description = listOf(
                        event.timeSlot.ifBlank { "All day" },
                        event.category
                    ).filter { it.isNotBlank() }.joinToString(" · "),
                    progress = if (event.isCompleted) 1f else 0.35f,
                    avatars = listOf(
                        PulseAvatarSpec(
                            initial = event.title.take(1).uppercase(),
                            color = categoryColorOf(event.category, fromDevice)
                        )
                    ),
                    extraCount = 0,
                    done = event.isCompleted,
                    menu = buildList {
                        if (!fromDevice) {
                            add(
                                PulseMenuItem(
                                    label = if (event.isCompleted) "Undo" else "Mark done",
                                    onClick = { onToggleEvent(event) }
                                )
                            )
                            add(
                                PulseMenuItem(
                                    label = "Delete",
                                    onClick = { onDeleteEvent(event.id) },
                                    danger = true
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .testTag("event_item_${event.id}"),
                    onClick = { if (!fromDevice) onToggleEvent(event) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixiOutlineButton(
                        text = "Add task",
                        onClick = onOpenAddTask,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calendar_add_task_btn")
                    )
                    PixiPrimaryButton(
                        text = "Add event",
                        onClick = onOpenAddEvent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PulseCalendarGrid(
    cells: List<MonthCell>,
    selectedMillis: Long,
    todayMillis: Long,
    onSelect: (Long) -> Unit,
    onLongPress: (Long) -> Unit = {},
    onSwipeMonth: (Int) -> Unit = {}
) {
    var dragAcc by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(pulseCard())
            .pointerInput(selectedMillis) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragAcc += amount
                    },
                    onDragEnd = {
                        when {
                            dragAcc > 72f -> onSwipeMonth(-1)
                            dragAcc < -72f -> onSwipeMonth(1)
                        }
                        dragAcc = 0f
                    },
                    onDragCancel = { dragAcc = 0f }
                )
            }
            .testTag("calendar_month_card")
    ) {
        PulseStripes(
            modifier = Modifier.matchParentSize(),
            color = com.example.ui.theme.PulseStripe
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                    Text(
                        text = label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = pulseMuted()
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            cells.chunked(7).forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    week.forEach { cell ->
                        PulseDayCell(
                            cell = cell,
                            isSelected = cell.millis != null &&
                                isSameDay(cell.millis, selectedMillis),
                            isToday = cell.millis != null &&
                                isSameDay(cell.millis, todayMillis),
                            onClick = { cell.millis?.let(onSelect) },
                            onLongPress = { cell.millis?.let(onLongPress) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

private data class MonthCell(
    val dayNumber: Int?,
    val millis: Long?,
    val inMonth: Boolean,
    val hasEvents: Boolean,
    val eventCount: Int,
    val hasOpen: Boolean = false,
    val allDone: Boolean = false
)

@Composable
private fun PulseDayCell(
    cell: MonthCell,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = when {
            !cell.inMonth || cell.dayNumber == null -> Color.Transparent
            isSelected -> PulseCoral
            isToday -> PulseInk
            cell.allDone && cell.hasEvents -> PulseInk
            cell.hasEvents || cell.hasOpen -> PulseMint
            else -> Color.Transparent
        },
        label = "pulseDayBg"
    )
    val fg by animateColorAsState(
        targetValue = when {
            !cell.inMonth -> Color.Transparent
            isSelected || isToday || ((cell.hasEvents || cell.hasOpen) && bg != Color.Transparent) ->
                Color.White
            else -> pulseInk()
        },
        label = "pulseDayFg"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(1.5.dp),
        contentAlignment = Alignment.Center
    ) {
        if (cell.dayNumber != null && cell.inMonth) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(bg)
                    .pointerInput(cell.millis) {
                        if (cell.millis == null) return@pointerInput
                        detectTapGestures(
                            onTap = { onClick() },
                            onLongPress = { onLongPress() }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cell.dayNumber.toString(),
                    fontSize = 15.sp,
                    fontWeight = if (isSelected || isToday || cell.hasEvents) FontWeight.Bold
                    else FontWeight.Medium,
                    color = fg
                )
            }
        }
    }
}

private fun categoryColorOf(category: String, fromDevice: Boolean = false): Color = when {
    fromDevice -> Color(0xFF64D2FF)
    category == "Deep Work" -> Color(0xFFC4A8F5)
    category == "Social & Hangouts" || category == "Social" -> Color(0xFFFF6BA8)
    category == "Fitness & Wellness" || category == "Fitness" -> PulseMint
    category == "Bill Payment" || category == "Bills" -> Color(0xFFFBBF24)
    category == "Meeting" -> Color(0xFF67D4E8)
    else -> Color(0xFF9B7AE8)
}

private fun startOfDay(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun startOfMonth(millis: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun shiftMonth(monthStart: Long, delta: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = monthStart
        add(Calendar.MONTH, delta)
    }.timeInMillis

private fun buildMonthGrid(
    monthStart: Long,
    events: List<CalendarEventEntity>,
    tasks: List<TaskEntity> = emptyList()
): List<MonthCell> {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStart }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDow = cal.get(Calendar.DAY_OF_WEEK)
    val leading = firstDow - Calendar.SUNDAY

    val eventDays = events
        .filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                c.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        }
        .groupBy {
            Calendar.getInstance().apply { timeInMillis = it.dateMillis }
                .get(Calendar.DAY_OF_MONTH)
        }

    val taskDays = tasks
        .filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dueDateMillis }
            c.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                c.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        }
        .groupBy {
            Calendar.getInstance().apply { timeInMillis = it.dueDateMillis }
                .get(Calendar.DAY_OF_MONTH)
        }

    val cells = mutableListOf<MonthCell>()
    repeat(leading) {
        cells += MonthCell(null, null, inMonth = false, hasEvents = false, eventCount = 0)
    }
    for (day in 1..daysInMonth) {
        val dayCal = Calendar.getInstance().apply {
            timeInMillis = monthStart
            set(Calendar.DAY_OF_MONTH, day)
        }
        val dayTasks = taskDays[day].orEmpty()
        val dayEvents = eventDays[day].orEmpty()
        val count = dayEvents.size + dayTasks.size
        val open = dayTasks.any { !it.isCompleted } || dayEvents.any { !it.isCompleted }
        val allDone = count > 0 && dayTasks.all { it.isCompleted } && dayEvents.all { it.isCompleted }
        cells += MonthCell(
            dayNumber = day,
            millis = dayCal.timeInMillis,
            inMonth = true,
            hasEvents = count > 0,
            eventCount = count,
            hasOpen = open,
            allDone = allDone
        )
    }
    while (cells.size % 7 != 0) {
        cells += MonthCell(null, null, inMonth = false, hasEvents = false, eventCount = 0)
    }
    return cells
}

fun isSameDay(ms1: Long, ms2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = ms1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = ms2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
