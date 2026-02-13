package com.flintsdk.hub.flint

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import com.flintsdk.hub.logging.HubLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Represents a discovered Flint-enabled application.
 */
data class FlintApp(
    val packageName: String,
    val appLabel: String,
    val authority: String,
    val schema: FlintSchema
)

/**
 * Parsed Flint schema from an app's ContentProvider.
 */
data class FlintSchema(
    val protocol: String,
    val version: String,
    val name: String,
    val tools: List<FlintToolDef>,
    val screens: List<String>
)

/**
 * A single tool definition from an Flint app's schema.
 */
data class FlintToolDef(
    val name: String,
    val description: String,
    val target: String,
    val inputSchema: JsonObject
)

/**
 * Scans installed packages for Flint ContentProviders.
 *
 * For each installed app, checks if a ContentProvider with authority
 * "${packageName}.flint" exists. If found, queries the provider for its
 * Flint schema and parses the tool definitions.
 *
 * Also registers a BroadcastReceiver for package install/remove/replace
 * events to trigger automatic rescans.
 */
@Singleton
class FlintScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: HubLogger
) {

    companion object {
        private const val TAG = "FlintScanner"
    }

    private val json = Json { ignoreUnknownKeys = true }

    var onAppsChanged: (() -> Unit)? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val packageName = intent.data?.schemeSpecificPart
            logger.i(TAG, "Package event: ${intent.action} for $packageName")
            onAppsChanged?.invoke()
        }
    }

    private var receiverRegistered = false

    /**
     * Register the BroadcastReceiver for package change events.
     * Safe to call multiple times; only registers once.
     */
    fun registerPackageReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(packageReceiver, filter)
        }
        receiverRegistered = true
        logger.i(TAG, "Package change receiver registered")
    }

    /**
     * Unregister the BroadcastReceiver.
     */
    fun unregisterPackageReceiver() {
        if (!receiverRegistered) return
        try {
            context.unregisterReceiver(packageReceiver)
        } catch (e: Exception) {
            logger.w(TAG, "Error unregistering receiver: ${e.message}")
        }
        receiverRegistered = false
    }

    /**
     * Scan all installed applications for Flint ContentProviders.
     *
     * For each app, checks if "${packageName}.flint" authority is registered.
     * If found, calls get_schema to retrieve the app's Flint schema.
     *
     * @return List of discovered Flint apps with their schemas.
     */
    suspend fun scanForFlintApps(): List<FlintApp> = withContext(Dispatchers.IO) {
        val discoveredApps = mutableListOf<FlintApp>()
        val packageManager = context.packageManager

        val installedApps = try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            logger.e(TAG, "Failed to get installed applications: ${e.message}")
            return@withContext emptyList()
        }

        logger.d(TAG, "Scanning ${installedApps.size} installed apps for Flint providers")

        for (appInfo in installedApps) {
            val authority = "${appInfo.packageName}.flint"
            try {
                val providerInfo = packageManager.resolveContentProvider(authority, 0)
                if (providerInfo != null) {
                    logger.d(TAG, "Found Flint provider: $authority")
                    val app = queryFlintApp(appInfo.packageName, authority, packageManager)
                    if (app != null) {
                        discoveredApps.add(app)
                        logger.i(TAG, "Discovered Flint app: ${app.appLabel} (${app.packageName}) with ${app.schema.tools.size} tools")
                    }
                }
            } catch (e: Exception) {
                // App might not be running or provider might be unavailable
                logger.d(TAG, "Skipping $authority: ${e.message}")
            }
        }

        logger.i(TAG, "Scan complete. Found ${discoveredApps.size} Flint apps")
        discoveredApps
    }

    /**
     * Query a specific Flint app's ContentProvider for its schema.
     */
    private fun queryFlintApp(
        packageName: String,
        authority: String,
        packageManager: PackageManager
    ): FlintApp? {
        return try {
            val uri = Uri.parse("content://$authority")
            val result = context.contentResolver.call(uri, "get_schema", null, null)

            if (result == null) {
                logger.w(TAG, "get_schema returned null for $authority")
                return null
            }

            val schemaJson = result.getString("schema")
            if (schemaJson.isNullOrBlank()) {
                val error = result.getString("_error")
                logger.w(TAG, "No schema from $authority: ${error ?: "empty response"}")
                return null
            }

            val schema = parseSchema(schemaJson)
            if (schema == null) {
                logger.w(TAG, "Failed to parse schema from $authority")
                return null
            }

            val appLabel = try {
                val appInfo = packageManager.getApplicationInfo(packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: Exception) {
                packageName.substringAfterLast('.')
            }

            FlintApp(
                packageName = packageName,
                appLabel = appLabel,
                authority = authority,
                schema = schema
            )
        } catch (e: SecurityException) {
            logger.w(TAG, "Permission denied for $authority: ${e.message}")
            null
        } catch (e: Exception) {
            logger.w(TAG, "Error querying $authority: ${e.message}")
            null
        }
    }

    /**
     * Parse the Flint schema JSON string into an FlintSchema data class.
     */
    private fun parseSchema(schemaJson: String): FlintSchema? {
        return try {
            val root = json.parseToJsonElement(schemaJson).jsonObject

            val protocol = root["protocol"]?.jsonPrimitive?.content ?: "flint"
            val version = root["version"]?.jsonPrimitive?.content ?: "1.0"
            val name = root["name"]?.jsonPrimitive?.content ?: ""

            val toolsArray = root["tools"]?.jsonArray ?: JsonArray(emptyList())
            val tools = toolsArray.mapNotNull { element ->
                try {
                    val toolObj = element.jsonObject
                    FlintToolDef(
                        name = toolObj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                        description = toolObj["description"]?.jsonPrimitive?.content ?: "",
                        target = toolObj["target"]?.jsonPrimitive?.content ?: "",
                        inputSchema = toolObj["inputSchema"]?.jsonObject ?: JsonObject(emptyMap())
                    )
                } catch (e: Exception) {
                    logger.w(TAG, "Failed to parse tool definition: ${e.message}")
                    null
                }
            }

            val screensArray = root["screens"]?.jsonArray ?: JsonArray(emptyList())
            val screens = screensArray.mapNotNull { element ->
                try {
                    element.jsonPrimitive.content
                } catch (e: Exception) {
                    null
                }
            }

            FlintSchema(
                protocol = protocol,
                version = version,
                name = name,
                tools = tools,
                screens = screens
            )
        } catch (e: Exception) {
            logger.e(TAG, "Schema parse error: ${e.message}")
            null
        }
    }
}
