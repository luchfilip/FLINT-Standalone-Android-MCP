package dev.acpsdk.hub.tools

import dev.acpsdk.hub.accessibility.HubAccessibilityService
import dev.acpsdk.hub.server.HubTool
import dev.acpsdk.hub.server.ToolContent
import dev.acpsdk.hub.server.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * Helper that returns an error ToolResult when the AccessibilityService is not connected.
 */
private fun serviceNotConnectedError(): ToolResult = ToolResult(
    content = listOf(
        ToolContent.TextContent(
            "AccessibilityService not connected. Please enable it in Settings > Accessibility."
        )
    ),
    isError = true
)

/**
 * MCP tool that performs a tap gesture at the given screen coordinates.
 */
class DeviceTapTool : HubTool {

    override val name = "device.tap"

    override val description = "Perform a tap at the given screen coordinates"

    override val inputSchema = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("x", buildJsonObject {
                put("type", "number")
                put("description", "X coordinate in screen pixels")
            })
            put("y", buildJsonObject {
                put("type", "number")
                put("description", "Y coordinate in screen pixels")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("x"))
            add(kotlinx.serialization.json.JsonPrimitive("y"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val service = HubAccessibilityService.instance ?: return serviceNotConnectedError()
        val x = params["x"]?.jsonPrimitive?.float
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: x")),
                isError = true
            )
        val y = params["y"]?.jsonPrimitive?.float
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: y")),
                isError = true
            )

        val success = service.tap(x, y)
        return ToolResult(
            content = listOf(
                ToolContent.TextContent(if (success) "Tap at ($x, $y) succeeded" else "Tap at ($x, $y) failed")
            ),
            isError = !success
        )
    }
}

/**
 * MCP tool that performs a long press gesture at the given screen coordinates.
 */
class DeviceLongPressTool : HubTool {

    override val name = "device.long_press"

    override val description = "Perform a long press at the given screen coordinates"

    override val inputSchema = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("x", buildJsonObject {
                put("type", "number")
                put("description", "X coordinate in screen pixels")
            })
            put("y", buildJsonObject {
                put("type", "number")
                put("description", "Y coordinate in screen pixels")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("x"))
            add(kotlinx.serialization.json.JsonPrimitive("y"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val service = HubAccessibilityService.instance ?: return serviceNotConnectedError()
        val x = params["x"]?.jsonPrimitive?.float
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: x")),
                isError = true
            )
        val y = params["y"]?.jsonPrimitive?.float
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: y")),
                isError = true
            )

        val success = service.longPress(x, y)
        return ToolResult(
            content = listOf(
                ToolContent.TextContent(
                    if (success) "Long press at ($x, $y) succeeded" else "Long press at ($x, $y) failed"
                )
            ),
            isError = !success
        )
    }
}

/**
 * MCP tool that performs a swipe gesture between two screen coordinates.
 */
class DeviceSwipeTool : HubTool {

    override val name = "device.swipe"

    override val description = "Perform a swipe gesture from one point to another"

    override val inputSchema = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("start_x", buildJsonObject {
                put("type", "number")
                put("description", "Start X coordinate in screen pixels")
            })
            put("start_y", buildJsonObject {
                put("type", "number")
                put("description", "Start Y coordinate in screen pixels")
            })
            put("end_x", buildJsonObject {
                put("type", "number")
                put("description", "End X coordinate in screen pixels")
            })
            put("end_y", buildJsonObject {
                put("type", "number")
                put("description", "End Y coordinate in screen pixels")
            })
            put("duration_ms", buildJsonObject {
                put("type", "number")
                put("description", "Duration of the swipe in milliseconds (default 300)")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("start_x"))
            add(kotlinx.serialization.json.JsonPrimitive("start_y"))
            add(kotlinx.serialization.json.JsonPrimitive("end_x"))
            add(kotlinx.serialization.json.JsonPrimitive("end_y"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val service = HubAccessibilityService.instance ?: return serviceNotConnectedError()
        val startX = params["start_x"]?.jsonPrimitive?.float
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: start_x")),
                isError = true
            )
        val startY = params["start_y"]?.jsonPrimitive?.float
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: start_y")),
                isError = true
            )
        val endX = params["end_x"]?.jsonPrimitive?.float
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: end_x")),
                isError = true
            )
        val endY = params["end_y"]?.jsonPrimitive?.float
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: end_y")),
                isError = true
            )
        val durationMs = params["duration_ms"]?.jsonPrimitive?.long ?: 300L

        val success = service.swipe(startX, startY, endX, endY, durationMs)
        return ToolResult(
            content = listOf(
                ToolContent.TextContent(
                    if (success) "Swipe from ($startX, $startY) to ($endX, $endY) succeeded"
                    else "Swipe from ($startX, $startY) to ($endX, $endY) failed"
                )
            ),
            isError = !success
        )
    }
}

/**
 * MCP tool that types text into the currently focused input field.
 */
class DeviceTypeTool : HubTool {

    override val name = "device.type"

    override val description = "Type text into the currently focused input field"

    override val inputSchema = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("text", buildJsonObject {
                put("type", "string")
                put("description", "The text to type into the focused field")
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("text"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val service = HubAccessibilityService.instance ?: return serviceNotConnectedError()
        val text = params["text"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: text")),
                isError = true
            )

        val success = service.typeText(text)
        return ToolResult(
            content = listOf(
                ToolContent.TextContent(
                    if (success) "Text typed successfully" else "Failed to type text (no focused input field?)"
                )
            ),
            isError = !success
        )
    }
}

/**
 * MCP tool that simulates a global key press (back, home, recents, etc.).
 */
class DevicePressKeyTool : HubTool {

    override val name = "device.press_key"

    override val description =
        "Simulate a global key press (back, home, recents, notifications, quick_settings, power_dialog)"

    override val inputSchema = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("key", buildJsonObject {
                put("type", "string")
                put("description", "Key to press: back, home, recents, notifications, quick_settings, power_dialog")
                put("enum", buildJsonArray {
                    add(kotlinx.serialization.json.JsonPrimitive("back"))
                    add(kotlinx.serialization.json.JsonPrimitive("home"))
                    add(kotlinx.serialization.json.JsonPrimitive("recents"))
                    add(kotlinx.serialization.json.JsonPrimitive("notifications"))
                    add(kotlinx.serialization.json.JsonPrimitive("quick_settings"))
                    add(kotlinx.serialization.json.JsonPrimitive("power_dialog"))
                })
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("key"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val service = HubAccessibilityService.instance ?: return serviceNotConnectedError()
        val key = params["key"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: key")),
                isError = true
            )

        val success = service.pressKey(key)
        return ToolResult(
            content = listOf(
                ToolContent.TextContent(
                    if (success) "Key '$key' pressed successfully" else "Key '$key' press failed (unknown key?)"
                )
            ),
            isError = !success
        )
    }
}

/**
 * MCP tool that performs a scroll gesture in a given direction.
 */
class DeviceScrollTool : HubTool {

    override val name = "device.scroll"

    override val description = "Scroll the screen in the given direction (up, down, left, right)"

    override val inputSchema = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("direction", buildJsonObject {
                put("type", "string")
                put("description", "Scroll direction: up, down, left, right")
                put("enum", buildJsonArray {
                    add(kotlinx.serialization.json.JsonPrimitive("up"))
                    add(kotlinx.serialization.json.JsonPrimitive("down"))
                    add(kotlinx.serialization.json.JsonPrimitive("left"))
                    add(kotlinx.serialization.json.JsonPrimitive("right"))
                })
            })
        })
        put("required", buildJsonArray {
            add(kotlinx.serialization.json.JsonPrimitive("direction"))
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val service = HubAccessibilityService.instance ?: return serviceNotConnectedError()
        val direction = params["direction"]?.jsonPrimitive?.content
            ?: return ToolResult(
                content = listOf(ToolContent.TextContent("Missing required parameter: direction")),
                isError = true
            )

        val success = service.scroll(direction)
        return ToolResult(
            content = listOf(
                ToolContent.TextContent(
                    if (success) "Scroll '$direction' succeeded" else "Scroll '$direction' failed"
                )
            ),
            isError = !success
        )
    }
}

/**
 * MCP tool that returns the current accessibility tree as JSON.
 * Off-screen nodes are excluded by default for compact output.
 */
class DeviceGetTreeTool : HubTool {

    override val name = "device.get_tree"

    override val description =
        "Get the current screen's accessibility tree as a JSON structure with UI elements, their properties, and bounds. " +
        "PREFERRED over device.screenshot for understanding screen content — faster and structured. " +
        "Use this first for navigation, finding elements, and reading text. " +
        "Only use device.screenshot when you need visual verification (images, colors, layout). " +
        "Off-screen nodes are excluded by default for compact output. " +
        "Set includeOffScreen to true to get the full tree including off-screen pages (useful for navigation planning)."

    override val inputSchema = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject {
            put("includeOffScreen", buildJsonObject {
                put("type", "boolean")
                put("description", "Include off-screen nodes in the tree (default false). Set to true to see elements beyond the visible viewport for navigation planning.")
            })
        })
    }

    override suspend fun execute(params: JsonObject): ToolResult {
        val service = HubAccessibilityService.instance ?: return serviceNotConnectedError()
        val includeOffScreen = try {
            params["includeOffScreen"]?.jsonPrimitive?.boolean ?: false
        } catch (_: Exception) {
            false
        }

        val tree = service.getTree(includeOffScreen)
        val hasError = tree.containsKey("error")

        return ToolResult(
            content = listOf(ToolContent.TextContent(tree.toString())),
            isError = hasError
        )
    }
}
