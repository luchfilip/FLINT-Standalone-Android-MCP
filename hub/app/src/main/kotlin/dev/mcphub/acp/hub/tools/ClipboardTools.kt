package dev.mcphub.acp.hub.tools

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import dev.mcphub.acp.hub.server.HubTool
import dev.mcphub.acp.hub.server.ToolContent
import dev.mcphub.acp.hub.server.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Tool that reads the current clipboard content.
 *
 * Note: On API 31+ (Android 12), background clipboard access is restricted.
 * The tool handles this gracefully by returning an appropriate message.
 */
class ClipboardGetTool(private val context: Context) : HubTool {

    override val name: String = "clipboard.get"

    override val description: String =
        "Read the current text content from the clipboard. " +
            "On Android 12+ clipboard access may be restricted when the app is in the background."

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {})
        put("required", buildJsonArray {})
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            if (!clipboardManager.hasPrimaryClip()) {
                return ToolResult(
                    content = listOf(ToolContent.TextContent("Clipboard is empty"))
                )
            }

            val clip = clipboardManager.primaryClip
            if (clip == null || clip.itemCount == 0) {
                return ToolResult(
                    content = listOf(ToolContent.TextContent("Clipboard is empty"))
                )
            }

            val item = clip.getItemAt(0)
            val text = item.coerceToText(context)?.toString()

            if (text.isNullOrEmpty()) {
                ToolResult(
                    content = listOf(
                        ToolContent.TextContent("Clipboard contains no text content (may contain other data types)")
                    )
                )
            } else {
                ToolResult(
                    content = listOf(ToolContent.TextContent(text))
                )
            }
        } catch (e: SecurityException) {
            // API 31+ may throw SecurityException for background clipboard access
            ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "Clipboard access denied. On Android 12+, clipboard access is restricted " +
                            "when the app is not in the foreground. Error: ${e.message}"
                    )
                ),
                isError = true
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to read clipboard: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Tool that sets text content on the clipboard.
 */
class ClipboardSetTool(private val context: Context) : HubTool {

    override val name: String = "clipboard.set"

    override val description: String = "Set text content on the clipboard"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("text", buildJsonObject {
                put("type", "string")
                put("description", "The text to copy to the clipboard")
            })
        })
        put("required", buildJsonArray {
            add(JsonPrimitive("text"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val text = params["text"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: text")),
                isError = true
            )

        return try {
            val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("ACP", text)
            clipboardManager.setPrimaryClip(clip)

            ToolResult(
                content = listOf(
                    ToolContent.TextContent("Text copied to clipboard (${text.length} characters)")
                )
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to set clipboard: ${e.message}")),
                isError = true
            )
        }
    }
}
