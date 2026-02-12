package dev.mcphub.acp.hub.tools

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.mcphub.acp.hub.server.ToolRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers all system-level tools (app management, system info, clipboard)
 * with the ToolRegistry.
 *
 * Should be called once during application startup to make these tools
 * available via the MCP server.
 */
@Singleton
class SystemToolRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val toolRegistry: ToolRegistry
) {
    fun registerAll() {
        // App management tools
        toolRegistry.registerTool(AppsListTool(context))
        toolRegistry.registerTool(AppsLaunchTool(context))
        toolRegistry.registerTool(AppsCloseTool(context))

        // System info and control tools
        toolRegistry.registerTool(SystemBatteryTool(context))
        toolRegistry.registerTool(SystemWifiTool(context))
        toolRegistry.registerTool(SystemBluetoothTool(context))
        toolRegistry.registerTool(SystemVolumeTool(context))

        // Clipboard tools
        toolRegistry.registerTool(ClipboardGetTool(context))
        toolRegistry.registerTool(ClipboardSetTool(context))
    }
}
