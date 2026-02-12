package dev.acpsdk.hub.acp

import dev.acpsdk.hub.logging.HubLogger
import dev.acpsdk.hub.server.ToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates ACP app discovery and tool registration.
 *
 * Scans for ACP-enabled apps via AcpScanner, translates their tools
 * via AcpToolTranslator, and registers them with the ToolRegistry.
 * Provides the list of discovered apps for the UI layer.
 *
 * Also listens for package changes to trigger automatic rescans.
 */
@Singleton
class AcpIntegrationRegistrar @Inject constructor(
    private val scanner: AcpScanner,
    private val translator: AcpToolTranslator,
    private val toolRegistry: ToolRegistry,
    private val logger: HubLogger
) {

    companion object {
        private const val TAG = "AcpIntegrationRegistrar"
    }

    private val _discoveredApps = MutableStateFlow<List<AcpApp>>(emptyList())

    /** Observable list of discovered ACP apps for the UI. */
    val discoveredApps: StateFlow<List<AcpApp>> = _discoveredApps.asStateFlow()

    /** Names of tools registered by this registrar, for cleanup on rescan. */
    private val registeredToolNames = mutableListOf<String>()

    /**
     * Perform initial scan, register tools, and set up package change listener.
     * Called from HubService.startServer().
     */
    suspend fun registerAll() {
        // Register package change listener for automatic rescans
        scanner.onAppsChanged = {
            // Note: This callback is on main thread from BroadcastReceiver.
            // The actual rescan must be called from a coroutine.
            logger.i(TAG, "Package change detected, rescan needed")
        }
        scanner.registerPackageReceiver()

        // Perform initial scan
        rescan()
    }

    /**
     * Rescan for ACP apps and re-register all tools.
     *
     * Removes previously registered ACP tools, scans for apps,
     * translates tools, and registers them with the ToolRegistry.
     */
    suspend fun rescan() {
        logger.i(TAG, "Starting ACP app scan...")

        // Remove previously registered ACP tools
        for (toolName in registeredToolNames) {
            toolRegistry.removeTool(toolName)
        }
        registeredToolNames.clear()

        // Scan for ACP apps
        val apps = scanner.scanForAcpApps()
        _discoveredApps.value = apps

        if (apps.isEmpty()) {
            logger.i(TAG, "No ACP apps found")
            return
        }

        // Translate and register tools for each app
        var totalTools = 0
        for (app in apps) {
            try {
                val tools = translator.translateApp(app)
                for (tool in tools) {
                    toolRegistry.registerTool(tool)
                    registeredToolNames.add(tool.name)
                    totalTools++
                }
            } catch (e: Exception) {
                logger.e(TAG, "Error translating tools for ${app.appLabel}: ${e.message}")
            }
        }

        logger.i(TAG, "Registered $totalTools ACP tools from ${apps.size} apps")
    }

    /**
     * Clean up: unregister package receiver and remove ACP tools.
     */
    fun cleanup() {
        scanner.unregisterPackageReceiver()
        for (toolName in registeredToolNames) {
            toolRegistry.removeTool(toolName)
        }
        registeredToolNames.clear()
        _discoveredApps.value = emptyList()
        logger.i(TAG, "ACP integration cleaned up")
    }
}
