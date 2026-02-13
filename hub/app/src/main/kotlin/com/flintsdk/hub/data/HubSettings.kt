package com.flintsdk.hub.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hub_settings")

data class HubSettingsData(
    val port: Int = DEFAULT_PORT,
    val authToken: String = "",
    val localhostOnly: Boolean = true
) {
    companion object {
        const val DEFAULT_PORT = 8080
    }
}

@Singleton
class HubSettings @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val PORT = intPreferencesKey("port")
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
        val LOCALHOST_ONLY = booleanPreferencesKey("localhost_only")
    }

    val settingsFlow: Flow<HubSettingsData> = context.dataStore.data.map { prefs ->
        HubSettingsData(
            port = prefs[Keys.PORT] ?: HubSettingsData.DEFAULT_PORT,
            authToken = prefs[Keys.AUTH_TOKEN] ?: "",
            localhostOnly = prefs[Keys.LOCALHOST_ONLY] ?: true
        )
    }

    suspend fun getSettings(): HubSettingsData {
        return settingsFlow.first()
    }

    suspend fun savePort(port: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PORT] = port
        }
    }

    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTH_TOKEN] = token
        }
    }

    suspend fun saveLocalhostOnly(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOCALHOST_ONLY] = enabled
        }
    }

    suspend fun saveAll(settings: HubSettingsData) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PORT] = settings.port
            prefs[Keys.AUTH_TOKEN] = settings.authToken
            prefs[Keys.LOCALHOST_ONLY] = settings.localhostOnly
        }
    }
}
