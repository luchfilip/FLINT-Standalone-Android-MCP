package com.flintsdk.hub.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flintsdk.hub.logging.LogEntry
import com.flintsdk.hub.logging.LogLevel
import kotlinx.collections.immutable.ImmutableList

@Composable
fun HubScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToFlintApps: () -> Unit,
    onNavigateToSetup: () -> Unit,
    viewModel: HubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onEvent(HubUiEvent.ObserveState)
    }

    HubContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToFlintApps = onNavigateToFlintApps,
        onNavigateToSetup = onNavigateToSetup
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubContent(
    uiState: HubUiState,
    onEvent: (HubUiEvent) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFlintApps: () -> Unit,
    onNavigateToSetup: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("FLINT Hub") },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            StatusCard(
                isRunning = uiState.isRunning,
                toolCount = uiState.toolCount,
                onEvent = onEvent,
                onNavigateToFlintApps = onNavigateToFlintApps,
                onNavigateToSetup = onNavigateToSetup
            )

            Spacer(modifier = Modifier.height(16.dp))

            LogsSection(
                logs = uiState.logs,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatusCard(
    isRunning: Boolean,
    toolCount: Int,
    onEvent: (HubUiEvent) -> Unit,
    onNavigateToFlintApps: () -> Unit,
    onNavigateToSetup: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ServerStatusRow(isRunning = isRunning)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Registered tools: $toolCount",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            ServerToggleButton(isRunning = isRunning, onEvent = onEvent)
            Spacer(modifier = Modifier.height(8.dp))
            QuickNavButtons(
                onNavigateToFlintApps = onNavigateToFlintApps,
                onNavigateToSetup = onNavigateToSetup
            )
        }
    }
}

@Composable
private fun ServerStatusRow(isRunning: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (isRunning) Color(0xFF4CAF50) else Color(0xFFE0E0E0))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isRunning) "Server Running" else "Server Stopped",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ServerToggleButton(isRunning: Boolean, onEvent: (HubUiEvent) -> Unit) {
    Button(
        onClick = {
            onEvent(if (isRunning) HubUiEvent.StopServer else HubUiEvent.StartServer)
        },
        modifier = Modifier.fillMaxWidth(),
        colors = if (isRunning) {
            ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        } else {
            ButtonDefaults.buttonColors()
        }
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(if (isRunning) "Stop Server" else "Start Server")
    }
}

@Composable
private fun QuickNavButtons(
    onNavigateToFlintApps: () -> Unit,
    onNavigateToSetup: () -> Unit
) {
    FilledTonalButton(
        onClick = onNavigateToFlintApps,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = Icons.Default.Apps, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Flint Apps")
    }
    Spacer(modifier = Modifier.height(8.dp))
    FilledTonalButton(
        onClick = onNavigateToSetup,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Setup")
    }
}

@Composable
private fun LogsSection(
    logs: ImmutableList<LogEntry>,
    modifier: Modifier = Modifier
) {
    Text(
        text = "Recent Logs",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No log entries yet",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val listState = rememberLazyListState()
            LaunchedEffect(logs.size) {
                if (logs.isNotEmpty()) {
                    listState.animateScrollToItem(logs.size - 1)
                }
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(logs) { entry ->
                    LogEntryRow(entry)
                }
            }
        }
    }
}

@Composable
private fun LogEntryRow(entry: LogEntry) {
    val levelColor = when (entry.level) {
        LogLevel.DEBUG -> Color.Gray
        LogLevel.INFO -> Color(0xFF2196F3)
        LogLevel.WARN -> Color(0xFFFFC107)
        LogLevel.ERROR -> Color(0xFFF44336)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = entry.level.name.first().toString(),
            color = levelColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "[${entry.tag}] ${entry.message}",
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
    }
}
