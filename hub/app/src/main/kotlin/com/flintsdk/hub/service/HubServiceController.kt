package com.flintsdk.hub.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface HubServiceController {
    fun startServer()
    fun stopServer()
}

@Singleton
class HubServiceControllerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HubServiceController {

    override fun startServer() {
        val intent = Intent(context, HubService::class.java).apply {
            action = HubService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    override fun stopServer() {
        val intent = Intent(context, HubService::class.java).apply {
            action = HubService.ACTION_STOP
        }
        context.startService(intent)
    }
}
