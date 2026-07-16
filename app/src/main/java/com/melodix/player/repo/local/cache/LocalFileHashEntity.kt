package com.melodix.player.repo.local.cache

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_file_hashes")
data class LocalFileHashEntity(
    @PrimaryKey val mediaStoreId: Long,
    val sizeBytes: Long,
    val dateModified: Long,
    val md5: String,
)
