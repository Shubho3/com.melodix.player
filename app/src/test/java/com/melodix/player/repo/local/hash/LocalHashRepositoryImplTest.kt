package com.melodix.player.repo.local.hash

import com.melodix.player.repo.local.cache.LocalFileHashDao
import com.melodix.player.repo.local.cache.LocalFileHashEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalHashRepositoryImplTest {

    private class FakeDao : LocalFileHashDao {
        val rows = mutableMapOf<Long, LocalFileHashEntity>()
        override suspend fun getById(id: Long) = rows[id]
        override suspend fun upsert(e: LocalFileHashEntity) { rows[e.mediaStoreId] = e }
    }

    @Test
    fun returnsCachedHashWithoutRecomputingWhenSizeAndDateUnchanged() = runBlocking {
        val dao = FakeDao()
        dao.rows[1L] = LocalFileHashEntity(1L, 100L, 55L, "cached-md5")
        var calls = 0
        val repo = LocalHashRepositoryImpl(dao)

        val result = repo.md5For(1L, 100L, 55L) { calls++; "fresh-md5" }

        assertEquals("cached-md5", result)
        assertEquals(0, calls)
    }

    @Test
    fun recomputesWhenSizeChanged() = runBlocking {
        val dao = FakeDao()
        dao.rows[1L] = LocalFileHashEntity(1L, 100L, 55L, "cached-md5")
        var calls = 0
        val repo = LocalHashRepositoryImpl(dao)

        val result = repo.md5For(1L, 200L, 55L) { calls++; "fresh-md5" }

        assertEquals("fresh-md5", result)
        assertEquals(1, calls)
        assertEquals("fresh-md5", dao.rows[1L]?.md5)
    }
}
