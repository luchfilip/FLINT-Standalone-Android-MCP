package com.flintsdk.sample.music.nav

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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.flintsdk.sample.music.data.FakeData
import com.flintsdk.sample.music.ui.components.MiniPlayer
import com.flintsdk.sample.music.ui.home.HomeScreen
import com.flintsdk.sample.music.ui.playlistdetail.PlaylistDetailScreen
import com.flintsdk.sample.music.ui.search.SearchResultsScreen
import com.flintsdk.sample.music.ui.trackdetail.TrackDetailScreen
import kotlinx.serialization.Serializable

@Serializable object Home
@Serializable data class Search(val query: String)
@Serializable data class Track(val trackId: String)
@Serializable data class Playlist(val playlistId: String)

@Composable
fun NavGraph(navController: NavHostController) {
    var currentTrackIndex by remember { mutableIntStateOf(0) }
    val currentTrack = FakeData.tracks[currentTrackIndex]

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.fillMaxSize()
        ) {
            composable<Home> {
                HomeScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate(Playlist(playlistId = playlistId))
                    },
                    onSearchClick = {
                        navController.navigate(Search(query = "all"))
                    }
                )
            }

            composable<Search> {
                SearchResultsScreen(
                    onTrackClick = { trackId ->
                        navController.navigate(Track(trackId = trackId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Track> {
                TrackDetailScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable<Playlist> {
                PlaylistDetailScreen(
                    onTrackClick = { trackId ->
                        navController.navigate(Track(trackId = trackId))
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
