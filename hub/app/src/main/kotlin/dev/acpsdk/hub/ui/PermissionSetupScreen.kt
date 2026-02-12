package dev.acpsdk.hub.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Guided setup screen that walks users through enabling all required
 * permissions and services for the ACP Hub to function correctly.
 *
 * Each step shows its current status (done/pending), a description,
 * and an action button to open the relevant system settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSetupScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current

    // Permission states - refreshed periodically
    var accessibilityEnabled by remember { mutableStateOf(false) }
    var notificationListenerEnabled by remember { mutableStateOf(false) }
    var notificationPermission by remember { mutableStateOf(false) }
    var smsPermission by remember { mutableStateOf(false) }
    var callPermission by remember { mutableStateOf(false) }
    var contactsPermission by remember { mutableStateOf(false) }
    var overlayPermission by remember { mutableStateOf(false) }

    // Periodically refresh permission states (e.g. user returns from settings)
    LaunchedEffect(Unit) {
        while (true) {
            accessibilityEnabled = PermissionHelper.isAccessibilityEnabled()
            notificationListenerEnabled = PermissionHelper.isNotificationListenerEnabled(context)
            notificationPermission = PermissionHelper.isNotificationPermissionGranted(context)
            smsPermission = PermissionHelper.isSmsPermissionGranted(context)
            callPermission = PermissionHelper.isCallPermissionGranted(context)
            contactsPermission = PermissionHelper.isContactsPermissionGranted(context)
            overlayPermission = PermissionHelper.isOverlayPermissionGranted(context)
            delay(1000)
        }
    }

    val totalSteps = 7
    val completedSteps = listOf(
        accessibilityEnabled,
        notificationListenerEnabled,
        notificationPermission,
        smsPermission,
        callPermission,
        contactsPermission,
        overlayPermission
    ).count { it }

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
            // Progress summary
            Text(
                text = "Permission Setup",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$completedSteps of $totalSteps steps completed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Step 1: Accessibility Service
            SetupStepCard(
                icon = Icons.Default.Accessibility,
                title = "Accessibility Service",
                description = "Required for screen reading, tap/swipe gestures, and device input control.",
                isComplete = accessibilityEnabled,
                buttonText = "Open Accessibility Settings",
                onAction = { PermissionHelper.openAccessibilitySettings(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 2: Notification Listener
            SetupStepCard(
                icon = Icons.Default.Notifications,
                title = "Notification Listener",
                description = "Required for reading and managing device notifications.",
                isComplete = notificationListenerEnabled,
                buttonText = "Open Notification Access",
                onAction = { PermissionHelper.openNotificationListenerSettings(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 3: POST_NOTIFICATIONS
            SetupStepCard(
                icon = Icons.Default.Notifications,
                title = "Notification Permission",
                description = "Allows the Hub to show its foreground service notification.",
                isComplete = notificationPermission,
                buttonText = "Open Notification Settings",
                onAction = { PermissionHelper.openAppNotificationSettings(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 4: SMS
            SetupStepCard(
                icon = Icons.Default.Sms,
                title = "SMS Permissions",
                description = "Required for sending and reading SMS messages.",
                isComplete = smsPermission,
                buttonText = "Open App Settings",
                onAction = { PermissionHelper.openAppSettings(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 5: Call
            SetupStepCard(
                icon = Icons.Default.Call,
                title = "Phone Call Permission",
                description = "Required for making phone calls.",
                isComplete = callPermission,
                buttonText = "Open App Settings",
                onAction = { PermissionHelper.openAppSettings(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 6: Contacts
            SetupStepCard(
                icon = Icons.Default.Contacts,
                title = "Contacts Permission",
                description = "Required for reading and managing contacts.",
                isComplete = contactsPermission,
                buttonText = "Open App Settings",
                onAction = { PermissionHelper.openAppSettings(context) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Step 7: Display Over Other Apps
            SetupStepCard(
                icon = Icons.Default.Layers,
                title = "Display Over Other Apps",
                description = "Required for launching apps from the background.",
                isComplete = overlayPermission,
                buttonText = "Open Overlay Settings",
                onAction = { PermissionHelper.openOverlaySettings(context) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Server configuration shortcut
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

            Spacer(modifier = Modifier.height(24.dp))
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

                // Status indicator
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
