package com.melodix.player.repo.local.cache

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LocalFileHashDao {
    @Query("SELECT * FROM local_file_hashes WHERE mediaStoreId = :id")
    suspend fun getById(id: Long): LocalFileHashEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: LocalFileHashEntity)
}
