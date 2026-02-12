package dev.acpsdk.hub.server

import dev.acpsdk.hub.logging.HubLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles MCP (Model Context Protocol) JSON-RPC messages.
 *
 * Supports:
 *   - initialize: Returns server capabilities
 *   - tools/list: Returns all registered tools
 *   - tools/call: Dispatches tool execution to ToolRegistry
 *   - notifications/initialized: Client acknowledgement
 *   - SSE event streaming for tools/list_changed notifications
 */
@Singleton
class McpProtocol @Inject constructor(
    private val toolRegistry: ToolRegistry,
    private val logger: HubLogger
) {
    companion object {
        private const val TAG = "McpProtocol"
        private const val JSONRPC_VERSION = "2.0"
        private const val MCP_PROTOCOL_VERSION = "2025-11-25"
        private const val SERVER_NAME = "acp-hub"
        private const val SERVER_VERSION = "1.0.0"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private val _notifications = MutableSharedFlow<JsonObject>(extraBufferCapacity = 64)
    val notifications: SharedFlow<JsonObject> = _notifications.asSharedFlow()

    init {
        toolRegistry.onToolsChanged = {
            emitToolsListChanged()
        }
    }

    /**
     * Process an incoming JSON-RPC request and return a response.
     * Returns null for notifications (no response expected).
     */
    suspend fun handleMessage(message: JsonObject): JsonObject? {
        val method = message["method"]?.jsonPrimitive?.content
        val id = message["id"]
        val params = message["params"]?.jsonObject

        logger.d(TAG, "Received method=$method id=$id")

        return when (method) {
            "initialize" -> handleInitialize(id, params)
            "notifications/initialized" -> {
                logger.i(TAG, "Client initialized")
                null // Notification, no response
            }
            "tools/list" -> handleToolsList(id)
            "tools/call" -> handleToolsCall(id, params)
            "ping" -> handlePing(id)
            else -> {
                logger.w(TAG, "Unknown method: $method")
                errorResponse(id, -32601, "Method not found: $method")
            }
        }
    }

    private fun handleInitialize(id: JsonElement?, params: JsonObject?): JsonObject {
        val clientInfo = params?.get("clientInfo")
        logger.i(TAG, "Initialize request from client: $clientInfo")

        return buildJsonObject {
            put("jsonrpc", JSONRPC_VERSION)
            put("id", id ?: JsonNull)
            put("result", buildJsonObject {
                put("protocolVersion", MCP_PROTOCOL_VERSION)
                put("capabilities", buildJsonObject {
                    put("tools", buildJsonObject {
                        put("listChanged", true)
                    })
                })
                put("serverInfo", buildJsonObject {
                    put("name", SERVER_NAME)
                    put("version", SERVER_VERSION)
                })
                put("instructions", buildString {
                    append("ACP Hub controls an Android device. Follow these rules:\n")
                    append("1. Use device.get_tree FIRST to understand screen content and find elements. It is fast and returns structured data.\n")
                    append("2. Use device.screenshot ONLY when you need visual verification (images, colors, layout). It is slow.\n")
                    append("3. For ACP-enabled apps (tools prefixed with app name), prefer app-specific tools (read_screen, get_screen) over device-level tools.\n")
                    append("4. Use element bounds from device.get_tree for tap coordinates — never guess from screenshots.\n")
                    append("5. For scrolling large distances, use device.swipe instead of device.scroll (scroll moves very little).")
                })
            })
        }
    }

    private fun handleToolsList(id: JsonElement?): JsonObject {
        val tools = toolRegistry.getAllTools()
        logger.d(TAG, "tools/list returning ${tools.size} tools")

        val toolsArray = buildJsonArray {
            for (tool in tools) {
                add(buildJsonObject {
                    put("name", tool.name)
                    put("description", tool.description)
                    put("inputSchema", tool.inputSchema)
                })
            }
        }

        return buildJsonObject {
            put("jsonrpc", JSONRPC_VERSION)
            put("id", id ?: JsonNull)
            put("result", buildJsonObject {
                put("tools", toolsArray)
            })
        }
    }

    private suspend fun handleToolsCall(id: JsonElement?, params: JsonObject?): JsonObject {
        val toolName = params?.get("name")?.jsonPrimitive?.content
        val arguments = params?.get("arguments")?.jsonObject ?: JsonObject(emptyMap())

        if (toolName == null) {
            return errorResponse(id, -32602, "Missing required parameter: name")
        }

        val tool = toolRegistry.getTool(toolName)
        if (tool == null) {
            logger.w(TAG, "Tool not found: $toolName")
            return errorResponse(id, -32602, "Tool not found: $toolName")
        }

        logger.i(TAG, "Calling tool: $toolName")

        return try {
            val result = tool.execute(arguments)
            val contentArray = buildJsonArray {
                for (content in result.content) {
                    add(when (content) {
                        is ToolContent.TextContent -> buildJsonObject {
                            put("type", "text")
                            put("text", content.text)
                        }
                        is ToolContent.ImageContent -> buildJsonObject {
                            put("type", "image")
                            put("data", content.data)
                            put("mimeType", content.mimeType)
                        }
                    })
                }
            }

            buildJsonObject {
                put("jsonrpc", JSONRPC_VERSION)
                put("id", id ?: JsonNull)
                put("result", buildJsonObject {
                    put("content", contentArray)
                    if (result.isError) {
                        put("isError", true)
                    }
                })
            }
        } catch (e: Exception) {
            logger.e(TAG, "Tool execution failed: ${e.message}")
            buildJsonObject {
                put("jsonrpc", JSONRPC_VERSION)
                put("id", id ?: JsonNull)
                put("result", buildJsonObject {
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", "Error: ${e.message}")
                        })
                    })
                    put("isError", true)
                })
            }
        }
    }

    private fun handlePing(id: JsonElement?): JsonObject {
        return buildJsonObject {
            put("jsonrpc", JSONRPC_VERSION)
            put("id", id ?: JsonNull)
            put("result", buildJsonObject { })
        }
    }

    private fun errorResponse(id: JsonElement?, code: Int, message: String): JsonObject {
        return buildJsonObject {
            put("jsonrpc", JSONRPC_VERSION)
            put("id", id ?: JsonNull)
            put("error", buildJsonObject {
                put("code", code)
                put("message", message)
            })
        }
    }

    private fun emitToolsListChanged() {
        val notification = buildJsonObject {
            put("jsonrpc", JSONRPC_VERSION)
            put("method", "notifications/tools/list_changed")
        }
        _notifications.tryEmit(notification)
    }
}
