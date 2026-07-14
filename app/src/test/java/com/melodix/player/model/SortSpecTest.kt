package com.melodix.player.model

import org.junit.Assert.assertEquals
import org.junit.Test

class SortSpecTest {

    private data class Row(
        val title: String,
        val artist: String,
        val duration: Long,
        val dateAdded: Long,
    )

    private val rows = listOf(
        Row("banana", "Zeta", duration = 300, dateAdded = 100),
        Row("Apple", "alpha", duration = 100, dateAdded = 300),
        Row("cherry", "Mike", duration = 200, dateAdded = 200),
    )

    private fun SortSpec.sort(list: List<Row>): List<Row> =
        list.sortedWith(comparator(Row::title, Row::artist, Row::duration, Row::dateAdded))

    @Test
    fun `title ascending is case insensitive`() {
        val result = SortSpec(SortKey.TITLE, ascending = true).sort(rows).map { it.title }
        assertEquals(listOf("Apple", "banana", "cherry"), result)
    }

    @Test
    fun `title descending reverses order`() {
        val result = SortSpec(SortKey.TITLE, ascending = false).sort(rows).map { it.title }
        assertEquals(listOf("cherry", "banana", "Apple"), result)
    }

    @Test
    fun `artist ascending is case insensitive`() {
        val result = SortSpec(SortKey.ARTIST, ascending = true).sort(rows).map { it.artist }
        assertEquals(listOf("alpha", "Mike", "Zeta"), result)
    }

    @Test
    fun `duration ascending puts shortest first`() {
        val result = SortSpec(SortKey.DURATION, ascending = true).sort(rows).map { it.duration }
        assertEquals(listOf(100L, 200L, 300L), result)
    }

    @Test
    fun `date added descending puts newest first`() {
        val result = SortSpec(SortKey.DATE_ADDED, ascending = false).sort(rows).map { it.dateAdded }
        assertEquals(listOf(300L, 200L, 100L), result)
    }
}
