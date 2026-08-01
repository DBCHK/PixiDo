package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.AppThemeOption
import com.example.data.BackupFrequency
import com.example.data.UserProfile
import com.example.ui.components.PixiCard
import com.example.ui.components.PixiCardShapeSm
import com.example.ui.components.PixiCloseButton
import com.example.ui.components.PixiFieldShape
import com.example.ui.components.PixiOutlineButton
import com.example.ui.components.PixiPillShape
import com.example.ui.components.PixiPrimaryButton
import com.example.ui.components.PixiSecondaryButton
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.displayName
import com.example.ui.theme.swatchColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Simplified profile sheet:
 *  - Avatar + display name
 *  - Google SSO (restore on reinstall)
 *  - Cloud backup: every 24h OR never
 *  - Sound / haptics
 *  - Compact theme picker
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileDialog(
    profile: UserProfile,
    authBusy: Boolean = false,
    backupBusy: Boolean = false,
    onDismiss: () -> Unit,
    onSaveName: (name: String) -> Unit,
    onAvatarPicked: (String) -> Unit,
    onThemeSelected: (AppThemeOption) -> Unit,
    onAccentSelected: (String) -> Unit = {},
    onSoundToggle: (Boolean) -> Unit = {},
    onHapticsToggle: (Boolean) -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onBackupFrequencyChange: (BackupFrequency) -> Unit = {},
    onBackupNow: () -> Unit = {},
    onRestoreNow: () -> Unit = {}
) {
    val sound = LocalSoundEngine.current
    var name by remember(profile.displayName) { mutableStateOf(profile.displayName) }

    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onAvatarPicked(it.toString())
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    )

    val avatarModel = profile.avatarUri.ifBlank { profile.googlePhotoUrl }
    val lastBackupLabel = remember(profile.lastBackupAt) {
        if (profile.lastBackupAt <= 0L) "Never"
        else SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
            .format(Date(profile.lastBackupAt))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        PixiCard(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("profile_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    PixiCloseButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_profile")
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Avatar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(88.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { imagePicker.launch(arrayOf("image/*")) }
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        )
                        .testTag("profile_avatar_picker"),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarModel.isNotBlank()) {
                        AsyncImage(
                            model = avatarModel,
                            contentDescription = "Avatar",
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Avatar",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Display name") },
                    placeholder = { Text("Your name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_profile_name"),
                    singleLine = true,
                    shape = PixiFieldShape,
                    colors = fieldColors
                )

                Spacer(modifier = Modifier.height(18.dp))

                // ── Google account ───────────────────────────────────
                SectionLabel("Google account")
                Spacer(modifier = Modifier.height(8.dp))

                if (profile.isSignedIn) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PixiCardShapeSm)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile.googlePhotoUrl.isNotBlank()) {
                                AsyncImage(
                                    model = profile.googlePhotoUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = (profile.googleEmail.ifBlank { "G" })
                                        .take(1)
                                        .uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Signed in",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = profile.googleEmail.ifBlank { profile.email },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    PixiOutlineButton(
                        text = if (authBusy) "Signing out…" else "Sign out",
                        onClick = {
                            if (!authBusy) {
                                sound.play(Sfx.SETTINGS_CHANGE)
                                onGoogleSignOut()
                            }
                        },
                        modifier = Modifier.testTag("google_sign_out_btn")
                    )
                } else {
                    Text(
                        text = "Sign in so tasks, budget, calendar & goals restore after reinstall.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GoogleSignInButton(
                        busy = authBusy,
                        onClick = {
                            sound.play(Sfx.TAP_CONFIRM)
                            onGoogleSignIn()
                        }
                    )
                }

                // ── Cloud backup ─────────────────────────────────────
                Spacer(modifier = Modifier.height(20.dp))
                SectionLabel("Cloud backup")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (profile.isSignedIn) {
                        "Last backup · $lastBackupLabel"
                    } else {
                        "Sign in with Google to enable cloud backup"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                BackupOptionRow(
                    selected = profile.backupFrequency == BackupFrequency.EVERY_24_HOURS,
                    enabled = profile.isSignedIn,
                    icon = Icons.Filled.CloudDone,
                    title = "Every 24 hours",
                    subtitle = "Auto-upload when online",
                    onClick = {
                        if (profile.isSignedIn) {
                            sound.play(Sfx.SETTINGS_CHANGE)
                            onBackupFrequencyChange(BackupFrequency.EVERY_24_HOURS)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(6.dp))
                BackupOptionRow(
                    selected = profile.backupFrequency == BackupFrequency.NEVER,
                    enabled = profile.isSignedIn,
                    icon = Icons.Filled.CloudOff,
                    title = "Don't backup",
                    subtitle = "Keep data only on this device",
                    onClick = {
                        if (profile.isSignedIn) {
                            sound.play(Sfx.SETTINGS_CHANGE)
                            onBackupFrequencyChange(BackupFrequency.NEVER)
                        }
                    }
                )

                if (profile.isSignedIn) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            PixiSecondaryButton(
                                text = if (backupBusy) "…" else "Backup now",
                                onClick = {
                                    if (!backupBusy) {
                                        sound.play(Sfx.TAP_CONFIRM)
                                        onBackupNow()
                                    }
                                },
                                enabled = !backupBusy
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            PixiOutlineButton(
                                text = if (backupBusy) "…" else "Restore",
                                onClick = {
                                    if (!backupBusy) {
                                        sound.play(Sfx.TAP_CONFIRM)
                                        onRestoreNow()
                                    }
                                }
                            )
                        }
                    }
                }

                // ── Feedback ─────────────────────────────────────────
                Spacer(modifier = Modifier.height(20.dp))
                SectionLabel("Preferences")
                Spacer(modifier = Modifier.height(4.dp))
                PreferenceSwitchRow(
                    title = "Sound",
                    subtitle = "Soft interaction tones",
                    checked = profile.soundEnabled,
                    onCheckedChange = onSoundToggle,
                    testTag = "toggle_sound"
                )
                PreferenceSwitchRow(
                    title = "Haptics",
                    subtitle = "Light vibration",
                    checked = profile.hapticsEnabled,
                    onCheckedChange = onHapticsToggle,
                    testTag = "toggle_haptics"
                )

                // ── Theme ────────────────────────────────────────────
                Spacer(modifier = Modifier.height(16.dp))
                SectionLabel("Theme")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "16 looks · pick a base, then customize accent",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppThemeOption.entries.forEach { option ->
                        val selected = profile.themeOption == option
                        val swatch = option.swatchColor()
                        Row(
                            modifier = Modifier
                                .clip(PixiPillShape)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    sound.play(Sfx.THEME_CHANGE)
                                    onThemeSelected(option)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("theme_${option.name}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Color.White.copy(alpha = 0.9f) else swatch)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = option.displayName(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // ── Accent color ─────────────────────────────────────
                Spacer(modifier = Modifier.height(18.dp))
                SectionLabel("Accent color")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tints buttons, chips & highlights. Tap again to clear.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Default = clear custom accent
                    val noneSelected = profile.accentColorHex.isBlank()
                    Box(
                        modifier = Modifier
                            .size(if (noneSelected) 36.dp else 32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(
                                width = if (noneSelected) 2.5.dp else 1.dp,
                                color = if (noneSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable {
                                sound.play(Sfx.THEME_CHANGE)
                                onAccentSelected("")
                            }
                            .testTag("accent_none"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "A",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AccentPalette.forEach { hex ->
                        val c = runCatching {
                            Color(android.graphics.Color.parseColor(hex))
                        }.getOrNull() ?: return@forEach
                        val selected = profile.accentColorHex.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(if (selected) 36.dp else 32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selected) 2.5.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable {
                                    sound.play(Sfx.THEME_CHANGE)
                                    // Toggle off if already selected
                                    onAccentSelected(
                                        if (selected) "" else hex
                                    )
                                }
                                .testTag("accent_$hex")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                PixiPrimaryButton(
                    text = "Save",
                    onClick = {
                        sound.play(Sfx.PROFILE_SAVE)
                        onSaveName(name.trim())
                        onDismiss()
                    },
                    modifier = Modifier.testTag("save_profile_btn")
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun GoogleSignInButton(
    busy: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(PixiPillShape)
            .background(Color(0xFF1C1C1E))
            .clickable(enabled = !busy, onClick = onClick)
            .testTag("google_sign_in_btn"),
        contentAlignment = Alignment.Center
    ) {
        if (busy) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Simple multicolor G mark
                Text(
                    text = "G",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = Color(0xFF4285F4)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Continue with Google",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun BackupOptionRow(
    selected: Boolean,
    enabled: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.45f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PixiCardShapeSm)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f * alpha)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f * alpha)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
            )
        }
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}

/** Backward-compatible overload for older call sites (bio/email/location). */
@Composable
fun ProfileDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onSaveProfile: (name: String, bio: String, email: String, location: String) -> Unit,
    onAvatarPicked: (String) -> Unit,
    onThemeSelected: (AppThemeOption) -> Unit,
    onSoundToggle: (Boolean) -> Unit = {},
    onHapticsToggle: (Boolean) -> Unit = {},
    onReduceMotionToggle: (Boolean) -> Unit = {}
) {
    ProfileDialog(
        profile = profile,
        onDismiss = onDismiss,
        onSaveName = { name -> onSaveProfile(name, profile.bio, profile.email, profile.location) },
        onAvatarPicked = onAvatarPicked,
        onThemeSelected = onThemeSelected,
        onSoundToggle = onSoundToggle,
        onHapticsToggle = onHapticsToggle
    )
}
