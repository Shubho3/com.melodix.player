package com.melodix.player.repo.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncDiffTest {

    @Test
    fun localWithMatchingHashIsNotLocalOnlyAndDriveFileIsNotDownloadable() {
        val locals = listOf(SyncDiff.Local(id = 1L, sizeBytes = 100L, md5 = "aaa"))
        val remotes = listOf(SyncDiff.Remote(id = "d1", sizeBytes = 100L, md5 = "aaa"))

        val result = SyncDiff.compute(locals, remotes, cachedRemoteIds = emptySet())

        assertEquals(emptySet<Long>(), result.localOnlyIds)
        assertEquals(emptySet<String>(), result.downloadableIds)
    }

    @Test
    fun unmatchedLocalIsLocalOnly() {
        val locals = listOf(SyncDiff.Local(id = 2L, sizeBytes = 50L, md5 = null))
        val remotes = listOf(SyncDiff.Remote(id = "d1", sizeBytes = 100L, md5 = "aaa"))

        val result = SyncDiff.compute(locals, remotes, cachedRemoteIds = emptySet())

        assertEquals(setOf(2L), result.localOnlyIds)
        assertEquals(setOf("d1"), result.downloadableIds)
    }

    @Test
    fun cachedDriveFileIsNotDownloadable() {
        val remotes = listOf(SyncDiff.Remote(id = "d1", sizeBytes = 100L, md5 = "aaa"))

        val result = SyncDiff.compute(emptyList(), remotes, cachedRemoteIds = setOf("d1"))

        assertEquals(emptySet<String>(), result.downloadableIds)
    }
}
