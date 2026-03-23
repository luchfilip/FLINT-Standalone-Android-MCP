package com.flintsdk.provider

import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import com.flintsdk.Flint
import kotlinx.serialization.json.Json
import com.flintsdk.model.FlintScreenSnapshot
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Shared request handling logic used by both FlintProvider (ADB/ContentProvider)
 * and FlintNetworkServer (WiFi/Ktor).
 *
 * All tool/action calls dispatch to the main thread and wait for Compose to settle.
 */
internal object FlintRequestHandler {

    private val json = Json { prettyPrint = true }
    private val mainHandler = Handler(Looper.getMainLooper())

    fun getSchema(): String {
        return Flint.liveSchema()
    }

    fun getScreen(): String {
        val screen = Flint.getScreenName() ?: ""
        return """{"screen":"$screen"}"""
    }

    fun readScreen(): String {
        val screen = Flint.getScreenName() ?: "unknown"
        val toolNames = Flint.registeredHandlers()
            .flatMap { it.describeTools() }
            .map { it.name }
        val snapshot = FlintTreeWalker.snapshot()
        return FlintTextRenderer.render(screen, toolNames, snapshot)
    }

    fun readScreenSnapshot(): FlintScreenSnapshot {
        return FlintTreeWalker.snapshot()
    }

    fun readScreenSnapshotJson(): String {
        val snapshot = FlintTreeWalker.snapshot()
        return json.encodeToString(FlintScreenSnapshot.serializer(), snapshot)
    }

    /**
     * Calls a tool on the main thread, waits for Compose to settle, then reads updated screen.
     * Returns the text-rendered screen state after the tool call.
     */
    fun callTool(toolName: String, params: Map<String, Any?>): String {
        // Execute tool on main thread
        val resultHolder = arrayOfNulls<Map<String, Any?>>(1)
        val toolLatch = CountDownLatch(1)

        mainHandler.post {
            try {
                resultHolder[0] = Flint.routeTool(toolName, params)
            } finally {
                toolLatch.countDown()
            }
        }

        toolLatch.await(10, TimeUnit.SECONDS)
        val result = resultHolder[0]
            ?: return """{"error":"unknown tool: $toolName"}"""

        // Wait one frame for Compose to settle after navigation
        val frameLatch = CountDownLatch(1)
        mainHandler.post {
            Choreographer.getInstance().postFrameCallback { frameLatch.countDown() }
        }
        frameLatch.await(5, TimeUnit.SECONDS)

        // Read updated screen state on main thread
        val screenHolder = arrayOfNulls<String>(1)
        val readLatch = CountDownLatch(1)
        mainHandler.post {
            try {
                val screen = Flint.getScreenName() ?: "unknown"
                val toolNames = Flint.registeredHandlers()
                    .flatMap { it.describeTools() }
                    .map { it.name }
                val snapshot = FlintTreeWalker.snapshot()
                screenHolder[0] = FlintTextRenderer.render(screen, toolNames, snapshot)
            } finally {
                readLatch.countDown()
            }
        }
        readLatch.await(10, TimeUnit.SECONDS)

        return screenHolder[0] ?: """{"error":"failed to read screen state"}"""
    }

    /**
     * Invokes a semantic action on the main thread.
     * Returns JSON with success boolean.
     */
    fun invokeAction(actionName: String, listId: String?, itemIndex: Int?): String {
        var success = false
        val latch = CountDownLatch(1)

        mainHandler.post {
            try {
                success = FlintActionInvoker.invoke(actionName, listId, itemIndex)
            } finally {
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        return """{"success":$success}"""
    }

    /**
     * Parses a string value into a typed value.
     * Tries Int, Long, Double, Boolean in order, falls back to String.
     */
    fun parseValue(value: String): Any {
        value.toIntOrNull()?.let { return it }
        value.toLongOrNull()?.let { return it }
        value.toDoubleOrNull()?.let { return it }
        if (value == "true" || value == "false") return value.toBoolean()
        return value
    }
}
