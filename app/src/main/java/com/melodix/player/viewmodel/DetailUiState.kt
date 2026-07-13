package com.melodix.player.viewmodel

import android.net.Uri
import com.melodix.player.model.Track

/** Shared state for the album / artist / playlist / liked-songs / recently-played detail screens. */
data class DetailUiState(
    val title: String = "",
    val subtitle: String = "",
    val artUri: Uri? = null,
    val tracks: List<Track> = emptyList(),
    val isLoading: Boolean = true,
)
