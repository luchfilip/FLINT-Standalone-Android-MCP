package com.flintsdk.hub.server

import com.flintsdk.hub.data.HubSettings
import com.flintsdk.hub.data.HubSettingsData
import com.flintsdk.hub.logging.HubLogger
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.bearer
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Embedded Ktor CIO server that serves MCP protocol over HTTP + SSE.
 *
 * Endpoints:
 *   GET  /health   - Health check
 *   GET  /sse      - SSE stream for MCP server-to-client communication
 *   POST /message  - MCP JSON-RPC messages from client
 *
 * Per the MCP SSE transport spec, JSON-RPC responses are sent back to
 * the client via SSE events (not in the HTTP POST response body).
 */
@Singleton
class McpServer @Inject constructor(
    private val mcpProtocol: McpProtocol,
    private val hubSettings: HubSettings,
    private val logger: HubLogger
) {
    companion object {
        private const val TAG = "McpServer"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }

    /** Flow for routing JSON-RPC responses from POST handler to SSE stream. */
    private val sseResponses = MutableSharedFlow<String>(extraBufferCapacity = 64)

    private var server: EmbeddedServer<*, *>? = null

    val isRunning: Boolean
        get() = server != null

    suspend fun start() {
        if (server != null) {
            logger.w(TAG, "Server already running")
            return
        }

        val settings = hubSettings.getSettings()
        val host = if (settings.localhostOnly) "127.0.0.1" else "0.0.0.0"
        val port = settings.port

        logger.i(TAG, "Starting MCP server on $host:$port")

        server = embeddedServer(CIO, port = port, host = host) {
            configureServer(settings)
        }.also {
            it.start(wait = false)
        }

        logger.i(TAG, "MCP server started on $host:$port")
    }

    fun stop() {
        server?.let {
            logger.i(TAG, "Stopping MCP server")
            it.stop(gracePeriodMillis = 1000, timeoutMillis = 3000)
            server = null
            logger.i(TAG, "MCP server stopped")
        }
    }

    private fun io.ktor.server.application.Application.configureServer(settings: HubSettingsData) {
        install(ContentNegotiation) {
            json(this@McpServer.json)
        }

        install(SSE)

        install(StatusPages) {
            status(HttpStatusCode.Unauthorized) { call, _ ->
                call.respondText(
                    text = """{"error":"unauthorized","message":"Invalid or missing bearer token. Provide a valid token in the Authorization header."}""",
                    contentType = io.ktor.http.ContentType.Application.Json,
                    status = HttpStatusCode.Unauthorized
                )
            }
        }

        val useAuth = settings.authToken.isNotBlank()

        if (useAuth) {
            install(Authentication) {
                bearer("mcp-auth") {
                    realm = "FLINT Hub MCP Server"
                    authenticate { tokenCredential ->
                        if (tokenCredential.token == settings.authToken) {
                            io.ktor.server.auth.UserIdPrincipal("mcp-client")
                        } else {
                            null
                        }
                    }
                }
            }
        }

        routing {
            get("/health") {
                call.respond(
                    HttpStatusCode.OK,
                    mapOf(
                        "status" to "ok",
                        "server" to "flint-hub",
                        "tools" to mcpProtocol.let {
                            // Access tool count via the protocol's registry is indirect;
                            // just return a simple health payload.
                            "available"
                        }
                    )
                )
            }

            if (useAuth) {
                authenticate("mcp-auth") {
                    mcpRoutes()
                }
            } else {
                mcpRoutes()
            }
        }
    }

    private fun io.ktor.server.routing.Route.mcpRoutes() {
        sse("/sse") {
            logger.i(TAG, "SSE client connected")

            // MCP SSE transport: send endpoint event so client knows where to POST.
            send(ServerSentEvent(data = "/message", event = "endpoint"))
            logger.d(TAG, "Sent endpoint event: /message")

            try {
                // Collect both JSON-RPC responses and protocol notifications,
                // forwarding them to the client over SSE.
                coroutineScope {
                    launch {
                        sseResponses.collect { data ->
                            logger.d(TAG, "SSE sending response: ${data.take(200)}")
                            send(ServerSentEvent(data = data, event = "message"))
                        }
                    }
                    launch {
                        mcpProtocol.notifications.collect { notification ->
                            val eventData = json.encodeToString(JsonObject.serializer(), notification)
                            logger.d(TAG, "SSE sending notification: ${eventData.take(200)}")
                            send(ServerSentEvent(data = eventData, event = "message"))
                        }
                    }
                }
            } finally {
                logger.i(TAG, "SSE client disconnected")
            }
        }

        post("/message") {
            try {
                val body = call.receiveText()
                logger.d(TAG, "Received message: ${body.take(200)}")

                val message = json.decodeFromString(JsonObject.serializer(), body)
                val response = mcpProtocol.handleMessage(message)

                if (response != null) {
                    val responseText = json.encodeToString(JsonObject.serializer(), response)
                    logger.d(TAG, "Routing response to SSE: ${responseText.take(200)}")
                    sseResponses.emit(responseText)
                }

                // MCP SSE transport: POST returns accepted, response goes via SSE.
                call.respond(HttpStatusCode.Accepted)
            } catch (e: Exception) {
                logger.e(TAG, "Error processing message: ${e.message}")
                call.respond(
                    HttpStatusCode.BadRequest,
                    mapOf("error" to (e.message ?: "Invalid request"))
                )
            }
        }
    }
}
