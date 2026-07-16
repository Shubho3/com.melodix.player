package com.melodix.player.repo.local.cache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CachedTrackEntity::class, LocalFileHashEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedTrackDao(): CachedTrackDao
    abstract fun localFileHashDao(): LocalFileHashDao
}
