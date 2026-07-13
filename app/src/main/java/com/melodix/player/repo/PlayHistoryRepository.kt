package com.melodix.player.repo

import kotlinx.coroutines.flow.Flow

interface PlayHistoryRepository {
    /** Track ids ordered most-recently-played first. */
    fun getHistoryIds(): Flow<List<Long>>
    suspend fun recordPlay(trackId: Long)
}
