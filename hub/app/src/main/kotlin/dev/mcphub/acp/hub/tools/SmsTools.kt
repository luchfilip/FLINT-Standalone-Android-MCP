package dev.mcphub.acp.hub.tools

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import dev.mcphub.acp.hub.server.HubTool
import dev.mcphub.acp.hub.server.ToolContent
import dev.mcphub.acp.hub.server.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Tool for sending SMS messages.
 */
class SmsSendTool(private val context: Context) : HubTool {

    override val name: String = "sms.send"

    override val description: String = "Send an SMS message to a phone number"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("phone_number", buildJsonObject {
                put("type", "string")
                put("description", "The phone number to send the SMS to")
            })
            put("message", buildJsonObject {
                put("type", "string")
                put("description", "The text message to send")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("phone_number"))
            add(kotlinx.serialization.json.JsonPrimitive("message"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val phoneNumber = params["phone_number"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: phone_number")),
                isError = true
            )

        val message = params["message"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: message")),
                isError = true
            )

        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            // Split long messages into parts
            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            }

            ToolResult(
                content = listOf(ToolContent.TextContent("SMS sent successfully to $phoneNumber"))
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to send SMS: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Tool for reading SMS messages from the inbox.
 */
class SmsReadTool(private val context: Context) : HubTool {

    override val name: String = "sms.read"

    override val description: String = "Read SMS messages from the inbox"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("limit", buildJsonObject {
                put("type", "integer")
                put("description", "Maximum number of messages to return (default: 10)")
            })
        })
        put("required", buildJsonArray {})
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val limit = params["limit"]?.jsonPrimitive?.int ?: 10

        return try {
            val messages = buildJsonArray {
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver.query(
                        Uri.parse("content://sms/inbox"),
                        arrayOf("address", "body", "date", "read"),
                        null,
                        null,
                        "date DESC"
                    )

                    if (cursor != null && cursor.moveToFirst()) {
                        var count = 0
                        do {
                            if (count >= limit) break
                            val address = cursor.getString(cursor.getColumnIndexOrThrow("address"))
                            val body = cursor.getString(cursor.getColumnIndexOrThrow("body"))
                            val date = cursor.getLong(cursor.getColumnIndexOrThrow("date"))
                            val read = cursor.getInt(cursor.getColumnIndexOrThrow("read"))

                            add(buildJsonObject {
                                put("address", address)
                                put("body", body)
                                put("date", date)
                                put("read", read == 1)
                            })
                            count++
                        } while (cursor.moveToNext())
                    }
                } finally {
                    cursor?.close()
                }
            }

            ToolResult(
                content = listOf(ToolContent.TextContent(messages.toString()))
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to read SMS messages: ${e.message}")),
                isError = true
            )
        }
    }
}
