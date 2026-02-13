package com.flintsdk.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.flintsdk.Flint
import com.flintsdk.model.FlintScreenSnapshot
import kotlinx.serialization.json.Json
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * ContentProvider that exposes Flint capabilities to the Hub.
 *
 * Supports the following methods via call():
 * - get_schema: Returns the app's Flint schema JSON
 * - get_screen: Returns the current screen name
 * - read_screen: Returns structured snapshot of current screen
 * - call_tool: Invokes a registered Flint tool
 * - invoke_action: Invokes an annotated semantic action
 */
class FlintProvider : ContentProvider() {

    private val json = Json { prettyPrint = true }
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(): Boolean = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        return when (method) {
            "get_schema" -> handleGetSchema()
            "get_screen" -> handleGetScreen()
            "read_screen" -> handleReadScreen()
            "call_tool" -> handleCallTool(extras)
            "invoke_action" -> handleInvokeAction(extras)
            else -> Bundle().apply { putString("_error", "unknown method: $method") }
        }
    }

    private fun handleGetSchema(): Bundle {
        return try {
            val schemaClass = Class.forName("com.flintsdk.generated.FlintSchemaHolder")
            val instance = schemaClass.getDeclaredField("INSTANCE").get(null)
            val schemaJson = schemaClass.getDeclaredField("JSON").get(instance) as String
            Bundle().apply { putString("schema", schemaJson) }
        } catch (e: ClassNotFoundException) {
            Bundle().apply { putString("_error", "FlintSchemaHolder not found. Is KSP configured?") }
        }
    }

    private fun handleGetScreen(): Bundle {
        val screen = Flint.getScreenName()
        return Bundle().apply {
            putString("screen", screen ?: "")
        }
    }

    private fun handleReadScreen(): Bundle {
        val snapshot = FlintTreeWalker.snapshot()
        val snapshotJson = json.encodeToString(FlintScreenSnapshot.serializer(), snapshot)
        return Bundle().apply {
            putString("snapshot", snapshotJson)
        }
    }

    private fun handleCallTool(extras: Bundle?): Bundle {
        val toolName = extras?.getString("_tool") ?: return Bundle().apply {
            putString("_error", "missing _tool parameter")
        }

        val params = extras.toFlintMap().filterKeys { !it.startsWith("_") }

        // Tool handlers may touch UI (e.g. NavController), so dispatch to main thread
        // and block the Binder thread until completion.
        val resultHolder = arrayOfNulls<Map<String, Any?>>(1)
        val latch = CountDownLatch(1)

        mainHandler.post {
            try {
                resultHolder[0] = Flint.routeTool(toolName, params)
            } finally {
                latch.countDown()
            }
        }

        latch.await(10, TimeUnit.SECONDS)
        val result = resultHolder[0]

        return if (result != null) {
            result.toBundle()
        } else {
            Bundle().apply { putString("_error", "unknown tool: $toolName") }
        }
    }

    private fun handleInvokeAction(extras: Bundle?): Bundle {
        val actionName = extras?.getString("_action") ?: return Bundle().apply {
            putString("_error", "missing _action parameter")
        }
        val listId = extras.getString("_list_id")
        val itemIndex = if (extras.containsKey("_item_index")) extras.getInt("_item_index") else null

        // Actions touch UI, so dispatch to main thread
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
        return Bundle().apply {
            putBoolean("success", success)
        }
    }

    // Required ContentProvider methods — unused for Flint
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
