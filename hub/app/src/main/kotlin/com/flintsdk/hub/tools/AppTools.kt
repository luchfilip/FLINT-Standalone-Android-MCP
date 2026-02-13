package com.flintsdk.hub.tools

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import com.flintsdk.hub.server.HubTool
import com.flintsdk.hub.server.ToolContent
import com.flintsdk.hub.server.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Tool that lists installed applications on the device.
 *
 * By default, only non-system apps are returned. Use the `include_system` parameter
 * to include system apps as well. An optional `filter` parameter allows filtering
 * apps by name (case-insensitive substring match).
 */
class AppsListTool(private val context: Context) : HubTool {

    override val name: String = "apps.list"

    override val description: String =
        "List installed applications on the device with package name, label, and version"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("filter", buildJsonObject {
                put("type", "string")
                put("description", "Filter apps by name (case-insensitive substring match)")
            })
            put("include_system", buildJsonObject {
                put("type", "boolean")
                put("description", "Include system apps in the list (default: false)")
            })
        })
        put("required", buildJsonArray {})
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val filter = params["filter"]?.jsonPrimitive?.content
        val includeSystem = params["include_system"]?.jsonPrimitive?.boolean ?: false

        return try {
            val pm = context.packageManager
            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }

            val result = buildJsonArray {
                for (appInfo in apps) {
                    // Filter out system apps unless requested
                    if (!includeSystem && (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) {
                        continue
                    }

                    val appLabel = pm.getApplicationLabel(appInfo).toString()

                    // Apply name filter if provided
                    if (filter != null && !appLabel.contains(filter, ignoreCase = true)) {
                        continue
                    }

                    val versionName = try {
                        pm.getPackageInfo(appInfo.packageName, 0).versionName ?: "unknown"
                    } catch (e: PackageManager.NameNotFoundException) {
                        "unknown"
                    }

                    add(buildJsonObject {
                        put("package_name", appInfo.packageName)
                        put("label", appLabel)
                        put("version", versionName)
                    })
                }
            }

            ToolResult(
                content = listOf(ToolContent.TextContent(result.toString()))
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to list apps: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Tool that launches an application by its package name.
 *
 * Uses the package manager to resolve the launch intent and starts the activity
 * with FLAG_ACTIVITY_NEW_TASK so it can be launched from a non-Activity context.
 */
class AppsLaunchTool(private val context: Context) : HubTool {

    override val name: String = "apps.launch"

    override val description: String = "Launch an application by its package name"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("package_name", buildJsonObject {
                put("type", "string")
                put("description", "The package name of the app to launch (e.g. com.example.app)")
            })
        })
        put("required", buildJsonArray {
            add(JsonPrimitive("package_name"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val packageName = params["package_name"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: package_name")),
                isError = true
            )

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return ToolResult(
                    content = listOf(
                        ToolContent.TextContent(
                            "No launch intent found for package: $packageName. " +
                                "The app may not be installed or may not have a launchable activity."
                        )
                    ),
                    isError = true
                )

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            ToolResult(
                content = listOf(ToolContent.TextContent("Successfully launched $packageName"))
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to launch $packageName: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Tool that closes (kills background processes of) an application by its package name.
 *
 * Uses ActivityManager.killBackgroundProcesses() which only affects background processes.
 * Foreground apps cannot be force-stopped this way. Requires KILL_BACKGROUND_PROCESSES permission.
 */
class AppsCloseTool(private val context: Context) : HubTool {

    override val name: String = "apps.close"

    override val description: String =
        "Close an application by killing its background processes. " +
            "Note: this only kills background processes, not foreground apps."

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("package_name", buildJsonObject {
                put("type", "string")
                put("description", "The package name of the app to close (e.g. com.example.app)")
            })
        })
        put("required", buildJsonArray {
            add(JsonPrimitive("package_name"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val packageName = params["package_name"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: package_name")),
                isError = true
            )

        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)

            ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "Killed background processes for $packageName. " +
                            "Note: if the app is in the foreground, it will not be affected."
                    )
                )
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to close $packageName: ${e.message}")),
                isError = true
            )
        }
    }
}
