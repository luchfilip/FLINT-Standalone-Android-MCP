package dev.mcphub.acp.hub.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.mcphub.acp.hub.accessibility.HubAccessibilityService
import dev.mcphub.acp.hub.notifications.HubNotificationListener

/**
 * Utility for checking permission statuses and launching the appropriate system settings.
 */
object PermissionHelper {

    // --- Status checks ---

    /** True when the accessibility service is connected. */
    fun isAccessibilityEnabled(): Boolean {
        return HubAccessibilityService.isConnected
    }

    /** True when the notification listener service is enabled. */
    fun isNotificationListenerEnabled(context: Context): Boolean {
        val packageName = context.packageName
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
        return enabledPackages.contains(packageName)
    }

    /** True when POST_NOTIFICATIONS permission is granted (always true below API 33). */
    fun isNotificationPermissionGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /** True when both SEND_SMS and READ_SMS are granted. */
    fun isSmsPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.READ_SMS
            ) == PackageManager.PERMISSION_GRANTED
    }

    /** True when CALL_PHONE is granted. */
    fun isCallPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.CALL_PHONE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** True when READ_CONTACTS is granted. */
    fun isContactsPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** True when SYSTEM_ALERT_WINDOW (display over other apps) is granted. */
    fun isOverlayPermissionGranted(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    // --- Settings intents ---

    /** Open the accessibility settings screen. */
    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /** Open the notification listener settings screen. */
    fun openNotificationListenerSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /** Open the app notification settings (for POST_NOTIFICATIONS). */
    fun openAppNotificationSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } else {
            openAppSettings(context)
        }
    }

    /** Open the app-specific settings page (for runtime permissions). */
    fun openAppSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = android.net.Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    /** Open the overlay permission settings for this app. */
    fun openOverlaySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.fromParts("package", context.packageName, null)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}
