package com.flintsdk.hub.ui.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.flintsdk.hub.data.HubSettings
import com.flintsdk.hub.data.HubSettingsData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
sealed interface SettingsUiState {
    data object Loading : SettingsUiState

    data class Loaded(
        val portText: String,
        val authToken: String,
        val localhostOnly: Boolean
    ) : SettingsUiState
}

@Immutable
sealed interface SettingsUiEvent {
    data object LoadSettings : SettingsUiEvent
    data class UpdatePort(val text: String) : SettingsUiEvent
    data class UpdateAuthToken(val token: String) : SettingsUiEvent
    data class ToggleLocalhostOnly(val enabled: Boolean) : SettingsUiEvent
    data object SaveSettings : SettingsUiEvent
}

@Immutable
sealed interface SettingsUiEffect {
    data class ShowSnackbar(val message: String) : SettingsUiEffect
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val hubSettings: HubSettings
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsUiEffect>()
    val effects: SharedFlow<SettingsUiEffect> = _effects.asSharedFlow()

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.LoadSettings -> loadSettings()
            is SettingsUiEvent.UpdatePort -> updatePort(event.text)
            is SettingsUiEvent.UpdateAuthToken -> updateAuthToken(event.token)
            is SettingsUiEvent.ToggleLocalhostOnly -> toggleLocalhostOnly(event.enabled)
            SettingsUiEvent.SaveSettings -> saveSettings()
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val settings = hubSettings.settingsFlow.first()
            _uiState.value = SettingsUiState.Loaded(
                portText = settings.port.toString(),
                authToken = settings.authToken,
                localhostOnly = settings.localhostOnly
            )
        }
    }

    private fun updatePort(text: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(portText = text.filter { it.isDigit() })
    }

    private fun updateAuthToken(token: String) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(authToken = token)
    }

    private fun toggleLocalhostOnly(enabled: Boolean) {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        _uiState.value = state.copy(localhostOnly = enabled)
    }

    private fun saveSettings() {
        val state = _uiState.value as? SettingsUiState.Loaded ?: return
        viewModelScope.launch {
            val port = state.portText.toIntOrNull() ?: HubSettingsData.DEFAULT_PORT
            try {
                hubSettings.saveAll(
                    HubSettingsData(
                        port = port,
                        authToken = state.authToken,
                        localhostOnly = state.localhostOnly
                    )
                )
                _effects.emit(SettingsUiEffect.ShowSnackbar("Settings saved. Restart the server to apply changes."))
            } catch (e: Exception) {
                _effects.emit(SettingsUiEffect.ShowSnackbar("Error saving settings: ${e.message}"))
            }
        }
    }
}
