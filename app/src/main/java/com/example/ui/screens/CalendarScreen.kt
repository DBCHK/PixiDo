package com.example.ui.screens

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalendarEventEntity
import com.example.ui.components.PixiBadge
import com.example.ui.components.PixiCard
import com.example.ui.components.PixiCardShapeSm
import com.example.ui.components.PixiEmptyState
import com.example.ui.components.PixiScreenHeader
import com.example.ui.components.PixiSectionLabel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    events: List<CalendarEventEntity>,
    selectedDateMillis: Long,
    onSelectDate: (Long) -> Unit,
    onToggleEvent: (CalendarEventEntity) -> Unit,
    onDeleteEvent: (Int) -> Unit,
    onOpenAddEvent: () -> Unit,
    modifier: Modifier = Modifier
) {
    val datesStrip = remember {
        val cal = Calendar.getInstance()
        // Normalize start of strip to local midnight of "today"
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        (0..13).map {
            val d = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            d
        }
    }

    // Only events that fall on the selected calendar day
    val dayEvents = remember(events, selectedDateMillis) {
        events
            .filter { isSameDay(it.dateMillis, selectedDateMillis) }
            .sortedBy { if (it.startMillis > 0) it.startMillis else it.dateMillis }
    }

    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val numFormat = SimpleDateFormat("dd", Locale.getDefault())
    val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val selectedDayLabel = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        .format(Date(selectedDateMillis))

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 24.dp, top = 16.dp)
        ) {
            item {
                PixiScreenHeader(
                    title = monthYearFormat.format(Date(selectedDateMillis)),
                    subtitle = "Time-block planner",
                    trailing = {
                        PixiBadge(
                            text = "${dayEvents.count { !it.isCompleted }} today"
                        )
                    }
                )
                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(bottom = 22.dp)
                ) {
                    items(datesStrip) { dateMillis ->
                        val isSelected = isSameDay(dateMillis, selectedDateMillis)
                        val dayStr = dayFormat.format(Date(dateMillis))
                        val numStr = numFormat.format(Date(dateMillis))
                        val hasEvents = events.any { isSameDay(it.dateMillis, dateMillis) }

                        Column(
                            modifier = Modifier
                                .clip(PixiCardShapeSm)
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { onSelectDate(dateMillis) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayStr.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = numStr,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            if (hasEvents) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(androidx.compose.foundation.shape.CircleShape)
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

            item {
                PixiSectionLabel(text = "Events · $selectedDayLabel")
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (dayEvents.isEmpty()) {
                item {
                    PixiEmptyState(
                        title = "No events on this day",
                        subtitle = "Tap the yellow + to schedule something for $selectedDayLabel",
                        actionLabel = "Add event",
                        onAction = onOpenAddEvent
                    )
                }
            } else {
                items(dayEvents, key = { it.id }) { event ->
                    CalendarEventCard(
                        event = event,
                        onToggle = { onToggleEvent(event) },
                        onDelete = { onDeleteEvent(event.id) }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun CalendarEventCard(
    event: CalendarEventEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val categoryColor = when (event.category) {
        "Deep Work" -> Color(0xFFC4A8F5)
        "Social & Hangouts", "Social" -> Color(0xFFFF6BA8)
        "Fitness & Wellness", "Fitness" -> Color(0xFF34D399)
        "Bill Payment", "Bills" -> Color(0xFFFBBF24)
        else -> Color(0xFF67D4E8)
    }

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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (event.isCompleted) Icons.Filled.CheckCircle
                    else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = "Toggle Complete",
                    tint = if (event.isCompleted) MaterialTheme.colorScheme.primary else categoryColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (event.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (event.isCompleted) TextDecoration.LineThrough
                    else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = "Time",
                        tint = categoryColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = event.timeSlot,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = categoryColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    PixiBadge(
                        text = event.category,
                        containerColor = categoryColor.copy(alpha = 0.15f),
                        contentColor = categoryColor
                    )
                }
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete Event",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

fun isSameDay(ms1: Long, ms2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = ms1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = ms2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
