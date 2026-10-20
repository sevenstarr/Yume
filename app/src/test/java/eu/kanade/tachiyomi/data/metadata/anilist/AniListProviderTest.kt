package eu.kanade.tachiyomi.data.metadata.anilist

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.metadata.model.MetadataFailureKind
import tachiyomi.domain.metadata.model.MetadataFormat
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataResult
import tachiyomi.domain.metadata.model.MetadataSeason
import tachiyomi.domain.metadata.service.MetadataSearch
import java.io.IOException

// AM (Yume metadata tests, 2026-10-20; new code assisted by Codex) -->
class AniListProviderTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val id = MetadataId("anilist", "7")
    private val clock = FakeClock()
    private val budget = AniListBudget(clock) { 0 }
    private val requests = mutableListOf<String>()
    private var enabled = true

    private fun provider(reply: suspend (Int) -> AniListResponse): AniListProvider = AniListProvider(
        AniListTransport { payload ->
            requests.add(payload)
            reply(requests.size)
        },
        json,
        budget,
        { enabled },
    )

    private fun ok(body: String = detail()) = AniListResponse(200, Headers.headersOf(), body)
    private fun detail(media: String = MEDIA, errors: String = "") = """{"data":{"Media":$media}$errors}"""
    private fun page(media: String = MEDIA, hasNext: String = "false", errors: String = "") =
        """{"data":{"Page":{"pageInfo":{"currentPage":1,"hasNextPage":$hasNext},"media":[$media]}}$errors}"""

    @Test
    fun `maps all public fields without authentication`() = runTest {
        val result = provider { ok() }.getById(id) as MetadataResult.Success
        val media = result.value
        assertTrue(tachiyomi.domain.metadata.service.AnimeMatcher.validId(media.id))
        val identity = tachiyomi.domain.metadata.model.LocalAnimeIdentity(1, 42, "/anime", "Synthetic")
        assertEquals(
            media.id,
            tachiyomi.domain.metadata.service.MetadataManualSelection.propose(identity, media.id, result, true)?.id,
        )
        assertTrue(result.complete)
        assertEquals("日本語", media.titles.native)
        assertEquals(listOf("Alias"), media.synonyms)
        assertEquals(1999, media.startDate?.year)
        assertNull(media.startDate?.month)
        assertEquals(MetadataFormat.TV, media.format)
        assertEquals(MetadataSeason.SPRING, media.season)
        assertEquals(3, media.covers.size)
        assertEquals(8, media.idMal)
        assertEquals(12, media.episodes)
        assertEquals(1999, media.seasonYear)
        val payload = json.parseToJsonElement(requests.single()).jsonObject
        assertEquals("7", payload.getValue("variables").jsonObject.getValue("id").jsonPrimitive.content)
        assertFalse(requests.single().contains("mutation"))
        assertFalse(requests.single().contains("token"))
    }

    @Test
    fun `search uses variables filters fixed page size and hasNextPage`() = runTest {
        val query = "A \"quoted\" 日本語 title"
        val result = provider { ok(page(hasNext = "true")) }.search(
            MetadataSearch(query, format = MetadataFormat.MOVIE, season = MetadataSeason.FALL, year = 2001),
        ) as MetadataResult.Success
        assertEquals(true, result.value.hasNextPage)
        val payload = json.parseToJsonElement(requests.single()).jsonObject
        val variables = payload.getValue("variables").jsonObject
        assertEquals(query, variables.getValue("search").jsonPrimitive.content)
        assertEquals("MOVIE", variables.getValue("format").jsonPrimitive.content)
        assertEquals("FALL", variables.getValue("season").jsonPrimitive.content)
        assertEquals("2001", variables.getValue("year").jsonPrimitive.content)
        assertTrue(payload.getValue("query").jsonPrimitive.content.contains("perPage: 20"))
        assertFalse(payload.getValue("query").jsonPrimitive.content.contains(query))
    }

    @Test
    fun `disabled gate prevents emission including recent cache`() = runTest {
        val client = provider { ok() }
        client.getById(id)
        enabled = false
        assertEquals(MetadataFailureKind.DISABLED, (client.getById(id) as MetadataResult.Failure).kind)
        assertEquals(1, requests.size)
    }

    @Test
    fun `invalid input makes no request`() = runTest {
        val client = provider { error("Unexpected request") }
        listOf("0", "-1", "2147483648", "007", "1x").forEach {
            assertEquals(
                MetadataFailureKind.INVALID_INPUT,
                (client.getById(id.copy(value = it)) as MetadataResult.Failure).kind,
            )
        }
        assertTrue(client.getById(id.copy(provider = "MAL")) is MetadataResult.Failure)
        listOf(
            MetadataSearch(" "),
            MetadataSearch("a".repeat(301)),
            MetadataSearch("a", 0),
            MetadataSearch("a", 51),
            MetadataSearch("a", year = -1),
        ).forEach {
            assertTrue(client.search(it) is MetadataResult.Failure)
        }
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `empty complete search is distinct from missing list`() = runTest {
        val empty = provider { ok(page(media = "")) }.search(MetadataSearch("Empty")) as MetadataResult.Success
        assertTrue(empty.complete)
        assertTrue(empty.value.media.isEmpty())
        val missing = provider { ok("""{"data":{"Page":{"pageInfo":{"currentPage":1,"hasNextPage":false}}}}""") }
            .search(MetadataSearch("Missing")) as MetadataResult.Success
        assertFalse(missing.complete)
    }

    @Test
    fun `nullable optional fields and future enums remain unknown`() = runTest {
        val sparse = """{"id":7,"type":"ANIME","title":{"romaji":"Synthetic"},"format":"FUTURE","season":"FUTURE"}"""
        val media = (provider { ok(detail(sparse)) }.getById(id) as MetadataResult.Success).value
        assertNull(media.format)
        assertNull(media.season)
        assertNull(media.startDate)
        assertTrue(media.covers.isEmpty())
        assertTrue(media.synonyms.isEmpty())
    }

    @Test
    fun `null exact media is absent only in intact response`() = runTest {
        assertEquals(MetadataResult.NotFound, provider { ok(detail("null")) }.getById(id))
        assertTrue(provider { ok(detail("null", IMAGE_ERROR)) }.getById(id) is MetadataResult.Failure)
        assertTrue(provider { ok("""{"data":{}}""") }.getById(id) is MetadataResult.Failure)
    }

    @Test
    fun `image partial retains identity but removes failed image data`() = runTest {
        val result = provider { ok(detail(errors = IMAGE_ERROR)) }.getById(id) as MetadataResult.Success
        assertEquals(id, result.value.id)
        assertFalse(result.complete)
        assertTrue(result.identityComplete)
        assertTrue(result.value.covers.isEmpty())
        assertEquals(listOf("Media", "coverImage", "extraLarge"), result.errors.single().path)
    }

    @Test
    fun `identity partial cannot authorize exact ID`() = runTest {
        val errors = """, "errors":[{"path":["Media","title","romaji"],"message":"private server text"}]"""
        val result = provider { ok(detail(errors = errors)) }.getById(id)
        assertEquals(MetadataFailureKind.CONTRACT, (result as MetadataResult.Failure).kind)
        assertFalse(result.toString().contains("private server text"))
    }

    @Test
    fun `search identity partial omits damaged candidate and marks incomplete`() = runTest {
        val errors = """, "errors":[{"path":["Page","media",0,"title","romaji"]}]"""
        val result = provider {
            ok(page(errors = errors))
        }.search(MetadataSearch("Synthetic")) as MetadataResult.Success
        assertFalse(result.complete)
        assertTrue(result.value.media.isEmpty())
    }

    @Test
    fun `invalid error roots fail contract and damaged whole nodes are omitted`() = runTest {
        val invalid = """, "errors":[{"path":["Page","coverImage","extraLarge"]}]"""
        assertEquals(
            MetadataFailureKind.CONTRACT,
            (provider { ok(page(errors = invalid)) }.search(MetadataSearch("Invalid")) as MetadataResult.Failure).kind,
        )
        val nodeError = """, "errors":[{"path":["Page","media",0]}]"""
        val result = provider { ok(page(errors = nodeError)) }.search(MetadataSearch("Node")) as MetadataResult.Success
        assertFalse(result.identityComplete)
        assertTrue(result.value.media.isEmpty())
    }

    @Test
    fun `null candidates and missing page info never create complete negative`() = runTest {
        val result = provider { ok("""{"data":{"Page":{"media":[null]}}}""") }
            .search(MetadataSearch("Synthetic")) as MetadataResult.Success
        assertFalse(result.complete)
        assertNull(result.value.hasNextPage)
    }

    @Test
    fun `mismatched ID wrong type or missing titles are rejected`() = runTest {
        for (media in listOf(
            MEDIA.replace("\"id\":7", "\"id\":9"),
            MEDIA.replace("ANIME", "MANGA"),
            """{"id":7,"type":"ANIME"}""",
        )) {
            assertTrue(provider { ok(detail(media)) }.getById(id) is MetadataResult.Failure)
        }
    }

    @Test
    fun `malformed JSON incompatible types and global errors are contract failures`() = runTest {
        for (body in listOf(
            "{",
            """{"data":{"Media":{"id":"wrong"}}}""",
            """{"errors":[{"message":"failure"}]}""",
            detail(errors = """, "errors":[{"path":[{}]}]"""),
        )) {
            assertEquals(
                MetadataFailureKind.CONTRACT,
                (
                    provider {
                        ok(body)
                    }.getById(id) as MetadataResult.Failure
                    ).kind,
            )
        }
    }

    @Test
    fun `unsafe image hosts credentials ports paths and http are rejected`() = runTest {
        val safe = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/a.png"
        for (unsafe in listOf(
            "http://s4.anilist.co/a",
            "https://s4.anilist.co.evil.test/a",
            "https://u:p@s4.anilist.co/a",
            "https://s4.anilist.co:8443/a",
            "https://127.0.0.1/a",
            "https://s4.anilist.co/other/a",
        )) {
            val dto = """{
                "id":7,"type":"ANIME","title":{"romaji":"Synthetic"},
                "coverImage":{"extraLarge":"$unsafe","large":"$safe","medium":"$safe"}
            }"""
            val result = provider { ok(detail(dto)) }.getById(id) as MetadataResult.Success
            assertEquals(listOf(safe), result.value.covers.map { it.url })
        }
    }

    @Test
    fun `concurrent identical requests collapse and fresh calls expire`() = runTest {
        val client = provider {
            delay(10)
            ok()
        }
        val first = async { client.getById(id) }
        val second = async { client.getById(id) }
        assertEquals(first.await(), second.await())
        assertEquals(1, requests.size)
        clock.time += 5_001
        client.getById(id)
        assertEquals(2, requests.size)
    }

    @Test
    fun `network errors retry at most three emissions with backoff`() = runTest {
        val result = provider { throw IOException("private URL") }.getById(id) as MetadataResult.Failure
        assertEquals(MetadataFailureKind.NETWORK, result.kind)
        assertEquals(3, requests.size)
        assertEquals(listOf(2_000L, 500L, 4_000L), clock.sleeps)
        assertFalse(result.toString().contains("private URL"))
    }

    @Test
    fun `server errors retry but successful final response wins`() = runTest {
        val result = provider {
            if (it <
                3
            ) {
                AniListResponse(503, Headers.headersOf(), "unavailable")
            } else {
                ok()
            }
        }.getById(id)
        assertTrue(result is MetadataResult.Success)
        assertEquals(3, requests.size)
    }

    @Test
    fun `client errors and redirects never retry`() = runTest {
        for (code in listOf(301, 302, 400, 401, 403, 404, 410)) {
            val before = requests.size
            val result = provider {
                AniListResponse(code, Headers.headersOf("Location", "https://evil.test"), "")
            }.getById(id)
            assertTrue(result is MetadataResult.Failure)
            assertEquals(before + 1, requests.size)
        }
    }

    @Test
    fun `graphql 429 is shared and fallback cooldown cannot be bypassed`() = runTest {
        val client = provider { ok("""{"data":null,"errors":[{"status":429,"message":"Too Many Requests"}]}""") }
        val result = client.getById(id) as MetadataResult.Failure
        assertEquals(MetadataFailureKind.RATE_LIMITED, result.kind)
        assertEquals(clock.wallMillis() + 60_000, result.retryAtMillis)
        val secondClient = provider { error("Cooldown must be shared") }
        assertTrue(secondClient.getById(id) is MetadataResult.Failure)
        assertEquals(1, requests.size)
    }

    @Test
    fun `Retry-After and reset use maximum relative to server Date`() = runTest {
        val headers = Headers.headersOf(
            "Date",
            "Tue, 20 Oct 2026 00:00:00 GMT",
            "Retry-After",
            "90",
            "X-RateLimit-Reset",
            "1792454520",
        )
        val result = provider { AniListResponse(429, headers, "") }.getById(id) as MetadataResult.Failure
        assertEquals(clock.wallMillis() + 120_000, result.retryAtMillis)
    }

    @Test
    fun `HTTP date Retry-After is supported and invalid headers fall back`() = runTest {
        val headers = Headers.headersOf(
            "Date",
            "Tue, 20 Oct 2026 00:00:00 GMT",
            "Retry-After",
            "Tue, 20 Oct 2026 00:02:00 GMT",
        )
        assertEquals(clock.wallMillis() + 120_000, budget.observe(headers, true))
        val other = AniListBudget(clock) { 0 }
        assertEquals(
            clock.wallMillis() + 60_000,
            other.observe(Headers.headersOf("Retry-After", "nonsense", "X-RateLimit-Reset", "-1"), true),
        )
    }

    @Test
    fun `valid long cooldown is never shortened to a local cap`() = runTest {
        val headers = Headers.headersOf("Retry-After", "315360000")
        assertEquals(clock.wallMillis() + 315_360_000_000, budget.observe(headers, true))
        assertEquals(clock.wallMillis() + 315_360_000_000, budget.acquire(clock.time + 45_000))
    }

    @Test
    fun `remaining zero blocks next request even after success`() = runTest {
        val client = provider { AniListResponse(200, Headers.headersOf("X-RateLimit-Remaining", "0"), detail()) }
        assertTrue(client.getById(id) is MetadataResult.Success)
        assertTrue(client.getById(id.copy(value = "8")) is MetadataResult.Failure)
        assertEquals(1, requests.size)
    }

    @Test
    fun `five transient emissions open circuit across operations`() = runTest {
        val client = provider { throw IOException() }
        client.getById(id)
        val result = client.getById(id.copy(value = "8")) as MetadataResult.Failure
        assertEquals(MetadataFailureKind.CIRCUIT_OPEN, result.kind)
        assertEquals(5, requests.size)
    }

    @Test
    fun `lower server quota never expands from a later higher limit`() = runTest {
        budget.observe(Headers.headersOf("X-RateLimit-Limit", "1"), false)
        budget.observe(Headers.headersOf("X-RateLimit-Limit", "90"), false)
        assertNull(budget.acquire(clock.time + 45_000))
        assertEquals(clock.wallMillis() + 60_000, budget.acquire(clock.time + 45_000))
    }

    @Test
    fun `enrichment subbudget caps emissions in rolling minute`() = runTest {
        repeat(12) { assertNull(budget.acquire(clock.time + 45_000)) }
        assertEquals(27_500L, clock.time)
        assertEquals(clock.wallMillis() + 32_500, budget.acquire(clock.time + 30_000))
    }

    @Test
    fun `cancellation is propagated without retry`() = runTest {
        val client = provider { throw CancellationException("cancelled") }
        var cancelled = false
        try {
            client.getById(id)
        } catch (_: CancellationException) {
            cancelled = true
        }
        assertTrue(cancelled)
        assertEquals(1, requests.size)
    }

    @Test
    fun `interactive timeout includes hanging transport`() = runTest {
        val result = provider { awaitCancellation() }.getById(id) as MetadataResult.Failure
        assertEquals(MetadataFailureKind.TIMEOUT, result.kind)
        assertEquals(1, requests.size)
    }

    @Test
    fun `opt out during quota wait prevents emission`() = runTest {
        val fakeClock = object : AniListClock by clock {
            override suspend fun sleep(millis: Long) {
                clock.sleep(millis)
                enabled = false
            }
        }
        val otherBudget = AniListBudget(fakeClock) { 0 }
        otherBudget.acquire(45_000)
        val client = AniListProvider(AniListTransport { error("Disabled during wait") }, json, otherBudget, { enabled })
        assertEquals(MetadataFailureKind.DISABLED, (client.getById(id) as MetadataResult.Failure).kind)
    }

    private class FakeClock : AniListClock {
        var time = 0L
        val sleeps = mutableListOf<Long>()
        override fun monotonicMillis() = time
        override fun wallMillis() = 1_000_000L + time
        override suspend fun sleep(millis: Long) {
            sleeps.add(millis)
            time += millis
        }
    }

    private companion object {
        const val IMAGE_ERROR = """, "errors":[{
            "path":["Media","coverImage","extraLarge"],"message":"private server text"
        }]"""
        const val MEDIA = """{
            "id":7,"idMal":8,"type":"ANIME",
            "title":{"romaji":"Synthetic","english":"Example","native":"日本語"},
            "synonyms":["Alias",null],"format":"TV","startDate":{"year":1999,"month":null},
            "season":"SPRING","seasonYear":1999,"episodes":12,"siteUrl":"https://anilist.co/anime/7",
            "coverImage":{
                "extraLarge":"https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/a.png",
                "large":"https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/a.png",
                "medium":"https://s4.anilist.co/file/anilistcdn/media/anime/cover/small/a.png"
            }
        }"""
    }
}
// <-- AM (Yume metadata tests)
