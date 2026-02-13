package com.flintsdk.hub.ui.setup

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Immutable
data class SetupUiState(
    val accessibilityEnabled: Boolean = false,
    val notificationListenerEnabled: Boolean = false,
    val notificationPermission: Boolean = false,
    val smsPermission: Boolean = false,
    val callPermission: Boolean = false,
    val contactsPermission: Boolean = false,
    val overlayPermission: Boolean = false
) {
    val totalSteps: Int get() = 7
    val completedSteps: Int get() = listOf(
        accessibilityEnabled,
        notificationListenerEnabled,
        notificationPermission,
        smsPermission,
        callPermission,
        contactsPermission,
        overlayPermission
    ).count { it }
}

@Immutable
sealed interface SetupUiEvent {
    data object CheckPermissions : SetupUiEvent
    data object OpenAccessibilitySettings : SetupUiEvent
    data object OpenNotificationListenerSettings : SetupUiEvent
    data object OpenNotificationSettings : SetupUiEvent
    data object OpenAppSettings : SetupUiEvent
    data object OpenOverlaySettings : SetupUiEvent
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val permissionChecker: PermissionChecker
) : ViewModel() {

    private val _uiState = MutableStateFlow(SetupUiState())
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun onEvent(event: SetupUiEvent) {
        when (event) {
            SetupUiEvent.CheckPermissions -> checkPermissions()
            SetupUiEvent.OpenAccessibilitySettings -> permissionChecker.openAccessibilitySettings()
            SetupUiEvent.OpenNotificationListenerSettings -> permissionChecker.openNotificationListenerSettings()
            SetupUiEvent.OpenNotificationSettings -> permissionChecker.openAppNotificationSettings()
            SetupUiEvent.OpenAppSettings -> permissionChecker.openAppSettings()
            SetupUiEvent.OpenOverlaySettings -> permissionChecker.openOverlaySettings()
        }
    }

    private fun checkPermissions() {
        _uiState.value = SetupUiState(
            accessibilityEnabled = permissionChecker.isAccessibilityEnabled(),
            notificationListenerEnabled = permissionChecker.isNotificationListenerEnabled(),
            notificationPermission = permissionChecker.isNotificationPermissionGranted(),
            smsPermission = permissionChecker.isSmsPermissionGranted(),
            callPermission = permissionChecker.isCallPermissionGranted(),
            contactsPermission = permissionChecker.isContactsPermissionGranted(),
            overlayPermission = permissionChecker.isOverlayPermissionGranted()
        )
    }
}
