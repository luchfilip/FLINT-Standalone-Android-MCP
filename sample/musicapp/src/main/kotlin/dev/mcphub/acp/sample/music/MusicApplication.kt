package dev.mcphub.acp.sample.music

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.mcphub.acp.Acp
import dev.mcphub.acp.sample.music.di.AppNavigatorHolder
import dev.mcphub.acp.sample.music.tools.MusicTools
import dev.mcphub.acp.sample.music.tools.AcpRouter_MusicTools
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
