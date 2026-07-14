package com.melodix.player.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFilterTest {

    @Test
    fun `default filter matches everything`() {
        val filter = LibraryFilter()
        assertTrue(filter.matches(durationMs = 0L, folderId = 1L))
        assertTrue(filter.matches(durationMs = 500_000L, folderId = 42L))
    }

    @Test
    fun `min duration excludes shorter tracks and keeps equal or longer`() {
        val filter = LibraryFilter(minDurationMs = 60_000L)
        assertFalse(filter.matches(durationMs = 59_999L, folderId = 1L))
        assertTrue(filter.matches(durationMs = 60_000L, folderId = 1L))
        assertTrue(filter.matches(durationMs = 60_001L, folderId = 1L))
    }

    @Test
    fun `excluded folder id is filtered out`() {
        val filter = LibraryFilter(excludedFolderIds = setOf(7L))
        assertFalse(filter.matches(durationMs = 120_000L, folderId = 7L))
        assertTrue(filter.matches(durationMs = 120_000L, folderId = 8L))
    }

    @Test
    fun `duration and folder rules combine`() {
        val filter = LibraryFilter(minDurationMs = 60_000L, excludedFolderIds = setOf(7L))
        assertFalse(filter.matches(durationMs = 30_000L, folderId = 8L)) // too short
        assertFalse(filter.matches(durationMs = 90_000L, folderId = 7L)) // excluded folder
        assertTrue(filter.matches(durationMs = 90_000L, folderId = 8L))
    }
}
