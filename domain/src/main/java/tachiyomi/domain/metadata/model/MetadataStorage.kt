@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package tachiyomi.domain.metadata.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber

// AM (Yume metadata persistence, 2026-10-20; new code assisted by Codex) -->
@Serializable
data class MetadataDecisionState(
    val identity: LocalAnimeIdentity,
    val association: MetadataAssociation? = null,
    val originalOnly: Boolean = false,
    val automaticBlocked: Boolean = false,
    val rejected: Set<MetadataId> = emptySet(),
    val outcome: String = "PENDING",
    val evidence: String? = null,
    val lastCheckedAt: Long? = null,
    val lastAttemptAt: Long? = null,
    val failure: MetadataFailureKind? = null,
    val needsReview: Boolean = false,
    val requiresRevalidation: Boolean = false,
    val schemaVersion: Int = 1,
)

data class MetadataRecord(val state: MetadataDecisionState, val revision: Long, val generation: Long)

data class MetadataControl(val enabled: Boolean, val epoch: Long, val hostNotBefore: Long)

/** A lease authorizes conditional local writes; it is not permission to issue HTTP. */
data class MetadataTicket(
    val animeId: Long,
    val revision: Long,
    val generation: Long,
    val epoch: Long,
    val fingerprint: String,
    val originalStamp: String,
    val leaseUntil: Long,
    val leaseId: String,
    val manualReview: Boolean = false,
    val previousOutcome: String = "PENDING",
    val syncGeneration: Long? = null,
)

@Serializable
data class MetadataLookupKey(
    val query: String,
    val page: Int = 1,
    val locale: String = "und",
    val filters: String = "",
    val provider: String = "anilist",
    val normalizationVersion: String = "title-nfc-v1",
    val matcherVersion: String = "matcher-v1",
)

data class MetadataCachedMedia(val media: MetadataMedia, val fetchedAt: Long, val fresh: Boolean)

/** Decisions only: deliberately excludes local IDs, catalog payload, URLs, errors and operational state. */
@Serializable
data class MetadataBackupDecision(
    @ProtoNumber(1) val source: Long,
    @ProtoNumber(2) val url: String,
    @ProtoNumber(3) val parentSource: Long? = null,
    @ProtoNumber(4) val parentUrl: String? = null,
    @ProtoNumber(5) val seasonNumber: Double = -1.0,
    @ProtoNumber(6) val provider: String? = null,
    @ProtoNumber(7) val remoteId: String? = null,
    @ProtoNumber(8) val manual: Boolean = false,
    @ProtoNumber(9) val originalOnly: Boolean = false,
    @ProtoNumber(10) val automaticBlocked: Boolean = false,
    @ProtoNumber(11) val rejected: List<MetadataId> = emptyList(),
)

@Serializable
data class MetadataBackup(
    @ProtoNumber(1) val version: Int = 1,
    @ProtoNumber(2) val decisions: List<MetadataBackupDecision> = emptyList(),
)

enum class MetadataRestoreOutcome { APPLIED, CONFLICT, UNMAPPED, INACTIVE, INVALID }

/** Pending/conflicting decisions are retained locally for later review, never attached arbitrarily. */
data class MetadataRestoreResult(val decision: MetadataBackupDecision, val outcome: MetadataRestoreOutcome)

object MetadataCachePolicy {
    const val DAY = 86_400_000L
    const val FRESH = 7 * DAY
    const val RETAIN = 30 * DAY
    const val LOOKUP = DAY
    const val LEASE = 60_000L
    const val MAX_LOOKUPS = 200L
    const val MAX_PAYLOAD = 65_536
}
// <-- AM (Yume metadata persistence)
