package com.flintsdk.sample.music

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.flintsdk.Flint
import com.flintsdk.sample.music.di.AppNavigatorHolder
import com.flintsdk.sample.music.tools.MusicTools
import com.flintsdk.sample.music.tools.FlintRouter_MusicTools
import javax.inject.Inject

@HiltAndroidApp
class MusicApplication : Application() {

    @Inject
    lateinit var navigatorHolder: AppNavigatorHolder

    override fun onCreate() {
        super.onCreate()
        Flint.init(this)

        val musicTools = MusicTools(navigatorHolder)
        val router = FlintRouter_MusicTools(musicTools)
        Flint.add(router)
    }
}
