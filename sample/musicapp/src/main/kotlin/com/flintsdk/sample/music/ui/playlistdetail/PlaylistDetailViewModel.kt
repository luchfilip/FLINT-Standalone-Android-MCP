package com.flintsdk.sample.music.ui.playlistdetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import com.flintsdk.sample.music.data.FakeData
import com.flintsdk.sample.music.data.Track
import com.flintsdk.sample.music.nav.Playlist as PlaylistRoute
import com.flintsdk.sample.music.data.Playlist
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Immutable
data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val tracks: ImmutableList<Track> = persistentListOf()
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<PlaylistRoute>()

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    fun onEvent(event: PlaylistDetailUiEvent) {
        when (event) {
            PlaylistDetailUiEvent.LoadPlaylist -> {
                _uiState.value = PlaylistDetailUiState(
                    playlist = FakeData.getPlaylist(route.playlistId),
                    tracks = FakeData.getTracksForPlaylist(route.playlistId).toImmutableList()
                )
            }
        }
    }
}

@Immutable
sealed interface PlaylistDetailUiEvent {
    data object LoadPlaylist : PlaylistDetailUiEvent
}
