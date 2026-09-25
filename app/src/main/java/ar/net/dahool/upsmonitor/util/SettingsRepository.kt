package ar.net.dahool.upsmonitor.util

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ups_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val LAST_REGISTERED_TOKEN = stringPreferencesKey("last_registered_token")
        val LAST_REGISTERED_URL = stringPreferencesKey("last_registered_url")
    }

    val serverUrlFlow: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[SERVER_URL] ?: "" }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { prefs ->
            prefs[SERVER_URL] = url
        }
    }

    /**
     * Returns a stable, app-scoped unique device identifier. Android does not expose
     * a reliable, privacy-compliant hardware ID (IMEI/serial require special permissions
     * and are restricted/deprecated), so we generate a random UUID on first launch and
     * persist it. It survives app restarts but resets on reinstall/data clear.
     */
    suspend fun getOrCreateDeviceId(): String {
        val existing = context.dataStore.data.map { it[DEVICE_ID] }.first()
        if (!existing.isNullOrBlank()) return existing

        val newId = UUID.randomUUID().toString()
        context.dataStore.edit { prefs -> prefs[DEVICE_ID] = newId }
        return newId
    }

    suspend fun getLastRegisteredToken(): String? =
        context.dataStore.data.map { it[LAST_REGISTERED_TOKEN] }.first()

    suspend fun getLastRegisteredUrl(): String? =
        context.dataStore.data.map { it[LAST_REGISTERED_URL] }.first()

    suspend fun saveLastRegistration(token: String, url: String) {
        context.dataStore.edit { prefs ->
            prefs[LAST_REGISTERED_TOKEN] = token
            prefs[LAST_REGISTERED_URL] = url
        }
    }
}

