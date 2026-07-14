package com.melodix.player.model

/**
 * Global library filter. Applied in the repository so Home, Library, Search, and playback queues
 * all see the same filtered set of tracks. Defaults keep everything.
 */
data class LibraryFilter(
    val minDurationMs: Long = 0L,
    val excludedFolderIds: Set<Long> = emptySet(),
) {
    /** True when a track of this duration/folder should be shown. */
    fun matches(durationMs: Long, folderId: Long): Boolean =
        durationMs >= minDurationMs && folderId !in excludedFolderIds
}
