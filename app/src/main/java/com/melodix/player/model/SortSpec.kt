package com.melodix.player.model

enum class SortKey { TITLE, DATE_ADDED, ARTIST, DURATION }

/**
 * A user-selected sort for track lists. [comparator] is generic over the field accessors so the
 * ordering logic can be unit-tested without constructing Android-backed [Track]s.
 */
data class SortSpec(
    val key: SortKey = SortKey.DATE_ADDED,
    val ascending: Boolean = false,
) {
    fun <T> comparator(
        title: (T) -> String,
        artist: (T) -> String,
        duration: (T) -> Long,
        dateAdded: (T) -> Long,
    ): Comparator<T> {
        val base: Comparator<T> = when (key) {
            SortKey.TITLE -> compareBy(String.CASE_INSENSITIVE_ORDER) { title(it) }
            SortKey.ARTIST -> compareBy(String.CASE_INSENSITIVE_ORDER) { artist(it) }
            SortKey.DURATION -> compareBy { duration(it) }
            SortKey.DATE_ADDED -> compareBy { dateAdded(it) }
        }
        return if (ascending) base else base.reversed()
    }
}
