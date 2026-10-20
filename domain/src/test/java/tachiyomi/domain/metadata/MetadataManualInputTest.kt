package tachiyomi.domain.metadata

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.service.MetadataManualInput

// AM (Yume manual review tests, 2026-10-20; new code assisted by Codex) -->
class MetadataManualInputTest {
    @TestFactory
    fun `official IDs and links`() = listOf(
        "1",
        " 1 ",
        "2147483647",
        "https://anilist.co/anime/1",
        "https://anilist.co/anime/1/",
        "https://anilist.co/anime/1/Cowboy-Bebop",
        "https://anilist.co/anime/1/Cowboy-Bebop/",
    ).map { value ->
        dynamicTest(value) {
            assertEquals(
                MetadataId(
                    "anilist",
                    if (value ==
                        "2147483647"
                    ) {
                        value
                    } else {
                        "1"
                    },
                ),
                MetadataManualInput.id(value),
            )
        }
    }

    @TestFactory
    fun `untrusted or noncanonical input cannot become a remote ID`() = listOf(
        "", "Title", "0", "001", "-1", "+1", "2147483648", "١", "１",
        "http://anilist.co/anime/1", "https://evil.invalid/anime/1", "https://anilist.co.evil.invalid/anime/1",
        "https://user@anilist.co/anime/1", "https://anilist.co:443/anime/1", "https://anilist.co/manga/1",
        "https://anilist.co/anime/1?x=1", "https://anilist.co/anime/1#x", "https://anilist.co/anime/01",
        "https://anilist.co/anime/%31", "https://anilist.co/anime/1/../2", "https://anilist.co/anime/1/slug/extra",
        "https://anilist.co/anime/1/%2e%2e", "//anilist.co/anime/1", "https://anilist.co/anime/1\\evil",
    ).map { value -> dynamicTest(value.ifEmpty { "empty" }) { assertNull(MetadataManualInput.id(value)) } }
}
// <-- AM (Yume manual review tests)
