package tachiyomi.domain.metadata.service

import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MetadataBackup
import tachiyomi.domain.metadata.model.MetadataCachedMedia
import tachiyomi.domain.metadata.model.MetadataControl
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataLookupKey
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataPage
import tachiyomi.domain.metadata.model.MetadataRecord
import tachiyomi.domain.metadata.model.MetadataRestoreResult
import tachiyomi.domain.metadata.model.MetadataResult
import tachiyomi.domain.metadata.model.MetadataTicket

// AM (Yume metadata persistence, 2026-10-20; new code assisted by Codex) -->
interface MetadataRepository {
    suspend fun control(): MetadataControl
    fun observeControl(): Flow<MetadataControl>
    suspend fun setEnabled(enabled: Boolean)
    suspend fun deferHost(until: Long)
    suspend fun get(animeId: Long): MetadataRecord?
    fun observe(animeIds: List<Long>): Flow<List<MetadataRecord>>
    suspend fun begin(identity: LocalAnimeIdentity, now: Long): MetadataTicket?

    /** Explicit review may retain original-only while searching; the observed revision must still match. */
    suspend fun beginReview(identity: LocalAnimeIdentity, expectedRevision: Long?, now: Long): MetadataTicket?
    suspend fun cancelAttempt(ticket: MetadataTicket): Boolean
    suspend fun recordEmission(ticket: MetadataTicket, now: Long): Boolean
    suspend fun failAttempt(ticket: MetadataTicket, failure: MetadataResult.Failure, now: Long): Boolean
    suspend fun selectManual(
        ticket: MetadataTicket,
        requestedId: MetadataId,
        verified: MetadataResult<MetadataMedia>,
        differencesAcknowledged: Boolean,
        now: Long,
    ): Boolean
    suspend fun applyMatch(
        ticket: MetadataTicket,
        candidates: List<MetadataMedia>,
        searchComplete: Boolean,
        now: Long,
    ): Boolean
    suspend fun refresh(ticket: MetadataTicket, result: MetadataResult<MetadataMedia>, now: Long): Boolean

    /** Null revision creates an original-only decision only if no decision exists; never enables enrichment. */
    suspend fun setOriginalOnly(animeId: Long, expectedRevision: Long?, originalOnly: Boolean): Boolean
    suspend fun removeAssociation(animeId: Long, expectedRevision: Long): Boolean
    suspend fun reject(animeId: Long, expectedRevision: Long, id: MetadataId): Boolean
    suspend fun readMedia(id: MetadataId, now: Long): MetadataCachedMedia?
    suspend fun readLookup(key: MetadataLookupKey, fingerprint: String, now: Long): MetadataPage?
    suspend fun cacheLookup(
        ticket: MetadataTicket,
        key: MetadataLookupKey,
        result: MetadataResult<MetadataPage>,
        now: Long,
    ): Boolean
    suspend fun clearCache()

    /** Stage external image writes here; the visual consumer must recheck epoch/revision before publication. */
    suspend fun storeImage(
        animeId: Long,
        revision: Long,
        epoch: Long,
        key: String,
        now: Long,
        write: suspend () -> Unit,
    ): Boolean
    suspend fun clearAll()
    suspend fun prune(now: Long)
    suspend fun exportDecisions(animeIds: List<Long>): MetadataBackup
    suspend fun restoreDecisions(backup: MetadataBackup): List<MetadataRestoreResult>
    suspend fun pendingRestores(): List<MetadataRestoreResult>
}
// <-- AM (Yume metadata persistence)
