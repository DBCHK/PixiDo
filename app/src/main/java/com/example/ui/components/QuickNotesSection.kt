package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.NoteEntity

@Composable
fun QuickNotesSection(
    notes: List<NoteEntity>,
    onAddNote: (String, String) -> Unit,
    onTogglePin: (NoteEntity) -> Unit,
    onDelete: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sound = LocalSoundEngine.current
    var draft by remember { mutableStateOf("") }
    val colors = listOf("#C4A8F5", "#FF6BA8", "#FFE566", "#34D399", "#67D4E8", "#FBBF24")
    var color by remember { mutableStateOf(colors[0]) }

    PixiCard(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .testTag("quick_notes_section")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Quick notes",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Capture ideas without creating a full task",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Jot something…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_quick_note"),
                shape = PixiFieldShape,
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        val c = Color(android.graphics.Color.parseColor(hex))
                        val selected = color == hex
                        Box(
                            modifier = Modifier
                                .size(if (selected) 24.dp else 20.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable {
                                    sound.play(Sfx.TAP_SOFT)
                                    color = hex
                                }
                        )
                    }
                }

                TextButton(
                    onClick = {
                        if (draft.isNotBlank()) {
                            sound.play(Sfx.NOTE_SAVE)
                            onAddNote(draft.trim(), color)
                            draft = ""
                        } else {
                            sound.play(Sfx.ERROR)
                        }
                    },
                    modifier = Modifier.testTag("save_quick_note_btn")
                ) {
                    Text(
                        "Save note",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(notes, key = { it.id }) { note ->
                        NoteChip(
                            note = note,
                            onPin = {
                                sound.play(Sfx.TAP_CRISP)
                                onTogglePin(note)
                            },
                            onDelete = {
                                sound.play(Sfx.DELETE)
                                onDelete(note.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteChip(
    note: NoteEntity,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val accent = runCatching { Color(android.graphics.Color.parseColor(note.colorHex)) }
        .getOrDefault(MaterialTheme.colorScheme.primary)

    PixiCard(
        modifier = Modifier
            .width(170.dp)
            .testTag("note_${note.id}"),
        containerColor = accent.copy(alpha = 0.14f),
        borderColor = accent.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.isPinned) {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = "Pinned",
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.width(14.dp))
                }
                Row {
                    IconButton(onClick = onPin, modifier = Modifier.size(26.dp)) {
                        Icon(
                            Icons.Filled.PushPin,
                            contentDescription = "Pin",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(26.dp)) {
                        Icon(
                            Icons.Filled.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
            Text(
                text = note.content,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
