package eu.kanade.tachiyomi.data.coil

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// AM (Yume CDN transport tests, 2026-10-20; new code assisted by Codex) -->
class CoverTransportTest {
    private val call = mockk<Call>(relaxed = true)
    private val factory = mockk<Call.Factory>()
    private val request = slot<Request>()
    private val callback = slot<Callback>()
    private val url = "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/test.jpg"

    private fun transport(): CoverTransport {
        every { factory.newCall(capture(request)) } returns call
        every { call.enqueue(capture(callback)) } returns Unit
        val base = OkHttpClient.Builder().addInterceptor { error("Source headers must not run") }.build()
        return CoverTransport(base, factory)
    }
    private fun response(code: Int = 200, body: String = "image") = Response.Builder()
        .request(request.captured).protocol(Protocol.HTTP_2).code(code).message("test")
        .body(body.toResponseBody()).build()

    @Test
    fun `neutral CDN client has no source cookies auth redirects or implicit retries`() {
        val client = transport().client
        assertTrue(client.interceptors.isEmpty())
        assertEquals(1, client.networkInterceptors.size)
        assertSame(CookieJar.NO_COOKIES, client.cookieJar)
        assertSame(okhttp3.Authenticator.NONE, client.authenticator)
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
        assertFalse(client.retryOnConnectionFailure)
        assertEquals(null, client.cache)
    }

    @Test
    fun `only verified HTTPS CDN requests carry public headers`() = runTest {
        val transport = transport()
        val pending = async { transport.read(url) }
        runCurrent()
        callback.captured.onResponse(call, response())
        assertEquals("image", pending.await().utf8())
        assertEquals("GET", request.captured.method)
        assertEquals(setOf("Accept", "User-Agent"), request.captured.headers.names())
        verify(exactly = 1) { factory.newCall(any()) }
    }

    @Test
    fun `unsafe URL is refused before creating a call`() = runTest {
        var refused = false
        try {
            transport().read("https://private.invalid/image")
        } catch (
            e: CoverFetchFailure,
        ) {
            refused = e.assetFailure
        }
        assertTrue(refused)
        verify(exactly = 0) { factory.newCall(any()) }
    }

    @Test
    fun `404 and redirect never follow another URL`() = runTest {
        for (code in listOf(404, 410, 302, 429, 503)) {
            val transport = transport()
            val pending = async {
                try {
                    transport.read(url)
                    null
                } catch (e: CoverFetchFailure) {
                    e
                }
            }
            runCurrent()
            callback.captured.onResponse(call, response(code))
            assertEquals(code in listOf(404, 410), pending.await()!!.assetFailure)
        }
        verify(exactly = 5) { factory.newCall(any()) }
    }

    @Test
    fun `cancel propagates to HTTP and late callback cannot publish`() = runTest {
        val transport = transport()
        val pending = async { transport.read(url) }
        runCurrent()
        pending.cancel()
        runCurrent()
        callback.captured.onResponse(call, response())
        assertTrue(pending.isCancelled)
        verify(exactly = 1) { call.cancel() }
    }

    @Test
    fun `oversized image is bounded before Coil decodes`() = runTest {
        val transport = transport()
        val pending = async {
            try {
                transport.read(url)
                false
            } catch (e: CoverFetchFailure) {
                e.assetFailure
            }
        }
        runCurrent()
        callback.captured.onResponse(call, response(body = "x".repeat(16 * 1024 * 1024 + 1)))
        assertTrue(pending.await())
    }
}
// <-- AM (Yume CDN transport tests)
