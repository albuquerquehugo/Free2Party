package com.example.free2party.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.free2party.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

class SettingsRepository(private val context: Context) {
    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOWN_NOTIFICATION_IDS = stringSetPreferencesKey("shown_notification_ids")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val themeName = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.AUTOMATIC.name
            ThemeMode.valueOf(themeName)
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode.name
        }
    }

    val shownNotificationIdsFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.SHOWN_NOTIFICATION_IDS] ?: emptySet()
        }

    suspend fun markNotificationAsShown(id: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SHOWN_NOTIFICATION_IDS] ?: emptySet()
            preferences[PreferencesKeys.SHOWN_NOTIFICATION_IDS] = current + id
        }
    }

    suspend fun clearShownNotification(id: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.SHOWN_NOTIFICATION_IDS] ?: emptySet()
            preferences[PreferencesKeys.SHOWN_NOTIFICATION_IDS] = current - id
        }
    }

    suspend fun clearAllShownNotifications() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.SHOWN_NOTIFICATION_IDS)
        }
    }
}
