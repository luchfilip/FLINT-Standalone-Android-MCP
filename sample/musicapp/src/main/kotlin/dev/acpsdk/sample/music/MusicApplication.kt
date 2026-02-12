package dev.acpsdk.sample.music

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.acpsdk.Acp
import dev.acpsdk.sample.music.di.AppNavigatorHolder
import dev.acpsdk.sample.music.tools.MusicTools
import dev.acpsdk.sample.music.tools.AcpRouter_MusicTools
import javax.inject.Inject

@HiltAndroidApp
class MusicApplication : Application() {

    @Inject
    lateinit var navigatorHolder: AppNavigatorHolder

    override fun onCreate() {
        super.onCreate()
        Acp.init(this)

        val musicTools = MusicTools(navigatorHolder)
        val router = AcpRouter_MusicTools(musicTools)
        Acp.add(router)
    }
}
