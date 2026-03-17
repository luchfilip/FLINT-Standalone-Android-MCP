package com.flintsdk.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import com.flintsdk.Flint
import com.flintsdk.model.FlintScreenSnapshot
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

    // --- ADB mode: openFile() returns clean JSON via pipe PFD ---

    private fun isAdbModeEnabled(): Boolean {
        // Flint.adbMode is set in Application.onCreate(), but ContentProvider starts first.
        // Fall back to checking the debuggable flag for cold-start ADB queries.
        if (Flint.adbMode) return true
        val appInfo = context?.applicationInfo ?: return false
        return appInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        if (!isAdbModeEnabled()) return null

        val method = uri.pathSegments.firstOrNull()
            ?: return jsonToPfd("""{"error":"missing method in URI path"}""")

        val result = try {
            when (method) {
                "get_schema" -> handleGetSchemaJson()
                "get_screen" -> handleGetScreenJson()
                "read_screen" -> handleReadScreenJson()
                "call_tool" -> handleCallToolJson(uri)
                "invoke_action" -> handleInvokeActionJson(uri)
                else -> """{"error":"unknown method: $method"}"""
            }
        } catch (e: Exception) {
            """{"error":"${e.message?.replace("\"", "\\\"")}"}"""
        }

        return jsonToPfd(result)
    }

    private fun handleGetSchemaJson(): String {
        return try {
            val schemaClass = Class.forName("com.flintsdk.generated.FlintSchemaHolder")
            val instance = schemaClass.getDeclaredField("INSTANCE").get(null)
            schemaClass.getDeclaredField("JSON").get(instance) as String
        } catch (e: ClassNotFoundException) {
            """{"error":"FlintSchemaHolder not found. Is KSP configured?"}"""
        }
    }

    private fun handleGetScreenJson(): String {
        val bundle = handleGetScreen()
        return bundleToJson(bundle)
    }

    private fun handleReadScreenJson(): String {
        val bundle = handleReadScreen()
        val snapshotJson = bundle.getString("snapshot")
        if (snapshotJson != null) return snapshotJson
        return bundleToJson(bundle)
    }

    private fun handleCallToolJson(uri: Uri): String {
        val toolName = uri.getQueryParameter("_tool")
            ?: return """{"error":"missing _tool parameter"}"""

        val extras = Bundle()
        extras.putString("_tool", toolName)
        for (name in uri.queryParameterNames) {
            if (name != "_tool") {
                val value = uri.getQueryParameter(name) ?: continue
                putParsedValue(extras, name, value)
            }
        }

        val result = handleCallTool(extras)
        return bundleToJson(result)
    }

    private fun handleInvokeActionJson(uri: Uri): String {
        val actionName = uri.getQueryParameter("_action")
            ?: return """{"error":"missing _action parameter"}"""

        val extras = Bundle()
        extras.putString("_action", actionName)
        uri.getQueryParameter("_list_id")?.let { extras.putString("_list_id", it) }
        uri.getQueryParameter("_item_index")?.let {
            extras.putInt("_item_index", it.toInt())
        }

        val result = handleInvokeAction(extras)
        return bundleToJson(result)
    }

    private fun putParsedValue(bundle: Bundle, key: String, value: String) {
        value.toIntOrNull()?.let { bundle.putInt(key, it); return }
        value.toLongOrNull()?.let { bundle.putLong(key, it); return }
        value.toDoubleOrNull()?.let { bundle.putDouble(key, it); return }
        if (value == "true" || value == "false") { bundle.putBoolean(key, value.toBoolean()); return }
        bundle.putString(key, value)
    }

    private fun bundleToJson(bundle: Bundle): String {
        val map = mutableMapOf<String, JsonElement>()
        for (key in bundle.keySet()) {
            @Suppress("DEPRECATION")
            val value = bundle.get(key)
            map[key] = when (value) {
                null -> JsonNull
                is Boolean -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Long -> JsonPrimitive(value)
                is Double -> JsonPrimitive(value)
                is Float -> JsonPrimitive(value)
                is String -> JsonPrimitive(value)
                is ArrayList<*> -> JsonArray(value.map { JsonPrimitive(it?.toString()) })
                else -> JsonPrimitive(value.toString())
            }
        }
        return JsonObject(map).toString()
    }

    private fun jsonToPfd(jsonString: String): ParcelFileDescriptor {
        val pipe = ParcelFileDescriptor.createPipe()
        val readEnd = pipe[0]
        val writeEnd = pipe[1]

        Thread {
            ParcelFileDescriptor.AutoCloseOutputStream(writeEnd).use { stream ->
                stream.write(jsonString.toByteArray(Charsets.UTF_8))
            }
        }.start()

        return readEnd
    }

    // Required ContentProvider methods — unused for Flint
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
