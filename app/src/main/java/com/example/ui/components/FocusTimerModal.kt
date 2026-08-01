package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R

@Composable
fun FocusTimerModal(
    secondsLeft: Int,
    isRunning: Boolean,
    onStart: (Int) -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val totalSeconds = 25 * 60
    val progress = secondsLeft.toFloat() / totalSeconds.toFloat()
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "timerProgress")

    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Dialog(onDismissRequest = onDismiss) {
        PixiCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("focus_timer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Focus Timer",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_focus_modal")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                PixiDoodle3D(
                    resId = R.drawable.doodle_focus,
                    size = 150.dp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    yawDegrees = 18f,
                    pitchDegrees = 12f,
                    orbitSeconds = 5
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.size(180.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        strokeWidth = 12.dp
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(180.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 12.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeFormatted,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isRunning) "In focus" else "Ready when you are",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15 to "15m", 25 to "25m", 45 to "45m").forEach { (mins, label) ->
                        PixiPopClickable(
                            onClick = { onStart(mins) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PixiPillShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        shape = CircleShape,
                        modifier = Modifier.testTag("reset_focus_btn")
                    ) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Reset")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        onClick = {
                            if (isRunning) onPause() else onStart(minutes.coerceAtLeast(1))
                        },
                        shape = PixiPillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .height(52.dp)
                            .width(150.dp)
                            .testTag("toggle_focus_timer_btn")
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isRunning) "Pause" else "Start",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Finish a session to earn +50 XP",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
