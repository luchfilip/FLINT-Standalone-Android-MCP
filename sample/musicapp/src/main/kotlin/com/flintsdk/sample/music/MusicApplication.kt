package com.flintsdk.sample.music

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.flintsdk.Flint

@HiltAndroidApp
class MusicApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Flint.init(this, adbMode = BuildConfig.DEBUG)
    }
}
