package dev.mcphub.acp.hub.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.coroutines.resume

/**
 * Android AccessibilityService that provides screen reading and device interaction
 * capabilities for the ACP Hub.
 *
 * Uses a singleton bridge pattern so other parts of the app (particularly the MCP
 * tool implementations) can access the running service instance via the companion object.
 *
 * Capabilities:
 *   - Screen reading via accessibility tree traversal
 *   - Gesture dispatch: tap, long press, swipe, scroll
 *   - Text input into focused fields
 *   - Global key press simulation (back, home, recents, etc.)
 */
class HubAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: HubAccessibilityService? = null
            private set

        val isConnected: Boolean get() = instance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // No-op - we use the service API directly, not event monitoring
    }

    override fun onInterrupt() {
        // Required override
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    // --- Public API ---

    /**
     * Returns a JSON representation of the current accessibility tree.
     * Each node includes class name, text, bounds, interaction state, and children.
     */
    fun getTree(includeOffScreen: Boolean = false): JsonObject {
        val rootNode = rootInActiveWindow ?: return buildJsonObject {
            put("error", "No active window")
        }
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        return try {
            val result = nodeToJson(rootNode, includeOffScreen, screenWidth, screenHeight)
            result as? JsonObject ?: buildJsonObject {
                put("error", "Empty accessibility tree")
            }
        } finally {
            rootNode.recycle()
        }
    }

    /**
     * Performs a tap gesture at the given screen coordinates.
     * Uses a 100ms stroke duration to simulate a finger tap.
     */
    suspend fun tap(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        return dispatchGesture(gesture)
    }

    /**
     * Performs a long press gesture at the given screen coordinates.
     * Uses a 1000ms stroke duration to simulate a long press.
     */
    suspend fun longPress(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 1000))
            .build()
        return dispatchGesture(gesture)
    }

    /**
     * Performs a swipe gesture from (startX, startY) to (endX, endY).
     * @param durationMs Duration of the swipe in milliseconds (default 300ms).
     */
    suspend fun swipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300
    ): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        return dispatchGesture(gesture)
    }

    /**
     * Performs a scroll gesture in the given direction (up, down, left, right).
     * Scrolls 40% of the screen dimension from center.
     */
    suspend fun scroll(direction: String): Boolean {
        val displayMetrics = resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        val centerX = width / 2
        val centerY = height / 2
        val scrollDistance = height * 0.4f

        return when (direction.lowercase()) {
            "up" -> swipe(centerX, centerY + scrollDistance / 2, centerX, centerY - scrollDistance / 2)
            "down" -> swipe(centerX, centerY - scrollDistance / 2, centerX, centerY + scrollDistance / 2)
            "left" -> swipe(centerX + scrollDistance / 2, centerY, centerX - scrollDistance / 2, centerY)
            "right" -> swipe(centerX - scrollDistance / 2, centerY, centerX + scrollDistance / 2, centerY)
            else -> false
        }
    }

    /**
     * Types the given text into the currently focused input field.
     * Uses ACTION_SET_TEXT to replace the field content.
     */
    fun typeText(text: String): Boolean {
        val focusedNode = findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return false
        return try {
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    text
                )
            }
            focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        } finally {
            focusedNode.recycle()
        }
    }

    /**
     * Simulates a global key press (back, home, recents, etc.).
     * @param key One of: back, home, recents, notifications, quick_settings, power_dialog
     */
    fun pressKey(key: String): Boolean {
        return when (key.lowercase()) {
            "back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "recents" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "quick_settings" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            "power_dialog" -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            else -> false
        }
    }

    // --- Private helpers ---

    /**
     * Dispatches a gesture and suspends until completion or cancellation.
     * Returns true if the gesture completed successfully, false otherwise.
     */
    private suspend fun dispatchGesture(gesture: GestureDescription): Boolean {
        return suspendCancellableCoroutine { cont ->
            val dispatched = dispatchGesture(
                gesture,
                object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        if (cont.isActive) cont.resume(false)
                    }
                },
                null
            )
            if (!dispatched) {
                cont.resume(false)
            }
        }
    }

    /**
     * Recursively converts an AccessibilityNodeInfo into a JSON tree.
     *
     * Optimizations applied:
     *   - Off-screen pruning: nodes fully outside the viewport are skipped (unless includeOffScreen).
     *   - Empty container pruning: non-information-bearing nodes with no visible children are
     *     dropped; those wrapping a single child are replaced by that child.
     *   - Omit-when-default booleans: clickable/scrollable/editable/focused/selected/checked are
     *     only emitted when true; enabled is only emitted when false.
     */
    private fun nodeToJson(
        node: AccessibilityNodeInfo,
        includeOffScreen: Boolean,
        screenWidth: Int,
        screenHeight: Int
    ): JsonElement? {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        // Off-screen pruning
        if (!includeOffScreen) {
            if (bounds.left >= bounds.right || bounds.top >= bounds.bottom) return null
            if (bounds.right <= 0 || bounds.bottom <= 0) return null
            if (bounds.left >= screenWidth || bounds.top >= screenHeight) return null
        }

        // Recursively process children, collecting non-null results
        val visibleChildren = mutableListOf<JsonElement>()
        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            try {
                val childJson = nodeToJson(child, includeOffScreen, screenWidth, screenHeight)
                if (childJson != null) {
                    visibleChildren.add(childJson)
                }
            } finally {
                child.recycle()
            }
        }

        // Check if this node carries meaningful information
        val hasInfo = node.text != null ||
                node.contentDescription != null ||
                node.isClickable ||
                node.isScrollable ||
                node.isEditable ||
                node.isChecked ||
                node.isFocused ||
                node.isSelected ||
                !node.isEnabled ||
                node.viewIdResourceName != null

        // Empty container pruning
        if (!hasInfo) {
            when (visibleChildren.size) {
                0 -> return null                // drop empty leaf wrapper
                1 -> return visibleChildren[0]  // promote single child through wrapper
                // else: keep node to preserve sibling grouping, fall through
            }
        }

        return buildJsonObject {
            node.className?.let { put("className", it.toString()) }
            node.text?.let { put("text", it.toString()) }
            node.contentDescription?.let { put("contentDescription", it.toString()) }

            // Omit-when-default booleans
            if (node.isClickable) put("clickable", true)
            if (node.isScrollable) put("scrollable", true)
            if (node.isEditable) put("editable", true)
            if (!node.isEnabled) put("enabled", false)
            if (node.isFocused) put("focused", true)
            if (node.isSelected) put("selected", true)
            if (node.isChecked) put("checked", true)

            put("bounds", buildJsonObject {
                put("left", bounds.left)
                put("top", bounds.top)
                put("right", bounds.right)
                put("bottom", bounds.bottom)
            })

            node.viewIdResourceName?.let { put("resourceId", it) }

            if (visibleChildren.isNotEmpty()) {
                put("children", buildJsonArray {
                    visibleChildren.forEach { add(it) }
                })
            }
        }
    }
}
