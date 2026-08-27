package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DailyHeaderBanner(
    profile: UserProfile,
    onOpenFocusMode: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier,
    openCount: Int = 0,
    doneToday: Int = 0,
    overdueCount: Int = 0
) {
    val d = rememberPixiDimens()
    val dateString = SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date())
    val greetingName = profile.displayName.ifBlank { "there" }
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val hello = when (hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }
    val greetingSize = when {
        greetingName.length > 18 -> 26.sp
        greetingName.length > 12 -> 30.sp
        else -> d.title
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateString,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PixiPopClickable(
                    onClick = onOpenFocusMode,
                    modifier = Modifier.testTag("focus_mode_button")
                ) {
                    PixiGlass(shape = PixiPillShape, elevation = 0.dp, frost = false) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Focus",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Focus",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    }
                }
                PixiPopClickable(
                    onClick = onOpenProfile,
                    modifier = Modifier.testTag("profile_avatar_button")
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile.avatarUri.isNotBlank()) {
                            AsyncImage(
                                model = profile.avatarUri,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (profile.displayName.isNotBlank()) {
                            Text(
                                text = profile.displayName.take(1).uppercase(),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = "Profile",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = greetingName.replaceFirstChar { it.titlecase(Locale.getDefault()) },
            fontSize = greetingSize,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = (greetingSize.value + 4f).sp,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = when {
                overdueCount > 0 -> "$overdueCount overdue"
                openCount + doneToday == 0 -> hello
                doneToday == 0 -> "$openCount open"
                openCount == 0 -> "All done"
                else -> "$doneToday of ${openCount + doneToday} done"
            },
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun TaskDayStatsStrip(
    openCount: Int,
    doneToday: Int,
    overdueCount: Int,
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
            label = "Overdue",
            value = overdueCount.toString(),
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
        highlight -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surface
    }
    val fg = when {
        danger -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(PixiCardShapeSm)
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("task_stat_$label")
    ) {
        Column {
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1
            )
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
