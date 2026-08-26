package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.HeatmapEmpty
import com.example.ui.theme.HeatmapEmptyLight
import com.example.ui.theme.HeatmapL1
import com.example.ui.theme.HeatmapL1Light
import com.example.ui.theme.HeatmapL2
import com.example.ui.theme.HeatmapL2Light
import com.example.ui.theme.HeatmapL3
import com.example.ui.theme.HeatmapL3Light
import com.example.ui.theme.HeatmapL4
import com.example.ui.theme.HeatmapL4Light
import com.example.ui.theme.rememberPixiDimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * GitHub-style contribution grid.
 * Used on Goals (per-goal progress days), not on the Tasks tab.
 */
@Composable
fun ContributionHeatmap(
    dayCounts: Map<String, Int>,
    modifier: Modifier = Modifier,
    weeks: Int = 17,
    title: String = "Activity",
    subtitle: String? = null,
    compact: Boolean = false
) {
    val d = rememberPixiDimens()
    val cell = if (compact) (d.heatmapCell.value * 0.85f).dp else d.heatmapCell
    val isDark = MaterialTheme.colorScheme.background.red +
        MaterialTheme.colorScheme.background.green +
        MaterialTheme.colorScheme.background.blue < 1.5f

    val weeksToShow = when {
        compact -> 10
        d.isCompact -> 12
        else -> weeks
    }
    val grid = remember(weeksToShow) {
        buildContributionGrid(weeksToShow)
    }

    val totalCompletions = dayCounts.values.sum()
    val activeDays = dayCounts.count { it.value > 0 }
    val bestDay = dayCounts.values.maxOrNull() ?: 0
    val resolvedSubtitle = subtitle
        ?: "$totalCompletions updates · $activeDays active days"

    PixiCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("contribution_heatmap")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 12.dp else 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = if (compact) MaterialTheme.typography.titleSmall
                        else MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = resolvedSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                if (bestDay > 0 && !compact) {
                    PixiBadge(text = "Best $bestDay/day")
                }
            }

            Spacer(modifier = Modifier.height(if (compact) 10.dp else 14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                grid.forEach { weekColumn ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        weekColumn.forEach { dayKey ->
                            val count = dayCounts[dayKey] ?: 0
                            Box(
                                modifier = Modifier
                                    .size(cell)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(heatmapColor(count, isDark))
                            )
                        }
                    }
                }
            }

            if (!compact) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Less",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    listOf(0, 1, 2, 4, 7).forEach { level ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 1.5.dp)
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(heatmapColor(level, isDark))
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "More",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Inline mini heatmap for a single goal card — no outer card chrome.
 */
@Composable
fun GoalMiniHeatmap(
    dayCounts: Map<String, Int>,
    modifier: Modifier = Modifier,
    weeks: Int = 12,
    cell: Dp = 8.dp
) {
    val isDark = MaterialTheme.colorScheme.background.red +
        MaterialTheme.colorScheme.background.green +
        MaterialTheme.colorScheme.background.blue < 1.5f
    val grid = remember(weeks) { buildContributionGrid(weeks) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .testTag("goal_mini_heatmap"),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        grid.forEach { weekColumn ->
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                weekColumn.forEach { dayKey ->
                    val count = dayCounts[dayKey] ?: 0
                    Box(
                        modifier = Modifier
                            .size(cell)
                            .clip(RoundedCornerShape(2.dp))
                            .background(heatmapColor(count, isDark))
                    )
                }
            }
        }
    }
}

private fun buildContributionGrid(weeks: Int): List<List<String>> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val today = Calendar.getInstance()
    val totalDays = weeks * 7
    val start = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        add(Calendar.DAY_OF_YEAR, -(totalDays - 1))
        val startDow = get(Calendar.DAY_OF_WEEK)
        add(Calendar.DAY_OF_YEAR, -(startDow - Calendar.SUNDAY))
    }

    val columns = mutableListOf<List<String>>()
    val cursor = start.clone() as Calendar
    val endBoundary = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        val endDow = get(Calendar.DAY_OF_WEEK)
        add(Calendar.DAY_OF_YEAR, Calendar.SATURDAY - endDow)
    }

    while (!cursor.after(endBoundary)) {
        val week = mutableListOf<String>()
        repeat(7) {
            week.add(fmt.format(cursor.time))
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        columns.add(week)
    }
    return if (columns.size > weeks) columns.takeLast(weeks) else columns
}

private fun heatmapColor(count: Int, isDark: Boolean): Color {
    return when {
        count <= 0 -> if (isDark) HeatmapEmpty else HeatmapEmptyLight
        count == 1 -> if (isDark) HeatmapL1 else HeatmapL1Light
        count <= 3 -> if (isDark) HeatmapL2 else HeatmapL2Light
        count <= 5 -> if (isDark) HeatmapL3 else HeatmapL3Light
        else -> if (isDark) HeatmapL4 else HeatmapL4Light
    }
}
