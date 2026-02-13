package com.flintsdk.hub

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.flintsdk.hub.logging.HubLogger
import javax.inject.Inject

@HiltAndroidApp
class HubApplication : Application() {

    @Inject lateinit var logger: HubLogger

    override fun onCreate() {
        super.onCreate()
        logger.i("HubApplication", "FLINT Hub application started")
    }
}
