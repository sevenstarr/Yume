package tachiyomi.data.metadata

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import tachiyomi.data.Anime_metadata
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.data.subscribeToOneOrNull
import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MatchInput
import tachiyomi.domain.metadata.model.MetadataAssociation
import tachiyomi.domain.metadata.model.MetadataBackup
import tachiyomi.domain.metadata.model.MetadataBackupDecision
import tachiyomi.domain.metadata.model.MetadataCachePolicy
import tachiyomi.domain.metadata.model.MetadataCachedMedia
import tachiyomi.domain.metadata.model.MetadataControl
import tachiyomi.domain.metadata.model.MetadataDecisionState
import tachiyomi.domain.metadata.model.MetadataFailureKind
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataLookupKey
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataPage
import tachiyomi.domain.metadata.model.MetadataRecord
import tachiyomi.domain.metadata.model.MetadataRestoreOutcome
import tachiyomi.domain.metadata.model.MetadataRestoreResult
import tachiyomi.domain.metadata.model.MetadataResult
import tachiyomi.domain.metadata.model.MetadataTicket
import tachiyomi.domain.metadata.service.AnimeMatcher
import tachiyomi.domain.metadata.service.MetadataImageCache
import tachiyomi.domain.metadata.service.MetadataManualSelection
import tachiyomi.domain.metadata.service.MetadataRepository
import tachiyomi.domain.metadata.service.MetadataTitleNormalizer
import java.security.MessageDigest
import java.util.UUID

// AM (Yume metadata persistence, 2026-10-20; new code assisted by Codex) -->
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class MetadataRepositoryImpl(
    private val database: Database,
    private val imageCache: MetadataImageCache,
) : MetadataRepository {
    private val queries get() = database.metadataQueries
    private val json = Json { encodeDefaults = true }
    private val imageWrites = Semaphore(8)
    private val imageBarrier = Mutex()

    override suspend fun control(): MetadataControl {
        queries.initialize()
        return queries.control().awaitAsOne().let { MetadataControl(it.enabled == 1L, it.epoch, it.host_not_before) }
    }

    override fun observeControl(): Flow<MetadataControl> = queries.control().subscribeToOneOrNull().map {
        it?.let { MetadataControl(it.enabled == 1L, it.epoch, it.host_not_before) } ?: MetadataControl(false, 0, 0)
    }

    override suspend fun setEnabled(enabled: Boolean) = database.transaction {
        queries.initialize()
        queries.setEnabled(if (enabled) 1 else 0)
        invalidateSearching()
        queries.deleteAllWork()
    }

    override suspend fun deferHost(until: Long) {
        require(until >= 0)
        queries.initialize()
        queries.deferHost(until)
    }

    override suspend fun get(animeId: Long): MetadataRecord? = queries.get(animeId).awaitAsOneOrNull()?.record()

    override fun observe(animeIds: List<Long>): Flow<List<MetadataRecord>> =
        queries.getBatch(animeIds.distinct()).subscribeToList().map { rows -> rows.map { it.record() } }

    override suspend fun begin(
        identity: LocalAnimeIdentity,
        now: Long,
    ): MetadataTicket? = beginInternal(identity, now, false, null)

    override suspend fun beginReview(
        identity: LocalAnimeIdentity,
        expectedRevision: Long?,
        now: Long,
    ): MetadataTicket? = beginInternal(identity, now, true, expectedRevision)

    private suspend fun beginInternal(
        identity: LocalAnimeIdentity,
        now: Long,
        manualReview: Boolean,
        expectedRevision: Long?,
    ): MetadataTicket? = database.transactionWithResult {
        require(now >= 0 && now <= Long.MAX_VALUE - MetadataCachePolicy.RETAIN)
        val control = control()
        if (!control.enabled || now < control.hostNotBefore) return@transactionWithResult null
        val original = queries.original(identity.animeId).awaitAsOneOrNull() ?: return@transactionWithResult null
        if (original.source != identity.sourceId || original.url != identity.sourceUrl ||
            original.title != identity.title || original.parent_id != identity.parentId
        ) {
            return@transactionWithResult null
        }
        val work = queries.work(identity.animeId).awaitAsOneOrNull()
        if (work != null && (work.lease_until > now || (!manualReview && work.emissions >= 3))) {
            return@transactionWithResult null
        }
        if (manualReview && get(identity.animeId)?.revision != expectedRevision) return@transactionWithResult null
        queries.insertDecision(identity.animeId, encode(MetadataDecisionState(identity)))
        val row = checkNotNull(get(identity.animeId))
        if (row.state.originalOnly && !manualReview) return@transactionWithResult null
        val previousOutcome = row.state.outcome.takeUnless { it == "SEARCHING" } ?: when {
            row.state.originalOnly -> "ORIGINAL_ONLY"
            row.state.association != null -> "ASSOCIATED"
            else -> "PENDING"
        }
        val fingerprint = MetadataTitleNormalizer.fingerprint(identity)
        val changed = row.state.association?.let { !it.manual && it.identityFingerprint != fingerprint } == true
        write(
            row,
            row.state.copy(
                identity = identity,
                lastAttemptAt = now,
                outcome = "SEARCHING",
                needsReview = changed,
            ),
        )
        val updated = checkNotNull(get(identity.animeId))
        val leaseUntil = now + MetadataCachePolicy.LEASE
        val leaseId = UUID.randomUUID().toString()
        queries.lease(
            identity.animeId,
            control.epoch,
            leaseUntil,
            leaseId,
            if (manualReview) {
                0
            } else {
                work?.emissions
                    ?: 0
            },
        )
        MetadataTicket(
            identity.animeId,
            updated.revision,
            updated.generation,
            control.epoch,
            fingerprint,
            originalStamp(identity.animeId)!!,
            leaseUntil,
            leaseId,
            manualReview,
            previousOutcome,
        )
    }

    override suspend fun cancelAttempt(ticket: MetadataTicket): Boolean = database.transactionWithResult {
        val row = get(ticket.animeId) ?: return@transactionWithResult false
        val work = queries.work(ticket.animeId).awaitAsOneOrNull() ?: return@transactionWithResult false
        if (row.revision != ticket.revision || work.lease_id != ticket.leaseId) return@transactionWithResult false
        write(row, row.state.copy(outcome = ticket.previousOutcome))
        queries.deleteWork(ticket.animeId)
        true
    }

    override suspend fun recordEmission(ticket: MetadataTicket, now: Long): Boolean = database.transactionWithResult {
        if (valid(ticket, now) == null || now < control().hostNotBefore) return@transactionWithResult false
        if (queries.work(ticket.animeId).awaitAsOne().emissions >= 3) return@transactionWithResult false
        queries.emit(ticket.animeId)
        true
    }

    override suspend fun failAttempt(
        ticket: MetadataTicket,
        failure: MetadataResult.Failure,
        now: Long,
    ): Boolean = database.transactionWithResult {
        val row = valid(ticket, now) ?: return@transactionWithResult false
        val retryAt = failure.retryAtMillis
        if (failure.kind == MetadataFailureKind.RATE_LIMITED) {
            deferHost(maxOf(retryAt ?: 0, now + 60_000))
        } else if (retryAt != null) {
            deferHost(retryAt)
        }
        write(row, row.state.copy(failure = failure.kind, outcome = "TRANSIENT_ERROR"))
        queries.deleteWork(ticket.animeId)
        true
    }

    override suspend fun selectManual(
        ticket: MetadataTicket,
        requestedId: MetadataId,
        verified: MetadataResult<MetadataMedia>,
        differencesAcknowledged: Boolean,
        now: Long,
    ): Boolean = database.transactionWithResult {
        val row = valid(ticket, now) ?: return@transactionWithResult false
        val pin = MetadataManualSelection.propose(row.state.identity, requestedId, verified, differencesAcknowledged)
            ?: return@transactionWithResult false
        val response = verified as MetadataResult.Success
        if (response.complete) putMedia(response.value, now)
        write(
            row,
            row.state.copy(
                association = pin,
                originalOnly = false,
                needsReview = false,
                requiresRevalidation = false,
                rejected = row.state.rejected - requestedId,
                outcome = "ASSOCIATED",
                failure = null,
                lastCheckedAt = now.takeIf { response.complete } ?: row.state.lastCheckedAt,
            ),
        )
        queries.deleteWork(ticket.animeId)
        true
    }

    override suspend fun applyMatch(
        ticket: MetadataTicket,
        candidates: List<MetadataMedia>,
        searchComplete: Boolean,
        now: Long,
    ): Boolean = database.transactionWithResult {
        val row = valid(ticket, now) ?: return@transactionWithResult false
        require(candidates.size <= 50)
        // Gate remains closed: callers cannot enable textual automation by supplying a score/configuration.
        val decision = AnimeMatcher().evaluate(
            MatchInput(
                row.state.identity,
                candidates,
                searchComplete,
                row.state.rejected,
                row.state.association,
                row.state.originalOnly,
                row.state.automaticBlocked,
            ),
        )
        val evidence = listOf(
            decision.reason.name,
            decision.normalizationVersion,
            decision.matcherVersion,
            decision.configurationVersion.orEmpty(),
        ).joinToString("|")
        write(row, row.state.copy(outcome = decision.outcome.name, evidence = evidence, failure = null))
        queries.deleteWork(ticket.animeId)
        true
    }

    override suspend fun refresh(
        ticket: MetadataTicket,
        result: MetadataResult<MetadataMedia>,
        now: Long,
    ): Boolean = database.transactionWithResult {
        val row = valid(ticket, now) ?: return@transactionWithResult false
        if (result is MetadataResult.Failure) return@transactionWithResult failAttempt(ticket, result, now)
        val association = row.state.association ?: return@transactionWithResult false
        when (result) {
            is MetadataResult.Success -> {
                if (!result.complete || !result.identityComplete || result.value.id != association.id) {
                    return@transactionWithResult false
                }
                putMedia(result.value, now)
                write(
                    row,
                    row.state.copy(
                        lastCheckedAt = now,
                        failure = null,
                        outcome = "ASSOCIATED",
                        requiresRevalidation = false,
                        needsReview = !association.manual && association.identityFingerprint != ticket.fingerprint,
                    ),
                )
            }
            is MetadataResult.Failure -> error("Failure handled above")
            MetadataResult.NotFound -> write(row, row.state.copy(outcome = "UNAVAILABLE"))
        }
        queries.deleteWork(ticket.animeId)
        true
    }

    override suspend fun setOriginalOnly(animeId: Long, expectedRevision: Long?, originalOnly: Boolean): Boolean {
        if (expectedRevision != null) return change(animeId, expectedRevision) { it.copy(originalOnly = originalOnly) }
        return database.transactionWithResult {
            if (!originalOnly || get(animeId) != null) return@transactionWithResult false
            val original = queries.original(animeId).awaitAsOneOrNull() ?: return@transactionWithResult false
            if (original.title.isBlank() || original.title.length > 500 || original.url.length > 4096) {
                return@transactionWithResult false
            }
            val identity = LocalAnimeIdentity(
                animeId,
                original.source,
                original.url,
                original.title,
                parentId = original.parent_id,
            )
            queries.insertDecision(animeId, encode(MetadataDecisionState(identity)))
            change(animeId, 0) { it.copy(originalOnly = true) }
        }
    }

    override suspend fun removeAssociation(animeId: Long, expectedRevision: Long): Boolean =
        change(animeId, expectedRevision) {
            it.copy(association = null, originalOnly = true, automaticBlocked = true, outcome = "PENDING")
        }

    override suspend fun reject(animeId: Long, expectedRevision: Long, id: MetadataId): Boolean {
        require(AnimeMatcher.validId(id))
        return change(animeId, expectedRevision) {
            require(it.rejected.size < 100 || id in it.rejected)
            it.copy(rejected = it.rejected + id)
        }
    }

    private suspend fun change(
        animeId: Long,
        revision: Long,
        transform: (MetadataDecisionState) -> MetadataDecisionState,
    ): Boolean = database.transactionWithResult {
        val row = get(animeId)?.takeIf { it.revision == revision } ?: return@transactionWithResult false
        val state = transform(row.state)
        val outcome = when {
            state.originalOnly -> "ORIGINAL_ONLY"
            state.association != null -> "ASSOCIATED"
            else -> "PENDING"
        }
        write(row, state.copy(outcome = outcome))
        queries.deleteWork(animeId)
        true
    }

    override suspend fun readMedia(id: MetadataId, now: Long): MetadataCachedMedia? {
        queries.pruneMedia(now - MetadataCachePolicy.RETAIN, now)
        val cached = queries.media(id.provider, id.value).awaitAsOneOrNull() ?: return null
        val age = now - cached.fetched_at
        if (now < cached.fetched_at || age >= MetadataCachePolicy.RETAIN) return null
        return MetadataCachedMedia(
            json.decodeFromString(cached.payload),
            cached.fetched_at,
            age < MetadataCachePolicy.FRESH,
        )
    }

    override suspend fun readLookup(key: MetadataLookupKey, fingerprint: String, now: Long): MetadataPage? =
        database.transactionWithResult {
            queries.pruneLookup(now - MetadataCachePolicy.LOOKUP, now)
            val hash = lookupHash(key)
            val cached = queries.lookup(hash).awaitAsOneOrNull() ?: return@transactionWithResult null
            if (cached.fingerprint != fingerprint || now < cached.fetched_at ||
                now - cached.fetched_at >= MetadataCachePolicy.LOOKUP
            ) {
                return@transactionWithResult null
            }
            queries.touchLookup(now, hash)
            json.decodeFromString<MetadataPage>(cached.payload)
        }

    override suspend fun cacheLookup(
        ticket: MetadataTicket,
        key: MetadataLookupKey,
        result: MetadataResult<MetadataPage>,
        now: Long,
    ): Boolean = database.transactionWithResult {
        if (valid(ticket, now) == null) return@transactionWithResult false
        val response = result as? MetadataResult.Success ?: return@transactionWithResult false
        val page = response.value
        if (!response.complete || !response.identityComplete || page.media.size > 50 || page.page != key.page ||
            (page.media.isEmpty() && (page.page != 1 || page.hasNextPage != false))
        ) {
            return@transactionWithResult false
        }
        require(page.media.all { it.id.provider == key.provider && AnimeMatcher.validId(it.id) })
        queries.putLookup(lookupHash(key), ticket.fingerprint, encode(page), now)
        queries.limitLookup()
        true
    }

    override suspend fun clearCache() {
        database.transaction {
            queries.initialize()
            queries.advanceEpoch()
            invalidateSearching()
            queries.deleteAllWork()
            queries.clearMedia()
            queries.clearLookup()
        }
        evictImages()
    }

    override suspend fun clearAll() {
        database.transaction {
            queries.initialize()
            queries.setEnabled(0)
            queries.clearDecisions()
            queries.clearMedia()
            queries.clearLookup()
            queries.clearPending()
        }
        evictImages()
    }

    override suspend fun prune(now: Long) {
        database.transaction {
            queries.pruneMedia(now - MetadataCachePolicy.RETAIN, now)
            queries.pruneLookup(now - MetadataCachePolicy.LOOKUP, now)
            queries.limitLookup()
        }
        withImageBarrier {
            val keys = queries.expiredImages(now - MetadataCachePolicy.RETAIN, now).awaitAsList().toSet()
            imageCache.remove(keys)
            queries.deleteImageKeys(keys.toList())
        }
    }

    override suspend fun storeImage(
        animeId: Long,
        revision: Long,
        epoch: Long,
        key: String,
        now: Long,
        write: suspend () -> Unit,
    ): Boolean = imageWrites.withPermit {
        require(key.matches(Regex("yume-metadata-v1:anilist:[1-9][0-9]*:[a-f0-9]{64}:[0-9]+")))
        suspend fun eligible(): Boolean = database.transactionWithResult {
            val control = control()
            val row = get(animeId) ?: return@transactionWithResult false
            val association = row.state.association ?: return@transactionWithResult false
            val id = association.id
            val original = queries.original(animeId).awaitAsOneOrNull() ?: return@transactionWithResult false
            val identity = row.state.identity
            if (original.source != identity.sourceId || original.url != identity.sourceUrl ||
                (
                    !association.manual &&
                        (original.title != identity.title || original.parent_id != identity.parentId)
                    )
            ) {
                return@transactionWithResult false
            }
            control.enabled && control.epoch == epoch && row.revision == revision &&
                !row.state.originalOnly && !row.state.needsReview && !row.state.requiresRevalidation &&
                key.startsWith("yume-metadata-v1:${id.provider}:${id.value}:") && readMedia(id, now) != null
        }
        if (!eligible()) return@withPermit false
        // Register before I/O: crash recovery/clear can still remove partially written external assets.
        queries.registerImage(key, now)
        var published = false
        try {
            write()
            published = eligible()
            published
        } finally {
            if (!published) {
                withContext(NonCancellable) {
                    imageCache.remove(setOf(key))
                    queries.deleteImageKeys(listOf(key))
                }
            }
        }
    }

    private suspend fun evictImages() = withImageBarrier {
        val keys = queries.imageKeys().awaitAsList().toSet()
        imageCache.remove(keys)
        queries.deleteImageKeys(keys.toList())
    }

    /** Concurrent image loads, with a finite barrier that drains every writer before clear completes. */
    private suspend fun <T> withImageBarrier(block: suspend () -> T): T = imageBarrier.withLock {
        var acquired = 0
        try {
            repeat(8) {
                imageWrites.acquire()
                acquired++
            }
            block()
        } finally {
            repeat(acquired) { imageWrites.release() }
        }
    }

    private suspend fun invalidateSearching() {
        queries.allDecisions().awaitAsList().map { it.record() }
            .filter { it.state.outcome == "SEARCHING" }.forEach { row ->
                val outcome = if (row.state.association != null) "ASSOCIATED" else "PENDING"
                write(row, row.state.copy(outcome = outcome))
            }
    }

    override suspend fun exportDecisions(animeIds: List<Long>): MetadataBackup = database.transactionWithResult {
        MetadataBackup(
            decisions = queries.getBatch(animeIds.distinct()).awaitAsList().mapNotNull { row ->
                val state = row.record().state
                val original = queries.original(row.anime_id).awaitAsOneOrNull() ?: return@mapNotNull null
                val parent = original.parent_id?.let { queries.original(it).awaitAsOneOrNull() }
                MetadataBackupDecision(
                    original.source, original.url, parent?.source, parent?.url, original.season_number,
                    state.association?.id?.provider, state.association?.id?.value,
                    state.association?.manual ?: false, state.originalOnly, state.automaticBlocked,
                    state.rejected.toList(),
                )
            },
        )
    }

    override suspend fun restoreDecisions(backup: MetadataBackup): List<MetadataRestoreResult> =
        database.transactionWithResult {
            require(backup.decisions.size <= 10_000)
            // Conflicting duplicates in the backup must not make the first pin win.
            val grouped = backup.decisions.groupBy {
                listOf(
                    it.source,
                    it.url,
                    it.parentSource,
                    it.parentUrl,
                    it.seasonNumber,
                )
            }
            backup.decisions.distinct().map { decision ->
                val outcome = when {
                    backup.version != 1 || !validBackup(decision) -> MetadataRestoreOutcome.INVALID
                    grouped.getValue(
                        listOf(
                            decision.source,
                            decision.url,
                            decision.parentSource,
                            decision.parentUrl,
                            decision.seasonNumber,
                        ),

                    ).distinct().size > 1 -> MetadataRestoreOutcome.CONFLICT
                    decision.provider != null && decision.provider != "anilist" -> MetadataRestoreOutcome.INACTIVE
                    else -> restoreOne(decision)
                }
                if (outcome != MetadataRestoreOutcome.APPLIED) {
                    queries.putPending(encode(decision), outcome.name)
                }
                MetadataRestoreResult(decision, outcome)
            }
        }

    override suspend fun pendingRestores(): List<MetadataRestoreResult> = queries.pending().awaitAsList().map {
        MetadataRestoreResult(json.decodeFromString(it.payload), MetadataRestoreOutcome.valueOf(it.outcome))
    }

    private suspend fun restoreOne(decision: MetadataBackupDecision): MetadataRestoreOutcome {
        val targets = queries.restoreTargets(decision.source, decision.url).awaitAsList().filter {
            it.season_number == decision.seasonNumber &&
                it.parent_source == decision.parentSource && it.parent_url == decision.parentUrl
        }
        if (targets.size != 1) return MetadataRestoreOutcome.UNMAPPED
        val target = targets.single()
        if (target.title.isBlank() || target.title.length > 500) return MetadataRestoreOutcome.INVALID
        if (get(target._id) != null) return MetadataRestoreOutcome.CONFLICT
        val identity = LocalAnimeIdentity(
            target._id,
            target.source,
            target.url,
            target.title,
            parentId = target.parent_id,
        )
        val association = decision.provider?.let {
            MetadataAssociation(
                MetadataId(
                    it,
                    decision.remoteId!!,
                ),
                decision.manual,
                MetadataTitleNormalizer.fingerprint(identity),
            )
        }
        queries.insertDecision(
            target._id,
            encode(
                MetadataDecisionState(
                    identity,
                    association,
                    decision.originalOnly,
                    decision.automaticBlocked,
                    decision.rejected.toSet(),
                    outcome = "RESTORED_UNVERIFIED",
                    needsReview = true,
                    requiresRevalidation = association != null,
                ),
            ),
        )
        return MetadataRestoreOutcome.APPLIED
    }

    private fun validBackup(value: MetadataBackupDecision): Boolean =
        value.url.isNotBlank() && value.url.length <= 4096 &&
            (value.parentSource == null) == (value.parentUrl == null) &&
            (value.parentUrl?.length ?: 0) <= 4096 && value.seasonNumber.isFinite() &&
            (value.provider == null) == (value.remoteId == null) &&
            (!value.manual || value.provider != null) && value.rejected.size <= 100 &&
            (value.provider?.length ?: 0) <= 64 && (value.remoteId?.length ?: 0) <= 128 &&
            (
                value.provider != "anilist" ||
                    AnimeMatcher.validId(MetadataId(value.provider!!, value.remoteId!!))
                ) &&
            value.rejected.all { it.provider.length <= 64 && it.value.length <= 128 }

    private suspend fun valid(ticket: MetadataTicket, now: Long): MetadataRecord? {
        val control = control()
        if (!control.enabled || control.epoch != ticket.epoch || now < 0 || now >= ticket.leaseUntil) return null
        val row = get(ticket.animeId) ?: return null
        val work = queries.work(ticket.animeId).awaitAsOneOrNull() ?: return null
        return row.takeIf {
            it.revision == ticket.revision && it.generation == ticket.generation &&
                work.epoch == ticket.epoch && work.lease_until == ticket.leaseUntil &&
                work.lease_id == ticket.leaseId &&
                MetadataTitleNormalizer.fingerprint(it.state.identity) == ticket.fingerprint &&
                originalStamp(ticket.animeId) == ticket.originalStamp && (!it.state.originalOnly || ticket.manualReview)
        }
    }

    private suspend fun originalStamp(animeId: Long): String? = queries.original(animeId).awaitAsOneOrNull()?.let {
        hash(
            json.encodeToString(
                listOf(
                    it.source.toString(),
                    it.url,
                    it.title,
                    it.parent_id.toString(),
                    it.season_number.toString(),
                    it.fetch_type.toString(),
                ),
            ),
        )
    }

    private suspend fun write(row: MetadataRecord, state: MetadataDecisionState) {
        queries.writeDecision(encode(state), state.identity.animeId, row.revision)
    }

    private suspend fun putMedia(media: MetadataMedia, now: Long) {
        require(AnimeMatcher.validId(media.id))
        queries.putMedia(media.id.provider, media.id.value, encode(media), now)
    }

    private fun Anime_metadata.record() = MetadataRecord(json.decodeFromString(payload), revision, generation)

    private inline fun <reified T> encode(value: T): String = json.encodeToString(value).also {
        require(it.length <= MetadataCachePolicy.MAX_PAYLOAD) { "Metadata payload exceeds limit" }
    }

    private fun lookupHash(key: MetadataLookupKey): String {
        require(key.provider == "anilist" && key.query.length in 1..500 && key.page in 1..20)
        require(key.locale.length <= 64 && key.filters.length <= 1024)
        require(key.normalizationVersion.length <= 64 && key.matcherVersion.length <= 64)
        return hash(encode(key))
    }

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
// <-- AM (Yume metadata persistence)
