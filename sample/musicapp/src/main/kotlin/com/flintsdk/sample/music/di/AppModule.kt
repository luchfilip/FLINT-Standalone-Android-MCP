package com.flintsdk.sample.music.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import com.flintsdk.sample.music.navigation.AppNavigator
import com.flintsdk.sample.music.navigation.AppNavigatorImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppNavigator(): AppNavigatorHolder {
        return AppNavigatorHolder()
    }
}

/**
 * Holds a mutable reference to the AppNavigator.
 * The NavController is only available when the Composable is active,
 * so we use a holder that gets updated when the NavHost is composed.
 */
class AppNavigatorHolder {
    private var navigator: AppNavigator? = null

    fun set(navigator: AppNavigator) {
        this.navigator = navigator
    }

    fun navigate(route: String) {
        navigator?.navigate(route)
    }
}
