package dev.mcphub.acp.hub

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.mcphub.acp.hub.logging.HubLogger
import javax.inject.Inject

@HiltAndroidApp
class HubApplication : Application() {

    @Inject lateinit var logger: HubLogger

    override fun onCreate() {
        super.onCreate()
        logger.i("HubApplication", "ACP Hub application started")
    }
}
