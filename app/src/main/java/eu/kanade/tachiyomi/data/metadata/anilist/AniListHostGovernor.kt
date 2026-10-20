package eu.kanade.tachiyomi.data.metadata.anilist

import android.content.Context
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.buffer
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// AM (Yume shared AniList host quota, 2026-10-20; new code assisted by Codex)
internal interface AniListQuotaStore {
    fun read(): Pair<List<Long>, Long>
    fun write(emissions: List<Long>, blockedUntil: Long)
    fun readLimit(): Int = 12
    fun writeLimit(limit: Int) = Unit
    fun readCircuit(): Pair<Int, Long> = 0 to 300_000L
    fun writeCircuit(failures: Int, duration: Long) = Unit
}

/** One durable window for every first-party GraphQL consumer, including HTTP follow-ups/retries. */
internal class AniListHostGovernor(
    private val store: AniListQuotaStore,
    private val now: () -> Long = System::currentTimeMillis,
    private val sleep: (Long) -> Unit = Thread::sleep,
    private val monotonic: () -> Long = now,
) {
    private val lock = ReentrantLock(true)
    private var limit = store.readLimit().coerceIn(1, 12)
    private val inFlight = Semaphore(1, true)
    private val foregroundWaiting = AtomicInteger()
    private var lastTick = Long.MIN_VALUE
    private var nextTick = Long.MIN_VALUE
    private var blockedTick = Long.MIN_VALUE

    fun acquire(cancelled: () -> Boolean) = acquire(cancelled, false, true)

    fun enter(cancelled: () -> Boolean, background: Boolean) {
        if (!background) foregroundWaiting.incrementAndGet()
        try {
            acquire(cancelled, true, background)
        } finally {
            if (!background) foregroundWaiting.decrementAndGet()
        }
    }

    fun leave() = inFlight.release()

    fun notBefore(): Long? = lock.withLock {
        val time = now()
        val tick = monotonic()
        val live = if (blockedTick > tick) add(time, blockedTick - tick) else 0
        maxOf(store.read().second, live).takeIf { it > time }
    }

    fun transientFailure() = lock.withLock {
        val (previous, duration) = store.readCircuit()
        val failures = (previous + 1).coerceAtMost(5)
        if (failures >= 5) {
            val (emissions, blocked) = store.read()
            store.write(emissions, maxOf(blocked, now() + duration.coerceIn(300_000, 3_600_000)))
            blockedTick = maxOf(blockedTick, monotonic() + duration.coerceIn(300_000, 3_600_000))
            store.writeCircuit(failures, (duration * 2).coerceAtMost(3_600_000))
        } else {
            store.writeCircuit(failures, duration)
        }
    }

    fun success() = lock.withLock { store.writeCircuit(0, 300_000) }

    private fun acquire(cancelled: () -> Boolean, requireSlot: Boolean, background: Boolean) {
        while (true) {
            if (cancelled()) throw IOException("Cancelled")
            val wait = lock.withLock {
                val time = now()
                val (saved, blocked) = store.read()
                val tick = monotonic()
                val untilTick = maxOf(nextTick, blockedTick)
                val liveDelay = if (untilTick > tick) untilTick - tick else 0
                // Future emissions mean the wall clock moved backwards: retain a full conservative window.
                val emissions = saved.filter { it > time - 60_000 }.map { minOf(it, time) }.sorted()
                val allowed = maxOf(
                    time,
                    add(time, liveDelay),
                    blocked,
                    emissions.lastOrNull()?.plus(5_000) ?: 0,
                    if (emissions.size >= limit) emissions[emissions.size - limit] + 60_000 else 0,
                )
                if (allowed <= time) {
                    if (requireSlot && ((background && foregroundWaiting.get() > 0) || !inFlight.tryAcquire())) {
                        return@withLock 100L
                    }
                    // Commit before sending. A crash can overcount an emission but never refunds one.
                    try {
                        store.write(emissions + time, blocked)
                        lastTick = monotonic()
                        nextTick = add(lastTick, spacing())
                    } catch (e: Exception) {
                        if (requireSlot) inFlight.release()
                        throw e
                    }
                    0L
                } else {
                    (allowed - time).coerceAtMost(100)
                }
            }
            if (wait == 0L) return
            try {
                sleep(wait)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Cancelled")
            }
        }
    }

    fun observe(headers: Headers, rateLimited: Boolean) = lock.withLock {
        headers["X-RateLimit-Limit"]?.toIntOrNull()?.takeIf { it > 0 }?.let {
            limit = minOf(limit, it)
            store.writeLimit(limit)
            if (lastTick != Long.MIN_VALUE) nextTick = maxOf(nextTick, add(lastTick, spacing()))
        }
        if (!rateLimited && headers["X-RateLimit-Remaining"]?.toIntOrNull() != 0) return@withLock
        val time = now()
        val serverTime = date(headers["Date"]) ?: time
        val retry = headers["Retry-After"]?.let {
            it.toLongOrNull()?.takeIf { seconds -> seconds in 0..(Long.MAX_VALUE / 1_000) }?.times(1_000)
                ?: date(it)?.let { target -> (target - serverTime).coerceAtLeast(0) }
        } ?: 0
        val reset = headers["X-RateLimit-Reset"]?.toLongOrNull()
            ?.takeIf { it in 0..(Long.MAX_VALUE / 1_000) }
            ?.let { (it * 1_000 - serverTime).coerceAtLeast(0) } ?: 0
        val delay = maxOf(60_000, retry, reset)
        val until = if (time > Long.MAX_VALUE - delay) Long.MAX_VALUE else time + delay
        blockedTick = maxOf(blockedTick, add(monotonic(), delay))
        val (emissions, blocked) = store.read()
        store.write(emissions, maxOf(until, blocked))
    }

    private fun date(value: String?): Long? = runCatching {
        value?.let { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
    }.getOrNull()

    private fun spacing() = maxOf(5_000L, (60_000L + limit - 1) / limit)
    private fun add(base: Long, delay: Long) = if (base > Long.MAX_VALUE - delay) Long.MAX_VALUE else base + delay
}

/** Install before graph initialization; all adapters share this object and never carry OAuth state. */
internal object AniListHostInterceptor : Interceptor {
    @Volatile private var governor: AniListHostGovernor? = null

    fun observe(headers: Headers, rateLimited: Boolean) {
        (governor ?: throw IOException("AniList quota unavailable")).observe(headers, rateLimited)
    }

    fun transientFailure() {
        (governor ?: throw IOException("AniList quota unavailable")).transientFailure()
    }

    fun success() {
        (governor ?: throw IOException("AniList quota unavailable")).success()
    }

    fun notBefore(): Long? = (governor ?: throw IOException("AniList quota unavailable")).notBefore()

    fun install(context: Context) {
        val preferences = context.getSharedPreferences("anilist_host_quota_v1", Context.MODE_PRIVATE)
        governor = AniListHostGovernor(
            object : AniListQuotaStore {
                override fun readCircuit() = Pair(
                    preferences.getInt("failures", 0),
                    preferences.getLong("circuit", 300_000),
                )
                override fun writeCircuit(failures: Int, duration: Long) {
                    if (!preferences.edit().putInt("failures", failures).putLong("circuit", duration).commit()) {
                        throw IOException("Unable to persist AniList quota")
                    }
                }

                override fun readLimit() = preferences.getInt("limit", 12)
                override fun writeLimit(limit: Int) {
                    if (!preferences.edit().putInt("limit", limit).commit()) {
                        throw IOException("Unable to persist AniList quota")
                    }
                }

                override fun read() = Pair(
                    preferences.getString("emissions", "").orEmpty().split(',').mapNotNull(String::toLongOrNull),
                    preferences.getLong("blocked", 0),
                )

                override fun write(emissions: List<Long>, blockedUntil: Long) {
                    if (!preferences.edit().putString("emissions", emissions.joinToString(","))
                            .putLong("blocked", blockedUntil).commit()
                    ) {
                        throw IOException("Unable to persist AniList quota")
                    }
                }
            },
            monotonic = { System.nanoTime() / 1_000_000 },
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.host != "graphql.anilist.co") return chain.proceed(chain.request())
        val budget = governor ?: throw IOException("AniList quota unavailable")
        return AniListQuotaInterceptor(budget).intercept(chain)
    }
}

internal class AniListQuotaInterceptor(private val budget: AniListHostGovernor) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.host != "graphql.anilist.co") return chain.proceed(chain.request())
        budget.enter({ chain.call().isCanceled() }, chain.request().tag(AniListBackground::class.java) != null)
        val released = AtomicBoolean(false)
        fun release() {
            if (released.compareAndSet(false, true)) budget.leave()
        }
        return try {
            val response = chain.proceed(chain.request())
            try {
                if (response.code >= 500) {
                    budget.transientFailure()
                } else if (response.code != 429 && chain.request().header("User-Agent") != "Yume-Metadata/1") {
                    budget.success()
                }
                budget.observe(response.headers, response.code == 429)
            } catch (e: Exception) {
                response.close()
                throw e
            }
            val body = response.body
            val source = object : ForwardingSource(body.source()) {
                override fun read(sink: Buffer, byteCount: Long): Long = try {
                    super.read(sink, byteCount).also { if (it == -1L) release() }
                } catch (e: Exception) {
                    try {
                        if (e is IOException && !chain.call().isCanceled()) budget.transientFailure()
                    } finally {
                        release()
                    }
                    throw e
                }

                override fun close() {
                    try {
                        super.close()
                    } finally {
                        release()
                    }
                }
            }.buffer()
            response.newBuilder().body(object : ResponseBody() {
                override fun contentType() = body.contentType()
                override fun contentLength() = body.contentLength()
                override fun source() = source
            }).build()
        } catch (e: Exception) {
            try {
                if (e is IOException && !chain.call().isCanceled()) budget.transientFailure()
            } finally {
                release()
            }
            throw e
        }
    }
}

internal object AniListBackground
