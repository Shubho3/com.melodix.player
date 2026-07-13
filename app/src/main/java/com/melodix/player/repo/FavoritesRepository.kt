package com.melodix.player.repo

import kotlinx.coroutines.flow.Flow

interface FavoritesRepository {
    fun getFavoriteIds(): Flow<Set<Long>>
    suspend fun toggleFavorite(trackId: Long)
    suspend fun setFavorite(trackId: Long, favorite: Boolean)
}
