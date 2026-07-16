package com.melodix.player.repo.local.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedTrackDao {
    @Query("SELECT * FROM cached_tracks ORDER BY downloadedAt DESC")
    fun observeAll(): Flow<List<CachedTrackEntity>>

    @Query("SELECT * FROM cached_tracks WHERE driveFileId = :id")
    suspend fun getById(id: String): CachedTrackEntity?

    @Query("SELECT driveFileId FROM cached_tracks")
    suspend fun cachedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedTrackEntity)

    @Query("DELETE FROM cached_tracks WHERE driveFileId = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM cached_tracks")
    suspend fun deleteAll()
}
