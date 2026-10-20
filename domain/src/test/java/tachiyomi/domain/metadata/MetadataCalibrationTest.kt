package tachiyomi.domain.metadata

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.metadata.model.MatchConfiguration
import tachiyomi.domain.metadata.service.AnimeMatcher
import java.io.File

// AM (Yume metadata calibration tests, 2026-10-20; new code assisted by Codex) -->
class MetadataCalibrationTest {
    private val matcher = AnimeMatcher()

    @Test
    fun `select parameters on calibration only then independently evaluate held out labels`() {
        val calibration = MetadataMatchFixtures.read("calibration")
        val grid = buildList {
            for (title in listOf(50, 60, 70)) {
                for (year in listOf(15, 20)) {
                    for (format in listOf(10, 20)) {
                        for (high in listOf(70, 80, 90)) {
                            for (review in listOf(30, 40)) {
                                for (margin in listOf(5, 10, 20)) {
                                    if (high <= title + year + format) {
                                        add(
                                            MatchConfiguration(
                                                "s5-calibration-v1",
                                                title,
                                                year,
                                                format,
                                                high,
                                                review,
                                                margin,
                                                true,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val safe = grid.filter { policy ->
            calibration.all { fixture ->
                val id = matcher.evaluate(fixture.input, policy).automaticId
                id == null || id == fixture.expectedId
            }
        }
        val selected = safe.sortedWith(
            compareByDescending<MatchConfiguration> { policy ->
                calibration.count {
                    it.expectedId != null &&
                        matcher.evaluate(it.input, policy).automaticId == it.expectedId
                }
            }.thenBy { it.titleWeight + it.yearWeight + it.formatWeight }
                .thenByDescending { it.highThreshold }.thenBy { it.minimumMargin }.thenBy { it.reviewThreshold },
        ).first()
        selected.copy(automationApproved = false) shouldBe MatchConfiguration.ProposalV1
        val validation = MetadataMatchFixtures.read("validation")
        calibration.map {
            it.input.identity.title
        }.toSet().intersect(validation.map { it.input.identity.title }.toSet()) shouldBe
            emptySet()
        val report = buildString {
            appendLine("configuration=$selected")
            appendLine("grid=${grid.size}; safe=${safe.size}")
            for ((name, fixtures) in listOf("calibration" to calibration, "validation" to validation)) {
                val decisions = fixtures.map { it to matcher.evaluate(it.input, selected) }
                val automatic = decisions.count { it.second.automaticId != null }
                val falsePositive = decisions.count {
                    it.second.automaticId != null &&
                        it.second.automaticId != it.first.expectedId
                }
                val positive = fixtures.count { it.expectedId != null }
                val correct = decisions.count {
                    it.first.expectedId != null &&
                        it.first.expectedId == it.second.automaticId
                }
                falsePositive shouldBe 0
                correct shouldBe positive
                appendLine(
                    "$name total=${fixtures.size} positive=$positive auto=$automatic correct=$correct " +
                        "falsePositive=$falsePositive abstain=${fixtures.size - automatic}",
                )
                decisions.forEach { (fixture, decision) ->
                    appendLine(
                        "${fixture.name}\texpected=${fixture.expectedId?.value ?: "abstain"}" +
                            "\tactual=${decision.automaticId?.value ?: "abstain"}\t${decision.outcome}\t${decision.reason}",
                    )
                }
            }
        }
        File("build/reports/metadata/calibration-v1.txt").apply {
            parentFile.mkdirs()
            writeText(report)
        }
    }
}
// <-- AM (Yume metadata calibration tests)
