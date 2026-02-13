package com.flintsdk.hub.tools

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.util.Base64
import com.flintsdk.hub.accessibility.HubAccessibilityService
import com.flintsdk.hub.server.HubTool
import com.flintsdk.hub.server.ToolContent
import com.flintsdk.hub.server.ToolResult
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * MCP tool that captures the current screen as a base64-encoded PNG image.
 *
 * Uses AccessibilityService.takeScreenshot() which is available on API 30+ (Android 11).
 * Returns an error with guidance for API 28-29 where MediaProjection would be needed.
 */
class DeviceScreenshotTool : HubTool {

    override val name = "device.screenshot"

    override val description =
        "Capture the current screen as a PNG image. " +
        "SLOW — prefer device.get_tree for reading screen content, finding elements, and navigation. " +
        "Use screenshots only when you need visual verification (images, colors, layout appearance)."

    override val inputSchema = buildJsonObject {
        put("type", "object")
        put("properties", buildJsonObject { })
    }

    private val executor: Executor = Executors.newSingleThreadExecutor()

    override suspend fun execute(params: JsonObject): ToolResult {
        val service = HubAccessibilityService.instance
            ?: return ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "AccessibilityService not connected. Please enable it in Settings > Accessibility."
                    )
                ),
                isError = true
            )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return ToolResult(
                content = listOf(
                    ToolContent.TextContent(
                        "Screenshot requires API 30+ (Android 11). " +
                            "Current device is API ${Build.VERSION.SDK_INT}. " +
                            "MediaProjection fallback not yet implemented."
                    )
                ),
                isError = true
            )
        }

        return try {
            val bitmap = takeScreenshot(service)
            if (bitmap != null) {
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                bitmap.recycle()
                val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                ToolResult(
                    content = listOf(
                        ToolContent.ImageContent(data = base64, mimeType = "image/png")
                    )
                )
            } else {
                ToolResult(
                    content = listOf(ToolContent.TextContent("Screenshot failed - null result")),
                    isError = true
                )
            }
        } catch (e: Exception) {
            ToolResult(
                content = listOf(ToolContent.TextContent("Screenshot failed: ${e.message}")),
                isError = true
            )
        }
    }

    @Suppress("NewApi")
    private suspend fun takeScreenshot(service: HubAccessibilityService): Bitmap? {
        return suspendCancellableCoroutine { cont ->
            service.takeScreenshot(
                android.view.Display.DEFAULT_DISPLAY,
                executor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        val hwBitmap = Bitmap.wrapHardwareBuffer(
                            result.hardwareBuffer,
                            result.colorSpace
                        )
                        result.hardwareBuffer.close()
                        // Convert hardware bitmap to software bitmap for PNG compression
                        val softBitmap = hwBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        hwBitmap?.recycle()
                        if (cont.isActive) cont.resume(softBitmap)
                    }

                    override fun onFailure(errorCode: Int) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
            )
        }
    }
}
