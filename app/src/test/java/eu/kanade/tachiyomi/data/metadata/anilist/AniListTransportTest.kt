package eu.kanade.tachiyomi.data.metadata.anilist

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
import java.io.IOException

// AM (Yume metadata transport tests, 2026-10-20; new code assisted by Codex) -->
class AniListTransportTest {
    private val call = mockk<Call>(relaxed = true)
    private val factory = mockk<Call.Factory>()
    private val request = slot<Request>()
    private val callback = slot<Callback>()

    private fun transport(background: Boolean = false): OkHttpAniListTransport {
        every { factory.newCall(capture(request)) } returns call
        every { call.enqueue(capture(callback)) } returns Unit
        val base = OkHttpClient.Builder()
            .addInterceptor { it.proceed(it.request().newBuilder().header("Authorization", "secret").build()) }
            .addNetworkInterceptor { error("Source interceptor must not run") }
            .build()
        return OkHttpAniListTransport(base, factory, background)
    }

    private fun response(body: String) = Response.Builder()
        .request(request.captured)
        .protocol(Protocol.HTTP_2)
        .code(200)
        .message("OK")
        .body(body.toResponseBody())
        .build()

    @Test
    fun `neutral client disables redirects hidden retries cookies and auth`() {
        val client = transport().client
        assertTrue(client.interceptors.isEmpty())
        assertEquals(listOf(AniListHostInterceptor), client.networkInterceptors)
        assertSame(CookieJar.NO_COOKIES, client.cookieJar)
        assertSame(okhttp3.Authenticator.NONE, client.authenticator)
        assertFalse(client.followRedirects)
        assertFalse(client.followSslRedirects)
        assertFalse(client.retryOnConnectionFailure)
        assertEquals(10_000, client.connectTimeoutMillis)
        assertEquals(20_000, client.readTimeoutMillis)
        assertEquals(30_000, client.callTimeoutMillis)
        assertEquals(null, client.cache)
    }

    @Test
    fun `fixed HTTPS POST sends only public headers`() = runTest {
        val transport = transport()
        val pending = async { transport.execute("{}") }
        runCurrent()
        callback.captured.onResponse(call, response("{}"))
        assertEquals("{}", pending.await().body)
        assertEquals("https://graphql.anilist.co/", request.captured.url.toString())
        assertEquals("POST", request.captured.method)
        assertEquals(true, request.captured.body?.isOneShot())
        assertEquals(setOf("Accept", "User-Agent", "Content-Type"), request.captured.headers.names() + "Content-Type")
        assertEquals("application/json; charset=utf-8", request.captured.body?.contentType().toString())
        verify(exactly = 1) { factory.newCall(any()) }
    }

    @Test
    fun `background provider priority is a local tag and never a transmitted header`() = runTest {
        val pending = async { transport(background = true).execute("{}") }
        runCurrent()
        assertSame(AniListBackground, request.captured.tag(AniListBackground::class.java))
        assertEquals(setOf("Accept", "User-Agent"), request.captured.headers.names())
        callback.captured.onResponse(call, response("{}"))
        pending.await()
    }

    @Test
    fun `coroutine cancellation cancels actual call and does not publish late response`() = runTest {
        val transport = transport()
        val pending = async { transport.execute("{}") }
        runCurrent()
        pending.cancel()
        runCurrent()
        verify(exactly = 1) { call.cancel() }
        callback.captured.onResponse(call, response("{}"))
        assertTrue(pending.isCancelled)
    }

    @Test
    fun `body size is bounded before deserialization`() = runTest {
        val transport = transport()
        val pending = async {
            try {
                transport.execute("{}")
                false
            } catch (_: AniListBodyTooLarge) {
                true
            }
        }
        runCurrent()
        callback.captured.onResponse(call, response("a".repeat(2 * 1024 * 1024 + 1)))
        assertTrue(pending.await())
    }

    @Test
    fun `network callback failure is propagated`() = runTest {
        val transport = transport()
        val pending = async {
            try {
                transport.execute("{}")
                false
            } catch (_: IOException) {
                true
            }
        }
        runCurrent()
        callback.captured.onFailure(call, IOException())
        assertTrue(pending.await())
    }
}
// <-- AM (Yume metadata transport tests)
