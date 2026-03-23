package com.flintsdk.server

import android.util.Log
import com.flintsdk.provider.FlintRequestHandler
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Embedded Ktor HTTP server that exposes Flint endpoints over WiFi.
 * Serves the same data as FlintProvider but via HTTP instead of ContentProvider/ADB.
 */
internal object FlintNetworkServer {

    private const val TAG = "FlintNetworkServer"
    private var server: EmbeddedServer<*, *>? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(port: Int = 6099) {
        if (server != null) {
            Log.w(TAG, "Server already running")
            return
        }

        scope.launch {
            try {
                val engine = embeddedServer(CIO, host = "0.0.0.0", port = port) {
                    install(ContentNegotiation) {
                        json()
                    }

                    routing {
                        get("/get_schema") {
                            val schema = FlintRequestHandler.getSchema()
                            call.respondText(schema, ContentType.Application.Json)
                        }

                        get("/get_screen") {
                            val screen = FlintRequestHandler.getScreen()
                            call.respondText(screen, ContentType.Application.Json)
                        }

                        get("/read_screen") {
                            val text = FlintRequestHandler.readScreen()
                            call.respondText(text, ContentType.Text.Plain)
                        }

                        get("/call_tool") {
                            val toolName = call.request.queryParameters["_tool"]
                            if (toolName == null) {
                                call.respondText(
                                    """{"error":"missing _tool parameter"}""",
                                    ContentType.Application.Json,
                                    HttpStatusCode.BadRequest
                                )
                                return@get
                            }

                            val params = mutableMapOf<String, Any?>()
                            for ((key, values) in call.request.queryParameters.entries()) {
                                if (key != "_tool" && values.isNotEmpty()) {
                                    params[key] = FlintRequestHandler.parseValue(values.first())
                                }
                            }

                            val result = FlintRequestHandler.callTool(toolName, params)
                            call.respondText(result, ContentType.Text.Plain)
                        }

                        get("/invoke_action") {
                            val actionName = call.request.queryParameters["_action"]
                            if (actionName == null) {
                                call.respondText(
                                    """{"error":"missing _action parameter"}""",
                                    ContentType.Application.Json,
                                    HttpStatusCode.BadRequest
                                )
                                return@get
                            }

                            val listId = call.request.queryParameters["_list_id"]
                            val itemIndex = call.request.queryParameters["_item_index"]?.toIntOrNull()

                            val result = FlintRequestHandler.invokeAction(actionName, listId, itemIndex)
                            call.respondText(result, ContentType.Application.Json)
                        }
                    }
                }

                server = engine
                Log.i(TAG, "Starting Flint network server on port $port")
                engine.start(wait = false)
                Log.i(TAG, "Flint network server started on port $port")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Flint network server", e)
            }
        }
    }

    fun stop() {
        server?.let {
            Log.i(TAG, "Stopping Flint network server")
            it.stop(1000, 2000)
            server = null
            Log.i(TAG, "Flint network server stopped")
        }
    }
}
