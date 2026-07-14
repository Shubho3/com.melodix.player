package com.melodix.player.repo.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.melodix.player.model.Playlist
import com.melodix.player.repo.FavoritesRepository
import com.melodix.player.repo.PlayHistoryRepository
import com.melodix.player.repo.PlaylistRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

/**
 * Two-way sync between the local DataStore repos and Firestore.
 *
 * All Melodix data is namespaced under a top-level `melodix` collection so it never collides with
 * the other apps sharing this Firebase project. Schema (per signed-in user):
 *
 *   melodix/users/{uid}/favorites   -> { ids: [Long], updatedAt }
 *   melodix/users/{uid}/history     -> { ids: [Long], updatedAt }
 *   melodix/users/{uid}/playlists   -> { items: [ {id,name,trackIds:[Long],updatedAt} ], updatedAt }
 *
 * (`melodix`=collection, `users`=anchor document, `{uid}`=collection, then one document per category.)
 *
 * Merge policy: favorites & history are unioned across devices; playlists are last-write-wins by
 * updatedAt. (Union means un-favoriting doesn't propagate deletes — acceptable for v1.)
 */
class SyncManager(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val favoritesRepository: FavoritesRepository,
    private val playHistoryRepository: PlayHistoryRepository,
    private val playlistRepository: PlaylistRepository,
) {
    suspend fun syncNow(): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: error("Not signed in")
        val userCol = firestore.collection(ROOT).document(USERS).collection(uid)
        syncFavorites(userCol)
        syncHistory(userCol)
        syncPlaylists(userCol)
        // Force a server round-trip so the status reflects real server acceptance, not just the
        // local (offline) commit — surfaces "database not created" / "rules denied" as a failure.
        firestore.waitForPendingWrites().await()
        userCol.document("favorites").get(Source.SERVER).await()
    }

    /** Write-only pushes used by auto-sync when local data changes (no read/merge). */
    suspend fun pushFavorites(): Result<Unit> = push { col ->
        col.document("favorites")
            .set(mapOf("ids" to favoritesRepository.getFavoriteIds().first().toList(), "updatedAt" to now())).await()
    }

    suspend fun pushHistory(): Result<Unit> = push { col ->
        col.document("history")
            .set(mapOf("ids" to playHistoryRepository.getHistoryIds().first().take(MAX_HISTORY), "updatedAt" to now())).await()
    }

    suspend fun pushPlaylists(): Result<Unit> = push { col ->
        val items = playlistRepository.getPlaylists().first().map {
            mapOf("id" to it.id, "name" to it.name, "trackIds" to it.trackIds, "updatedAt" to now())
        }
        col.document("playlists").set(mapOf("items" to items, "updatedAt" to now())).await()
    }

    private suspend fun push(block: suspend (CollectionReference) -> Unit): Result<Unit> = runCatching {
        val uid = auth.currentUser?.uid ?: return@runCatching
        block(firestore.collection(ROOT).document(USERS).collection(uid))
    }

    private suspend fun syncFavorites(userCol: CollectionReference) {
        val doc = userCol.document("favorites")
        val local = favoritesRepository.getFavoriteIds().first()
        val remote = doc.get(Source.SERVER).await().longList("ids").toSet()
        val merged = local + remote
        (merged - local).forEach { favoritesRepository.setFavorite(it, true) }
        doc.set(mapOf("ids" to merged.toList(), "updatedAt" to now())).await()
    }

    private suspend fun syncHistory(userCol: CollectionReference) {
        val doc = userCol.document("history")
        val local = playHistoryRepository.getHistoryIds().first()
        val remote = doc.get().await().longList("ids")
        (remote.toSet() - local.toSet()).forEach { playHistoryRepository.recordPlay(it) }
        val merged = (local + remote).distinct().take(MAX_HISTORY)
        doc.set(mapOf("ids" to merged, "updatedAt" to now())).await()
    }

    private suspend fun syncPlaylists(userCol: CollectionReference) {
        val doc = userCol.document("playlists")
        val local = playlistRepository.getPlaylists().first()
        val remote = doc.get().await().playlistItems()

        // Merge by id; newer updatedAt wins (local uses createdAt as its timestamp proxy).
        val byId = linkedMapOf<String, Pair<Playlist, Long>>()
        local.forEach { byId[it.id] = it to it.createdAt }
        remote.forEach { (pl, updated) ->
            val existing = byId[pl.id]
            if (existing == null || updated >= existing.second) byId[pl.id] = pl to updated
        }
        val merged = byId.values.map { it.first }

        playlistRepository.replaceAll(merged)
        val items = merged.map {
            mapOf("id" to it.id, "name" to it.name, "trackIds" to it.trackIds, "updatedAt" to now())
        }
        doc.set(mapOf("items" to items, "updatedAt" to now())).await()
    }

    private fun now() = System.currentTimeMillis()

    private fun DocumentSnapshot.longList(field: String): List<Long> =
        (get(field) as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList()

    private fun DocumentSnapshot.playlistItems(): List<Pair<Playlist, Long>> =
        (get("items") as? List<*>)?.mapNotNull { raw ->
            val map = raw as? Map<*, *> ?: return@mapNotNull null
            val name = map["name"] as? String ?: return@mapNotNull null
            val trackIds = (map["trackIds"] as? List<*>)?.mapNotNull { (it as? Number)?.toLong() } ?: emptyList()
            val updatedAt = (map["updatedAt"] as? Number)?.toLong() ?: 0L
            Playlist(
                id = map["id"] as? String ?: return@mapNotNull null,
                name = name,
                trackIds = trackIds,
            ) to updatedAt
        } ?: emptyList()

    private companion object {
        const val ROOT = "melodix"
        const val USERS = "users"
        const val MAX_HISTORY = 300
    }
}
