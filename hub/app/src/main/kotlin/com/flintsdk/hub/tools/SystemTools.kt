package com.flintsdk.hub.tools

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import com.flintsdk.hub.server.HubTool
import com.flintsdk.hub.server.ToolContent
import com.flintsdk.hub.server.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * Tool that returns current battery status information.
 *
 * Uses a sticky broadcast to retrieve battery level, charging status, and plug type
 * without needing to register a persistent receiver.
 */
class SystemBatteryTool(private val context: Context) : HubTool {

    override val name: String = "system.battery"

    override val description: String =
        "Get current battery status including level, charging state, and power source"

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {})
        put("required", buildJsonArray {})
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        return try {
            val batteryIntent = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            if (batteryIntent == null) {
                return ToolResult(
                    content = listOf(ToolContent.TextContent("Unable to read battery status")),
                    isError = true
                )
            }

            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percentage = if (level >= 0 && scale > 0) (level * 100) / scale else -1

            val status = when (batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
                BatteryManager.BATTERY_STATUS_CHARGING -> "charging"
                BatteryManager.BATTERY_STATUS_DISCHARGING -> "discharging"
                BatteryManager.BATTERY_STATUS_FULL -> "full"
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "not_charging"
                else -> "unknown"
            }

            val plugged = when (batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
                BatteryManager.BATTERY_PLUGGED_AC -> "ac"
                BatteryManager.BATTERY_PLUGGED_USB -> "usb"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "wireless"
                else -> "unplugged"
            }

            val temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            val tempCelsius = if (temperature > 0) temperature / 10.0 else null

            val result = buildJsonObject {
                put("level", percentage)
                put("status", status)
                put("plugged", plugged)
                if (tempCelsius != null) {
                    put("temperature_celsius", tempCelsius)
                }
            }

            ToolResult(
                content = listOf(ToolContent.TextContent(result.toString()))
            )
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to get battery status: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * Tool for querying and controlling WiFi state.
 *
 * When called with no parameters, returns current WiFi status including SSID,
 * connection state, and IP address. When called with `enabled` parameter,
 * attempts to toggle WiFi (on API 29+, opens the WiFi settings panel instead
 * since direct programmatic toggling is restricted).
 */
class SystemWifiTool(private val context: Context) : HubTool {

    override val name: String = "system.wifi"

    override val description: String =
        "Get WiFi status or toggle WiFi. Without parameters returns current status. " +
            "With 'enabled' parameter attempts to toggle WiFi state."

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("enabled", buildJsonObject {
                put("type", "boolean")
                put("description", "Set to true to enable WiFi, false to disable. " +
                    "On Android 10+ this opens the WiFi settings panel instead.")
            })
        })
        put("required", buildJsonArray {})
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val enabled = params["enabled"]?.jsonPrimitive?.boolean

        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (enabled != null) {
                return toggleWifi(wifiManager, enabled)
            }

            // Return current WiFi status
            getWifiStatus(wifiManager)
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to get WiFi status: ${e.message}")),
                isError = true
            )
        }
    }

    private fun toggleWifi(wifiManager: WifiManager, enabled: Boolean): ToolResult {
        // On API 29+ (Android 10), apps can't toggle WiFi programmatically
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "On Android 10+, apps cannot toggle WiFi programmatically. " +
                            "Opened the WiFi settings panel for the user to toggle manually."
                    )
                )
            )
        }

        // API 28: can toggle directly
        @Suppress("DEPRECATION")
        wifiManager.isWifiEnabled = enabled
        return ToolResult(
            content = listOf(
                ToolContent.TextContent("WiFi ${if (enabled) "enabled" else "disabled"} successfully")
            )
        )
    }

    private fun getWifiStatus(wifiManager: WifiManager): ToolResult {
        val isEnabled = wifiManager.isWifiEnabled

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val isConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        @Suppress("DEPRECATION")
        val connectionInfo = wifiManager.connectionInfo

        @Suppress("DEPRECATION")
        val ssid = if (isConnected) {
            connectionInfo?.ssid?.removeSurrounding("\"") ?: "unknown"
        } else {
            "not connected"
        }

        @Suppress("DEPRECATION")
        val ipInt = connectionInfo?.ipAddress ?: 0
        val ipAddress = if (isConnected && ipInt != 0) {
            "${ipInt and 0xFF}.${(ipInt shr 8) and 0xFF}.${(ipInt shr 16) and 0xFF}.${(ipInt shr 24) and 0xFF}"
        } else {
            "none"
        }

        val result = buildJsonObject {
            put("enabled", isEnabled)
            put("connected", isConnected)
            put("ssid", ssid)
            put("ip_address", ipAddress)
        }

        return ToolResult(
            content = listOf(ToolContent.TextContent(result.toString()))
        )
    }
}

/**
 * Tool for querying and controlling Bluetooth state.
 *
 * When called with no parameters, returns current Bluetooth status.
 * When called with `enabled` parameter, attempts to toggle Bluetooth
 * (on API 33+ uses an intent since direct toggling is restricted).
 */
class SystemBluetoothTool(private val context: Context) : HubTool {

    override val name: String = "system.bluetooth"

    override val description: String =
        "Get Bluetooth status or toggle Bluetooth. Without parameters returns current status. " +
            "With 'enabled' parameter attempts to toggle Bluetooth state."

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("enabled", buildJsonObject {
                put("type", "boolean")
                put("description", "Set to true to enable Bluetooth, false to disable. " +
                    "On Android 13+ this opens Bluetooth settings instead.")
            })
        })
        put("required", buildJsonArray {})
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val enabled = params["enabled"]?.jsonPrimitive?.boolean

        return try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bluetoothAdapter = bluetoothManager?.adapter

            if (bluetoothAdapter == null) {
                return ToolResult(
                    content = listOf(ToolContent.TextContent("Bluetooth is not available on this device")),
                    isError = true
                )
            }

            if (enabled != null) {
                return toggleBluetooth(bluetoothAdapter, enabled)
            }

            // Return current Bluetooth status
            getBluetoothStatus(bluetoothAdapter)
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to get Bluetooth status: ${e.message}")),
                isError = true
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun toggleBluetooth(adapter: BluetoothAdapter, enabled: Boolean): ToolResult {
        // On API 33+ (Android 13), apps can't toggle Bluetooth directly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            return ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "On Android 13+, apps cannot toggle Bluetooth programmatically. " +
                            "Opened the Bluetooth settings for the user to toggle manually."
                    )
                )
            )
        }

        // API 28-32: can toggle directly (requires BLUETOOTH_ADMIN on older APIs)
        @Suppress("DEPRECATION")
        val success = if (enabled) adapter.enable() else adapter.disable()
        return if (success) {
            ToolResult(
                content = listOf(
                    ToolContent.TextContent("Bluetooth ${if (enabled) "enabling" else "disabling"}...")
                )
            )
        } else {
            ToolResult(
                content = listOf(
                    ToolContent.TextContent("Failed to ${if (enabled) "enable" else "disable"} Bluetooth")
                ),
                isError = true
            )
        }
    }

    @SuppressLint("MissingPermission", "HardwareIds")
    private fun getBluetoothStatus(adapter: BluetoothAdapter): ToolResult {
        val state = when (adapter.state) {
            BluetoothAdapter.STATE_OFF -> "off"
            BluetoothAdapter.STATE_ON -> "on"
            BluetoothAdapter.STATE_TURNING_OFF -> "turning_off"
            BluetoothAdapter.STATE_TURNING_ON -> "turning_on"
            else -> "unknown"
        }

        val result = buildJsonObject {
            put("enabled", adapter.isEnabled)
            put("state", state)
            put("name", adapter.name ?: "unknown")
            put("address", adapter.address ?: "unknown")
        }

        return ToolResult(
            content = listOf(ToolContent.TextContent(result.toString()))
        )
    }
}

/**
 * Tool for querying and setting audio volume levels.
 *
 * Supports the following audio streams: music, ring, notification, alarm, system.
 * When called without `level`, returns current volume levels for all streams.
 * When called with `level` (and optionally `stream`), sets the volume for the specified stream.
 */
class SystemVolumeTool(private val context: Context) : HubTool {

    override val name: String = "system.volume"

    override val description: String =
        "Get or set audio volume levels. Without parameters returns all volume levels. " +
            "With 'level' parameter sets volume for the specified stream."

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("stream", buildJsonObject {
                put("type", "string")
                put("description", "Audio stream to control: music, ring, notification, alarm, system (default: music)")
                put("enum", buildJsonArray {
                    add(JsonPrimitive("music"))
                    add(JsonPrimitive("ring"))
                    add(JsonPrimitive("notification"))
                    add(JsonPrimitive("alarm"))
                    add(JsonPrimitive("system"))
                })
            })
            put("level", buildJsonObject {
                put("type", "integer")
                put("description", "Volume level to set (0 to max for the stream)")
            })
        })
        put("required", buildJsonArray {})
    }

    companion object {
        private val STREAM_MAP = mapOf(
            "music" to AudioManager.STREAM_MUSIC,
            "ring" to AudioManager.STREAM_RING,
            "notification" to AudioManager.STREAM_NOTIFICATION,
            "alarm" to AudioManager.STREAM_ALARM,
            "system" to AudioManager.STREAM_SYSTEM
        )
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val streamName = params["stream"]?.jsonPrimitive?.content ?: "music"
        val level = params["level"]?.jsonPrimitive?.int

        return try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            if (level != null) {
                return setVolume(audioManager, streamName, level)
            }

            // Return current volume levels for all streams
            getVolumeLevels(audioManager)
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Failed to manage volume: ${e.message}")),
                isError = true
            )
        }
    }

    private fun setVolume(audioManager: AudioManager, streamName: String, level: Int): ToolResult {
        val streamType = STREAM_MAP[streamName]
            ?: return ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "Unknown stream: $streamName. Valid streams: ${STREAM_MAP.keys.joinToString(", ")}"
                    )
                ),
                isError = true
            )

        val maxVolume = audioManager.getStreamMaxVolume(streamType)
        val clampedLevel = level.coerceIn(0, maxVolume)

        audioManager.setStreamVolume(streamType, clampedLevel, 0)

        return ToolResult(
            content = listOf(
                ToolContent.TextContent(
                    "Volume for $streamName set to $clampedLevel/$maxVolume"
                )
            )
        )
    }

    private fun getVolumeLevels(audioManager: AudioManager): ToolResult {
        val result = buildJsonObject {
            for ((name, streamType) in STREAM_MAP) {
                put(name, buildJsonObject {
                    put("current", audioManager.getStreamVolume(streamType))
                    put("max", audioManager.getStreamMaxVolume(streamType))
                })
            }
        }

        return ToolResult(
            content = listOf(ToolContent.TextContent(result.toString()))
        )
    }
}
