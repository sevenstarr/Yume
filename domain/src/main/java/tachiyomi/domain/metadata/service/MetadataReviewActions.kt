package tachiyomi.domain.metadata.service

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MetadataFailureKind
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataLookupKey
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataPage
import tachiyomi.domain.metadata.model.MetadataResult
import tachiyomi.domain.metadata.model.MetadataTicket
import java.net.URI

// AM (Yume manual metadata review, 2026-10-20; new code assisted by Codex) -->
/** Foreground actions only. No worker, library scan, textual auto-association or tracking mutations. */
class MetadataReviewActions(
    private val repository: MetadataRepository,
    private val provider: MetadataProvider,
    private val now: () -> Long = System::currentTimeMillis,
) {
    data class Proposal(
        val identity: LocalAnimeIdentity,
        val verified: MetadataResult.Success<MetadataMedia>,
        val revision: Long,
        val epoch: Long,
        val verifiedAt: Long,
        val originalStamp: String,
    )

    sealed interface Result<out T> {
        data class Value<T>(val value: T) : Result<T>
        data class Failed(val kind: MetadataFailureKind) : Result<Nothing>
        data object Conflict : Result<Nothing>
    }

    suspend fun search(identity: LocalAnimeIdentity, revision: Long?, query: String, page: Int): Result<MetadataPage> {
        if (query.trim().length !in 1..300 || page !in 1..20) return Result.Failed(MetadataFailureKind.INVALID_INPUT)
        return attempt(identity, revision) { ticket ->
            val key = MetadataLookupKey(query.trim(), page)
            val cached = repository.readLookup(key, ticket.fingerprint, now())
            val response = if (cached != null) {
                MetadataResult.Success(cached)
            } else {
                reserve(ticket)
                provider.search(MetadataSearch(query.trim(), page))
            }
            when (response) {
                is MetadataResult.Success -> {
                    // Incomplete pages are useful for review, but never cached as complete/negative results.
                    repository.cacheLookup(ticket, key, response, now())
                    val current = repository.get(identity.animeId)
                    if (current?.state?.association != null) {
                        repository.cancelAttempt(ticket)
                    } else {
                        repository.applyMatch(ticket, response.value.media, false, now())
                    }
                    Result.Value(
                        response.value.copy(
                            hasNextPage = response.value.hasNextPage.takeIf {
                                response.complete
                            },
                        ),
                    )
                }
                is MetadataResult.Failure -> failure(ticket, response)
                MetadataResult.NotFound -> failure(ticket, MetadataResult.Failure(MetadataFailureKind.CONTRACT))
            }
        }
    }

    suspend fun verify(identity: LocalAnimeIdentity, revision: Long?, id: MetadataId): Result<Proposal> {
        if (!AnimeMatcher.validId(id)) return Result.Failed(MetadataFailureKind.INVALID_INPUT)
        return attempt(identity, revision) { ticket ->
            reserve(ticket)
            when (val response = provider.getById(id)) {
                is MetadataResult.Success -> {
                    if (!response.identityComplete || response.value.id != id) {
                        return@attempt failure(ticket, MetadataResult.Failure(MetadataFailureKind.CONTRACT))
                    }
                    if (!repository.cancelAttempt(ticket)) return@attempt Result.Conflict
                    Result.Value(
                        Proposal(identity, response, ticket.revision + 1, ticket.epoch, now(), ticket.originalStamp),
                    )
                }
                is MetadataResult.Failure -> failure(ticket, response)
                MetadataResult.NotFound -> failure(ticket, MetadataResult.Failure(MetadataFailureKind.UNAVAILABLE))
            }
        }
    }

    suspend fun confirm(proposal: Proposal, acknowledged: Boolean): Result<Unit> {
        val control = repository.control()
        if (!acknowledged || control.epoch != proposal.epoch || now() < proposal.verifiedAt ||
            now() - proposal.verifiedAt >= 300_000
        ) {
            return Result.Conflict
        }
        return attempt(proposal.identity, proposal.revision) { ticket ->
            if (ticket.epoch != proposal.epoch || ticket.originalStamp != proposal.originalStamp) {
                return@attempt Result.Conflict
            }
            if (repository.selectManual(ticket, proposal.verified.value.id, proposal.verified, true, now())) {
                Result.Value(Unit)
            } else {
                Result.Conflict
            }
        }
    }

    suspend fun refresh(identity: LocalAnimeIdentity, revision: Long?): Result<Unit> {
        val record = repository.get(identity.animeId)
        if (record?.revision != revision) return Result.Conflict
        val id = record?.state?.association?.id ?: return Result.Conflict
        if (!AnimeMatcher.validId(id)) return Result.Failed(MetadataFailureKind.INVALID_INPUT)
        return attempt(identity, revision) { ticket ->
            reserve(ticket)
            val result = provider.getById(id)
            if (result is MetadataResult.Failure) return@attempt failure(ticket, result)
            if (result is MetadataResult.Success &&
                (!result.complete || !result.identityComplete || result.value.id != id)
            ) {
                return@attempt failure(ticket, MetadataResult.Failure(MetadataFailureKind.CONTRACT))
            }
            if (repository.refresh(ticket, result, now())) Result.Value(Unit) else Result.Conflict
        }
    }

    private suspend fun <T> attempt(
        identity: LocalAnimeIdentity,
        revision: Long?,
        block: suspend (MetadataTicket) -> Result<T>,
    ): Result<T> {
        val control = repository.control()
        if (!control.enabled) return Result.Failed(MetadataFailureKind.DISABLED)
        if (now() < control.hostNotBefore) return Result.Failed(MetadataFailureKind.RATE_LIMITED)
        val ticket = repository.beginReview(identity, revision, now()) ?: return Result.Conflict
        try {
            return coroutineScope {
                val operationScope = this
                val watcher = launch(start = CoroutineStart.UNDISPATCHED) {
                    repository.observeControl().first { !it.enabled || it.epoch != ticket.epoch }
                    operationScope.cancel("Metadata policy changed")
                }
                try {
                    block(ticket)
                } finally {
                    watcher.cancel()
                }
            }
        } finally {
            // Closing the screen/turning off releases only this lease, never another operation's decision.
            withContext(NonCancellable) { repository.cancelAttempt(ticket) }
        }
    }

    private suspend fun reserve(ticket: MetadataTicket) {
        // The isolated provider can emit at most three times. Reserve that upper bound before entering it.
        // Physical emission accounting/shared legacy quota is the session 9 coordinator's responsibility.
        repeat(3) { check(repository.recordEmission(ticket, now())) }
    }

    private suspend fun failure(ticket: MetadataTicket, failure: MetadataResult.Failure): Result.Failed {
        repository.failAttempt(ticket, failure, now())
        return Result.Failed(failure.kind)
    }
}

/** Parse only a canonical positive ID or an official HTTPS anime link, never fetch user-supplied URLs. */
object MetadataManualInput {
    fun id(input: String): MetadataId? {
        val text = input.trim()
        val value = if (text.all(Char::isDigit)) {
            text
        } else {
            val uri = runCatching { URI(text) }.getOrNull() ?: return null
            if (uri.scheme != "https" || uri.host != "anilist.co" || uri.port != -1 ||
                uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null
            ) {
                return null
            }
            val path = uri.rawPath ?: return null
            Regex("/anime/([1-9][0-9]*)(?:/[A-Za-z0-9_-]+)?/?").matchEntire(path)?.groupValues?.get(1) ?: return null
        }
        return MetadataId("anilist", value).takeIf(AnimeMatcher::validId)
    }
}
// <-- AM (Yume manual metadata review)
