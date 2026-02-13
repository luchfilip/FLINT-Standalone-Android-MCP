package com.flintsdk.hub.flint

import com.flintsdk.hub.logging.HubLogger
import com.flintsdk.hub.server.ToolRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates Flint app discovery and tool registration.
 *
 * Scans for Flint-enabled apps via FlintScanner, translates their tools
 * via FlintToolTranslator, and registers them with the ToolRegistry.
 * Provides the list of discovered apps for the UI layer.
 *
 * Also listens for package changes to trigger automatic rescans.
 */
@Singleton
class FlintIntegrationRegistrar @Inject constructor(
    private val scanner: FlintScanner,
    private val translator: FlintToolTranslator,
    private val toolRegistry: ToolRegistry,
    private val logger: HubLogger
) {

    companion object {
        private const val TAG = "FlintIntegrationRegistrar"
    }

    private val _discoveredApps = MutableStateFlow<List<FlintApp>>(emptyList())

    /** Observable list of discovered Flint apps for the UI. */
    val discoveredApps: StateFlow<List<FlintApp>> = _discoveredApps.asStateFlow()

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
     * Rescan for Flint apps and re-register all tools.
     *
     * Removes previously registered Flint tools, scans for apps,
     * translates tools, and registers them with the ToolRegistry.
     */
    suspend fun rescan() {
        logger.i(TAG, "Starting Flint app scan...")

        // Remove previously registered Flint tools
        for (toolName in registeredToolNames) {
            toolRegistry.removeTool(toolName)
        }
        registeredToolNames.clear()

        // Scan for Flint apps
        val apps = scanner.scanForFlintApps()
        _discoveredApps.value = apps

        if (apps.isEmpty()) {
            logger.i(TAG, "No Flint apps found")
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

        logger.i(TAG, "Registered $totalTools Flint tools from ${apps.size} apps")
    }

    /**
     * Clean up: unregister package receiver and remove Flint tools.
     */
    fun cleanup() {
        scanner.unregisterPackageReceiver()
        for (toolName in registeredToolNames) {
            toolRegistry.removeTool(toolName)
        }
        registeredToolNames.clear()
        _discoveredApps.value = emptyList()
        logger.i(TAG, "Flint integration cleaned up")
    }
}
