package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.example.ui.theme.PulseMint
import com.example.ui.theme.rememberPixiDimens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Goals activity card — weekly bars + mint contribution grid in one surface.
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
    val cell = if (compact) (d.heatmapCell.value * 0.9f).dp else (d.heatmapCell.value + 1f).dp
    val isDark = isPulseDark()

    val weeksToShow = when {
        compact -> 10
        d.isCompact -> 13
        else -> weeks
    }
    val grid = remember(weeksToShow) { buildContributionGrid(weeksToShow) }
    val last7 = remember { lastSevenDays() }

    val totalCompletions = dayCounts.values.sum()
    val activeDays = dayCounts.count { it.value > 0 }
    val weekTotal = last7.sumOf { dayCounts[it.key] ?: 0 }
    val resolvedSubtitle = subtitle
        ?: if (activeDays > 0) "Last 7 days · $weekTotal check-ins" else "Log a habit to fill this week"

    PulseSurfaceCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("contribution_heatmap")
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = pulseMuted()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = totalCompletions.toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = pulseInk(),
                letterSpacing = (-0.6).sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "check-ins",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = pulseMuted(),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "$activeDays active days",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = pulseMuted(),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = resolvedSubtitle,
            fontSize = 12.sp,
            color = pulseMuted()
        )

        if (!compact) {
            Spacer(modifier = Modifier.height(16.dp))
            WeekActivityBars(
                days = last7,
                dayCounts = dayCounts,
                isDark = isDark
            )
        }

        Spacer(modifier = Modifier.height(if (compact) 12.dp else 18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            grid.forEach { weekColumn ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    weekColumn.forEach { dayKey ->
                        val count = dayCounts[dayKey] ?: 0
                        Box(
                            modifier = Modifier
                                .size(cell)
                                .clip(RoundedCornerShape(4.dp))
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
                    color = pulseMuted()
                )
                Spacer(modifier = Modifier.width(6.dp))
                listOf(0, 1, 2, 4, 7).forEach { level ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 1.5.dp)
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(heatmapColor(level, isDark))
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "More",
                    fontSize = 10.sp,
                    color = pulseMuted()
                )
            }
        }
    }
}

@Composable
private fun WeekActivityBars(
    days: List<LabeledDay>,
    dayCounts: Map<String, Int>,
    isDark: Boolean
) {
    val max = days.maxOfOrNull { (dayCounts[it.key] ?: 0) }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val count = dayCounts[day.key] ?: 0
            val frac = (count / max.toFloat()).coerceIn(0f, 1f)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .height(84.dp)
                        .width(18.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(18.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isDark) Color.White.copy(alpha = 0.06f)
                                else Color(0xFFF1F6F2)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight((0.10f + frac * 0.90f).coerceIn(0.10f, 1f))
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    count <= 0 -> PulseMint.copy(alpha = 0.18f)
                                    day.isToday -> PulseMint
                                    else -> PulseMint.copy(alpha = 0.55f + 0.45f * frac)
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = day.label,
                    fontSize = 11.sp,
                    fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Medium,
                    color = if (day.isToday) pulseInk() else pulseMuted()
                )
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
    val isDark = isPulseDark()
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

private data class LabeledDay(
    val key: String,
    val label: String,
    val isToday: Boolean
)

private fun lastSevenDays(): List<LabeledDay> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labels = arrayOf("S", "M", "T", "W", "T", "F", "S")
    val today = Calendar.getInstance()
    val todayKey = fmt.format(today.time)
    return (6 downTo 0).map { ago ->
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -ago) }
        val key = fmt.format(cal.time)
        val dow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun
        LabeledDay(key = key, label = labels[dow - 1], isToday = key == todayKey)
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
