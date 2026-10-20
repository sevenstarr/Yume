package eu.kanade.tachiyomi.data.metadata.anilist

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import okhttp3.Headers
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.random.Random

// AM (Yume metadata, 2026-10-20; new code assisted by Codex) -->
internal interface AniListClock {
    fun monotonicMillis(): Long
    fun wallMillis(): Long
    suspend fun sleep(millis: Long)
}

internal object SystemAniListClock : AniListClock {
    override fun monotonicMillis() = System.nanoTime() / 1_000_000
    override fun wallMillis() = System.currentTimeMillis()
    override suspend fun sleep(millis: Long) = delay(millis)
}

/** Process-local budget. One instance must be shared by all metadata calls, including retries. */
internal class AniListBudget(
    val clock: AniListClock = SystemAniListClock,
    private val onObserved: (Headers, Boolean) -> Unit = { _, _ -> },
    private val onGraphqlFailure: () -> Unit = {},
    private val onSuccess: () -> Unit = {},
    val jitter: () -> Long = { Random.nextLong(0, 501) },
) {
    val mutex = Mutex()
    private val emissions = ArrayDeque<Long>()
    private var limit = 12
    private var nextEmission = Long.MIN_VALUE
    private var blockedUntil = Long.MIN_VALUE
    private var failures = 0
    private var circuitDuration = 300_000L
    private var circuitUntil = Long.MIN_VALUE

    /** Called while holding mutex. A delay larger than the operation deadline is returned, never slept. */
    suspend fun acquire(deadline: Long): Long? {
        val now = clock.monotonicMillis()
        while (emissions.isNotEmpty() && emissions.first() <= now - 60_000) emissions.removeFirst()
        val window = if (emissions.size >= limit) emissions[emissions.size - limit] + 60_000 else now
        val allowed = maxOf(now, nextEmission, blockedUntil, circuitUntil, window)
        if (allowed >= deadline) return addDelay(clock.wallMillis(), allowed - now)
        if (allowed > now) clock.sleep(allowed - now)
        val emittedAt = clock.monotonicMillis()
        emissions.addLast(emittedAt)
        nextEmission = emittedAt + 2_500
        return null
    }

    fun observe(headers: Headers, rateLimited: Boolean): Long? {
        onObserved(headers, rateLimited)
        headers["X-RateLimit-Limit"]?.toIntOrNull()?.takeIf { it > 0 }?.let { limit = minOf(limit, it) }
        val now = clock.monotonicMillis()
        val serverNow = httpDate(headers["Date"]) ?: clock.wallMillis()
        val retry = headers["Retry-After"]?.let { value ->
            value.toLongOrNull()?.takeIf { it in 0..MAX_SECONDS }?.times(1_000)
                ?: httpDate(value)?.let { (it - serverNow).takeIf { duration -> duration >= 0 } }
        }
        val reset = headers["X-RateLimit-Reset"]?.toLongOrNull()
            ?.takeIf { it in 0..(Long.MAX_VALUE / 1_000) }
            ?.let { (it * 1_000 - serverNow).takeIf { duration -> duration >= 0 } }
        if (rateLimited || headers["X-RateLimit-Remaining"]?.toIntOrNull() == 0) {
            val wait = maxOf(retry ?: 0, reset ?: 0, if (retry == null && reset == null) 60_000 else 0)
            blockedUntil = max(blockedUntil, addDelay(addDelay(now, wait), jitter().coerceIn(0, 500)))
        }
        return blockedUntil.takeIf { it > now }?.let { addDelay(clock.wallMillis(), it - now) }
    }

    fun transientFailure() {
        failures++
        if (failures >= 5) {
            circuitUntil = clock.monotonicMillis() + circuitDuration
            circuitDuration = (circuitDuration * 2).coerceAtMost(3_600_000)
        }
    }

    fun graphqlFailure() = onGraphqlFailure()

    fun success() {
        onSuccess()
        failures = 0
        circuitUntil = Long.MIN_VALUE
        circuitDuration = 300_000
    }

    fun isCircuitOpen() = clock.monotonicMillis() < circuitUntil

    private fun addDelay(base: Long, delay: Long) = if (base > Long.MAX_VALUE - delay) Long.MAX_VALUE else base + delay

    private fun httpDate(value: String?): Long? = try {
        value?.let { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
    } catch (_: java.time.DateTimeException) {
        null
    }

    private companion object {
        const val MAX_SECONDS = Long.MAX_VALUE / 1_000
    }
}
// <-- AM (Yume metadata)
