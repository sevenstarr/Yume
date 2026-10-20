package tachiyomi.data.metadata

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.animesource.model.FetchType
import kotlinx.coroutines.flow.map
import tachiyomi.data.Database
import tachiyomi.data.subscribeToOneOrNull
import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MetadataScope
import tachiyomi.domain.metadata.service.MetadataSyncItem
import tachiyomi.domain.metadata.service.MetadataSyncQueue
import tachiyomi.domain.metadata.service.MetadataSyncStatus

// AM (Yume durable metadata queue, 2026-10-20; new code assisted by Codex)
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class MetadataSyncQueueImpl(private val database: Database) : MetadataSyncQueue {
    private val queries get() = database.metadata_syncQueries

    override fun observe() = queries.summary().subscribeToOneOrNull().map { status() }

    override suspend fun status(): MetadataSyncStatus {
        if (queries.summary().awaitAsOneOrNull() == null) queries.initialize()
        return queries.summary().awaitAsOne().let {
            MetadataSyncStatus(
                it.generation, it.epoch, it.status, it.on_add == 1L, it.total,
                it.success, it.ambiguous, it.errors, it.skipped, it.charging == 1L,
            )
        }
    }

    override suspend fun start(scheduled: Boolean): Boolean = database.transactionWithResult {
        val control = database.metadataQueries
        control.initialize()
        val policy = control.control().awaitAsOne()
        if (policy.enabled != 1L || status().status in setOf("RUNNING", "PAUSED")) {
            return@transactionWithResult false
        }
        queries.clear()
        queries.enqueueLibrary()
        queries.start(policy.epoch, if (scheduled) 1 else 0)
        true
    }

    override suspend fun transition(status: String) = database.transaction {
        require(status in setOf("PAUSED", "CANCELLED", "RUNNING"))
        val current = this@MetadataSyncQueueImpl.status()
        if (status == "RUNNING") {
            val policy = database.metadataQueries.control().awaitAsOneOrNull()
            if (current.status != "PAUSED" || policy?.enabled != 1L || policy.epoch != current.epoch) {
                return@transaction
            }
        }
        queries.transition(status)
        if (status == "CANCELLED") queries.skipPending()
    }

    override suspend fun setOnAdd(enabled: Boolean) {
        queries.initialize()
        queries.setOnAdd(if (enabled) 1 else 0)
    }

    override suspend fun next(generation: Long): MetadataSyncItem? {
        val control = queries.controlState().awaitAsOneOrNull() ?: return null
        if (control.generation != generation || control.status != "RUNNING") return null
        return queries.next().awaitAsOneOrNull()?.let { row ->
            val identity = if (row.favorite_at == null) {
                null
            } else {
                runCatching {
                    LocalAnimeIdentity(
                        row.anime_id,
                        row.source,
                        row.url,
                        row.title,
                        scope = when {
                            row.fetch_type == FetchType.Seasons -> MetadataScope.COLLECTION
                            row.parent_id != null -> MetadataScope.CHILD_SEASON
                            else -> MetadataScope.UNKNOWN
                        },
                        parentId = row.parent_id,
                        seasonNumber = row.season_number.toInt().takeIf { number ->
                            number > 0 && number.toDouble() == row.season_number
                        },
                    )
                }.getOrNull()
            }
            MetadataSyncItem(identity, row.anime_id, row.attempts, row.not_before)
        }
    }

    override suspend fun finish(generation: Long, animeId: Long, outcome: String, due: Long) {
        require(outcome in setOf("SUCCESS", "AMBIGUOUS", "ERROR", "SKIPPED", "PENDING"))
        queries.finish(outcome, due, animeId, generation)
    }

    override suspend fun claim(generation: Long, animeId: Long): Boolean = database.transactionWithResult {
        queries.claim(animeId, generation)
        queries.changes().awaitAsOne() == 1L
    }

    override suspend fun complete(generation: Long) {
        queries.complete(generation)
    }
}
