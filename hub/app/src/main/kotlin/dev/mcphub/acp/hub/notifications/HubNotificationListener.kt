package dev.mcphub.acp.hub.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Notification listener service that provides access to active notifications.
 *
 * This service uses a singleton bridge pattern: when the system connects the listener,
 * the instance is stored in a companion object so that notification tools can query
 * active notifications on demand.
 *
 * The user must grant Notification Access permission in system settings for this
 * service to function.
 */
class HubNotificationListener : NotificationListenerService() {

    companion object {
        @Volatile
        var instance: HubNotificationListener? = null
            private set
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
    }

    override fun onListenerDisconnected() {
        instance = null
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        // No-op — we query on demand via the tools
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // No-op
    }
}
