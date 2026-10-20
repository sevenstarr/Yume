package tachiyomi.domain.metadata

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import tachiyomi.domain.metadata.model.MatchConfiguration
import tachiyomi.domain.metadata.model.MatchOutcome
import tachiyomi.domain.metadata.model.MatchReason
import tachiyomi.domain.metadata.model.MatchSignal
import tachiyomi.domain.metadata.model.MatchVeto
import tachiyomi.domain.metadata.model.MetadataAssociation
import tachiyomi.domain.metadata.model.MetadataDate
import tachiyomi.domain.metadata.model.MetadataFailureKind
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataResult
import tachiyomi.domain.metadata.model.MetadataSeason
import tachiyomi.domain.metadata.service.AnimeMatcher
import tachiyomi.domain.metadata.service.MetadataManualSelection
import tachiyomi.domain.metadata.service.MetadataTitleNormalizer

// AM (Yume metadata matching tests, 2026-10-20; new code assisted by Codex) -->
class AnimeMatcherTest {
    private val matcher = AnimeMatcher()
    private val policy = MatchConfiguration.ProposalV1.copy(automationApproved = true)
    private val example get() = MetadataMatchFixtures.read("calibration").first().input

    @TestFactory
    fun `held out labels and permutation determinism`() = MetadataMatchFixtures.read("validation").map { fixture ->
        dynamicTest(fixture.name) {
            val result = matcher.evaluate(fixture.input, policy)
            result.automaticId shouldBe fixture.expectedId
            matcher.evaluate(fixture.input.copy(candidates = fixture.input.candidates.reversed()), policy) shouldBe
                result
        }
    }

    @Test
    fun `automation is disabled without reviewed configuration`() {
        matcher.evaluate(example).reason shouldBe MatchReason.AUTOMATION_DISABLED
        matcher.evaluate(example, MatchConfiguration.ProposalV1).automaticId shouldBe null
    }

    @Test
    fun `unknown data is missing rather than agreeing and launch season is not narrative season`() {
        val input = example.copy(
            candidates = listOf(
                example.candidates.first().copy(
                    startDate = null,
                    seasonYear = null,
                    format = null,
                    season = MetadataSeason.SUMMER,
                ),
            ),
        )
        val result = matcher.evaluate(input, policy)
        result.outcome shouldBe MatchOutcome.INSUFFICIENT_EVIDENCE
        result.candidates.single().missing.containsAll(listOf(MatchSignal.YEAR, MatchSignal.FORMAT)) shouldBe true
        result.candidates.single().agreeing shouldBe setOf(MatchSignal.TITLE)
    }

    @Test
    fun `conflicting duplicate rows fail closed identical duplicates deduplicate`() {
        val one = example.candidates.first()
        matcher.evaluate(example.copy(candidates = listOf(one, one)), policy).automaticId shouldBe one.id
        val conflict = example.copy(candidates = listOf(one, one.copy(startDate = MetadataDate(2000, null, null))))
        matcher.evaluate(conflict, policy).outcome shouldBe MatchOutcome.INCOMPLETE_SEARCH
        matcher.evaluate(conflict.copy(candidates = conflict.candidates.reversed()), policy) shouldBe
            matcher.evaluate(conflict, policy)
    }

    @Test
    fun `incomplete empty is never a negative and complete empty is not found`() {
        matcher.evaluate(example.copy(candidates = emptyList(), searchComplete = false), policy).outcome shouldBe
            MatchOutcome.INCOMPLETE_SEARCH
        matcher.evaluate(example.copy(candidates = emptyList()), policy).outcome shouldBe MatchOutcome.NOT_FOUND
    }

    @Test
    fun `numbered official title cannot hide behind unnumbered alias`() {
        val input = example
        val remote = input.candidates.first().copy(synonyms = listOf("Hikari no Tabi 2"))
        matcher.evaluate(input.copy(candidates = listOf(remote)), policy).automaticId shouldBe null
    }

    @Test
    fun `conflicting remote years cannot be counted as agreement`() {
        val input = example
        val remote = input.candidates.first().copy(seasonYear = 2000)
        val result = matcher.evaluate(input.copy(candidates = listOf(remote)), policy)
        result.candidates.single().vetoes shouldBe setOf(MatchVeto.YEAR)
        (MatchSignal.YEAR in result.candidates.single().agreeing) shouldBe false
    }

    @Test
    fun `no shared title evidence produces low confidence rather than a suggestion from context alone`() {
        val input = example.copy(identity = example.identity.copy(title = "A Completely Different Name"))
        matcher.evaluate(input, policy).outcome shouldBe MatchOutcome.NOT_FOUND
    }

    @TestFactory
    fun `accepted identities are retained through refresh correction and material changes`() =
        listOf(true, false).flatMap { manual ->
            listOf(true, false).flatMap { changed ->
                listOf(true, false).map { correction ->
                    dynamicTest("manual=$manual changed=$changed correction=$correction") {
                        val input = example
                        val binding = MetadataAssociation(
                            MetadataId("anilist", "999"),
                            manual,
                            if (changed) "old" else MetadataTitleNormalizer.fingerprint(input.identity),
                        )
                        val result = matcher.evaluate(
                            input.copy(association = binding, correction = correction),
                            policy,
                        )
                        result.retainedId shouldBe binding.id
                        result.automaticId shouldBe null
                        result.outcome shouldBe when {
                            correction -> MatchOutcome.AMBIGUOUS
                            changed && !manual -> MatchOutcome.REVIEW_EXISTING
                            else -> MatchOutcome.REFRESH_EXISTING
                        }
                    }
                }
            }
        }

    @Test
    fun `original only and removal block have priority`() {
        matcher.evaluate(example.copy(originalOnly = true), policy).outcome shouldBe MatchOutcome.ORIGINAL_ONLY
        matcher.evaluate(example.copy(automaticBlocked = true), policy).reason shouldBe MatchReason.AUTOMATIC_BLOCKED
    }

    @Test
    fun `manual pin requires successful provider validation and acknowledgment of differences`() {
        val input = example
        val remote = input.candidates.last()
        val response = MetadataResult.Success(remote)
        MetadataManualSelection.propose(input.identity, remote.id, response, false) shouldBe null
        val pin = MetadataManualSelection.propose(input.identity, remote.id, response, true)!!
        pin.manual shouldBe true
        pin.id shouldBe remote.id
        matcher.evaluate(input.copy(association = pin), policy).retainedId shouldBe remote.id
        listOf(
            MetadataResult.Failure(MetadataFailureKind.NETWORK),
            MetadataResult.NotFound,
            response.copy(identityComplete = false),
            MetadataResult.Success(input.candidates.first()),
        ).forEach { invalid ->
            MetadataManualSelection.propose(input.identity, remote.id, invalid, true) shouldBe null
        }
    }

    @TestFactory
    fun `invalid ids cannot match or be manually pinned`() = listOf("0", "-1", "01", "2147483648", "a").map { id ->
        dynamicTest(id) {
            val input = example
            val remote = input.candidates.first().copy(id = MetadataId("anilist", id))
            matcher.evaluate(input.copy(candidates = listOf(remote)), policy).candidates.single().vetoes shouldBe
                setOf(MatchVeto.INVALID_ID)
            MetadataManualSelection.propose(input.identity, remote.id, MetadataResult.Success(remote), true) shouldBe
                null
        }
    }

    @Test
    fun `numeric markers cannot overflow and matcher input is bounded`() {
        val input = example.copy(identity = example.identity.copy(title = "A Season ${"9".repeat(100)}"))
        matcher.evaluate(input, policy).automaticId shouldBe null
        assertThrows<IllegalArgumentException> {
            matcher.evaluate(example.copy(candidates = List(81) { example.candidates.first() }))
        }
    }
}
// <-- AM (Yume metadata matching tests)
