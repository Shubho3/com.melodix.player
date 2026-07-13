package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.model.Album
import com.melodix.player.model.Artist
import com.melodix.player.model.Track
import com.melodix.player.repo.MusicRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
)

class SearchViewModel(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                tracks = emptyList(),
                albums = emptyList(),
                artists = emptyList(),
                hasSearched = false,
            )
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.value = _uiState.value.copy(isSearching = true)
            launch {
                musicRepository.searchTracks(query)
                    .catch { }
                    .collect {
                        _uiState.value = _uiState.value.copy(tracks = it, isSearching = false, hasSearched = true)
                    }
            }
            launch {
                musicRepository.searchAlbums(query)
                    .catch { }
                    .collect { _uiState.value = _uiState.value.copy(albums = it) }
            }
            launch {
                musicRepository.searchArtists(query)
                    .catch { }
                    .collect { _uiState.value = _uiState.value.copy(artists = it) }
            }
        }
    }

    fun clearSearch() {
        onQueryChange("")
    }
}
