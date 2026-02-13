package com.flintsdk.hub.ui.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun PermissionSetupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onEvent(SetupUiEvent.CheckPermissions)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PermissionSetupContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        onNavigateBack = onNavigateBack,
        onNavigateToSettings = onNavigateToSettings
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupContent(
    uiState: SetupUiState,
    onEvent: (SetupUiEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                .verticalScroll(rememberScrollState())
        ) {
            SetupHeader(uiState = uiState)

            Spacer(modifier = Modifier.height(20.dp))

            PermissionSteps(uiState = uiState, onEvent = onEvent)

            Spacer(modifier = Modifier.height(20.dp))

            ServerConfigCard(onNavigateToSettings = onNavigateToSettings)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SetupHeader(uiState: SetupUiState) {
    Text(
        text = "Permission Setup",
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = "${uiState.completedSteps} of ${uiState.totalSteps} steps completed",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PermissionSteps(
    uiState: SetupUiState,
    onEvent: (SetupUiEvent) -> Unit
) {
    SetupStepCard(
        icon = Icons.Default.Accessibility,
        title = "Accessibility Service",
        description = "Required for screen reading, tap/swipe gestures, and device input control.",
        isComplete = uiState.accessibilityEnabled,
        buttonText = "Open Accessibility Settings",
        onAction = { onEvent(SetupUiEvent.OpenAccessibilitySettings) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    SetupStepCard(
        icon = Icons.Default.Notifications,
        title = "Notification Listener",
        description = "Required for reading and managing device notifications.",
        isComplete = uiState.notificationListenerEnabled,
        buttonText = "Open Notification Access",
        onAction = { onEvent(SetupUiEvent.OpenNotificationListenerSettings) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    SetupStepCard(
        icon = Icons.Default.Notifications,
        title = "Notification Permission",
        description = "Allows the Hub to show its foreground service notification.",
        isComplete = uiState.notificationPermission,
        buttonText = "Open Notification Settings",
        onAction = { onEvent(SetupUiEvent.OpenNotificationSettings) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    SetupStepCard(
        icon = Icons.Default.Sms,
        title = "SMS Permissions",
        description = "Required for sending and reading SMS messages.",
        isComplete = uiState.smsPermission,
        buttonText = "Open App Settings",
        onAction = { onEvent(SetupUiEvent.OpenAppSettings) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    SetupStepCard(
        icon = Icons.Default.Call,
        title = "Phone Call Permission",
        description = "Required for making phone calls.",
        isComplete = uiState.callPermission,
        buttonText = "Open App Settings",
        onAction = { onEvent(SetupUiEvent.OpenAppSettings) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    SetupStepCard(
        icon = Icons.Default.Contacts,
        title = "Contacts Permission",
        description = "Required for reading and managing contacts.",
        isComplete = uiState.contactsPermission,
        buttonText = "Open App Settings",
        onAction = { onEvent(SetupUiEvent.OpenAppSettings) }
    )

    Spacer(modifier = Modifier.height(12.dp))

    SetupStepCard(
        icon = Icons.Default.Layers,
        title = "Display Over Other Apps",
        description = "Required for launching apps from the background.",
        isComplete = uiState.overlayPermission,
        buttonText = "Open Overlay Settings",
        onAction = { onEvent(SetupUiEvent.OpenOverlaySettings) }
    )
}

@Composable
private fun ServerConfigCard(onNavigateToSettings: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Server Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Configure the MCP server port and authentication token.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onNavigateToSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Server Settings")
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    icon: ImageVector,
    title: String,
    description: String,
    isComplete: Boolean,
    buttonText: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isComplete) {
                        Color(0xFF4CAF50)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            if (isComplete) Color(0xFF4CAF50) else Color(0xFFE0E0E0)
                        )
                )
            }

            if (!isComplete) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonText)
                }
            }
        }
    }
}
