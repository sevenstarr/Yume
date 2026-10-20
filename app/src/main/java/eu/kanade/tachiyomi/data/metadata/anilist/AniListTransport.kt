package eu.kanade.tachiyomi.data.metadata.anilist

import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CookieJar
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// AM (Yume metadata, 2026-10-20; new code assisted by Codex) -->
internal data class AniListResponse(val code: Int, val headers: Headers, val body: String)

internal fun interface AniListTransport {
    suspend fun execute(payload: String): AniListResponse
}

/** Shares the app's connection pool/DNS/TLS, but no source/tracker interceptors or user state. */
internal class OkHttpAniListTransport(
    baseClient: OkHttpClient,
    private val callFactory: Call.Factory? = null,
) : AniListTransport {
    internal val client = baseClient.newBuilder().apply {
        interceptors().clear()
        networkInterceptors().clear()
        authenticator(okhttp3.Authenticator.NONE)
        proxyAuthenticator(okhttp3.Authenticator.NONE)
        cookieJar(CookieJar.NO_COOKIES)
        cache(null)
        followRedirects(false)
        followSslRedirects(false)
        retryOnConnectionFailure(false)
        connectTimeout(10, TimeUnit.SECONDS)
        readTimeout(20, TimeUnit.SECONDS)
        callTimeout(30, TimeUnit.SECONDS)
    }.build()

    override suspend fun execute(payload: String): AniListResponse = suspendCancellableCoroutine { continuation ->
        val body = payload.toRequestBody(jsonMime)
        val request = Request.Builder()
            .url("https://graphql.anilist.co")
            .header("Accept", "application/json")
            .header("User-Agent", "Yume-Metadata/1")
            // OkHttp can retry 503/Retry-After:0 even with retryOnConnectionFailure=false.
            // One-shot bodies prevent every transparent follow-up; provider retries create a new body.
            .post(object : RequestBody() {
                override fun contentType() = body.contentType()
                override fun contentLength() = body.contentLength()
                override fun isOneShot() = true
                override fun writeTo(sink: BufferedSink) = body.writeTo(sink)
            })
            .build()
        val call = (callFactory ?: client).newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!continuation.isCancelled) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val result = response.use {
                        // Keep cancellation attached until the body has been consumed, and bound memory use.
                        val source = it.body.source()
                        if (source.request(MAX_BODY_BYTES + 1)) throw AniListBodyTooLarge()
                        AniListResponse(it.code, it.headers, source.readUtf8())
                    }
                    if (!continuation.isCancelled) continuation.resume(result)
                } catch (e: IOException) {
                    if (!continuation.isCancelled) continuation.resumeWithException(e)
                }
            }
        })
    }

    private companion object {
        const val MAX_BODY_BYTES = 2L * 1024 * 1024
    }
}

internal class AniListBodyTooLarge : IOException()
// <-- AM (Yume metadata)
