package com.flintsdk.hub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.flintsdk.hub.ui.FlintAppsScreen
import com.flintsdk.hub.ui.HubScreen
import com.flintsdk.hub.ui.PermissionSetupScreen
import com.flintsdk.hub.ui.SettingsScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "hub_main"
                    ) {
                        composable("hub_main") {
                            HubScreen(
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                },
                                onNavigateToFlintApps = {
                                    navController.navigate("flint_apps")
                                },
                                onNavigateToSetup = {
                                    navController.navigate("permission_setup")
                                }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("flint_apps") {
                            FlintAppsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        composable("permission_setup") {
                            PermissionSetupScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToSettings = {
                                    navController.navigate("settings")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
