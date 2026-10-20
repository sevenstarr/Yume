package tachiyomi.domain.metadata.service

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.metadata.model.LocalAnimeIdentity

// AM (Yume background metadata sync, 2026-10-20; new code assisted by Codex)
data class MetadataSyncStatus(
    val generation: Long = 0,
    val epoch: Long = 0,
    val status: String = "IDLE",
    val onAdd: Boolean = false,
    val total: Long = 0,
    val success: Long = 0,
    val ambiguous: Long = 0,
    val errors: Long = 0,
    val skipped: Long = 0,
    val charging: Boolean = false,
) {
    val completed get() = success + ambiguous + errors + skipped
}

data class MetadataSyncItem(val identity: LocalAnimeIdentity?, val animeId: Long, val attempts: Long, val due: Long)

interface MetadataSyncQueue {
    fun observe(): Flow<MetadataSyncStatus>
    suspend fun status(): MetadataSyncStatus
    suspend fun start(scheduled: Boolean = false): Boolean
    suspend fun transition(status: String)
    suspend fun setOnAdd(enabled: Boolean)
    suspend fun next(generation: Long): MetadataSyncItem?
    suspend fun claim(generation: Long, animeId: Long): Boolean
    suspend fun finish(generation: Long, animeId: Long, outcome: String, due: Long = 0)
    suspend fun complete(generation: Long)
}
