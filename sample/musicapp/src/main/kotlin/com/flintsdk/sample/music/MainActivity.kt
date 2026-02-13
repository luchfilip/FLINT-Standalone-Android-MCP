package com.flintsdk.sample.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dagger.hilt.android.AndroidEntryPoint
import com.flintsdk.sample.music.data.FakeData
import com.flintsdk.sample.music.di.AppNavigatorHolder
import com.flintsdk.sample.music.navigation.AppNavigatorImpl
import com.flintsdk.sample.music.ui.components.MiniPlayer
import com.flintsdk.sample.music.ui.screens.HomeScreen
import com.flintsdk.sample.music.ui.screens.PlaylistDetailScreen
import com.flintsdk.sample.music.ui.screens.SearchResultsScreen
import com.flintsdk.sample.music.ui.screens.TrackDetailScreen
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
                MusicApp(navigatorHolder)
            }
        }
    }
}

@Composable
fun MusicApp(navigatorHolder: AppNavigatorHolder) {
    val navController = rememberNavController()
    navigatorHolder.set(AppNavigatorImpl(navController))

    var currentTrackIndex by remember { mutableIntStateOf(0) }
    val currentTrack = FakeData.tracks[currentTrackIndex]

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize()
        ) {
            composable("home") {
                HomeScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate("playlist/$playlistId")
                    },
                    onSearchClick = {
                        // Default search showing all tracks
                        navController.navigate("search/all")
                    }
                )
            }

            composable(
                "search/{query}",
                arguments = listOf(navArgument("query") { type = NavType.StringType })
            ) { backStackEntry ->
                val query = backStackEntry.arguments?.getString("query") ?: ""
                val effectiveQuery = if (query == "all") "" else query
                SearchResultsScreen(
                    query = effectiveQuery,
                    onTrackClick = { trackId ->
                        navController.navigate("track/$trackId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "track/{trackId}",
                arguments = listOf(navArgument("trackId") { type = NavType.StringType })
            ) { backStackEntry ->
                val trackId = backStackEntry.arguments?.getString("trackId") ?: ""
                TrackDetailScreen(
                    trackId = trackId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                "playlist/{playlistId}",
                arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    onTrackClick = { trackId ->
                        navController.navigate("track/$trackId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        MiniPlayer(
            track = currentTrack,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            onNext = {
                currentTrackIndex = (currentTrackIndex + 1) % FakeData.tracks.size
            }
        )
    }
}
