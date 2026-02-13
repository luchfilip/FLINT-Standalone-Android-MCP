package com.flintsdk.hub.ui.flintapps

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.flintsdk.hub.flint.FlintApp
import com.flintsdk.hub.flint.FlintIntegrationRegistrar
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class FlintAppsUiState(
    val apps: ImmutableList<FlintApp> = persistentListOf(),
    val isScanning: Boolean = false
)

@Immutable
sealed interface FlintAppsUiEvent {
    data object ObserveApps : FlintAppsUiEvent
    data object Rescan : FlintAppsUiEvent
}

@HiltViewModel
class FlintAppsViewModel @Inject constructor(
    private val flintIntegrationRegistrar: FlintIntegrationRegistrar
) : ViewModel() {

    private val _uiState = MutableStateFlow(FlintAppsUiState())
    val uiState: StateFlow<FlintAppsUiState> = _uiState.asStateFlow()

    fun onEvent(event: FlintAppsUiEvent) {
        when (event) {
            FlintAppsUiEvent.ObserveApps -> observeApps()
            FlintAppsUiEvent.Rescan -> rescan()
        }
    }

    private fun observeApps() {
        viewModelScope.launch {
            flintIntegrationRegistrar.discoveredApps.collect { apps ->
                _uiState.value = _uiState.value.copy(
                    apps = apps.toImmutableList()
                )
            }
        }
    }

    private fun rescan() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)
            try {
                flintIntegrationRegistrar.rescan()
            } finally {
                _uiState.value = _uiState.value.copy(isScanning = false)
            }
        }
    }
}
