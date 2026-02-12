package dev.mcphub.acp.hub.acp

import android.content.Context
import android.net.Uri
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mcphub.acp.hub.logging.HubLogger
import dev.mcphub.acp.hub.server.HubTool
import dev.mcphub.acp.hub.server.ToolContent
import dev.mcphub.acp.hub.server.ToolResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Translates ACP app tools into MCP-compatible HubTool implementations.
 *
 * For each discovered ACP app, creates:
 * 1. App-specific tools: {appname}.{toolname} - routes to ContentProvider call_tool
 * 2. Standard tools per app:
 *    - {appname}.read_screen - routes to ContentProvider read_screen
 *    - {appname}.get_screen - routes to ContentProvider get_screen
 *    - {appname}.action - routes to ContentProvider invoke_action
 */
@Singleton
class AcpToolTranslator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: HubLogger
) {

    companion object {
        private const val TAG = "AcpToolTranslator"

        /** Maximum time to poll for screen verification (ms). */
        internal const val SCREEN_VERIFY_TIMEOUT_MS = 2000L

        /** Interval between screen verification polls (ms). */
        internal const val SCREEN_VERIFY_POLL_MS = 100L
    }

    /**
     * Derive a safe MCP tool prefix from an app label.
     * Converts to lowercase, replaces spaces with underscores, strips non-alphanumeric chars.
     */
    fun derivePrefix(appLabel: String): String {
        return appLabel.lowercase()
            .replace(" ", "_")
            .replace(Regex("[^a-z0-9_]"), "")
    }

    /**
     * Translate all tools from a discovered ACP app into HubTool instances.
     *
     * @param app The discovered ACP app.
     * @return List of HubTool implementations ready to register.
     */
    fun translateApp(app: AcpApp): List<HubTool> {
        val prefix = derivePrefix(app.appLabel)
        val tools = mutableListOf<HubTool>()

        // 1. App-specific tools from schema
        for (toolDef in app.schema.tools) {
            tools.add(
                AcpRemoteTool(
                    context = context,
                    app = app,
                    toolDef = toolDef,
                    prefix = prefix,
                    logger = logger
                )
            )
        }

        // 2. Standard tools for every ACP app

        // read_screen
        tools.add(
            AcpReadScreenTool(
                context = context,
                app = app,
                prefix = prefix,
                logger = logger
            )
        )

        // get_screen
        tools.add(
            AcpGetScreenTool(
                context = context,
                app = app,
                prefix = prefix,
                logger = logger
            )
        )

        // action
        tools.add(
            AcpActionTool(
                context = context,
                app = app,
                prefix = prefix,
                logger = logger
            )
        )

        logger.i(TAG, "Translated ${tools.size} tools for ${app.appLabel} (prefix=$prefix)")
        return tools
    }
}

/**
 * Routes an app-specific tool call through the ACP ContentProvider.
 *
 * After execution, if the result contains a _target screen, polls
 * get_screen to verify the app navigated to the expected screen.
 */
class AcpRemoteTool(
    private val context: Context,
    private val app: AcpApp,
    private val toolDef: AcpToolDef,
    private val prefix: String,
    private val logger: HubLogger
) : HubTool {

    companion object {
        private const val TAG = "AcpRemoteTool"
    }

    override val name: String = "$prefix.${toolDef.name}"
    override val description: String = "[${app.appLabel}] ${toolDef.description}"
    override val inputSchema: JsonObject = toolDef.inputSchema

    override suspend fun execute(params: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse("content://${app.authority}")
            val extras = Bundle().apply {
                putString("_tool", toolDef.name)
                for ((key, value) in params) {
                    val str = value.jsonPrimitive.contentOrNull
                    if (str != null) putString(key, str)
                }
            }

            logger.d(TAG, "Calling tool ${toolDef.name} on ${app.authority}")
            val result = context.contentResolver.call(uri, "call_tool", null, extras)

            if (result == null) {
                return@withContext ToolResult(
                    content = listOf(ToolContent.TextContent("Error: ContentProvider returned null")),
                    isError = true
                )
            }

            val error = result.getString("_error")
            if (error != null) {
                return@withContext ToolResult(
                    content = listOf(ToolContent.TextContent("Error: $error")),
                    isError = true
                )
            }

            val target = result.getString("_target")
            val resultText = buildString {
                append("Tool '${toolDef.name}' executed successfully on ${app.appLabel}.")
                // Include any non-internal keys from the result
                for (key in result.keySet()) {
                    if (!key.startsWith("_")) {
                        append("\n$key: ${result.getString(key)}")
                    }
                }
            }

            // Screen verification polling
            if (target != null) {
                val screenResult = verifyScreen(uri, target)
                return@withContext ToolResult(
                    content = listOf(ToolContent.TextContent("$resultText\nTarget screen: $target\n$screenResult"))
                )
            }

            ToolResult(content = listOf(ToolContent.TextContent(resultText)))
        } catch (e: SecurityException) {
            logger.e(TAG, "Permission denied calling ${app.authority}: ${e.message}")
            ToolResult(
                content = listOf(ToolContent.TextContent("Error: Permission denied - ${e.message}")),
                isError = true
            )
        } catch (e: Exception) {
            logger.e(TAG, "Error calling tool ${toolDef.name}: ${e.message}")
            ToolResult(
                content = listOf(ToolContent.TextContent("Error: ${e.message}")),
                isError = true
            )
        }
    }

    /**
     * Poll get_screen to verify the app navigated to the expected target screen.
     */
    private suspend fun verifyScreen(uri: Uri, expectedScreen: String): String {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < AcpToolTranslator.SCREEN_VERIFY_TIMEOUT_MS) {
            try {
                val screenResult = context.contentResolver.call(uri, "get_screen", null, null)
                val currentScreen = screenResult?.getString("screen")
                if (currentScreen == expectedScreen) {
                    return "Screen verified: $currentScreen"
                }
            } catch (e: Exception) {
                // Provider might be temporarily unavailable during navigation
            }
            delay(AcpToolTranslator.SCREEN_VERIFY_POLL_MS)
        }
        return "Screen verification timed out (expected: $expectedScreen)"
    }
}

/**
 * Reads the structured screen snapshot from an ACP app.
 */
class AcpReadScreenTool(
    private val context: Context,
    private val app: AcpApp,
    private val prefix: String,
    private val logger: HubLogger
) : HubTool {

    companion object {
        private const val TAG = "AcpReadScreenTool"
    }

    override val name: String = "$prefix.read_screen"
    override val description: String = "[${app.appLabel}] Read the current screen content as structured data. " +
        "PREFERRED way to understand what's on screen — faster and more precise than device.screenshot."
    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject { })
    }

    override suspend fun execute(params: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse("content://${app.authority}")
            logger.d(TAG, "Reading screen from ${app.authority}")
            val result = context.contentResolver.call(uri, "read_screen", null, null)

            if (result == null) {
                return@withContext ToolResult(
                    content = listOf(ToolContent.TextContent("Error: ContentProvider returned null")),
                    isError = true
                )
            }

            val error = result.getString("_error")
            if (error != null) {
                return@withContext ToolResult(
                    content = listOf(ToolContent.TextContent("Error: $error")),
                    isError = true
                )
            }

            val snapshot = result.getString("snapshot") ?: "No snapshot data"
            ToolResult(content = listOf(ToolContent.TextContent(snapshot)))
        } catch (e: SecurityException) {
            logger.e(TAG, "Permission denied reading screen from ${app.authority}: ${e.message}")
            ToolResult(
                content = listOf(ToolContent.TextContent("Error: Permission denied - ${e.message}")),
                isError = true
            )
        } catch (e: Exception) {
            logger.e(TAG, "Error reading screen from ${app.authority}: ${e.message}")
            ToolResult(
                content = listOf(ToolContent.TextContent("Error: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Gets the current screen name from an ACP app.
 */
class AcpGetScreenTool(
    private val context: Context,
    private val app: AcpApp,
    private val prefix: String,
    private val logger: HubLogger
) : HubTool {

    companion object {
        private const val TAG = "AcpGetScreenTool"
    }

    override val name: String = "$prefix.get_screen"
    override val description: String = "[${app.appLabel}] Get the current screen name"
    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject { })
    }

    override suspend fun execute(params: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse("content://${app.authority}")
            logger.d(TAG, "Getting screen from ${app.authority}")
            val result = context.contentResolver.call(uri, "get_screen", null, null)

            if (result == null) {
                return@withContext ToolResult(
                    content = listOf(ToolContent.TextContent("Error: ContentProvider returned null")),
                    isError = true
                )
            }

            val screen = result.getString("screen") ?: ""
            ToolResult(content = listOf(ToolContent.TextContent("Current screen: $screen")))
        } catch (e: SecurityException) {
            logger.e(TAG, "Permission denied getting screen from ${app.authority}: ${e.message}")
            ToolResult(
                content = listOf(ToolContent.TextContent("Error: Permission denied - ${e.message}")),
                isError = true
            )
        } catch (e: Exception) {
            logger.e(TAG, "Error getting screen from ${app.authority}: ${e.message}")
            ToolResult(
                content = listOf(ToolContent.TextContent("Error: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Invokes a semantic action on an ACP app via its ContentProvider.
 */
class AcpActionTool(
    private val context: Context,
    private val app: AcpApp,
    private val prefix: String,
    private val logger: HubLogger
) : HubTool {

    companion object {
        private const val TAG = "AcpActionTool"
    }

    override val name: String = "$prefix.action"
    override val description: String = "[${app.appLabel}] Invoke a semantic action (tap button, select item, etc.)"
    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("action", buildJsonObject {
                put("type", "string")
                put("description", "The action name to invoke")
            })
            put("list", buildJsonObject {
                put("type", "string")
                put("description", "Optional list ID for list item actions")
            })
            put("index", buildJsonObject {
                put("type", "integer")
                put("description", "Optional item index for list item actions")
            })
        })
        put("required", kotlinx.serialization.json.JsonArray(listOf(JsonPrimitive("action"))))
    }

    override suspend fun execute(params: JsonObject): ToolResult = withContext(Dispatchers.IO) {
        try {
            val actionName = params["action"]?.jsonPrimitive?.contentOrNull
            if (actionName.isNullOrBlank()) {
                return@withContext ToolResult(
                    content = listOf(ToolContent.TextContent("Error: 'action' parameter is required")),
                    isError = true
                )
            }

            val uri = Uri.parse("content://${app.authority}")
            val extras = Bundle().apply {
                putString("_action", actionName)
                val listId = params["list"]?.jsonPrimitive?.contentOrNull
                if (listId != null) putString("_list_id", listId)
                val itemIndex = params["index"]?.jsonPrimitive?.intOrNull
                if (itemIndex != null) putInt("_item_index", itemIndex)
            }

            logger.d(TAG, "Invoking action '$actionName' on ${app.authority}")
            val result = context.contentResolver.call(uri, "invoke_action", null, extras)

            if (result == null) {
                return@withContext ToolResult(
                    content = listOf(ToolContent.TextContent("Error: ContentProvider returned null")),
                    isError = true
                )
            }

            val error = result.getString("_error")
            if (error != null) {
                return@withContext ToolResult(
                    content = listOf(ToolContent.TextContent("Error: $error")),
                    isError = true
                )
            }

            val success = result.getBoolean("success", false)
            if (success) {
                ToolResult(content = listOf(ToolContent.TextContent("Action '$actionName' invoked successfully on ${app.appLabel}")))
            } else {
                ToolResult(
                    content = listOf(ToolContent.TextContent("Action '$actionName' failed on ${app.appLabel}")),
                    isError = true
                )
            }
        } catch (e: SecurityException) {
            logger.e(TAG, "Permission denied invoking action on ${app.authority}: ${e.message}")
            ToolResult(
                content = listOf(ToolContent.TextContent("Error: Permission denied - ${e.message}")),
                isError = true
            )
        } catch (e: Exception) {
            logger.e(TAG, "Error invoking action on ${app.authority}: ${e.message}")
            ToolResult(
                content = listOf(ToolContent.TextContent("Error: ${e.message}")),
                isError = true
            )
        }
    }
}
