package eu.kanade.tachiyomi.data.coil

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.ByteString
import okio.FileSystem
import tachiyomi.domain.metadata.service.CoverPolicy
import tachiyomi.domain.metadata.service.ExternalCover
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// AM (Yume isolated cover transport, 2026-10-20; new code assisted by Codex) -->
internal class CoverFetchFailure(val assetFailure: Boolean = false, val cacheMiss: Boolean = false) : IOException()

internal class CoverTransport(base: OkHttpClient, private val calls: Call.Factory? = null) {
    val client = base.newBuilder().apply {
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
        // Prevent OkHttp's follow-up for 503/Retry-After:0 and coalesced 421 as well.
        addNetworkInterceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code in listOf(408, 421, 503)) {
                response.close()
                throw CoverFetchFailure()
            }
            response
        }
    }.build()

    suspend fun read(url: String): ByteString {
        if (!CoverPolicy.safeUrl(url)) throw CoverFetchFailure(assetFailure = true)
        return suspendCancellableCoroutine { continuation ->
            val request = Request.Builder().url(url)
                .header("Accept", "image/*").header("User-Agent", "Yume-Metadata/1").build()
            val call = (calls ?: client).newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) continuation.resumeWithException(CoverFetchFailure())
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        val bytes = response.use {
                            if (!it.isSuccessful) throw CoverFetchFailure(it.code in listOf(404, 410))
                            val source = it.body.source()
                            if (source.request(MAX_BYTES + 1)) throw CoverFetchFailure(assetFailure = true)
                            source.readByteString()
                        }
                        if (!continuation.isCancelled) continuation.resume(bytes)
                    } catch (e: IOException) {
                        val failure = e as? CoverFetchFailure ?: CoverFetchFailure()
                        if (!continuation.isCancelled) continuation.resumeWithException(failure)
                    }
                }
            })
        }
    }

    private companion object {
        const val MAX_BYTES = 16L * 1024 * 1024
    }
}

internal class ExternalCoverFetcher(
    private val cover: ExternalCover,
    private val options: Options,
    private val loader: ImageLoader,
    private val transport: CoverTransport,
) : Fetcher {
    override suspend fun fetch(): SourceFetchResult {
        val cache = loader.diskCache
        if (options.diskCachePolicy.readEnabled) {
            cache?.openSnapshot(cover.key)?.let { snapshot ->
                return SourceFetchResult(
                    ImageSource(snapshot.data, cache.fileSystem, cover.key, snapshot),
                    "image/*",
                    DataSource.DISK,
                )
            }
        }
        if (!options.networkCachePolicy.readEnabled) throw CoverFetchFailure(cacheMiss = true)
        val bytes = transport.read(cover.url)
        if (options.diskCachePolicy.writeEnabled) {
            val editor = cache?.openEditor(cover.key)
            if (editor != null) {
                try {
                    cache.fileSystem.write(editor.metadata) { }
                    cache.fileSystem.write(editor.data) { write(bytes) }
                    val snapshot = editor.commitAndOpenSnapshot()
                    if (snapshot != null) {
                        return SourceFetchResult(
                            ImageSource(snapshot.data, cache.fileSystem, cover.key, snapshot),
                            "image/*",
                            DataSource.NETWORK,
                        )
                    }
                } catch (e: Exception) {
                    editor.abort()
                    throw e
                }
            }
        }
        return SourceFetchResult(
            ImageSource(Buffer().write(bytes), FileSystem.SYSTEM),
            "image/*",
            DataSource.NETWORK,
        )
    }

    class Factory(network: NetworkHelper) : Fetcher.Factory<ExternalCover> {
        private val transport by lazy { CoverTransport(network.client) }
        override fun create(data: ExternalCover, options: Options, imageLoader: ImageLoader): Fetcher =
            ExternalCoverFetcher(data, options, imageLoader, transport)
    }
}
// <-- AM (Yume isolated cover transport)
