package com.flintsdk.sample.music.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import com.flintsdk.sample.music.data.FakeData
import com.flintsdk.sample.music.data.Playlist
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Immutable
data class HomeUiState(
    val playlists: ImmutableList<Playlist> = persistentListOf()
)

@Immutable
sealed interface HomeUiEvent {
    data object LoadPlaylists : HomeUiEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.LoadPlaylists -> {
                _uiState.value = HomeUiState(
                    playlists = FakeData.playlists.toImmutableList()
                )
            }
        }
    }
}
