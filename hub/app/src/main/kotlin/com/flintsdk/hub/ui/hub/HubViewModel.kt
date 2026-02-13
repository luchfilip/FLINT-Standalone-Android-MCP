package com.flintsdk.hub.ui.hub

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import com.flintsdk.hub.logging.LogEntry
import com.flintsdk.hub.logging.Logger
import com.flintsdk.hub.server.McpServer
import com.flintsdk.hub.server.ToolRegistry
import com.flintsdk.hub.service.HubServiceController
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@Immutable
data class HubUiState(
    val isRunning: Boolean = false,
    val toolCount: Int = 0,
    val logs: ImmutableList<LogEntry> = persistentListOf()
)

@Immutable
sealed interface HubUiEvent {
    data object StartServer : HubUiEvent
    data object StopServer : HubUiEvent
    data object ObserveState : HubUiEvent
}

@HiltViewModel
class HubViewModel @Inject constructor(
    private val serviceController: HubServiceController,
    private val mcpServer: McpServer,
    private val toolRegistry: ToolRegistry,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(HubUiState())
    val uiState: StateFlow<HubUiState> = _uiState.asStateFlow()

    fun onEvent(event: HubUiEvent) {
        when (event) {
            HubUiEvent.StartServer -> serviceController.startServer()
            HubUiEvent.StopServer -> serviceController.stopServer()
            HubUiEvent.ObserveState -> observeState()
        }
    }

    private fun observeState() {
        viewModelScope.launch {
            combine(
                mcpServer.isRunning,
                toolRegistry.toolCount,
                logger.recentLogs
            ) { isRunning, toolCount, logs ->
                HubUiState(
                    isRunning = isRunning,
                    toolCount = toolCount,
                    logs = logs.toImmutableList()
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }
}
