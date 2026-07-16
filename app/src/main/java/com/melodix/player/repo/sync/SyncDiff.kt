package com.melodix.player.repo.sync

/** Pure content-hash diff between the local library and a Drive folder listing. */
object SyncDiff {
    data class Local(val id: Long, val sizeBytes: Long, val md5: String?)
    data class Remote(val id: String, val sizeBytes: Long, val md5: String?)
    data class Result(val localOnlyIds: Set<Long>, val downloadableIds: Set<String>)

    fun compute(locals: List<Local>, remotes: List<Remote>, cachedRemoteIds: Set<String>): Result {
        val remoteMd5s = remotes.mapNotNull { it.md5 }.toSet()
        val localMd5s = locals.mapNotNull { it.md5 }.toSet()

        val localOnly = locals
            .filter { it.md5 == null || it.md5 !in remoteMd5s }
            .map { it.id }
            .toSet()

        val downloadable = remotes
            .filter { it.id !in cachedRemoteIds && (it.md5 == null || it.md5 !in localMd5s) }
            .map { it.id }
            .toSet()

        return Result(localOnly, downloadable)
    }
}
