package tachiyomi.domain.metadata.service

import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MatchConfidence
import tachiyomi.domain.metadata.model.MatchConfiguration
import tachiyomi.domain.metadata.model.MatchDecision
import tachiyomi.domain.metadata.model.MatchInput
import tachiyomi.domain.metadata.model.MatchOutcome
import tachiyomi.domain.metadata.model.MatchReason
import tachiyomi.domain.metadata.model.MatchSignal
import tachiyomi.domain.metadata.model.MatchVeto
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataScope
import tachiyomi.domain.metadata.model.RankedMetadataCandidate

// AM (Yume metadata matching, 2026-10-20; new code assisted by Codex) -->
/** Pure proposal engine. Callers must supply completeness across every planned search/page, not just the last page. */
class AnimeMatcher {
    fun evaluate(input: MatchInput, configuration: MatchConfiguration? = null): MatchDecision {
        require(input.candidates.size <= 80)
        require(input.candidates.all { media -> titles(media).size <= 103 && titles(media).all { it.length <= 500 } })
        val fingerprint = MetadataTitleNormalizer.fingerprint(input.identity)
        val groups = input.candidates.groupBy { it.id }
        // Conflicting copies are not resolved by arrival order or silently merged into stronger evidence.
        val complete = input.searchComplete && groups.values.all { it.distinct().size == 1 }
        val ranked = groups.values.filter { it.distinct().size == 1 }.map { rows ->
            rank(input.identity, rows.first(), input.rejected, configuration ?: MatchConfiguration.ProposalV1)
        }.sortedWith(
            compareBy<RankedMetadataCandidate> { it.vetoes.isNotEmpty() }.thenByDescending { it.score }
                .thenBy { it.id.provider }.thenBy { it.id.value },
        )
        fun decision(outcome: MatchOutcome, reason: MatchReason) = MatchDecision(
            outcome,
            reason,
            ranked,
            fingerprint,
            MetadataTitleNormalizer.VERSION,
            VERSION,
            configuration?.version,
            input.association?.id,
        )
        if (input.originalOnly) return decision(MatchOutcome.ORIGINAL_ONLY, MatchReason.ORIGINAL_ONLY)
        input.association?.let { existing ->
            if (!input.correction) {
                val changed = existing.identityFingerprint != fingerprint
                return decision(
                    if (changed && !existing.manual) MatchOutcome.REVIEW_EXISTING else MatchOutcome.REFRESH_EXISTING,
                    if (changed) MatchReason.IDENTITY_CHANGED else MatchReason.EXISTING_ASSOCIATION,
                )
            }
        }
        if (!complete) return decision(MatchOutcome.INCOMPLETE_SEARCH, MatchReason.INCOMPLETE_SEARCH)
        val policy = configuration ?: MatchConfiguration.ProposalV1
        val eligible = ranked.filter { it.vetoes.isEmpty() && it.score >= policy.reviewThreshold }
        val best = eligible.firstOrNull()
            ?: return decision(MatchOutcome.NOT_FOUND, MatchReason.NO_ELIGIBLE_CANDIDATES)
        if (!best.sufficient || best.score < policy.highThreshold) {
            return decision(MatchOutcome.INSUFFICIENT_EVIDENCE, MatchReason.MISSING_CONTEXT)
        }
        val runnerUp = eligible.getOrNull(1)
        if (runnerUp != null && best.score - runnerUp.score < policy.minimumMargin) {
            return decision(MatchOutcome.AMBIGUOUS, MatchReason.CLOSE_CANDIDATES)
        }
        if (input.correction) return decision(MatchOutcome.AMBIGUOUS, MatchReason.CORRECTION_PROPOSAL)
        if (input.automaticBlocked) return decision(MatchOutcome.AMBIGUOUS, MatchReason.AUTOMATIC_BLOCKED)
        if (configuration?.automationApproved != true) {
            return decision(MatchOutcome.AMBIGUOUS, MatchReason.AUTOMATION_DISABLED)
        }
        return decision(MatchOutcome.AUTO_MATCH, MatchReason.CALIBRATED)
    }

    private fun rank(
        local: LocalAnimeIdentity,
        remote: MetadataMedia,
        rejected: Set<MetadataId>,
        policy: MatchConfiguration,
    ): RankedMetadataCandidate {
        val title = MetadataTitleNormalizer.normalize(local.title)
        val alternatives = titles(remote).filter(String::isNotBlank).map(MetadataTitleNormalizer::normalize)
        val exact = title.comparison.isNotBlank() && alternatives.any { it.comparison == title.comparison }
        val similarity = alternatives.maxOfOrNull { candidate ->
            when {
                candidate.comparison == title.comparison -> 1.0
                candidate.compatibility == title.compatibility -> 0.9
                candidate.accentFolded == title.accentFolded -> 0.8
                else -> dice(title.comparison, candidate.comparison) * 0.7
            }
        } ?: 0.0
        val agreeing = mutableSetOf<MatchSignal>()
        val missing = mutableSetOf<MatchSignal>()
        val conflicting = mutableSetOf<MatchSignal>()
        val vetoes = mutableSetOf<MatchVeto>()
        if (!validId(remote.id)) vetoes += MatchVeto.INVALID_ID
        if (remote.id in rejected) vetoes += MatchVeto.REJECTED
        if (local.scope == MetadataScope.COLLECTION) vetoes += MatchVeto.SCOPE
        if (exact) {
            agreeing += MatchSignal.TITLE
        } else if (alternatives.isEmpty()) {
            missing += MatchSignal.TITLE
        } else {
            conflicting += MatchSignal.TITLE
        }
        fun compare(signal: MatchSignal, left: Any?, right: Any?, veto: MatchVeto) {
            when {
                left == null || right == null -> missing += signal
                left == right -> agreeing += signal
                else -> {
                    conflicting += signal
                    vetoes += veto
                }
            }
        }
        compare(MatchSignal.YEAR, local.year, remote.startDate?.year ?: remote.seasonYear, MatchVeto.YEAR)
        if (remote.startDate?.year != null && remote.seasonYear != null && remote.startDate.year != remote.seasonYear) {
            agreeing -= MatchSignal.YEAR
            conflicting += MatchSignal.YEAR
            vetoes += MatchVeto.YEAR
        }
        compare(MatchSignal.FORMAT, local.format, remote.format, MatchVeto.FORMAT)
        val localEdition = edition(title.comparison, local.seasonNumber)
        val remoteEditions = alternatives.map { edition(it.comparison, null) }.filter { it.isNotEmpty() }.distinct()
        when {
            remoteEditions.size > 1 -> {
                conflicting += MatchSignal.EDITION
                vetoes += MatchVeto.EDITION
            }
            localEdition.isEmpty() && remoteEditions.isEmpty() -> missing += MatchSignal.EDITION
            remoteEditions.singleOrNull() == localEdition -> agreeing += MatchSignal.EDITION
            localEdition.isEmpty() || remoteEditions.isEmpty() -> missing += MatchSignal.EDITION
            else -> {
                conflicting += MatchSignal.EDITION
                vetoes += MatchVeto.EDITION
            }
        }
        // An explicit remote edition must never disappear through a bare alias.
        if (localEdition.isEmpty() && remoteEditions.isNotEmpty()) vetoes += MatchVeto.EDITION
        val significantNumbers = Regex("\\b(?:\\d+|ii|iii|iv|v|vi)\\b")
        val localNumbers = significantNumbers.findAll(title.comparison).map { it.value }.toSet()
        val remoteNumbers = alternatives.map { candidate ->
            significantNumbers.findAll(candidate.comparison).map { it.value }.toSet()
        }
        // Numbers remain title discriminators, without being guessed to mean a season.
        if (remoteNumbers.any { it.isNotEmpty() && it != localNumbers } ||
            (!exact && localNumbers.isNotEmpty() && remoteNumbers.none { it == localNumbers })
        ) {
            agreeing -= MatchSignal.EDITION
            missing -= MatchSignal.EDITION
            conflicting += MatchSignal.EDITION
            vetoes += MatchVeto.EDITION
        }
        val compact = title.comparison.replace(" ", "")
        val acronym = local.title.trim().let {
            it.length <= 6 && it.any(Char::isUpperCase) && it.none(Char::isLowerCase)
        }
        val sufficient = exact && MatchSignal.YEAR in agreeing && MatchSignal.FORMAT in agreeing &&
            local.scope in setOf(MetadataScope.WORK, MetadataScope.CHILD_SEASON) &&
            (local.scope != MetadataScope.CHILD_SEASON || local.seasonNumber != null) &&
            (localEdition.isEmpty() || MatchSignal.EDITION in agreeing) &&
            compact.codePointCount(0, compact.length) > 3 && !acronym
        val score = (similarity * policy.titleWeight).toInt() +
            (if (MatchSignal.YEAR in agreeing) policy.yearWeight else 0) +
            (if (MatchSignal.FORMAT in agreeing) policy.formatWeight else 0)
        val confidence = when {
            vetoes.isNotEmpty() || score < policy.reviewThreshold -> MatchConfidence.LOW
            sufficient && score >= policy.highThreshold -> MatchConfidence.HIGH
            else -> MatchConfidence.INTERMEDIATE
        }
        return RankedMetadataCandidate(
            remote.id, score, confidence, agreeing, missing, conflicting, vetoes, exact, sufficient,
        )
    }

    private fun titles(media: MetadataMedia) =
        listOfNotNull(media.titles.romaji, media.titles.english, media.titles.native) + media.synonyms

    private fun dice(left: String, right: String): Double {
        val a = left.split(' ').filter(String::isNotBlank).toSet()
        val b = right.split(' ').filter(String::isNotBlank).toSet()
        return if (a.isEmpty() || b.isEmpty()) 0.0 else 2.0 * a.intersect(b).size / (a.size + b.size)
    }

    private fun edition(title: String, season: Int?): Set<String> = buildSet {
        if (season != null) add("season:$season")
        Regex("\\b(?:season|temporada) (\\d+)\\b|\\b(\\d+)(?:st|nd|rd|th) season\\b|\\b(\\d+) temporada\\b")
            .findAll(title).forEach { match ->
                add("season:${match.groupValues.drop(1).first { it.isNotEmpty() }.trimStart('0').ifEmpty { "0" }}")
            }
        Regex("\\b(part|parte|cour) (\\d+|ii|iii|iv)\\b").findAll(title)
            .forEach { add("${if (it.groupValues[1] == "parte") "part" else it.groupValues[1]}:${it.groupValues[2]}") }
        Regex("\\b(movie|filme|ova|special|especial|recap|remake|final|compilation)\\b").findAll(title)
            .forEach {
                add(
                    when (it.value) {
                        "filme" -> "movie"
                        "especial" -> "special"
                        else -> it.value
                    },
                )
            }
        Regex("\\b(ii|iii|iv|v|vi)$").findAll(title).forEach { add("numeral:${it.value}") }
    }

    companion object {
        const val VERSION = "matcher-v1"

        fun validId(id: MetadataId): Boolean = id.provider == "anilist" &&
            id.value.toIntOrNull()?.let { it > 0 && it.toString() == id.value } == true
    }
}
// <-- AM (Yume metadata matching)
