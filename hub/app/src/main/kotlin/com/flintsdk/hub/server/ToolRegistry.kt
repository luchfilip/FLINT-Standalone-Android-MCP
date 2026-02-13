package com.flintsdk.hub.server

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

sealed class ToolContent {
    data class TextContent(val text: String) : ToolContent()
    data class ImageContent(val data: String, val mimeType: String) : ToolContent()
}

data class ToolResult(
    val content: List<ToolContent>,
    val isError: Boolean = false
)

interface HubTool {
    val name: String
    val description: String
    val inputSchema: JsonObject
    suspend fun execute(params: JsonObject): ToolResult
}

@Singleton
class ToolRegistry @Inject constructor() {

    private val tools = ConcurrentHashMap<String, HubTool>()

    @Volatile
    var onToolsChanged: (() -> Unit)? = null

    private val _toolCount = MutableStateFlow(0)
    val toolCount: StateFlow<Int> = _toolCount.asStateFlow()

    fun registerTool(tool: HubTool) {
        tools[tool.name] = tool
        _toolCount.value = tools.size
        onToolsChanged?.invoke()
    }

    fun removeTool(name: String): Boolean {
        val removed = tools.remove(name) != null
        if (removed) {
            _toolCount.value = tools.size
            onToolsChanged?.invoke()
        }
        return removed
    }

    fun getTool(name: String): HubTool? = tools[name]

    fun getAllTools(): List<HubTool> = tools.values.toList()

    fun getToolCount(): Int = tools.size

    fun clear() {
        tools.clear()
        _toolCount.value = 0
        onToolsChanged?.invoke()
    }
}
