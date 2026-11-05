package com.florilegia.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.florilegiaDataStore by preferencesDataStore(name = "florilegia_prefs")

class ReaderPreferencesStore(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")

    val darkModeFlow: Flow<Boolean?> = context.florilegiaDataStore.data.map { prefs ->
        if (prefs.contains(darkModeKey)) prefs[darkModeKey] else null
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.florilegiaDataStore.edit { prefs ->
            prefs[darkModeKey] = enabled
        }
    }
}