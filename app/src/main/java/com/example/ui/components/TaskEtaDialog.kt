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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.notify.EtaRingtonePlayer

/**
 * Full popup when a task ETA / due time arrives.
 * Plays PixiDo's custom calm ringtone until the user acts.
 */
@Composable
fun TaskEtaDialog(
    title: String,
    body: String,
    onMarkDone: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        EtaRingtonePlayer.start(context, loop = true)
        onDispose { EtaRingtonePlayer.stop() }
    }

    Dialog(
        onDismissRequest = {
            EtaRingtonePlayer.stop()
            onDismiss()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = true
        )
    ) {
        PixiCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("task_eta_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Alarm,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "It's time",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = title.ifBlank { "Task reminder" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("task_eta_title")
                )
                if (body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = body,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                PixiPrimaryButton(
                    text = "Mark done",
                    onClick = {
                        EtaRingtonePlayer.stop()
                        onMarkDone()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_eta_done")
                )
                Spacer(modifier = Modifier.height(10.dp))
                PixiSecondaryButton(
                    text = "Snooze 10 min",
                    onClick = {
                        EtaRingtonePlayer.stop()
                        onSnooze()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_eta_snooze")
                )
                Spacer(modifier = Modifier.height(10.dp))
                PixiOutlineButton(
                    text = "Dismiss",
                    onClick = {
                        EtaRingtonePlayer.stop()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_eta_dismiss")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "PixiDo calm chime",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
