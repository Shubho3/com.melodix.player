package com.melodix.player.repo.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class TransferReducerTest {

    @Test
    fun joinsTrackedItemsWithWorkStatusesPreservingOrder() {
        val tracked = listOf(
            TransferReducer.Tracked("download_a", "A.mp3", Direction.DOWNLOAD),
            TransferReducer.Tracked("download_b", "B.mp3", Direction.DOWNLOAD),
        )
        val statuses = mapOf(
            "download_a" to TransferReducer.WorkStatus(TransferItemState.RUNNING, 40),
            "download_b" to TransferReducer.WorkStatus(TransferItemState.DONE, 100),
        )

        val items = TransferReducer.reduce(tracked, statuses)

        assertEquals(2, items.size)
        assertEquals("A.mp3", items[0].displayName)
        assertEquals(TransferItemState.RUNNING, items[0].state)
        assertEquals(40, items[0].progress)
        assertEquals(TransferItemState.DONE, items[1].state)
        assertEquals(100, items[1].progress)
    }

    @Test
    fun missingStatusDefaultsToQueued() {
        val tracked = listOf(TransferReducer.Tracked("upload_x", "X.mp3", Direction.UPLOAD))
        val items = TransferReducer.reduce(tracked, emptyMap())
        assertEquals(TransferItemState.QUEUED, items[0].state)
        assertEquals(0, items[0].progress)
    }
}
