package com.melodix.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodix.player.model.Album
import com.melodix.player.model.Artist
import com.melodix.player.model.SortSpec
import com.melodix.player.model.Track
import com.melodix.player.repo.CacheRepository
import com.melodix.player.repo.MusicRepository
import com.melodix.player.repo.SettingsRepository
import com.melodix.player.repo.local.cache.toTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class LibraryTab { SONGS, ALBUMS, ARTISTS, PLAYLISTS }

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.SONGS,
    val sortSpec: SortSpec = SortSpec(),
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
)

class LibraryViewModel(
    private val musicRepository: MusicRepository,
    private val cacheRepository: CacheRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    fun setSortSpec(spec: SortSpec) {
        viewModelScope.launch { settingsRepository.setSortSpec(spec) }
    }

    private fun loadAll() {
        viewModelScope.launch {
            combine(
                musicRepository.getTracks(),
                cacheRepository.observeCached(),
                settingsRepository.getSortSpec(),
            ) { tracks, cached, spec ->
                // Local library + Drive-cached songs, excluding cached copies already present locally
                // (matched by title + artist).
                val localKeys = tracks.mapTo(HashSet()) { it.title.lowercase() to it.artist.lowercase() }
                val cachedOnly = cached.map { it.toTrack() }
                    .filter { (it.title.lowercase() to it.artist.lowercase()) !in localKeys }
                val all = tracks + cachedOnly
                val sorted = all.sortedWith(
                    spec.comparator(Track::title, Track::artist, Track::duration, Track::dateAdded),
                )
                spec to sorted
            }
                .catch { }
                .collect { (spec, sorted) ->
                    _uiState.value = _uiState.value.copy(
                        sortSpec = spec,
                        tracks = sorted,
                        isLoading = false,
                    )
                }
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
