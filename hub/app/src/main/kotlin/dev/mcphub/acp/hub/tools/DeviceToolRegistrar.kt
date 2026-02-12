package dev.mcphub.acp.hub.tools

import dev.mcphub.acp.hub.server.ToolRegistry
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers all device-level MCP tools with the ToolRegistry.
 *
 * Called from HubService during server startup to make device interaction
 * tools (screenshot, tap, swipe, type, etc.) available to MCP clients.
 */
@Singleton
class DeviceToolRegistrar @Inject constructor(
    private val toolRegistry: ToolRegistry
) {
    fun registerAll() {
        toolRegistry.registerTool(DeviceScreenshotTool())
        toolRegistry.registerTool(DeviceTapTool())
        toolRegistry.registerTool(DeviceLongPressTool())
        toolRegistry.registerTool(DeviceSwipeTool())
        toolRegistry.registerTool(DeviceTypeTool())
        toolRegistry.registerTool(DevicePressKeyTool())
        toolRegistry.registerTool(DeviceScrollTool())
        toolRegistry.registerTool(DeviceGetTreeTool())
    }
}
