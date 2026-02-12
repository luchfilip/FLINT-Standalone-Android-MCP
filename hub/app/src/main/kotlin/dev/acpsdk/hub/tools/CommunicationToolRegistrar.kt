package dev.acpsdk.hub.tools

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.acpsdk.hub.server.ToolRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers all communication-related tools (SMS, Call, Contacts, Notifications)
 * with the central ToolRegistry.
 */
@Singleton
class CommunicationToolRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toolRegistry: ToolRegistry
) {
    fun registerAll() {
        // SMS
        toolRegistry.registerTool(SmsSendTool(context))
        toolRegistry.registerTool(SmsReadTool(context))
        // Call
        toolRegistry.registerTool(CallDialTool(context))
        toolRegistry.registerTool(CallAnswerTool(context))
        // Contacts
        toolRegistry.registerTool(ContactsSearchTool(context))
        toolRegistry.registerTool(ContactsCreateTool(context))
        // Notifications
        toolRegistry.registerTool(NotificationsListTool())
        toolRegistry.registerTool(NotificationsDismissTool())
        toolRegistry.registerTool(NotificationsTapTool())
        toolRegistry.registerTool(NotificationsReplyTool())
    }
}
