package eu.kanade.tachiyomi.data.metadata.anilist

import io.mockk.every
import io.mockk.mockk
import okhttp3.Call
import okhttp3.Headers.Companion.headersOf
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// AM (Yume shared quota tests, 2026-10-20; new code assisted by Codex)
class AniListHostGovernorTest {
    private class Store : AniListQuotaStore {
        var emissions = emptyList<Long>()
        var blocked = 0L
        var limit = 12
        var circuit = 0 to 300_000L
        override fun readCircuit() = circuit
        override fun writeCircuit(failures: Int, duration: Long) {
            circuit = failures to duration
        }
        override fun readLimit() = limit
        override fun writeLimit(limit: Int) {
            this.limit = limit
        }
        override fun read() = emissions to blocked
        override fun write(emissions: List<Long>, blockedUntil: Long) {
            this.emissions = emissions
            blocked = blockedUntil
        }
    }

    @Test
    fun `mixed consumers and retries share twelve physical reservations per minute`() {
        val store = Store()
        var now = 1_800_000_000_000L
        val sent = mutableListOf<Long>()
        val governor = AniListHostGovernor(store, { now }, { now += it })
        repeat(50) {
            governor.acquire { false }
            sent += now
        }
        assertTrue(sent.zipWithNext().all { (a, b) -> b - a >= 5_000 })
        assertTrue(sent.all { time -> sent.count { it > time - 60_000 && it <= time } <= 12 })
    }

    @Test
    fun `restart retains window and cooldown`() {
        val store = Store()
        var now = 1_800_000_000_000L
        AniListHostGovernor(store, { now }, { now += it }).apply {
            acquire { false }
            observe(headersOf("Retry-After", "120"), true)
        }
        val before = now
        AniListHostGovernor(store, { now }, { now += it }).acquire { false }
        assertEquals(120_000, now - before)
    }

    @Test
    fun `remaining zero and smaller limit constrain every consumer`() {
        val store = Store()
        var now = 1_800_000_000_000L
        val governor = AniListHostGovernor(store, { now }, { now += it })
        governor.acquire { false }
        governor.observe(headersOf("X-RateLimit-Limit", "1"), false)
        val before = now
        governor.acquire { false }
        assertEquals(60_000, now - before)
        governor.observe(headersOf("X-RateLimit-Remaining", "0"), false)
        governor.acquire { false }
        assertEquals(120_000, now - before)
    }

    @Test
    fun `HTTP date retry and reset use server date despite local clock skew`() {
        val store = Store()
        var now = 1_800_000_000_000L
        val governor = AniListHostGovernor(store, { now }, { now += it })
        governor.observe(
            headersOf("Date", "Wed, 21 Oct 2015 07:28:00 GMT", "Retry-After", "Wed, 21 Oct 2015 07:30:00 GMT"),
            true,
        )
        val before = now
        governor.acquire { false }
        assertEquals(120_000, now - before)
    }

    @Test
    fun `cancel while waiting never reserves another emission`() {
        val store = Store()
        var now = 1_800_000_000_000L
        var cancelled = false
        val governor = AniListHostGovernor(store, { now }, {
            now += it
            cancelled = true
        })
        governor.acquire { false }
        assertThrows(IOException::class.java) { governor.acquire { cancelled } }
        assertEquals(1, store.emissions.size)
    }

    @Test
    fun `storage failure fails closed before HTTP reservation`() {
        val governor = AniListHostGovernor(object : AniListQuotaStore {
            override fun read() = emptyList<Long>() to 0L
            override fun write(emissions: List<Long>, blockedUntil: Long) = throw IOException("fixture")
        })
        assertThrows(IOException::class.java) { governor.acquire { false } }
    }

    @Test
    fun `clock rollback retains a conservative full window`() {
        val store = Store()
        var now = 1_800_000_000_000L
        store.emissions = List(12) { now + 100_000 }
        val governor = AniListHostGovernor(store, { now }, { now += it })
        val before = now
        governor.acquire { false }
        assertTrue(now - before >= 160_000)
    }

    @Test
    fun `reduced server limit is durable across governor restart`() {
        val store = Store()
        var now = 1_800_000_000_000L
        AniListHostGovernor(store, { now }, { now += it }).apply {
            acquire { false }
            observe(headersOf("X-RateLimit-Limit", "1"), false)
        }
        val before = now
        AniListHostGovernor(store, { now }, { now += it }).acquire { false }
        assertEquals(60_000, now - before)
    }

    @Test
    fun `provider GraphQL rate limit observation defers the shared host budget`() {
        val store = Store()
        var now = 1_800_000_000_000L
        val governor = AniListHostGovernor(store, { now }, { now += it })
        val providerBudget = AniListBudget(onObserved = governor::observe, jitter = { 0 })
        providerBudget.observe(headersOf("Retry-After", "180"), true)
        val before = now
        governor.acquire { false }
        assertEquals(180_000, now - before)
    }

    @Test
    fun `global circuit and exponential probe delay survive governor restart`() {
        val store = Store()
        var now = 1_800_000_000_000L
        val governor = AniListHostGovernor(store, { now }, { now += it })
        repeat(5) { governor.transientFailure() }
        val before = now
        val restarted = AniListHostGovernor(store, { now }, { now += it })
        restarted.acquire { false }
        assertEquals(300_000, now - before)
        restarted.transientFailure()
        restarted.acquire { false }
        assertEquals(900_000, now - before)
        restarted.success()
        assertEquals(0 to 300_000L, store.circuit)
    }

    @Test
    fun `one in flight request and waiting foreground takes precedence over batch`() {
        val store = Store()
        val time = AtomicLong(1_800_000_000_000L)
        val foregroundWaiting = CountDownLatch(1)
        val backgroundWaiting = CountDownLatch(1)
        val order = ConcurrentLinkedQueue<String>()
        val failures = ConcurrentLinkedQueue<Throwable>()
        val stop = AtomicBoolean(false)
        val governor = AniListHostGovernor(store, time::get, sleep = { duration ->
            time.addAndGet(duration)
            if (Thread.currentThread().name == "foreground") foregroundWaiting.countDown()
            if (Thread.currentThread().name == "batch") backgroundWaiting.countDown()
            Thread.yield()
        })
        governor.enter({ false }, false)
        fun requester(name: String, background: Boolean) = Thread({
            try {
                governor.enter(stop::get, background)
                order.add(name)
                governor.leave()
            } catch (e: Throwable) {
                failures.add(e)
            }
        }, name)
        val batch = requester("batch", true)
        val foreground = requester("foreground", false)
        try {
            batch.start()
            assertTrue(backgroundWaiting.await(2, TimeUnit.SECONDS))
            foreground.start()
            assertTrue(foregroundWaiting.await(2, TimeUnit.SECONDS))
            assertTrue(order.isEmpty())
            governor.leave()
            foreground.join(2000)
            batch.join(2000)
            assertEquals(listOf("foreground", "batch"), order.toList())
            assertTrue(failures.isEmpty())
        } finally {
            stop.set(true)
            foreground.join(2000)
            batch.join(2000)
        }
    }

    @Test
    fun `HTTP body completion and repeated close release exactly one in flight slot`() {
        val store = Store()
        var now = 1_800_000_000_000L
        var cancelled = false
        var stopOnSleep = false
        val governor = AniListHostGovernor(store, { now }, {
            now += it
            if (stopOnSleep) cancelled = true
        })
        val request = Request.Builder().url("https://graphql.anilist.co").build()
        val chain = mockk<Interceptor.Chain>()
        val call = mockk<Call>()
        every { chain.request() } returns request
        every { chain.call() } returns call
        every { call.isCanceled() } answers { cancelled }
        every { chain.proceed(request) } answers {
            Response.Builder().request(request).protocol(Protocol.HTTP_2).code(200).message("OK")
                .body("{}".toResponseBody()).build()
        }
        val interceptor = AniListQuotaInterceptor(governor)
        val first = interceptor.intercept(chain)
        assertEquals("{}", first.body.string())
        first.close()
        first.close()
        val second = interceptor.intercept(chain)
        now += 5000
        stopOnSleep = true
        assertThrows(IOException::class.java) { interceptor.intercept(chain) }
        assertTrue(cancelled)
        second.close()
        stopOnSleep = false
        governor.enter({ false }, false)
        governor.leave()
    }

    @Test
    fun `non AniList hosts keep their request and never consume the quota`() {
        val store = Store()
        val governor = AniListHostGovernor(store)
        val request = Request.Builder().url("https://api.aniskip.com/test").build()
        val chain = mockk<Interceptor.Chain>()
        val response = Response.Builder().request(request).protocol(Protocol.HTTP_2).code(200).message("OK")
            .body("{}".toResponseBody()).build()
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response
        assertEquals(response, AniListQuotaInterceptor(governor).intercept(chain))
        assertTrue(store.emissions.isEmpty())
        response.close()
    }

    @Test
    fun `wall clock jump cannot bypass live spacing or cooldown`() {
        val store = Store()
        var wall = 1_800_000_000_000L
        var tick = 0L
        val governor = AniListHostGovernor(
            store,
            { wall },
            {
                wall += it
                tick += it
            },
            monotonic = { tick },
        )
        governor.acquire { false }
        wall += 3_600_000
        governor.acquire { false }
        assertEquals(5_000, tick)
        governor.observe(headersOf("Retry-After", "120"), true)
        wall += 3_600_000
        governor.acquire { false }
        assertEquals(125_000, tick)
    }
}
