package ar.net.dahool.upsmonitor

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ar.net.dahool.upsmonitor.data.model.DeviceRegistration
import ar.net.dahool.upsmonitor.data.model.UpsMetrics
import ar.net.dahool.upsmonitor.data.repository.RegistrationResult
import ar.net.dahool.upsmonitor.data.repository.UpsRepository
import ar.net.dahool.upsmonitor.data.repository.UpsResult
import ar.net.dahool.upsmonitor.util.SettingsRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class UiState {
    object Loading : UiState()
    data class Success(val metrics: UpsMetrics, val lastUpdated: Long = System.currentTimeMillis()) : UiState()
    data class Error(val message: String) : UiState()
    object Unconfigured : UiState()
}

class UpsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "UpsViewModel"
    }

    private val repository = UpsRepository()
    private val settings = SettingsRepository(application)

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            settings.serverUrlFlow.collect { url ->
                _serverUrl.value = url
                if (url.isBlank()) {
                    _uiState.value = UiState.Unconfigured
                } else {
                    refresh()
                    registerDeviceIfNeeded(url)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val url = settings.serverUrlFlow.first()
            if (url.isBlank()) {
                _uiState.value = UiState.Unconfigured
                return@launch
            }
            // Keep last data visible during refresh if we had success before
            if (_uiState.value !is UiState.Success) {
                _uiState.value = UiState.Loading
            }
            when (val result = repository.fetchStatus(url)) {
                is UpsResult.Success -> _uiState.value = UiState.Success(result.metrics)
                is UpsResult.Error -> _uiState.value = UiState.Error(result.message)
            }
        }
    }

    fun startPolling() {
        stopPolling()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(60_000L) // 1 minute
                refresh()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun saveServerUrl(url: String) {
        viewModelScope.launch {
            val trimmed = url.trim()
            settings.saveServerUrl(trimmed)
            if (trimmed.isNotBlank()) {
                registerDeviceIfNeeded(trimmed, force = true)
            }
        }
    }

    /**
     * Registers this device's FCM token with the server at POST /api/register.
     * Skips the call if the same (token, url) pair was already registered last time,
     * unless [force] is true (e.g. user just changed the server URL).
     */
    private fun registerDeviceIfNeeded(baseUrl: String, force: Boolean = false) {
        viewModelScope.launch {
            try {
                val token = FirebaseMessaging.getInstance().token.await()
                val lastToken = settings.getLastRegisteredToken()
                val lastUrl = settings.getLastRegisteredUrl()

                if (!force && token == lastToken && baseUrl == lastUrl) {
                    Log.d(TAG, "Device already registered with this server/token, skipping.")
                    return@launch
                }

                val deviceId = settings.getOrCreateDeviceId()
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

                val registration = DeviceRegistration(
                    deviceToken = token,
                    deviceName = deviceName,
                    deviceId = deviceId
                )

                when (val result = repository.registerDevice(baseUrl, registration)) {
                    is RegistrationResult.Success -> {
                        Log.i(TAG, "Device registered successfully with $baseUrl")
                        settings.saveLastRegistration(token, baseUrl)
                    }
                    is RegistrationResult.Error -> {
                        Log.w(TAG, "Device registration failed: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not retrieve FCM token for registration: ${e.localizedMessage}")
            }
        }
    }

    /** Re-checks registration; called by the FCM service on new token, and as a safety net on app resume. */
    fun onFcmTokenRefreshed(force: Boolean = true) {
        viewModelScope.launch {
            val url = settings.serverUrlFlow.first()
            if (url.isNotBlank()) {
                registerDeviceIfNeeded(url, force = force)
            }
        }
    }
}

