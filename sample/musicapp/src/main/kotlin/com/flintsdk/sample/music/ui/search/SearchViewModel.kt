package com.flintsdk.sample.music.ui.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import com.flintsdk.sample.music.data.FakeData
import com.flintsdk.sample.music.data.Track
import com.flintsdk.sample.music.nav.Search
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@Immutable
data class SearchUiState(
    val query: String = "",
    val results: ImmutableList<Track> = persistentListOf()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val route = savedStateHandle.toRoute<Search>()

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onEvent(event: SearchUiEvent) {
        when (event) {
            SearchUiEvent.LoadResults -> {
                val effectiveQuery = if (route.query == "all") "" else route.query
                _uiState.value = SearchUiState(
                    query = effectiveQuery,
                    results = FakeData.searchTracks(effectiveQuery).toImmutableList()
                )
            }
        }
    }
}

@Immutable
sealed interface SearchUiEvent {
    data object LoadResults : SearchUiEvent
}
