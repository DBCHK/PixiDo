package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AuraRepository
import com.example.data.DailyActivityEntity
import com.example.data.TaskEntity
import java.util.Calendar

@Composable
fun StatsStrip(
    tasks: List<TaskEntity>,
    activity: List<DailyActivityEntity>,
    modifier: Modifier = Modifier
) {
    val pending = remember(tasks) { tasks.count { !it.isCompleted } }
    val done = remember(tasks) { tasks.count { it.isCompleted } }
    val todayKey = remember { AuraRepository.dayKey() }
    val todayDone = remember(activity, todayKey) {
        activity.find { it.dateKey == todayKey }?.completedCount ?: 0
    }
    val streak = remember(activity) { computeStreak(activity) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_strip"),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCell("Pending", pending.toString(), Modifier.weight(1f))
        StatCell("Done", done.toString(), Modifier.weight(1f))
        StatCell("Today", todayDone.toString(), Modifier.weight(1f))
        StatCell("Streak", "${streak}d", Modifier.weight(1f))
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(PixiCardShapeSm)
            .background(MaterialTheme.colorScheme.surface)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Consecutive days (ending today or yesterday) with at least one completion. */
private fun computeStreak(activity: List<DailyActivityEntity>): Int {
    if (activity.isEmpty()) return 0
    val map = activity.associate { it.dateKey to it.completedCount }
    val cal = Calendar.getInstance()
    var streak = 0
    val todayKey = AuraRepository.dayKey(cal.timeInMillis)
    if ((map[todayKey] ?: 0) == 0) {
        cal.add(Calendar.DAY_OF_YEAR, -1)
    }
    while (true) {
        val key = AuraRepository.dayKey(cal.timeInMillis)
        val count = map[key] ?: 0
        if (count <= 0) break
        streak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
        if (streak > 365) break
    }
    return streak
}
