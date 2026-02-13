package com.flintsdk.sample.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import com.flintsdk.sample.music.di.AppNavigatorHolder
import com.flintsdk.sample.music.nav.NavGraph
import com.flintsdk.sample.music.ui.theme.MusicAppTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var navigatorHolder: AppNavigatorHolder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicAppTheme {
                val navController = rememberNavController()
                navigatorHolder.set(navController)
                NavGraph(navController)
            }
        }
    }
}
