package com.flintsdk.sample.music.di

import androidx.navigation.NavController
import com.flintsdk.sample.music.nav.Home
import com.flintsdk.sample.music.nav.Playlist
import com.flintsdk.sample.music.nav.Search
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppNavigatorHolder(): AppNavigatorHolder {
        return AppNavigatorHolder()
    }
}

class AppNavigatorHolder {
    private var navController: NavController? = null

    fun set(navController: NavController) {
        this.navController = navController
    }

    fun navigateToSearch(query: String) {
        navController?.navigate(Search(query = query)) {
            launchSingleTop = true
        }
    }

    fun navigateToPlaylist(playlistId: String) {
        navController?.navigate(Playlist(playlistId = playlistId)) {
            launchSingleTop = true
        }
    }

    fun navigateHome() {
        navController?.navigate(Home) {
            launchSingleTop = true
        }
    }
}
