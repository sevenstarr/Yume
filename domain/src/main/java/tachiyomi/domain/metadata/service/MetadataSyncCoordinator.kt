package tachiyomi.domain.metadata.service

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.domain.metadata.model.MetadataCachePolicy
import tachiyomi.domain.metadata.model.MetadataFailureKind
import tachiyomi.domain.metadata.model.MetadataLookupKey
import tachiyomi.domain.metadata.model.MetadataResult
import tachiyomi.domain.metadata.model.MetadataTicket
import kotlin.coroutines.coroutineContext

// AM (Yume background metadata sync, 2026-10-20; new code assisted by Codex)
/** A bounded slice; WorkManager owns device constraints, retry scheduling and cancellation. */
class MetadataSyncCoordinator(
    private val queue: MetadataSyncQueue,
    private val repository: MetadataRepository,
    private val provider: MetadataProvider,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** True means durable pending work remains. No long sleep, polling or busy retry here. */
    suspend fun runSlice(maxItems: Int = 20): Boolean {
        require(maxItems in 1..100)
        val batch = queue.status()
        val control = repository.control()
        if (batch.status != "RUNNING") return false
        if (!control.enabled || control.epoch != batch.epoch) {
            queue.transition("CANCELLED")
            return false
        }
        return coroutineScope {
            val operation = this
            val policyWatcher = launch(start = CoroutineStart.UNDISPATCHED) {
                repository.observeControl().first { !it.enabled || it.epoch != batch.epoch }
                operation.cancel("Metadata policy changed")
            }
            val queueWatcher = launch(start = CoroutineStart.UNDISPATCHED) {
                queue.observe().first { it.generation != batch.generation || it.status in setOf("PAUSED", "CANCELLED") }
                operation.cancel("Metadata batch changed")
            }
            try {
                val deadline = now() + 180_000
                repeat(maxItems) {
                    coroutineContext.ensureActive()
                    if (now() >= deadline || repository.control().hostNotBefore > now()) return@coroutineScope true
                    val item = queue.next(batch.generation) ?: run {
                        queue.complete(batch.generation)
                        return@coroutineScope false
                    }
                    if (item.due > now()) return@coroutineScope true
                    process(batch.generation, item)
                }
                queue.complete(batch.generation)
                queue.status().status == "RUNNING"
            } finally {
                policyWatcher.cancel()
                queueWatcher.cancel()
            }
        }
    }

    private suspend fun process(generation: Long, item: MetadataSyncItem) {
        val identity = item.identity
        val row = repository.get(item.animeId)
        if (identity == null || identity.title.length > 300 ||
            row?.state?.let { it.originalOnly || it.automaticBlocked } == true
        ) {
            queue.finish(generation, item.animeId, "SKIPPED")
            return
        }
        val checked = row?.state?.lastCheckedAt
        if (checked != null && now() >= checked && now() - checked < MetadataCachePolicy.FRESH &&
            !row.state.requiresRevalidation && row.state.identity == identity &&
            row.state.association?.id?.let { repository.readMedia(it, now())?.fresh } == true
        ) {
            queue.finish(generation, item.animeId, "SKIPPED")
            return
        }
        if ((row?.state?.association == null || row.state.outcome == "UNAVAILABLE") && row?.state?.lastAttemptAt?.let {
                now() >= it && now() - it < MetadataCachePolicy.LOOKUP &&
                    row.state.outcome !in setOf("SEARCHING", "TRANSIENT_ERROR", "PENDING")
            } == true
        ) {
            queue.finish(generation, item.animeId, "SKIPPED")
            return
        }
        val ticket = repository.beginSync(identity, row?.revision, now(), generation)
        if (ticket == null) {
            // A foreground review/abandoned lease may own the item; never steal or reset its budget.
            queue.finish(generation, item.animeId, "PENDING", now() + 60_000)
            return
        }
        try {
            if (!queue.claim(generation, item.animeId)) {
                queue.finish(generation, item.animeId, "ERROR")
                return
            }
            val association = row?.state?.association
            val outcome = if (association != null) {
                reserve(ticket)
                when (val result = provider.getById(association.id)) {
                    is MetadataResult.Failure -> return retry(generation, item, ticket, result)
                    else -> if (repository.refresh(ticket, result, now())) {
                        if (result == MetadataResult.NotFound) "SKIPPED" else "SUCCESS"
                    } else {
                        "ERROR"
                    }
                }
            } else {
                val key = MetadataLookupKey(identity.title)
                val cached = repository.readLookup(key, ticket.fingerprint, now())
                val result = if (cached != null) {
                    MetadataResult.Success(cached)
                } else {
                    reserve(ticket)
                    provider.search(MetadataSearch(identity.title))
                }
                when (result) {
                    is MetadataResult.Failure -> return retry(generation, item, ticket, result)
                    MetadataResult.NotFound -> "ERROR"
                    is MetadataResult.Success -> {
                        repository.cacheLookup(ticket, key, result, now())
                        // A single page is not a complete search when more pages exist. No textual auto-pin.
                        if (repository.applyMatch(
                                ticket,
                                result.value.media,
                                result.complete && result.value.hasNextPage == false,
                                now(),
                            )
                        ) {
                            if (result.complete && result.value.media.isEmpty()) "SKIPPED" else "AMBIGUOUS"
                        } else {
                            "SKIPPED"
                        }
                    }
                }
            }
            coroutineContext.ensureActive()
            queue.finish(generation, item.animeId, outcome)
        } finally {
            withContext(NonCancellable) { repository.cancelAttempt(ticket) }
        }
    }

    private suspend fun reserve(ticket: MetadataTicket) {
        // Upper bound per provider operation; the shared HTTP governor counts physical emissions too.
        repeat(3) { check(repository.recordEmission(ticket, now())) }
    }

    private suspend fun retry(
        generation: Long,
        item: MetadataSyncItem,
        ticket: MetadataTicket,
        failure: MetadataResult.Failure,
    ) {
        repository.failAttempt(ticket, failure, now())
        val retryable = failure.retryable || failure.kind in setOf(
            MetadataFailureKind.NETWORK,
            MetadataFailureKind.TIMEOUT,
            MetadataFailureKind.RATE_LIMITED,
            MetadataFailureKind.CIRCUIT_OPEN,
        )
        queue.finish(
            generation,
            item.animeId,
            if (retryable && item.attempts < 2) "PENDING" else "ERROR",
            maxOf(now() + 60_000L * (1L shl item.attempts.coerceAtMost(2).toInt()), failure.retryAtMillis ?: 0),
        )
    }
}
