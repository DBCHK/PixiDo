package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.UserProfile
import com.example.ui.theme.rememberPixiDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Soft Lilac tasks header:
 * greeting + Focus chip + circular avatar.
 * (Contribution heatmaps live on the Goals tab, per goal.)
 */
@Composable
fun DailyHeaderBanner(
    userXp: Int,
    profile: UserProfile,
    onOpenFocusMode: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    openCount: Int = 0,
    doneToday: Int = 0,
    overdueCount: Int = 0
) {
    val d = rememberPixiDimens()
    val dateFormat = SimpleDateFormat(
        if (d.isCompact) "EEE · MMM d" else "EEEE · MMM dd",
        Locale.getDefault()
    )
    val dateString = dateFormat.format(Date())
    val greetingName = profile.displayName.ifBlank { "there" }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    text = dateString,
                    fontSize = d.label,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Hey $greetingName",
                    fontSize = d.title,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!d.isCompact) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "What will you finish today?",
                        fontSize = d.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(if (d.isCompact) 8.dp else 12.dp)
            ) {
                PixiPopClickable(
                    onClick = onOpenFocusMode,
                    modifier = Modifier.testTag("focus_mode_button")
                ) {
                    Box(
                        modifier = Modifier
                            .clip(PixiPillShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(
                                horizontal = if (d.isCompact) 12.dp else 14.dp,
                                vertical = if (d.isCompact) 9.dp else 11.dp
                            )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Focus Mode",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(d.iconSm)
                            )
                            if (!d.isCompact) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Focus",
                                    fontSize = d.caption,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                PixiPopClickable(
                    onClick = onOpenProfile,
                    modifier = Modifier.testTag("profile_avatar_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(d.avatar)
                            .clip(CircleShape)
                            .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = profile.avatarUri,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(d.avatar)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (profile.displayName.isNotBlank()) {
                            Text(
                                text = profile.displayName.take(1).uppercase(),
                                fontSize = if (d.isCompact) 16.sp else 18.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(d.iconMd)
                            )
                        }
                    }
                }
            }
        }

        // Compact day stats — designer-led snapshot of task health
        Spacer(modifier = Modifier.height(d.listGap))
        TaskDayStatsStrip(
            openCount = openCount,
            doneToday = doneToday,
            overdueCount = overdueCount,
            userXp = userXp
        )
    }
}

@Composable
fun TaskDayStatsStrip(
    openCount: Int,
    doneToday: Int,
    overdueCount: Int,
    userXp: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(
            label = "Open",
            value = openCount.toString(),
            modifier = Modifier.weight(1f),
            highlight = openCount > 0
        )
        StatChip(
            label = "Done",
            value = doneToday.toString(),
            modifier = Modifier.weight(1f),
            highlight = doneToday > 0
        )
        StatChip(
            label = if (overdueCount > 0) "Overdue" else "XP",
            value = if (overdueCount > 0) overdueCount.toString() else userXp.toString(),
            modifier = Modifier.weight(1f),
            highlight = overdueCount > 0,
            danger = overdueCount > 0
        )
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    danger: Boolean = false
) {
    val bg = when {
        danger -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        highlight -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val fg = when {
        danger -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(PixiCardShapeSm)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 10.dp)
            .testTag("task_stat_$label")
    ) {
        Column {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = fg,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
