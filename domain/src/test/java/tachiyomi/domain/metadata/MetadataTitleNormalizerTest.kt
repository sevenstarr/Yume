package tachiyomi.domain.metadata

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.assertThrows
import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MetadataFormat
import tachiyomi.domain.metadata.model.MetadataScope
import tachiyomi.domain.metadata.service.MetadataTitleNormalizer

// AM (Yume metadata matching tests, 2026-10-20; new code assisted by Codex) -->
class MetadataTitleNormalizerTest {
    @TestFactory
    fun `normalization retains identity tokens`() = listOf(
        "  Coração—de   Cristal! " to "coração de cristal",
        "Cafe\u0301" to "café",
        "星の旅人 (映画)" to "星の旅人 映画",
        "Sky Road II Part 2 (2024) Final Movie OVA Special" to "sky road ii part 2 2024 final movie ova special",
        "86" to "86",
        "Dublado no Jardim" to "dublado no jardim",
        "HD" to "hd",
        "Legendado" to "legendado",
        "A Story (Director Cut)" to "a story director cut",
        "Moon Riders (Dublado) HD" to "moon riders",
        "Violet Compass - Legendado [1080p]" to "violet compass",
        "Paper Wings 720p" to "paper wings",
        "Paper\u00a0Wings\u00a0Dublado" to "paper wings",
        "Movie HD Edition" to "movie hd edition",
    ).map { (raw, expected) ->
        dynamicTest(raw) {
            MetadataTitleNormalizer.normalize(raw).comparison shouldBe expected
            MetadataTitleNormalizer.normalize(raw).raw shouldBe raw
        }
    }

    @Test
    fun `compatibility and accents are auxiliary and search has at most two variants`() {
        val title = MetadataTitleNormalizer.normalize("Ｃａｆé (Dublado)")
        title.comparison shouldBe "ｃａｆé"
        title.compatibility shouldBe "café"
        title.accentFolded shouldBe "cafe"
        title.searchVariants shouldBe listOf("ｃａｆé dublado", "ｃａｆé")
        MetadataTitleNormalizer.normalize("Istanbul").comparison shouldBe "istanbul"
    }

    @Test
    fun `fingerprint covers material origin and context with collision safe framing`() {
        val identity = LocalAnimeIdentity(1, 2, "a:bc", "Voyage", 2020, MetadataFormat.TV, MetadataScope.WORK)
        val fingerprint = MetadataTitleNormalizer.fingerprint(identity)
        fingerprint shouldBe MetadataTitleNormalizer.fingerprint(identity.copy())
        listOf(
            identity.copy(animeId = 2), identity.copy(sourceId = 3), identity.copy(sourceUrl = "a:b"),
            identity.copy(
                title = "Voyage II",
            ),
            identity.copy(year = null), identity.copy(format = MetadataFormat.MOVIE),
            identity.copy(
                scope = MetadataScope.CHILD_SEASON,
            ),
            identity.copy(parentId = 1), identity.copy(seasonNumber = 2),
        ).forEach { (MetadataTitleNormalizer.fingerprint(it) == fingerprint) shouldBe false }
        fingerprint.length shouldBe 76
    }

    @Test
    fun `bounded title and structured context reject invalid input`() {
        assertThrows<IllegalArgumentException> { MetadataTitleNormalizer.normalize("x".repeat(501)) }
        assertThrows<IllegalArgumentException> { LocalAnimeIdentity(1, 2, "/a", " ") }
        assertThrows<IllegalArgumentException> { LocalAnimeIdentity(1, 2, "/a", "Voyage", year = 0) }
        assertThrows<IllegalArgumentException> { LocalAnimeIdentity(1, 2, "/a", "Voyage", seasonNumber = 0) }
    }
}
// <-- AM (Yume metadata matching tests)
