package com.melodix.player.repo.sync

enum class Direction { DOWNLOAD, UPLOAD }
enum class TransferItemState { QUEUED, RUNNING, DONE, FAILED }

data class TransferItem(
    val uniqueName: String,
    val displayName: String,
    val direction: Direction,
    val state: TransferItemState,
    val progress: Int,
)

data class TransferSnapshot(val items: List<TransferItem> = emptyList()) {
    val activeCount: Int get() = items.count { it.state == TransferItemState.QUEUED || it.state == TransferItemState.RUNNING }
    val doneCount: Int get() = items.count { it.state == TransferItemState.DONE }
    val failedCount: Int get() = items.count { it.state == TransferItemState.FAILED }
    val isActive: Boolean get() = activeCount > 0
    val overallPercent: Int get() = if (items.isEmpty()) 0 else items.sumOf { it.progress } / items.size
}
