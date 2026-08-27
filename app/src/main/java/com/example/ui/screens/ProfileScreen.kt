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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.audio.LocalSoundEngine
import com.example.audio.Sfx
import com.example.data.AppThemeOption
import com.example.data.BackupFrequency
import com.example.data.NotificationSoundOption
import com.example.data.UserProfile
import com.example.notify.displayName
import com.example.ui.components.PixiGlass
import com.example.ui.components.PixiGlassHost
import com.example.ui.components.PixiGlassWeight
import com.example.ui.components.PixiToggle
import com.example.ui.theme.AccentPalette
import com.example.ui.theme.displayName
import com.example.ui.theme.swatchColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * iOS Settings-style profile sheet: grouped inset lists, hairline rows,
 * trailing switches, Done in the navigation bar.
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
    onGlassEffectToggle: (Boolean) -> Unit = {},
    onSmsImportToggle: (Boolean) -> Unit = {},
    onCalendarSyncToggle: (Boolean) -> Unit = {},
    onNotificationSoundSelected: (NotificationSoundOption) -> Unit = {},
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

    val avatarModel = profile.avatarUri.ifBlank { profile.googlePhotoUrl }
    val lastBackupLabel = remember(profile.lastBackupAt) {
        if (profile.lastBackupAt <= 0L) "Never"
        else SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
            .format(Date(profile.lastBackupAt))
    }

    fun finish() {
        sound.play(Sfx.PROFILE_SAVE)
        onSaveName(name.trim())
        onDismiss()
    }

    PixiGlassHost(onDismissRequest = { finish() }) {
        PixiGlass(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .testTag("profile_dialog"),
            shape = RoundedCornerShape(28.dp),
            elevation = 24.dp,
            frost = true,
            liquid = true,
            weight = PixiGlassWeight.Sheet
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .testTag("close_profile")
                        .clickable { finish() }
                        .padding(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Done",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("save_profile_btn")
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp)
            ) {
                IosSection(title = "Profile") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
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
                                        .size(60.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Avatar",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Name",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BasicTextField(
                                value = name,
                                onValueChange = { name = it },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_profile_name"),
                                decorationBox = { inner ->
                                    if (name.isEmpty()) {
                                        Text(
                                            text = "Your name",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                }

                IosSection(
                    title = "Account",
                    footer = if (profile.isSignedIn) null
                    else "Sign in so tasks, budget, calendar and goals restore after reinstall."
                ) {
                    if (profile.isSignedIn) {
                        IosRow(
                            title = "Google",
                            value = profile.googleEmail.ifBlank { profile.email },
                            leading = {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (profile.googlePhotoUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = profile.googlePhotoUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Text(
                                            text = (profile.googleEmail.ifBlank { "G" })
                                                .take(1)
                                                .uppercase(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            },
                            showDivider = true
                        )
                        IosRow(
                            title = if (authBusy) "Signing out…" else "Sign Out",
                            titleColor = MaterialTheme.colorScheme.error,
                            showChevron = false,
                            showDivider = false,
                            onClick = {
                                if (!authBusy) {
                                    sound.play(Sfx.SETTINGS_CHANGE)
                                    onGoogleSignOut()
                                }
                            },
                            testTag = "google_sign_out_btn"
                        )
                    } else {
                        IosRow(
                            title = if (authBusy) "Signing in…" else "Sign in with Google",
                            showChevron = true,
                            showDivider = false,
                            onClick = {
                                if (!authBusy) {
                                    sound.play(Sfx.TAP_CONFIRM)
                                    onGoogleSignIn()
                                }
                            },
                            testTag = "google_sign_in_btn",
                            trailing = {
                                if (authBusy) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        )
                    }
                }

                IosSection(
                    title = "iCloud Backup",
                    footer = if (profile.isSignedIn) {
                        "Last backup · $lastBackupLabel"
                    } else {
                        "Sign in with Google to enable cloud backup."
                    }
                ) {
                    IosToggleRow(
                        title = "Automatic Backup",
                        subtitle = "Every 24 hours when online",
                        checked = profile.isSignedIn &&
                            profile.backupFrequency == BackupFrequency.EVERY_24_HOURS,
                        enabled = profile.isSignedIn,
                        showDivider = true,
                        onCheckedChange = { enabled ->
                            onBackupFrequencyChange(
                                if (enabled) BackupFrequency.EVERY_24_HOURS
                                else BackupFrequency.NEVER
                            )
                        }
                    )
                    IosRow(
                        title = if (backupBusy) "Backing up…" else "Back Up Now",
                        enabled = profile.isSignedIn && !backupBusy,
                        showChevron = false,
                        showDivider = true,
                        onClick = {
                            if (profile.isSignedIn && !backupBusy) {
                                sound.play(Sfx.TAP_CONFIRM)
                                onBackupNow()
                            }
                        }
                    )
                    IosRow(
                        title = if (backupBusy) "Restoring…" else "Restore from Cloud",
                        enabled = profile.isSignedIn && !backupBusy,
                        showChevron = false,
                        showDivider = false,
                        onClick = {
                            if (profile.isSignedIn && !backupBusy) {
                                sound.play(Sfx.TAP_CONFIRM)
                                onRestoreNow()
                            }
                        }
                    )
                }

                IosSection(title = "Sounds") {
                    IosToggleRow(
                        title = "Sound Effects",
                        subtitle = "Soft piano tones",
                        checked = profile.soundEnabled,
                        onCheckedChange = onSoundToggle,
                        testTag = "toggle_sound",
                        showDivider = true
                    )
                    IosToggleRow(
                        title = "Haptics",
                        subtitle = "Light vibration",
                        checked = profile.hapticsEnabled,
                        onCheckedChange = onHapticsToggle,
                        testTag = "toggle_haptics",
                        showDivider = true
                    )
                    NotificationSoundOption.entries.forEachIndexed { index, option ->
                        val selected = profile.notificationSound == option
                        IosRow(
                            title = option.displayName(),
                            subtitle = if (index == 0) "Task & event reminders" else null,
                            showDivider = index < NotificationSoundOption.entries.lastIndex,
                            onClick = {
                                sound.play(Sfx.SETTINGS_CHANGE)
                                onNotificationSoundSelected(option)
                            },
                            testTag = "notif_sound_${option.name}",
                            trailing = {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }
                }

                IosSection(
                    title = "Calendar",
                    footer = "Events from Google and other phone calendars appear in PixiDo."
                ) {
                    IosToggleRow(
                        title = "Sync Phone Calendar",
                        subtitle = "Show device events in Calendar",
                        checked = profile.calendarSyncEnabled,
                        onCheckedChange = onCalendarSyncToggle,
                        testTag = "toggle_calendar_sync",
                        showDivider = false
                    )
                }

                IosSection(
                    title = "Bank SMS",
                    footer = "When on, debit and credit SMS are offered for Budget."
                ) {
                    IosToggleRow(
                        title = "Import Transactions",
                        subtitle = "Read bank SMS automatically",
                        checked = profile.smsImportEnabled,
                        onCheckedChange = onSmsImportToggle,
                        testTag = "toggle_sms_import",
                        showDivider = false
                    )
                }

                IosSection(
                    title = "Appearance",
                    footer = "Glass frosts and blurs the tab bar, banners, and alerts."
                ) {
                    IosToggleRow(
                        title = "Glass Effect",
                        subtitle = "Frosted blur on chrome",
                        checked = profile.glassEffectEnabled,
                        onCheckedChange = onGlassEffectToggle,
                        testTag = "toggle_glass_effect",
                        showDivider = true
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Theme",
                            fontSize = 13.sp,
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
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                        )
                                        .clickable {
                                            sound.play(Sfx.THEME_CHANGE)
                                            onThemeSelected(option)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .testTag("theme_${option.name}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) Color.White.copy(alpha = 0.9f) else swatch
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = option.displayName(),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Accent",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val noneSelected = profile.accentColorHex.isBlank()
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(
                                        width = if (noneSelected) 2.dp else 0.5.dp,
                                        color = if (noneSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
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
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(c)
                                        .border(
                                            width = if (selected) 2.dp else 0.dp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            sound.play(Sfx.THEME_CHANGE)
                                            onAccentSelected(if (selected) "" else hex)
                                        }
                                        .testTag("accent_$hex")
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun IosSection(
    title: String,
    footer: String? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title.uppercase(Locale.getDefault()),
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 6.dp)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            content()
        }
        if (!footer.isNullOrBlank()) {
            Text(
                text = footer,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp, end = 8.dp)
            )
        }
    }
}

@Composable
private fun IosRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    showChevron: Boolean = false,
    showDivider: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val alpha = if (enabled) 1f else 0.4f
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick)
                    else Modifier
                )
                .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                Spacer(modifier = Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    color = titleColor.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                        maxLines = 2
                    )
                }
            }
            if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f, fill = false)
                )
            }
            if (trailing != null) {
                Spacer(modifier = Modifier.width(8.dp))
                trailing()
            }
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f * alpha),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )
        }
    }
}

@Composable
private fun IosToggleRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    IosRow(
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        showChevron = false,
        showDivider = showDivider,
        testTag = testTag,
        trailing = {
            val sound = LocalSoundEngine.current
            PixiToggle(
                checked = checked,
                enabled = enabled,
                onCheckedChange = { newValue ->
                    if (!enabled) return@PixiToggle
                    sound.play(if (newValue) Sfx.TOGGLE_ON else Sfx.TOGGLE_OFF)
                    onCheckedChange(newValue)
                }
            )
        }
    )
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
