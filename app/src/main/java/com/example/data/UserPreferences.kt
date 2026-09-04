package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixido_prefs")

enum class AppThemeOption {
    MATERIAL_YOU,
    SYSTEM,
    PIXIDO_LIGHT,
    PIXIDO_DARK,
    OCEAN,
    SUNSET,
    FOREST,
    MIDNIGHT,
    // New soft / bold presets
    ROSE,
    SAND,
    SKY,
    PEACH,
    AURORA,
    CHERRY,
    GRAPHITE,
    MINT
}

/** Cloud backup preference: automatic daily vs never. */
enum class BackupFrequency {
    EVERY_24_HOURS,
    NEVER
}

/** Reminder notification sound presets (bundled custom chimes + system default). */
enum class NotificationSoundOption {
    SOFT,
    BRIGHT,
    CALM,
    SYSTEM
}

data class UserProfile(
    val displayName: String = "",
    val bio: String = "",
    val email: String = "",
    val location: String = "",
    val avatarUri: String = "",
    val themeOption: AppThemeOption = AppThemeOption.PIXIDO_LIGHT,
    /** Optional accent override (e.g. "#C4A8F5"). Empty = theme default. */
    val accentColorHex: String = "",
    val currencyCode: String = "USD",
    val monthlyBudgetLimit: Double = 0.0,
    val userXp: Int = 0,
    val onboardingDone: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false,
    /** Frosted glass (backdrop blur) on the tab island, banners, and alerts. */
    val glassEffectEnabled: Boolean = true,
    /** Custom reminder notification sound. */
    val notificationSound: NotificationSoundOption = NotificationSoundOption.SOFT,
    // Google account + cloud backup
    val googleUid: String = "",
    val googleEmail: String = "",
    val googlePhotoUrl: String = "",
    val backupFrequency: BackupFrequency = BackupFrequency.EVERY_24_HOURS,
    val lastBackupAt: Long = 0L,
    /**
     * When true, bank / UPI transaction SMS are detected and offered for Budget import
     * on the next app open (requires READ_SMS + RECEIVE_SMS).
     */
    val smsImportEnabled: Boolean = true,
    /** Last account used when assigning an SMS transaction (0 = none). */
    val lastSmsAccountId: Int = 0,
    /** Show events from the phone / Google calendar inside PixiDo Calendar. */
    val calendarSyncEnabled: Boolean = true,
    /** Comma-separated CalendarContract ids the user chose to display. */
    val selectedCalendarIds: String = "",
    /** True once the user has confirmed which device calendars to show. */
    val calendarSourcesPicked: Boolean = false
) {
    val isSignedIn: Boolean get() = googleUid.isNotBlank()
    val hasCustomAccent: Boolean get() = accentColorHex.isNotBlank()
    val selectedCalendarIdSet: Set<Long>
        get() = DeviceCalendars.parseIds(selectedCalendarIds)
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val BIO = stringPreferencesKey("bio")
        val EMAIL = stringPreferencesKey("email")
        val LOCATION = stringPreferencesKey("location")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")
        val THEME = stringPreferencesKey("theme_option")
        val ACCENT_COLOR = stringPreferencesKey("accent_color_hex")
        val CURRENCY = stringPreferencesKey("currency_code")
        val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget_limit")
        val USER_XP = intPreferencesKey("user_xp")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val GLASS_EFFECT = booleanPreferencesKey("glass_effect_enabled")
        val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
        val GOOGLE_UID = stringPreferencesKey("google_uid")
        val GOOGLE_EMAIL = stringPreferencesKey("google_email")
        val GOOGLE_PHOTO = stringPreferencesKey("google_photo_url")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val LAST_BACKUP_AT = longPreferencesKey("last_backup_at")
        val SMS_IMPORT = booleanPreferencesKey("sms_import_enabled")
        val LAST_SMS_ACCOUNT = intPreferencesKey("last_sms_account_id")
        val CALENDAR_SYNC = booleanPreferencesKey("calendar_sync_enabled")
        val SELECTED_CALENDAR_IDS = stringPreferencesKey("selected_calendar_ids")
        val CALENDAR_SOURCES_PICKED = booleanPreferencesKey("calendar_sources_picked")
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        prefs.toProfile()
    }

    suspend fun currentProfile(): UserProfile = context.dataStore.data.first().toProfile()

    private fun Preferences.toProfile(): UserProfile = UserProfile(
        displayName = this[Keys.DISPLAY_NAME].orEmpty(),
        bio = this[Keys.BIO].orEmpty(),
        email = this[Keys.EMAIL].orEmpty(),
        location = this[Keys.LOCATION].orEmpty(),
        avatarUri = this[Keys.AVATAR_URI].orEmpty(),
        themeOption = runCatching {
            AppThemeOption.valueOf(this[Keys.THEME] ?: AppThemeOption.PIXIDO_LIGHT.name)
        }.getOrDefault(AppThemeOption.PIXIDO_LIGHT),
        accentColorHex = this[Keys.ACCENT_COLOR].orEmpty(),
        currencyCode = this[Keys.CURRENCY] ?: "USD",
        monthlyBudgetLimit = this[Keys.MONTHLY_BUDGET] ?: 0.0,
        userXp = this[Keys.USER_XP] ?: 0,
        onboardingDone = this[Keys.ONBOARDING] ?: false,
        soundEnabled = this[Keys.SOUND] ?: true,
        hapticsEnabled = this[Keys.HAPTICS] ?: true,
        reduceMotion = this[Keys.REDUCE_MOTION] ?: false,
        glassEffectEnabled = this[Keys.GLASS_EFFECT] ?: true,
        notificationSound = runCatching {
            NotificationSoundOption.valueOf(
                this[Keys.NOTIFICATION_SOUND] ?: NotificationSoundOption.SOFT.name
            )
        }.getOrDefault(NotificationSoundOption.SOFT),
        googleUid = this[Keys.GOOGLE_UID].orEmpty(),
        googleEmail = this[Keys.GOOGLE_EMAIL].orEmpty(),
        googlePhotoUrl = this[Keys.GOOGLE_PHOTO].orEmpty(),
        backupFrequency = runCatching {
            BackupFrequency.valueOf(
                this[Keys.BACKUP_FREQUENCY] ?: BackupFrequency.EVERY_24_HOURS.name
            )
        }.getOrDefault(BackupFrequency.EVERY_24_HOURS),
        lastBackupAt = this[Keys.LAST_BACKUP_AT] ?: 0L,
        // Default on so bank SMS prompts work once permission is granted
        smsImportEnabled = this[Keys.SMS_IMPORT] ?: true,
        lastSmsAccountId = this[Keys.LAST_SMS_ACCOUNT] ?: 0,
        calendarSyncEnabled = this[Keys.CALENDAR_SYNC] ?: true,
        selectedCalendarIds = this[Keys.SELECTED_CALENDAR_IDS].orEmpty(),
        calendarSourcesPicked = this[Keys.CALENDAR_SOURCES_PICKED] ?: false
    )

    suspend fun updateProfile(
        displayName: String? = null,
        bio: String? = null,
        email: String? = null,
        location: String? = null,
        avatarUri: String? = null
    ) {
        context.dataStore.edit { prefs ->
            displayName?.let { prefs[Keys.DISPLAY_NAME] = it }
            bio?.let { prefs[Keys.BIO] = it }
            email?.let { prefs[Keys.EMAIL] = it }
            location?.let { prefs[Keys.LOCATION] = it }
            avatarUri?.let { prefs[Keys.AVATAR_URI] = it }
        }
    }

    suspend fun setGoogleAccount(
        uid: String,
        email: String,
        displayName: String?,
        photoUrl: String?
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOOGLE_UID] = uid
            prefs[Keys.GOOGLE_EMAIL] = email
            if (!displayName.isNullOrBlank()) {
                prefs[Keys.DISPLAY_NAME] = displayName
            }
            if (!photoUrl.isNullOrBlank()) {
                prefs[Keys.GOOGLE_PHOTO] = photoUrl
                prefs[Keys.AVATAR_URI] = photoUrl
            }
            if (email.isNotBlank()) {
                prefs[Keys.EMAIL] = email
            }
        }
    }

    suspend fun clearGoogleAccount() {
        context.dataStore.edit { prefs ->
            prefs[Keys.GOOGLE_UID] = ""
            prefs[Keys.GOOGLE_EMAIL] = ""
            prefs[Keys.GOOGLE_PHOTO] = ""
        }
    }

    suspend fun setBackupFrequency(frequency: BackupFrequency) {
        context.dataStore.edit { it[Keys.BACKUP_FREQUENCY] = frequency.name }
    }

    suspend fun setLastBackupAt(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_AT] = millis }
    }

    /** Preferences map stored inside the cloud snapshot. */
    suspend fun exportPreferencesMap(): Map<String, Any?> {
        val p = currentProfile()
        return mapOf(
            "displayName" to p.displayName,
            "bio" to p.bio,
            "email" to p.email,
            "location" to p.location,
            "avatarUri" to p.avatarUri,
            "themeOption" to p.themeOption.name,
            "accentColorHex" to p.accentColorHex,
            "currencyCode" to p.currencyCode,
            "monthlyBudgetLimit" to p.monthlyBudgetLimit,
            "userXp" to p.userXp,
            "onboardingDone" to p.onboardingDone,
            "soundEnabled" to p.soundEnabled,
            "hapticsEnabled" to p.hapticsEnabled,
            "reduceMotion" to p.reduceMotion,
            "glassEffectEnabled" to p.glassEffectEnabled,
            "notificationSound" to p.notificationSound.name,
            "backupFrequency" to p.backupFrequency.name,
            "smsImportEnabled" to p.smsImportEnabled,
            "lastSmsAccountId" to p.lastSmsAccountId,
            "calendarSyncEnabled" to p.calendarSyncEnabled,
            "selectedCalendarIds" to p.selectedCalendarIds,
            "calendarSourcesPicked" to p.calendarSourcesPicked
        )
    }

    suspend fun importPreferencesMap(map: Map<String, Any?>) {
        context.dataStore.edit { prefs ->
            (map["displayName"] as? String)?.let { prefs[Keys.DISPLAY_NAME] = it }
            (map["bio"] as? String)?.let { prefs[Keys.BIO] = it }
            (map["email"] as? String)?.let { prefs[Keys.EMAIL] = it }
            (map["location"] as? String)?.let { prefs[Keys.LOCATION] = it }
            (map["avatarUri"] as? String)?.let { prefs[Keys.AVATAR_URI] = it }
            (map["themeOption"] as? String)?.let { prefs[Keys.THEME] = it }
            (map["accentColorHex"] as? String)?.let { prefs[Keys.ACCENT_COLOR] = it }
            (map["currencyCode"] as? String)?.let { prefs[Keys.CURRENCY] = it }
            when (val v = map["monthlyBudgetLimit"]) {
                is Number -> prefs[Keys.MONTHLY_BUDGET] = v.toDouble()
            }
            when (val v = map["userXp"]) {
                is Number -> prefs[Keys.USER_XP] = v.toInt()
            }
            (map["onboardingDone"] as? Boolean)?.let { prefs[Keys.ONBOARDING] = it }
            (map["soundEnabled"] as? Boolean)?.let { prefs[Keys.SOUND] = it }
            (map["hapticsEnabled"] as? Boolean)?.let { prefs[Keys.HAPTICS] = it }
            (map["reduceMotion"] as? Boolean)?.let { prefs[Keys.REDUCE_MOTION] = it }
            (map["glassEffectEnabled"] as? Boolean)?.let { prefs[Keys.GLASS_EFFECT] = it }
            (map["notificationSound"] as? String)?.let { prefs[Keys.NOTIFICATION_SOUND] = it }
            (map["backupFrequency"] as? String)?.let { prefs[Keys.BACKUP_FREQUENCY] = it }
            (map["smsImportEnabled"] as? Boolean)?.let { prefs[Keys.SMS_IMPORT] = it }
            when (val v = map["lastSmsAccountId"]) {
                is Number -> prefs[Keys.LAST_SMS_ACCOUNT] = v.toInt()
            }
            (map["calendarSyncEnabled"] as? Boolean)?.let { prefs[Keys.CALENDAR_SYNC] = it }
            (map["selectedCalendarIds"] as? String)?.let { prefs[Keys.SELECTED_CALENDAR_IDS] = it }
            (map["calendarSourcesPicked"] as? Boolean)?.let { prefs[Keys.CALENDAR_SOURCES_PICKED] = it }
        }
    }

    suspend fun setTheme(option: AppThemeOption) {
        context.dataStore.edit { it[Keys.THEME] = option.name }
    }

    suspend fun setAccentColorHex(hex: String) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = hex }
    }

    suspend fun setCurrency(code: String) {
        context.dataStore.edit { it[Keys.CURRENCY] = code }
    }

    suspend fun setMonthlyBudgetLimit(limit: Double) {
        context.dataStore.edit { it[Keys.MONTHLY_BUDGET] = limit }
    }

    suspend fun setUserXp(xp: Int) {
        context.dataStore.edit { it[Keys.USER_XP] = xp }
    }

    suspend fun addXp(amount: Int) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.USER_XP] ?: 0
            prefs[Keys.USER_XP] = current + amount
        }
    }

    suspend fun setOnboardingDone(done: Boolean = true) {
        context.dataStore.edit { it[Keys.ONBOARDING] = done }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND] = enabled }
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.HAPTICS] = enabled }
    }

    suspend fun setReduceMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCE_MOTION] = enabled }
    }

    suspend fun setGlassEffectEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GLASS_EFFECT] = enabled }
    }

    suspend fun setNotificationSound(option: NotificationSoundOption) {
        context.dataStore.edit { it[Keys.NOTIFICATION_SOUND] = option.name }
    }

    suspend fun setSmsImportEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SMS_IMPORT] = enabled }
    }

    suspend fun setLastSmsAccountId(accountId: Int) {
        context.dataStore.edit { it[Keys.LAST_SMS_ACCOUNT] = accountId }
    }

    suspend fun setCalendarSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.CALENDAR_SYNC] = enabled }
    }

    suspend fun setSelectedCalendarSources(ids: Set<Long>, picked: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SELECTED_CALENDAR_IDS] = DeviceCalendars.encodeIds(ids)
            prefs[Keys.CALENDAR_SOURCES_PICKED] = picked
        }
    }
}

/** Supported currencies for budget tracking. */
object Currencies {
    data class Info(val code: String, val symbol: String, val name: String)

    val all = listOf(
        Info("USD", "$", "US Dollar"),
        Info("EUR", "€", "Euro"),
        Info("GBP", "£", "British Pound"),
        Info("INR", "₹", "Indian Rupee"),
        Info("JPY", "¥", "Japanese Yen"),
        Info("CAD", "C$", "Canadian Dollar"),
        Info("AUD", "A$", "Australian Dollar"),
        Info("CHF", "Fr", "Swiss Franc"),
        Info("CNY", "¥", "Chinese Yuan"),
        Info("KRW", "₩", "South Korean Won"),
        Info("BRL", "R$", "Brazilian Real"),
        Info("MXN", "MX$", "Mexican Peso"),
        Info("SGD", "S$", "Singapore Dollar"),
        Info("AED", "د.إ", "UAE Dirham"),
        Info("ZAR", "R", "South African Rand")
    )

    fun symbolOf(code: String): String =
        all.find { it.code == code }?.symbol ?: code

    fun format(amount: Double, code: String): String {
        val symbol = symbolOf(code)
        return "$symbol${"%.2f".format(amount)}"
    }

    /** Integer + cents split so the wallet hero can render $7,854 .43 */
    data class SplitMoney(
        val symbol: String,
        val whole: String,
        val cents: String,
        val negative: Boolean
    ) {
        val signedSymbol: String get() = if (negative) "-$symbol" else symbol
    }

    fun split(amount: Double, code: String): SplitMoney {
        val negative = amount < 0
        val centsTotal = kotlin.math.round(kotlin.math.abs(amount) * 100.0).toLong()
        val wholeNum = centsTotal / 100L
        val centsNum = (centsTotal % 100L).toInt()
        val grouped = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(wholeNum)
        return SplitMoney(
            symbol = symbolOf(code),
            whole = grouped,
            cents = "%02d".format(centsNum),
            negative = negative
        )
    }
}
