package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "pixido_prefs")

enum class AppThemeOption {
    MATERIAL_YOU,
    SYSTEM,
    PIXIDO_DARK,
    PIXIDO_LIGHT,
    OCEAN,
    SUNSET,
    FOREST,
    MIDNIGHT
}

data class UserProfile(
    val displayName: String = "",
    val bio: String = "",
    val email: String = "",
    val location: String = "",
    val avatarUri: String = "",
    val themeOption: AppThemeOption = AppThemeOption.PIXIDO_DARK,
    val currencyCode: String = "USD",
    val monthlyBudgetLimit: Double = 0.0,
    val userXp: Int = 0,
    val onboardingDone: Boolean = false,
    val soundEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val reduceMotion: Boolean = false
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val BIO = stringPreferencesKey("bio")
        val EMAIL = stringPreferencesKey("email")
        val LOCATION = stringPreferencesKey("location")
        val AVATAR_URI = stringPreferencesKey("avatar_uri")
        val THEME = stringPreferencesKey("theme_option")
        val CURRENCY = stringPreferencesKey("currency_code")
        val MONTHLY_BUDGET = doublePreferencesKey("monthly_budget_limit")
        val USER_XP = intPreferencesKey("user_xp")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val HAPTICS = booleanPreferencesKey("haptics_enabled")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
    }

    val userProfile: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        UserProfile(
            displayName = prefs[Keys.DISPLAY_NAME].orEmpty(),
            bio = prefs[Keys.BIO].orEmpty(),
            email = prefs[Keys.EMAIL].orEmpty(),
            location = prefs[Keys.LOCATION].orEmpty(),
            avatarUri = prefs[Keys.AVATAR_URI].orEmpty(),
            themeOption = runCatching {
                AppThemeOption.valueOf(prefs[Keys.THEME] ?: AppThemeOption.PIXIDO_DARK.name)
            }.getOrDefault(AppThemeOption.PIXIDO_DARK),
            currencyCode = prefs[Keys.CURRENCY] ?: "USD",
            monthlyBudgetLimit = prefs[Keys.MONTHLY_BUDGET] ?: 0.0,
            userXp = prefs[Keys.USER_XP] ?: 0,
            onboardingDone = prefs[Keys.ONBOARDING] ?: false,
            soundEnabled = prefs[Keys.SOUND] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true,
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false
        )
    }

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

    suspend fun setTheme(option: AppThemeOption) {
        context.dataStore.edit { it[Keys.THEME] = option.name }
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
}
