package com.schwarmakdev.controldehoras.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.schwarmakdev.controldehoras.ui.theme.AppThemeColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

/**
 * Persiste las preferencias del usuario entre sesiones usando DataStore.
 * Cubre: modo oscuro/claro, color de acento, notificaciones habilitadas,
 * umbral de horas para alerta.
 */
class PreferencesRepository(private val context: Context) {

    companion object {
        private val KEY_IS_DARK               = booleanPreferencesKey("is_dark_mode")
        private val KEY_COLOR_THEME           = stringPreferencesKey("color_theme")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_NOTIFICATION_HOURS    = intPreferencesKey("notification_hours_threshold")
    }

    // ── Reads ──────────────────────────────────────────────────────────────────

    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_DARK] ?: true
    }

    val colorTheme: Flow<AppThemeColor> = context.dataStore.data.map { prefs ->
        try {
            AppThemeColor.valueOf(prefs[KEY_COLOR_THEME] ?: AppThemeColor.MINT_CYAN.name)
        } catch (_: IllegalArgumentException) {
            AppThemeColor.MINT_CYAN
        }
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATIONS_ENABLED] ?: true
    }

    val notificationHoursThreshold: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_NOTIFICATION_HOURS] ?: 5
    }

    // ── Writes ─────────────────────────────────────────────────────────────────

    suspend fun setDarkMode(isDark: Boolean) {
        context.dataStore.edit { it[KEY_IS_DARK] = isDark }
    }

    suspend fun setColorTheme(theme: AppThemeColor) {
        context.dataStore.edit { it[KEY_COLOR_THEME] = theme.name }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setNotificationHoursThreshold(hours: Int) {
        context.dataStore.edit { it[KEY_NOTIFICATION_HOURS] = hours }
    }
}
