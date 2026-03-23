package com.flintsdk

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.flintsdk.annotations.FlintToolHandler
import com.flintsdk.server.FlintNetworkServer
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Flint SDK singleton entry point.
 * Manages tool handlers, screen tracking, and tool routing.
 */
object Flint {
    lateinit var context: Context
        private set

    var adbMode: Boolean = false
        private set

    var networkMode: Boolean = false
        private set

    var networkPort: Int = 6099
        private set

    internal var currentScreen: String? = null
        private set

    private val handlers = CopyOnWriteArrayList<FlintToolHandler>()

    fun init(
        context: Context,
        adbMode: Boolean = false,
        networkMode: Boolean = false,
        networkPort: Int = 6099
    ) {
        this.context = context.applicationContext
        this.adbMode = adbMode
        this.networkMode = networkMode
        this.networkPort = networkPort
        if (networkMode) {
            FlintNetworkServer.start(networkPort)
        }
    }

    fun add(handler: FlintToolHandler) {
        handlers.add(handler)
    }

    fun remove(handler: FlintToolHandler) {
        handlers.remove(handler)
    }

    @Composable
    fun screen(name: String) {
        DisposableEffect(name) {
            currentScreen = name
            onDispose {
                if (currentScreen == name) currentScreen = null
            }
        }
    }

    internal fun routeTool(name: String, params: Map<String, Any?>): Map<String, Any?>? {
        for (handler in handlers) {
            val result = handler.onToolCall(name, params)
            if (result != null) return result
        }
        return null
    }

    @Composable
    fun tools(block: FlintToolsScope.() -> Unit) {
        val handler = remember(block) {
            FlintToolsScope().apply(block).buildHandler()
        }
        DisposableEffect(handler) {
            add(handler)
            onDispose { remove(handler) }
        }
    }

    internal fun registeredHandlers(): List<FlintToolHandler> = handlers.toList()

    internal fun liveSchema(): String {
        val allTools = handlers.flatMap { it.describeTools() }
        val json = buildJsonObject {
            put("protocol", "flint")
            put("version", "2.0")
            put("tools", buildJsonArray {
                for (tool in allTools) {
                    add(buildJsonObject {
                        put("name", tool.name)
                        put("description", tool.description)
                        put("inputSchema", buildJsonObject {
                            put("type", "object")
                            put("properties", buildJsonObject {
                                for (param in tool.params) {
                                    put(param.name, buildJsonObject {
                                        put("type", param.type)
                                        put("description", param.description)
                                    })
                                }
                            })
                            put("required", buildJsonArray {
                                for (param in tool.params.filter { it.required }) {
                                    add(kotlinx.serialization.json.JsonPrimitive(param.name))
                                }
                            })
                        })
                    })
                }
            })
        }
        return kotlinx.serialization.json.Json { prettyPrint = true }
            .encodeToString(kotlinx.serialization.json.JsonObject.serializer(), json)
    }

    internal fun getScreenName(): String? = currentScreen
}
