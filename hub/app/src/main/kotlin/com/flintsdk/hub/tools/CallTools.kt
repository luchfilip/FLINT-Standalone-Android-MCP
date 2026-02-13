package com.flintsdk.hub.tools

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telecom.TelecomManager
import com.flintsdk.hub.server.HubTool
import com.flintsdk.hub.server.ToolContent
import com.flintsdk.hub.server.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Tool for dialing a phone number.
 */
class CallDialTool(private val context: Context) : HubTool {

    override val name: String = "call.dial"

    override val description: String = "Dial a phone number to initiate a call"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("phone_number", buildJsonObject {
                put("type", "string")
                put("description", "The phone number to dial")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("phone_number"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val phoneNumber = params["phone_number"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: phone_number")),
                isError = true
            )

        return try {
            val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$phoneNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

            ToolResult(
                content = listOf(ToolContent.TextContent("Dialing $phoneNumber"))
            )
        } catch (e: SecurityException) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Permission denied: CALL_PHONE permission is required")),
                isError = true
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to dial: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Tool for answering an incoming phone call.
 */
class CallAnswerTool(private val context: Context) : HubTool {

    override val name: String = "call.answer"

    override val description: String = "Answer an incoming phone call"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {})
        put("required", buildJsonArray {})
    }

    @Suppress("DEPRECATION")
    override suspend fun execute(params: JsonObject): ToolResult {
        return try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            telecomManager.acceptRingingCall()

            ToolResult(
                content = listOf(ToolContent.TextContent("Answered incoming call"))
            )
        } catch (e: SecurityException) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Permission denied: ANSWER_PHONE_CALLS permission is required")),
                isError = true
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to answer call: ${e.message}")),
                isError = true
            )
        }
    }
}
