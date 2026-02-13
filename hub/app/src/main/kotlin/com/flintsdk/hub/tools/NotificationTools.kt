package com.flintsdk.hub.tools

import android.app.RemoteInput
import android.content.Intent
import com.flintsdk.hub.notifications.HubNotificationListener
import com.flintsdk.hub.server.HubTool
import com.flintsdk.hub.server.ToolContent
import com.flintsdk.hub.server.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Tool for listing active notifications.
 */
class NotificationsListTool : HubTool {

    override val name: String = "notifications.list"

    override val description: String = "List all active notifications on the device"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {})
        put("required", buildJsonArray {})
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val listener = HubNotificationListener.instance
            ?: return ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "Notification listener is not connected. " +
                            "Please grant Notification Access permission in system settings."
                    )
                ),
                isError = true
            )

        return try {
            val notifications = listener.activeNotifications
            val result = buildJsonArray {
                for (sbn in notifications) {
                    val notification = sbn.notification
                    val extras = notification.extras
                    add(buildJsonObject {
                        put("package", sbn.packageName)
                        put("title", extras.getCharSequence("android.title")?.toString() ?: "")
                        put("text", extras.getCharSequence("android.text")?.toString() ?: "")
                        put("key", sbn.key)
                        put("postTime", sbn.postTime)
                        put("isClearable", sbn.isClearable)
                    })
                }
            }

            ToolResult(
                content = listOf(ToolContent.TextContent(result.toString()))
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to list notifications: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Tool for dismissing a notification by its key.
 */
class NotificationsDismissTool : HubTool {

    override val name: String = "notifications.dismiss"

    override val description: String = "Dismiss a notification by its key"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("key", buildJsonObject {
                put("type", "string")
                put("description", "The notification key to dismiss")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("key"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val key = params["key"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: key")),
                isError = true
            )

        val listener = HubNotificationListener.instance
            ?: return ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "Notification listener is not connected. " +
                            "Please grant Notification Access permission in system settings."
                    )
                ),
                isError = true
            )

        return try {
            listener.cancelNotification(key)
            ToolResult(
                content = listOf(ToolContent.TextContent("Notification dismissed: $key"))
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to dismiss notification: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Tool for tapping (activating) a notification's content intent.
 */
class NotificationsTapTool : HubTool {

    override val name: String = "notifications.tap"

    override val description: String = "Tap a notification to activate its content intent"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("key", buildJsonObject {
                put("type", "string")
                put("description", "The notification key to tap")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("key"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val key = params["key"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: key")),
                isError = true
            )

        val listener = HubNotificationListener.instance
            ?: return ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "Notification listener is not connected. " +
                            "Please grant Notification Access permission in system settings."
                    )
                ),
                isError = true
            )

        return try {
            val sbn = listener.activeNotifications.find { it.key == key }
                ?: return ToolResult(
                    content = listOf(ToolContent.TextContent("Notification not found with key: $key")),
                    isError = true
                )

            val contentIntent = sbn.notification.contentIntent
                ?: return ToolResult(
                    content = listOf(ToolContent.TextContent("Notification has no content intent")),
                    isError = true
                )

            contentIntent.send()
            ToolResult(
                content = listOf(ToolContent.TextContent("Notification tapped: $key"))
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to tap notification: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Tool for replying to a notification that supports inline reply (RemoteInput).
 */
class NotificationsReplyTool : HubTool {

    override val name: String = "notifications.reply"

    override val description: String = "Reply to a notification that supports inline reply"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("key", buildJsonObject {
                put("type", "string")
                put("description", "The notification key to reply to")
            })
            put("text", buildJsonObject {
                put("type", "string")
                put("description", "The reply text to send")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("key"))
            add(kotlinx.serialization.json.JsonPrimitive("text"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val key = params["key"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: key")),
                isError = true
            )

        val text = params["text"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: text")),
                isError = true
            )

        val listener = HubNotificationListener.instance
            ?: return ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "Notification listener is not connected. " +
                            "Please grant Notification Access permission in system settings."
                    )
                ),
                isError = true
            )

        return try {
            val sbn = listener.activeNotifications.find { it.key == key }
                ?: return ToolResult(
                    content = listOf(ToolContent.TextContent("Notification not found with key: $key")),
                    isError = true
                )

            val notification = sbn.notification
            val actions = notification.actions
                ?: return ToolResult(
                    content = listOf(ToolContent.TextContent("Notification has no actions")),
                    isError = true
                )

            // Find an action with RemoteInput (inline reply)
            for (action in actions) {
                val remoteInputs = action.remoteInputs
                if (remoteInputs != null && remoteInputs.isNotEmpty()) {
                    val intent = Intent()
                    val bundle = android.os.Bundle()

                    for (remoteInput in remoteInputs) {
                        bundle.putCharSequence(remoteInput.resultKey, text)
                    }

                    RemoteInput.addResultsToIntent(remoteInputs, intent, bundle)
                    action.actionIntent.send(
                        listener,
                        0,
                        intent
                    )

                    return ToolResult(
                        content = listOf(ToolContent.TextContent("Reply sent to notification: $key"))
                    )
                }
            }

            ToolResult(
                content = listOf(ToolContent.TextContent("Notification does not support inline reply")),
                isError = true
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to reply to notification: ${e.message}")),
                isError = true
            )
        }
    }
}
