package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CalendarEventEntity
import com.example.data.TaskEntity
import com.example.ui.components.PixiBadge
import com.example.ui.components.PixiCard
import com.example.ui.components.PixiDoodle3D
import com.example.ui.components.PixiOutlineButton
import com.example.ui.components.PixiPillShape
import com.example.ui.components.PixiPrimaryButton
import com.example.ui.theme.rememberPixiDimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Practical Soft Lilac calendar:
 *  - Month grid with circular day cells (reference chip language)
 *  - Prev / next circular nav
 *  - Selected-day agenda as a time-block timeline
 *  - Tasks due on a day appear alongside events
 */
@Composable
fun CalendarScreen(
    events: List<CalendarEventEntity>,
    tasks: List<TaskEntity> = emptyList(),
    selectedDateMillis: Long,
    onSelectDate: (Long) -> Unit,
    onToggleEvent: (CalendarEventEntity) -> Unit,
    onDeleteEvent: (Int) -> Unit,
    onOpenAddEvent: () -> Unit,
    onOpenAddTask: () -> Unit = {},
    onToggleTask: (TaskEntity) -> Unit = {},
    onEditTask: (TaskEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val d = rememberPixiDimens()
    val todayStart = remember { startOfDay(System.currentTimeMillis()) }

    // Visible month (first day of month)
    var visibleMonth by remember(selectedDateMillis) {
        mutableStateOf(startOfMonth(selectedDateMillis))
    }

    val dayEvents = remember(events, selectedDateMillis) {
        events
            .filter { isSameDay(it.dateMillis, selectedDateMillis) }
            .sortedBy { if (it.startMillis > 0) it.startMillis else it.dateMillis }
    }

    val dayTasks = remember(tasks, selectedDateMillis) {
        tasks
            .filter { isSameDay(it.dueDateMillis, selectedDateMillis) }
            .sortedWith(compareBy<TaskEntity> { it.isCompleted }.thenBy { it.dueDateMillis })
    }

    val monthTitle = remember(visibleMonth) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(visibleMonth))
    }
    val selectedLabel = remember(selectedDateMillis) {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date(selectedDateMillis))
    }

    val monthCells = remember(visibleMonth, events, tasks) {
        buildMonthGrid(visibleMonth, events, tasks)
    }

    val upcomingCount = remember(events, tasks, todayStart) {
        events.count { !it.isCompleted && it.dateMillis >= todayStart } +
            tasks.count { !it.isCompleted && startOfDay(it.dueDateMillis) >= todayStart }
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
                top = d.screenVertical,
                bottom = d.screenVertical + 8.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Header ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calendar",
                            style = MaterialTheme.typography.displayLarge,
                            fontSize = d.title,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (upcomingCount == 0) "Nothing upcoming" else "$upcomingCount upcoming",
                            fontSize = d.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    // Jump to today
                    Box(
                        modifier = Modifier
                            .clip(PixiPillShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable {
                                visibleMonth = startOfMonth(todayStart)
                                onSelectDate(todayStart)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .testTag("calendar_today_btn")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Today,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(d.iconSm)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Today",
                                fontSize = d.caption,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(d.sectionGap))
            }

            // ── Month navigator + grid card ─────────────────────────
            item {
                PixiCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calendar_month_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(d.cardPadding)
                    ) {
                        // Month row with circular chevrons (reference language)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircleNavButton(
                                onClick = {
                                    val prev = Calendar.getInstance().apply {
                                        timeInMillis = visibleMonth
                                        add(Calendar.MONTH, -1)
                                    }.timeInMillis
                                    visibleMonth = startOfMonth(prev)
                                },
                                filled = false
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous month",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Text(
                                text = monthTitle,
                                fontSize = d.headline,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp)
                            )

                            CircleNavButton(
                                onClick = {
                                    val next = Calendar.getInstance().apply {
                                        timeInMillis = visibleMonth
                                        add(Calendar.MONTH, 1)
                                    }.timeInMillis
                                    visibleMonth = startOfMonth(next)
                                },
                                filled = true
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next month",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Weekday headers
                        Row(modifier = Modifier.fillMaxWidth()) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
                                Text(
                                    text = label,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = d.label,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Days grid — 7 columns
                        monthCells.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                week.forEach { cell ->
                                    DayCell(
                                        cell = cell,
                                        isSelected = cell.millis != null &&
                                            isSameDay(cell.millis, selectedDateMillis),
                                        isToday = cell.millis != null &&
                                            isSameDay(cell.millis, todayStart),
                                        onClick = {
                                            cell.millis?.let {
                                                onSelectDate(it)
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(d.sectionGap))
            }

            // ── Selected day agenda header ──────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedLabel,
                            fontSize = d.headline,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                dayEvents.isEmpty() && dayTasks.isEmpty() -> "No plans yet"
                                else -> {
                                    val e = if (dayEvents.isEmpty()) ""
                                    else "${dayEvents.size} event${if (dayEvents.size == 1) "" else "s"}"
                                    val t = if (dayTasks.isEmpty()) ""
                                    else "${dayTasks.size} task${if (dayTasks.size == 1) "" else "s"}"
                                    listOf(e, t).filter { it.isNotBlank() }.joinToString(" · ")
                                }
                            },
                            fontSize = d.caption,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    PixiBadge(
                        text = if (isSameDay(selectedDateMillis, todayStart)) "Today"
                        else SimpleDateFormat("MMM d", Locale.getDefault())
                            .format(Date(selectedDateMillis))
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Time-block agenda ───────────────────────────────────
            if (dayEvents.isEmpty() && dayTasks.isEmpty()) {
                item {
                    PixiCard(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(d.cardPadding),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Free day",
                                fontSize = d.headline,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Add a task or event for this date — we’ll remind you",
                                fontSize = d.caption,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            PixiPrimaryButton(
                                text = "Add task for this day",
                                onClick = onOpenAddTask,
                                modifier = Modifier
                                    .fillMaxWidth(if (d.isCompact) 1f else 0.7f)
                                    .testTag("calendar_empty_add_task")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            PixiOutlineButton(
                                text = "Add event",
                                onClick = onOpenAddEvent,
                                modifier = Modifier
                                    .fillMaxWidth(if (d.isCompact) 1f else 0.7f)
                                    .testTag("calendar_empty_add")
                            )
                        }
                    }
                }
            } else {
                if (dayTasks.isNotEmpty()) {
                    item {
                        Text(
                            text = "Tasks",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(dayTasks, key = { "task_${it.id}" }) { task ->
                        CalendarTaskRow(
                            task = task,
                            onToggle = { onToggleTask(task) },
                            onEdit = { onEditTask(task) },
                            compact = d.isCompact
                        )
                        Spacer(modifier = Modifier.height(d.listGap))
                    }
                }
                if (dayEvents.isNotEmpty()) {
                    item {
                        Text(
                            text = "Events",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }
                    items(dayEvents, key = { "event_${it.id}" }) { event ->
                        TimelineEventRow(
                            event = event,
                            onToggle = { onToggleEvent(event) },
                            onDelete = { onDeleteEvent(event.id) },
                            compact = d.isCompact
                        )
                        Spacer(modifier = Modifier.height(d.listGap))
                    }
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

            // Quick hours overview for the day (like reference time list)
            item {
                Spacer(modifier = Modifier.height(d.sectionGap))
                Text(
                    text = "Day overview",
                    fontSize = d.headline,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                DayHourOverview(
                    dayEvents = dayEvents,
                    dayTasks = dayTasks,
                    selectedDateMillis = selectedDateMillis
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CircleNavButton(
    onClick: () -> Unit,
    filled: Boolean,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private data class MonthCell(
    val dayNumber: Int?,
    val millis: Long?,
    val inMonth: Boolean,
    val hasEvents: Boolean,
    val eventCount: Int
)

@Composable
private fun CalendarTaskRow(
    task: TaskEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    compact: Boolean
) {
    val accent = when (task.priority) {
        "HIGH_FIRE" -> Color(0xFFFF6BA8)
        "QUICK_WIN" -> Color(0xFF67D4E8)
        "CORE_GOAL" -> Color(0xFFC4A8F5)
        else -> Color(0xFF9B7AE8)
    }
    PixiCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("calendar_task_${task.id}")
            .clickable(onClick = onEdit),
        containerColor = if (task.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (compact) 44.dp else 52.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Filled.CheckCircle
                    else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Toggle task",
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = if (compact) 14.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough
                    else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = task.dueTimeStr.ifBlank { "All day" },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    PixiBadge(
                        text = if (task.isRepeating) task.repeat.shortLabel else "Task",
                        containerColor = accent.copy(alpha = 0.15f),
                        contentColor = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    cell: MonthCell,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        label = "dayBg"
    )
    val fg by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            !cell.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.onSurface
        },
        label = "dayFg"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(bg)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                } else Modifier
            )
            .clickable(
                enabled = cell.millis != null && cell.inMonth,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (cell.dayNumber != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = cell.dayNumber.toString(),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium,
                    color = fg
                )
                if (cell.hasEvents) {
                    Box(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.primary
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineEventRow(
    event: CalendarEventEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    compact: Boolean
) {
    val categoryColor = categoryColorOf(event.category)
    val timeLabel = event.timeSlot.ifBlank { "All day" }

    PixiCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("event_item_${event.id}"),
        containerColor = if (event.isCompleted) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Soft left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (compact) 44.dp else 52.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(categoryColor)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Time column
            Column(
                modifier = Modifier.width(if (compact) 56.dp else 68.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = timeLabel.split("–", "-").first().trim(),
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = categoryColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onToggle,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (event.isCompleted) Icons.Filled.CheckCircle
                    else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Toggle",
                    tint = if (event.isCompleted) MaterialTheme.colorScheme.primary else categoryColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = if (compact) 14.sp else 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (event.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough
                    else TextDecoration.None,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = categoryColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = categoryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    PixiBadge(
                        text = event.category,
                        containerColor = categoryColor.copy(alpha = 0.15f),
                        contentColor = categoryColor
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.DeleteOutline,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Hour strip showing busy hours for the selected day — inspired by the
 * reference start-time list with soft radio-style rows.
 */
@Composable
private fun DayHourOverview(
    dayEvents: List<CalendarEventEntity>,
    dayTasks: List<TaskEntity>,
    selectedDateMillis: Long
) {
    val titlesByHour = remember(dayEvents, dayTasks, selectedDateMillis) {
        val map = mutableMapOf<Int, MutableList<String>>()
        dayEvents.forEach { event ->
            val hour = eventHourOf(event) ?: return@forEach
            map.getOrPut(hour) { mutableListOf() }.add(event.title)
        }
        dayTasks.forEach { task ->
            val hour = millisHourOf(task.dueDateMillis)
            map.getOrPut(hour) { mutableListOf() }.add(task.title)
        }
        map
    }
    val hours = remember(titlesByHour) {
        val base = (8..20).toMutableSet()
        base.addAll(titlesByHour.keys)
        base.sorted()
    }

    PixiCard(modifier = Modifier.fillMaxWidth().testTag("day_hour_overview")) {
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            hours.forEach { hour ->
                val titles = titlesByHour[hour].orEmpty()
                val busy = titles.isNotEmpty()
                val label = if (busy) titles.joinToString(" · ") else "—"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = String.format("%02d:00", hour),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (busy) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(52.dp)
                    )
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = if (busy) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .background(
                                if (busy) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (busy) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimary)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = label,
                        fontSize = 13.sp,
                        fontWeight = if (busy) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (busy) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (hour != hours.last()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 66.dp)
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}

private fun millisHourOf(millis: Long): Int =
    Calendar.getInstance().apply { timeInMillis = millis }.get(Calendar.HOUR_OF_DAY)

// ── helpers ──────────────────────────────────────────────────────────

private fun categoryColorOf(category: String): Color = when (category) {
    "Deep Work" -> Color(0xFFC4A8F5)
    "Social & Hangouts", "Social" -> Color(0xFFFF6BA8)
    "Fitness & Wellness", "Fitness" -> Color(0xFF34D399)
    "Bill Payment", "Bills" -> Color(0xFFFBBF24)
    "Meeting" -> Color(0xFF67D4E8)
    else -> Color(0xFF9B7AE8)
}

private fun eventHourOf(event: CalendarEventEntity): Int? {
    if (event.startMillis > 0) {
        return Calendar.getInstance().apply { timeInMillis = event.startMillis }
            .get(Calendar.HOUR_OF_DAY)
    }
    // Parse "10:00 AM" style from timeSlot
    val first = event.timeSlot.split("–", "-").firstOrNull()?.trim().orEmpty()
    val m = Regex("""(?i)(\d{1,2})""").find(first) ?: return null
    var h = m.groupValues[1].toIntOrNull() ?: return null
    if (first.contains("PM", ignoreCase = true) && h < 12) h += 12
    if (first.contains("AM", ignoreCase = true) && h == 12) h = 0
    return h
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

private fun buildMonthGrid(
    monthStart: Long,
    events: List<CalendarEventEntity>,
    tasks: List<TaskEntity> = emptyList()
): List<MonthCell> {
    val cal = Calendar.getInstance().apply { timeInMillis = monthStart }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    // Calendar.SUNDAY = 1 … Saturday = 7 → leading blanks for Sunday-start grid
    val firstDow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun
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
        val count = (eventDays[day]?.size ?: 0) + (taskDays[day]?.size ?: 0)
        cells += MonthCell(
            dayNumber = day,
            millis = dayCal.timeInMillis,
            inMonth = true,
            hasEvents = count > 0,
            eventCount = count
        )
    }
    // Pad to full weeks
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
