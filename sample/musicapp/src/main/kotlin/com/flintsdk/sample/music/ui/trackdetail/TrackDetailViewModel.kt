package com.flintsdk.sample.music.ui.trackdetail

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import com.flintsdk.sample.music.data.FakeData
import com.flintsdk.sample.music.nav.Track as TrackRoute
import com.flintsdk.sample.music.data.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Immutable
data class TrackDetailUiState(
    val track: Track? = null,
    val isFavorite: Boolean = false
)

@Immutable
sealed interface TrackDetailUiEvent {
    data object LoadTrack : TrackDetailUiEvent
    data object ToggleFavorite : TrackDetailUiEvent
}

@HiltViewModel
class TrackDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<TrackRoute>()

    private val _uiState = MutableStateFlow(TrackDetailUiState())
    val uiState: StateFlow<TrackDetailUiState> = _uiState.asStateFlow()

    fun onEvent(event: TrackDetailUiEvent) {
        when (event) {
            TrackDetailUiEvent.LoadTrack -> {
                _uiState.value = TrackDetailUiState(
                    track = FakeData.getTrack(route.trackId)
                )
            }
            TrackDetailUiEvent.ToggleFavorite -> {
                _uiState.value = _uiState.value.copy(
                    isFavorite = !_uiState.value.isFavorite
                )
            }
        }
    }
}
