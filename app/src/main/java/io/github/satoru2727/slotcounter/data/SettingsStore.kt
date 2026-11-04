package io.github.satoru2727.slotcounter.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

data class AppSettings(
    val haptics: Boolean = true,
    val silentAudio: Boolean = true,
)

class SettingsStore(private val context: Context) {

    val flow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            haptics = prefs[KEY_HAPTICS] ?: true,
            silentAudio = prefs[KEY_SILENT_AUDIO] ?: true,
        )
    }

    suspend fun setHaptics(enabled: Boolean) {
        context.dataStore.edit { it[KEY_HAPTICS] = enabled }
    }

    suspend fun setSilentAudio(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SILENT_AUDIO] = enabled }
    }

    private companion object {
        val KEY_HAPTICS = booleanPreferencesKey("haptics")
        val KEY_SILENT_AUDIO = booleanPreferencesKey("silent_audio")
    }
}
