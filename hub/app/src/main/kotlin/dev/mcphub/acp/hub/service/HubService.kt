package dev.mcphub.acp.hub.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import dev.mcphub.acp.hub.MainActivity
import dev.mcphub.acp.hub.acp.AcpIntegrationRegistrar
import dev.mcphub.acp.hub.logging.HubLogger
import dev.mcphub.acp.hub.server.McpServer
import dev.mcphub.acp.hub.tools.CommunicationToolRegistrar
import dev.mcphub.acp.hub.tools.DeviceToolRegistrar
import dev.mcphub.acp.hub.tools.SystemToolRegistrar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that hosts the embedded Ktor MCP server.
 *
 * Shows a persistent notification while running and manages the
 * server lifecycle. Uses START_STICKY to restart after system kills.
 */
@AndroidEntryPoint
class HubService : Service() {

    companion object {
        private const val TAG = "HubService"
        private const val NOTIFICATION_CHANNEL_ID = "acp_hub_channel"
        private const val NOTIFICATION_ID = 1
        const val ACTION_START = "dev.mcphub.acp.hub.action.START"
        const val ACTION_STOP = "dev.mcphub.acp.hub.action.STOP"
    }

    @Inject lateinit var mcpServer: McpServer
    @Inject lateinit var logger: HubLogger
    @Inject lateinit var deviceToolRegistrar: DeviceToolRegistrar
    @Inject lateinit var communicationToolRegistrar: CommunicationToolRegistrar
    @Inject lateinit var systemToolRegistrar: SystemToolRegistrar
    @Inject lateinit var acpIntegrationRegistrar: AcpIntegrationRegistrar

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        logger.i(TAG, "HubService created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                logger.i(TAG, "Stop requested")
                stopServer()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> {
                logger.i(TAG, "Start requested")
                startForeground(NOTIFICATION_ID, createNotification())
                startServer()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        logger.i(TAG, "HubService destroyed")
        stopServer()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startServer() {
        serviceScope.launch {
            try {
                deviceToolRegistrar.registerAll()
                communicationToolRegistrar.registerAll()
                systemToolRegistrar.registerAll()
                acpIntegrationRegistrar.registerAll()
                logger.i(TAG, "All tools registered (including ACP apps)")
                mcpServer.start()
                logger.i(TAG, "MCP server started successfully")
            } catch (e: Exception) {
                logger.e(TAG, "Failed to start MCP server: ${e.message}")
            }
        }
    }

    private fun stopServer() {
        try {
            mcpServer.stop()
        } catch (e: Exception) {
            logger.e(TAG, "Error stopping MCP server: ${e.message}")
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "ACP Hub Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps the ACP Hub MCP server running"
            setShowBadge(false)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, HubService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("ACP Hub Running")
            .setContentText("MCP server is active")
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pendingIntent)
            .addAction(
                Notification.Action.Builder(
                    null,
                    "Stop",
                    stopIntent
                ).build()
            )
            .setOngoing(true)
            .build()
    }
}
