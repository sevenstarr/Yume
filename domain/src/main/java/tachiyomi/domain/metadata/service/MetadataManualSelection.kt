package tachiyomi.domain.metadata.service

import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MatchInput
import tachiyomi.domain.metadata.model.MetadataAssociation
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataResult

// AM (Yume metadata matching, 2026-10-20; new code assisted by Codex) -->
/** Provider validation is required before proposing a pin; persistence/revision checks belong to the repository. */
object MetadataManualSelection {
    fun propose(
        identity: LocalAnimeIdentity,
        requestedId: MetadataId,
        verified: MetadataResult<MetadataMedia>,
        differencesAcknowledged: Boolean,
    ): MetadataAssociation? {
        if (!AnimeMatcher.validId(requestedId)) return null
        val response = verified as? MetadataResult.Success ?: return null
        if (!response.identityComplete || response.value.id != requestedId) return null
        val candidate = AnimeMatcher().evaluate(MatchInput(identity, listOf(response.value), searchComplete = true))
            .candidates.single()
        if (!differencesAcknowledged &&
            (!candidate.exactTitle || candidate.vetoes.isNotEmpty() || !candidate.sufficient)
        ) {
            return null
        }
        return MetadataAssociation(requestedId, manual = true, MetadataTitleNormalizer.fingerprint(identity))
    }
}
// <-- AM (Yume metadata matching)
