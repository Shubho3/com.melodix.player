package com.melodix.player.repo.local.cache

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CachedTrackEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cachedTrackDao(): CachedTrackDao
}
