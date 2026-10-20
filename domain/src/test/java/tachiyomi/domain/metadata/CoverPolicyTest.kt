package tachiyomi.domain.metadata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.metadata.model.CoverVariant
import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MetadataAssociation
import tachiyomi.domain.metadata.model.MetadataCachePolicy
import tachiyomi.domain.metadata.model.MetadataCachedMedia
import tachiyomi.domain.metadata.model.MetadataControl
import tachiyomi.domain.metadata.model.MetadataDecisionState
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataRecord
import tachiyomi.domain.metadata.model.MetadataTitles
import tachiyomi.domain.metadata.service.CoverPolicy

// AM (Yume cover policy tests, 2026-10-20; new code assisted by Codex) -->
class CoverPolicyTest {
    private val now = 1_800_000_000_000L
    private val id = MetadataId("anilist", "1")
    private val control = MetadataControl(true, 5, 0)
    private val state = MetadataDecisionState(
        LocalAnimeIdentity(1, 42, "/1", "Test title"),
        association = MetadataAssociation(id, true, "fingerprint"),
        outcome = "ASSOCIATED",
    )
    private val record = MetadataRecord(state, 2, 3)
    private fun url(name: String) = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/$name.jpg"
    private val media = MetadataMedia(
        id, null, MetadataTitles("Test title", null, null), emptyList(), null, null, null, null, null, null,
        listOf(
            CoverVariant("medium", url("medium")),
            CoverVariant("large", url("large")),
            CoverVariant("extraLarge", url("extra")),
        ),
    )
    private val cached = MetadataCachedMedia(media, now, true)

    @Test
    fun `largest available distinct safe variants are selected without invented dimensions`() {
        val result = CoverPolicy.resolve(control, record, cached, now)
        assertEquals(listOf(url("extra"), url("large"), url("medium")), result.external.map { it.url })
        val duplicate = cached.copy(
            media = media.copy(
                covers = listOf(
                    CoverVariant("large", url("same")),
                    CoverVariant("extraLarge", url("same")),
                    CoverVariant("medium", "http://unsafe.invalid/cover.jpg"),
                ),
            ),
        )
        assertEquals(listOf(url("same")), CoverPolicy.resolve(control, record, duplicate, now).external.map { it.url })
        assertTrue(
            CoverPolicy.resolve(control, record, cached.copy(media = media.copy(covers = emptyList())), now)
                .external.isEmpty(),
        )
    }

    @Test
    fun `off original review restored unavailable and missing associations never use external`() {
        assertTrue(CoverPolicy.resolve(control.copy(enabled = false), record, cached, now).external.isEmpty())
        for (variant in listOf(
            state.copy(originalOnly = true),
            state.copy(needsReview = true),
            state.copy(requiresRevalidation = true),
            state.copy(outcome = "UNAVAILABLE"),
            state.copy(association = null),
        )) {
            assertTrue(CoverPolicy.resolve(control, record.copy(state = variant), cached, now).external.isEmpty())
        }
        assertTrue(CoverPolicy.resolve(control, null, cached, now).external.isEmpty())
        assertTrue(CoverPolicy.resolve(control, record, null, now).external.isEmpty())
    }

    @Test
    fun `transient provider failures keep the accepted cached identity usable`() {
        val failed = record.copy(state = state.copy(outcome = "TRANSIENT_ERROR"))
        assertEquals(3, CoverPolicy.resolve(control, failed, cached, now).external.size)
        assertEquals(id, failed.state.association!!.id)
    }

    @Test
    fun `retention and clock rollback veto external without expiring the pin`() {
        assertEquals(
            3,
            CoverPolicy.resolve(control, record, cached, now + MetadataCachePolicy.RETAIN - 1).external.size,
        )
        assertTrue(CoverPolicy.resolve(control, record, cached, now + MetadataCachePolicy.RETAIN).external.isEmpty())
        assertTrue(CoverPolicy.resolve(control, record, cached, now - 1).external.isEmpty())
        assertEquals(id, record.state.association!!.id)
    }

    @Test
    fun `wrong or unsupported identities cannot supply cover bytes`() {
        assertTrue(
            CoverPolicy.resolve(control, record, cached.copy(media = media.copy(id = id.copy(value = "2"))), now)
                .external.isEmpty(),
        )
        val unsupported = record.copy(
            state = state.copy(association = MetadataAssociation(id.copy(provider = "other"), true, "x")),
        )
        assertTrue(CoverPolicy.resolve(control, unsupported, cached, now).external.isEmpty())
    }

    @Test
    fun `asset keys share bytes between local sources and change on snapshot or URL revision`() {
        val first = CoverPolicy.resolve(control, record, cached, now).external.first().key
        val other = record.copy(state = state.copy(identity = state.identity.copy(animeId = 2, sourceId = 43)))
        assertEquals(first, CoverPolicy.resolve(control, other, cached, now).external.first().key)
        assertNotEquals(
            first,
            CoverPolicy.resolve(control, record, cached.copy(fetchedAt = now - 1), now).external.first().key,
        )
        assertTrue(first.matches(Regex("yume-metadata-v1:anilist:1:[a-f0-9]{64}:$now")))
    }

    @Test
    fun `original only remains a distinct reversible selection with no asset`() {
        val original = CoverPolicy.resolve(control, record.copy(state = state.copy(originalOnly = true)), cached, now)
        assertTrue(original.originalOnly)
        assertTrue(original.external.isEmpty())
        assertFalse(CoverPolicy.resolve(control, record, cached, now).originalOnly)
    }

    @Test
    fun `URL policy rejects credentials redirects ports traversal and non CDN addresses`() {
        assertTrue(CoverPolicy.safeUrl(url("valid")))
        listOf(
            "http://s4.anilist.co/file/anilistcdn/media/anime/cover/a.jpg",
            "https://s4.anilist.co.evil.invalid/file/anilistcdn/media/anime/cover/a.jpg",
            "https://user:pass@s4.anilist.co/file/anilistcdn/media/anime/cover/a.jpg",
            "https://s4.anilist.co:444/file/anilistcdn/media/anime/cover/a.jpg", url("x") + "#fragment",
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/../private.jpg",
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/%2e%2e/private.jpg",
            "https://127.0.0.1/cover.jpg", "file:///private.jpg", "not a URL",
        ).forEach {
            assertFalse(CoverPolicy.safeUrl(it), it)
        }
    }
}
// <-- AM (Yume cover policy tests)
