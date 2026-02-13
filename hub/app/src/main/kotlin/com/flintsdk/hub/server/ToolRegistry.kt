package com.flintsdk.hub.server

import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a single piece of content returned by a tool execution.
 */
sealed class ToolContent {
    data class TextContent(val text: String) : ToolContent()
    data class ImageContent(val data: String, val mimeType: String) : ToolContent()
}

/**
 * Result of a tool execution.
 */
data class ToolResult(
    val content: List<ToolContent>,
    val isError: Boolean = false
)

/**
 * Interface that all hub tools must implement.
 */
interface HubTool {
    /** Namespaced tool name, e.g. "device.screenshot" or "musicapp.search" */
    val name: String

    /** Human-readable description of the tool */
    val description: String

    /** JSON Schema describing the expected input parameters */
    val inputSchema: JsonObject

    /** Execute the tool with the given parameters */
    suspend fun execute(params: JsonObject): ToolResult
}

/**
 * Dynamic registry for managing MCP tools.
 * Tools are namespaced (e.g., "device.screenshot", "musicapp.search").
 */
@Singleton
class ToolRegistry @Inject constructor() {

    private val tools = ConcurrentHashMap<String, HubTool>()

    @Volatile
    var onToolsChanged: (() -> Unit)? = null

    fun registerTool(tool: HubTool) {
        tools[tool.name] = tool
        onToolsChanged?.invoke()
    }

    fun removeTool(name: String): Boolean {
        val removed = tools.remove(name) != null
        if (removed) {
            onToolsChanged?.invoke()
        }
        return removed
    }

    fun getTool(name: String): HubTool? = tools[name]

    fun getAllTools(): List<HubTool> = tools.values.toList()

    fun getToolCount(): Int = tools.size

    fun clear() {
        tools.clear()
        onToolsChanged?.invoke()
    }
}
