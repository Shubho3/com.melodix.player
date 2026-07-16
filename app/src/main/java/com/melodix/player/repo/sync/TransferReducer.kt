package com.melodix.player.repo.sync

object TransferReducer {
    data class Tracked(val uniqueName: String, val displayName: String, val direction: Direction)
    data class WorkStatus(val state: TransferItemState, val progress: Int)

    fun reduce(tracked: List<Tracked>, statuses: Map<String, WorkStatus>): List<TransferItem> =
        tracked.map { t ->
            val s = statuses[t.uniqueName] ?: WorkStatus(TransferItemState.QUEUED, 0)
            TransferItem(
                uniqueName = t.uniqueName,
                displayName = t.displayName,
                direction = t.direction,
                state = s.state,
                progress = if (s.state == TransferItemState.DONE) 100 else s.progress,
            )
        }
}
