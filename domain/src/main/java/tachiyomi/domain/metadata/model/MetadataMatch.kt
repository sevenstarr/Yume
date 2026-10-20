package tachiyomi.domain.metadata.model

// AM (Yume metadata matching, 2026-10-20; new code assisted by Codex) -->
enum class MetadataScope { WORK, CHILD_SEASON, COLLECTION, UNKNOWN }

/** Year/format must come from trusted structured context, never guessed from a title/episode list. */
data class LocalAnimeIdentity(
    val animeId: Long,
    val sourceId: Long,
    val sourceUrl: String,
    val title: String,
    val year: Int? = null,
    val format: MetadataFormat? = null,
    val scope: MetadataScope = MetadataScope.UNKNOWN,
    val parentId: Long? = null,
    val seasonNumber: Int? = null,
) {
    init {
        require(title.isNotBlank() && title.length <= 500)
        require(sourceUrl.length <= 4096)
        require(year == null || year in 1..9999)
        require(seasonNumber == null || seasonNumber > 0)
    }
}

data class MetadataAssociation(val id: MetadataId, val manual: Boolean, val identityFingerprint: String)

data class MatchInput(
    val identity: LocalAnimeIdentity,
    val candidates: List<MetadataMedia>,
    val searchComplete: Boolean,
    val rejected: Set<MetadataId> = emptySet(),
    val association: MetadataAssociation? = null,
    val originalOnly: Boolean = false,
    val automaticBlocked: Boolean = false,
    val correction: Boolean = false,
)

enum class MatchSignal { TITLE, YEAR, FORMAT, EDITION }

enum class MatchVeto { INVALID_ID, REJECTED, YEAR, FORMAT, EDITION, SCOPE }

enum class MatchOutcome {
    AUTO_MATCH,
    AMBIGUOUS,
    INSUFFICIENT_EVIDENCE,
    NOT_FOUND,
    INCOMPLETE_SEARCH,
    REFRESH_EXISTING,
    REVIEW_EXISTING,
    ORIGINAL_ONLY,
}

enum class MatchReason {
    CALIBRATED,
    AUTOMATION_DISABLED,
    CLOSE_CANDIDATES,
    MISSING_CONTEXT,
    NO_ELIGIBLE_CANDIDATES,
    INCOMPLETE_SEARCH,
    EXISTING_ASSOCIATION,
    IDENTITY_CHANGED,
    ORIGINAL_ONLY,
    CORRECTION_PROPOSAL,
    AUTOMATIC_BLOCKED,
}

enum class MatchConfidence { HIGH, INTERMEDIATE, LOW }

data class RankedMetadataCandidate(
    val id: MetadataId,
    val score: Int,
    val confidence: MatchConfidence,
    val agreeing: Set<MatchSignal>,
    val missing: Set<MatchSignal>,
    val conflicting: Set<MatchSignal>,
    val vetoes: Set<MatchVeto>,
    val exactTitle: Boolean,
    val sufficient: Boolean,
)

/** Scores are ranking units, not probabilities. Null configuration always disables textual automation. */
data class MatchConfiguration(
    val version: String,
    val titleWeight: Int,
    val yearWeight: Int,
    val formatWeight: Int,
    val highThreshold: Int,
    val reviewThreshold: Int,
    val minimumMargin: Int,
    val automationApproved: Boolean = false,
) {
    init {
        require(version.isNotBlank())
        require(listOf(titleWeight, yearWeight, formatWeight, minimumMargin).all { it in 1..100 })
        require(reviewThreshold in 1..highThreshold && highThreshold <= titleWeight + yearWeight + formatWeight)
    }

    companion object {
        // Selected only on calibration fixtures; validation is held out. Review/activation remains a separate gate.
        val ProposalV1 = MatchConfiguration("s5-calibration-v1", 50, 15, 10, 70, 30, 10)
    }
}

data class MatchDecision(
    val outcome: MatchOutcome,
    val reason: MatchReason,
    val candidates: List<RankedMetadataCandidate>,
    val fingerprint: String,
    val normalizationVersion: String,
    val matcherVersion: String,
    val configurationVersion: String?,
    val retainedId: MetadataId? = null,
) {
    val automaticId: MetadataId? get() = candidates.firstOrNull()?.id.takeIf { outcome == MatchOutcome.AUTO_MATCH }
    val confidence: MatchConfidence get() = when (outcome) {
        MatchOutcome.AUTO_MATCH -> MatchConfidence.HIGH
        MatchOutcome.NOT_FOUND, MatchOutcome.ORIGINAL_ONLY -> MatchConfidence.LOW
        else -> MatchConfidence.INTERMEDIATE
    }
}
// <-- AM (Yume metadata matching)
