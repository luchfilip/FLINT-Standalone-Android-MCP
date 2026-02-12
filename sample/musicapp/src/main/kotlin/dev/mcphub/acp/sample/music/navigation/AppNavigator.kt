package dev.mcphub.acp.sample.music.navigation

import androidx.navigation.NavController

interface AppNavigator {
    fun navigate(route: String)
}

class AppNavigatorImpl(private val navController: NavController) : AppNavigator {
    override fun navigate(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
        }
    }
}
