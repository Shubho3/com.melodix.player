package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.model.Album
import com.melodix.player.model.Artist
import com.melodix.player.model.Track
import com.melodix.player.repo.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

enum class LibraryTab { SONGS, ALBUMS, ARTISTS, PLAYLISTS }
enum class SortOrder { RECENTLY_ADDED, TITLE, ARTIST }

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.SONGS,
    val sortOrder: SortOrder = SortOrder.RECENTLY_ADDED,
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
)

class LibraryViewModel(
    private val musicRepository: MusicRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.value = _uiState.value.copy(sortOrder = order)
    }

    private fun loadAll() {
        viewModelScope.launch {
            musicRepository.getTracks()
                .catch { }
                .collect { _uiState.value = _uiState.value.copy(tracks = it, isLoading = false) }
        }
        viewModelScope.launch {
            musicRepository.getAlbums()
                .catch { }
                .collect { _uiState.value = _uiState.value.copy(albums = it) }
        }
        viewModelScope.launch {
            musicRepository.getArtists()
                .catch { }
                .collect { _uiState.value = _uiState.value.copy(artists = it) }
        }
    }
}
