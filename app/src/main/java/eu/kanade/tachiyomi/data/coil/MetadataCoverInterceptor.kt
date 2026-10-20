package eu.kanade.tachiyomi.data.coil

import coil3.Extras
import coil3.getOrDefault
import coil3.intercept.Interceptor
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.cache.CoverCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.domain.metadata.model.MetadataCachePolicy
import tachiyomi.domain.metadata.service.CoverResolver
import tachiyomi.domain.metadata.service.CoverSelection
import tachiyomi.domain.metadata.service.MetadataImageCache
import tachiyomi.domain.metadata.service.MetadataRepository

// AM (Yume finite cover fallback, 2026-10-20; new code assisted by Codex) -->
val originalCoverKey = Extras.Key(false)
internal val manualCoverAttemptKey = Extras.Key(false)

fun ImageRequest.Builder.originalCover(enabled: Boolean) = apply { extras[originalCoverKey] = enabled }

/** Runs before Coil's keying/fetch/decode. Each fallback re-enters the downstream chain with its own key. */
@Inject
@SingleIn(AppScope::class)
class MetadataCoverInterceptor(
    private val resolver: CoverResolver,
    private val repository: MetadataRepository,
    private val covers: CoverCache,
    private val imageCache: MetadataImageCache,
) : Interceptor {
    private data class Failure(val until: Long, val permanent: Boolean)
    private val failures = object : LinkedHashMap<String, Failure>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Failure>?): Boolean = size > 512
    }
    private class LoadLock(val mutex: Mutex = Mutex(), var users: Int = 0)
    private val locks = mutableMapOf<String, LoadLock>()

    @Volatile private var hostNotBefore = 0L

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        if (request.extras.getOrDefault(useBackgroundKey)) return chain.proceed()
        val local = when (val data = request.data) {
            is Anime -> CoverInput(data.id, data.source, data.thumbnailUrl, data.coverLastModified)
            is AnimeCover -> CoverInput(data.animeId, data.sourceId, data.url, data.lastModified)
            else -> return chain.proceed()
        }
        val selection = resolver.resolve(local.id)
        val previewOriginal = request.extras.getOrDefault(originalCoverKey)
        val allowManual = request.extras.getOrDefault(AnimeImageFetcher.USE_CUSTOM_COVER_KEY)
        if (!previewOriginal && !selection.originalOnly && allowManual &&
            withContext(Dispatchers.IO) { covers.getCustomCoverFile(local.id).exists() }
        ) {
            val manual = request.newBuilder()
                .memoryCacheKey("yume-manual:${local.id}:${local.modified}")
                .diskCacheKey("yume-manual:${local.id}:${local.modified}")
                .apply { extras[manualCoverAttemptKey] = true }
                .build()
            val result = chain.withRequest(manual).proceed()
            if (result is SuccessResult && !resolver.resolve(local.id).originalOnly) return result
        }
        if (!previewOriginal && !selection.originalOnly && selection.sourceId == local.source) {
            var cacheOnly = !request.networkCachePolicy.readEnabled || System.currentTimeMillis() < hostNotBefore
            for (asset in selection.external) {
                val failure = synchronized(failures) { failures[asset.key] }
                    ?.takeIf { it.until > System.currentTimeMillis() }
                if (failure?.permanent == true) continue
                var result: ImageResult? = null
                val eligible = try {
                    withLoadLock(asset.key) {
                        guardSelection(local.id, selection) {
                            repository.storeImage(
                                local.id,
                                selection.revision,
                                selection.epoch,
                                asset.key,
                                System.currentTimeMillis(),
                            ) {
                                val external = request.newBuilder().data(asset)
                                    .memoryCacheKey(asset.key).diskCacheKey(asset.key)
                                    .apply {
                                        if (cacheOnly || failure != null) networkCachePolicy(CachePolicy.DISABLED)
                                    }
                                    .build()
                                result = chain.withRequest(external).proceed()
                            }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    false
                }
                if (!eligible) break
                if (result is SuccessResult) {
                    // Also apply the current retention clock and shared selection after decode/cache hits.
                    if (resolver.resolve(local.id) == selection) return result!!
                    break
                }
                val cause = (result as? ErrorResult)?.throwable
                if (cause is CoverFetchFailure && cause.cacheMiss) continue
                val permanent = cause !is CoverFetchFailure || cause.assetFailure
                synchronized(failures) {
                    failures[asset.key] = Failure(
                        System.currentTimeMillis() + if (permanent) MetadataCachePolicy.DAY else 15 * 60_000L,
                        permanent,
                    )
                }
                try {
                    imageCache.remove(setOf(asset.key))
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    // Eviction failure must still allow the source fallback.
                }
                if (!permanent) {
                    cacheOnly = true
                    hostNotBefore = System.currentTimeMillis() + 15 * 60_000L
                }
            }
        }
        // Original request is finite and bypasses both manual and external selection.
        val original = request.newBuilder()
            .memoryCacheKey("anime;${local.url};${local.modified}")
            .diskCacheKey("anime;${local.url};${local.modified}")
            .apply {
                extras[AnimeImageFetcher.USE_CUSTOM_COVER_KEY] = false
                extras[manualCoverAttemptKey] = false
                extras[originalCoverKey] = true
            }.build()
        return chain.withRequest(original).proceed()
    }

    private suspend fun <T> withLoadLock(key: String, block: suspend () -> T): T {
        val lock = synchronized(locks) { locks.getOrPut(key) { LoadLock() }.also { it.users++ } }
        try {
            return lock.mutex.withLock { block() }
        } finally {
            synchronized(locks) { if (--lock.users == 0) locks.remove(key) }
        }
    }

    private class SelectionChanged : CancellationException()

    private suspend fun guardSelection(
        animeId: Long,
        selection: CoverSelection,
        load: suspend () -> Boolean,
    ): Boolean = coroutineScope {
        val loading = async { load() }
        val watcher = launch {
            resolver.observe(listOf(animeId)).collect {
                if (it[animeId] != selection) loading.cancel(SelectionChanged())
            }
        }
        try {
            loading.await()
        } catch (_: SelectionChanged) {
            false
        } finally {
            watcher.cancel()
        }
    }

    private data class CoverInput(val id: Long, val source: Long, val url: String?, val modified: Long)
}
// <-- AM (Yume finite cover fallback)
