package com.immichtv.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "immich_settings")

class PreferencesRepository(private val context: Context) {

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val API_KEY = stringPreferencesKey("api_key")
        val AUTH_MODE = stringPreferencesKey("auth_mode")
    }

    val serverUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SERVER_URL] ?: ""
    }

    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[API_KEY] ?: ""
    }

    val authMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[AUTH_MODE] ?: "api_key"
    }

    suspend fun saveSettings(serverUrl: String, apiKey: String, authMode: String = "api_key") {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL] = serverUrl
            prefs[API_KEY] = apiKey
            prefs[AUTH_MODE] = authMode
        }
    }
}
