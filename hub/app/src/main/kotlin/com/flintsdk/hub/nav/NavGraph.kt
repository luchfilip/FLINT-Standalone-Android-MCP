package com.flintsdk.hub.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.flintsdk.hub.ui.flintapps.FlintAppsScreen
import com.flintsdk.hub.ui.hub.HubScreen
import com.flintsdk.hub.ui.settings.SettingsScreen
import com.flintsdk.hub.ui.setup.PermissionSetupScreen
import kotlinx.serialization.Serializable

@Serializable object HubMain
@Serializable object Settings
@Serializable object FlintApps
@Serializable object PermissionSetup

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = HubMain
    ) {
        composable<HubMain> {
            HubScreen(
                onNavigateToSettings = { navController.navigate(Settings) },
                onNavigateToFlintApps = { navController.navigate(FlintApps) },
                onNavigateToSetup = { navController.navigate(PermissionSetup) }
            )
        }
        composable<Settings> {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<FlintApps> {
            FlintAppsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable<PermissionSetup> {
            PermissionSetupScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(Settings) }
            )
        }
    }
}
