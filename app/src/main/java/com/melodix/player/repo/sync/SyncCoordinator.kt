package com.melodix.player.repo.sync

import com.melodix.player.repo.AuthRepository
import com.melodix.player.repo.FavoritesRepository
import com.melodix.player.repo.PlayHistoryRepository
import com.melodix.player.repo.PlaylistRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Keeps Firestore in sync automatically while signed in — no manual button needed.
 *  - On sign-in: a full two-way sync (pull remote + push local).
 *  - After that: pushes favorites/history/playlists whenever they change (debounced), so a
 *    favorite tap, a play (history), or a playlist create/edit all sync on their own.
 */
class SyncCoordinator(
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager,
    private val favoritesRepository: FavoritesRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val playlistRepository: PlaylistRepository,
) {
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            authRepository.currentUser
                .distinctUntilChangedBy { it?.uid }
                .collectLatest { user ->
                    if (user == null) return@collectLatest
                    runCatching { syncManager.syncNow() }   // full sync on login
                    // Push on change. drop(1) skips each flow's initial value (already covered above).
                    coroutineScope {
                        launch {
                            favoritesRepository.getFavoriteIds().drop(1).debounce(DEBOUNCE_MS)
                                .collectLatest { syncManager.pushFavorites() }
                        }
                        launch {
                            playHistoryRepository.getHistoryIds().drop(1).debounce(DEBOUNCE_MS)
                                .collectLatest { syncManager.pushHistory() }
                        }
                        launch {
                            playlistRepository.getPlaylists().drop(1).debounce(DEBOUNCE_MS)
                                .collectLatest { syncManager.pushPlaylists() }
                        }
                    }
                }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 800L
    }
}
