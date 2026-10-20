package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.Image
import coil3.getOrDefault
import coil3.intercept.Interceptor
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Size
import eu.kanade.tachiyomi.data.cache.CoverCache
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.asAnimeCover
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.service.CoverResolver
import tachiyomi.domain.metadata.service.CoverSelection
import tachiyomi.domain.metadata.service.ExternalCover
import tachiyomi.domain.metadata.service.MetadataImageCache
import tachiyomi.domain.metadata.service.MetadataRepository
import java.nio.file.Files

// AM (Yume Coil chain contract tests, 2026-10-20; new code assisted by Codex) -->
class MetadataCoverInterceptorTest {
    private val context = mockk<Context>(relaxed = true)
    private val resolver = mockk<CoverResolver>()
    private val repository = mockk<MetadataRepository>()
    private val covers = mockk<CoverCache>()
    private val directory = Files.createTempDirectory("yume-cover-chain")
    private val manual = directory.resolve("manual.jpg")
    private val removed = mutableSetOf<String>()
    private val interceptor =
        MetadataCoverInterceptor(resolver, repository, covers, MetadataImageCache { removed.addAll(it) })
    private val anime = Anime.create().copy(id = 1, source = 42, thumbnailUrl = "https://source.invalid/original.jpg")
    private val assets = (1..3).map {
        ExternalCover(
            MetadataId("anilist", "1"),
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/$it.jpg",
            "yume-metadata-v1:anilist:1:${it.toString().repeat(64)}:1",
        )
    }
    private var selection = CoverSelection(revision = 2, epoch = 3, sourceId = 42, external = assets)
    private val seen = mutableListOf<ImageRequest>()

    init {
        every { covers.getCustomCoverFile(any()) } returns manual.toFile()
        coEvery { resolver.resolve(1) } answers { selection }
        every { resolver.observe(any()) } answers { flowOf(mapOf(1L to selection)) }
        coEvery { repository.storeImage(any(), any(), any(), any(), any(), any()) } coAnswers {
            arg<suspend () -> Unit>(5).invoke()
            true
        }
    }

    @AfterEach
    fun cleanup() {
        Files.deleteIfExists(manual)
        Files.delete(directory)
    }

    private class Chain(
        override val request: ImageRequest,
        private val seen: MutableList<ImageRequest>,
        private val load: suspend (ImageRequest) -> ImageResult,
    ) : Interceptor.Chain {
        override var size: Size = Size.ORIGINAL
            private set
        override fun withRequest(request: ImageRequest) = Chain(request, seen, load).also { it.size = size }
        override fun withSize(size: Size) = Chain(request, seen, load).also { it.size = size }
        override suspend fun proceed(): ImageResult {
            seen.add(request)
            return load(request)
        }
    }
    private fun success(request: ImageRequest) = SuccessResult(mockk<Image>(), request)
    private fun error(request: ImageRequest, cause: Throwable = CoverFetchFailure()) = ErrorResult(null, request, cause)
    private fun request(model: Any = anime) = ImageRequest.Builder(context).data(model).build()

    @Test
    fun `both existing models use external snapshot before keying and preserve request constraints`() = runTest {
        for (model in listOf(anime, anime.asAnimeCover())) {
            val original = request(model).newBuilder().memoryCacheKey("caller-key").diskCacheKey("caller-key").build()
            val result = interceptor.intercept(Chain(original, seen, ::success))
            assertTrue(result is SuccessResult)
            assertEquals(assets.first(), result.request.data)
            assertEquals(assets.first().key, result.request.memoryCacheKey)
            assertEquals(assets.first().key, result.request.diskCacheKey)
            assertSame(original.context, result.request.context)
            assertSame(original.target, result.request.target)
            assertSame(original.sizeResolver, result.request.sizeResolver)
        }
        assertEquals(2, seen.size)
    }

    @Test
    fun `manual file wins and decode failure falls through without deleting user bytes`() = runTest {
        Files.writeString(manual, "user bytes")
        interceptor.intercept(Chain(request(), seen, ::success))
        assertEquals(1, seen.size)
        assertTrue(seen.single().extras.getOrDefault(manualCoverAttemptKey))
        seen.clear()
        val result = interceptor.intercept(
            Chain(request(), seen) {
                if (it.data is ExternalCover) success(it) else error(it, IllegalStateException("decode"))
            },
        )
        assertTrue(result.request.data is ExternalCover)
        assertEquals(2, seen.size)
        assertEquals("user bytes", Files.readString(manual))
    }

    @Test
    fun `404 and decode errors try distinct variants then quarantine failed assets`() = runTest {
        val result = interceptor.intercept(
            Chain(request(), seen) {
                when (it.data) {
                    assets[0] -> error(it, CoverFetchFailure(assetFailure = true))
                    assets[1] -> error(it, IllegalStateException("decode"))
                    else -> success(it)
                }
            },
        )
        assertEquals(assets[2], result.request.data)
        assertEquals(assets.take(2).map { it.key }.toSet(), removed)
        seen.clear()
        interceptor.intercept(Chain(request(), seen, ::success))
        assertEquals(listOf(assets[2]), seen.map { it.data })
    }

    @Test
    fun `offline tries other variants only in cache and uses an eligible cached image`() = runTest {
        val result = interceptor.intercept(
            Chain(request(), seen) {
                when (it.data) {
                    assets[0] -> error(it)
                    assets[1] -> error(it, CoverFetchFailure(cacheMiss = true))
                    else -> success(it)
                }
            },
        )
        assertEquals(assets[2], result.request.data)
        assertTrue(seen.first().networkCachePolicy.readEnabled)
        assertTrue(seen.drop(1).all { !it.networkCachePolicy.readEnabled })
    }

    @Test
    fun `cache only remains cache only through original failure and terminates`() = runTest {
        val start = request().newBuilder().networkCachePolicy(CachePolicy.DISABLED).build()
        val result = interceptor.intercept(
            Chain(start, seen) {
                error(it, CoverFetchFailure(cacheMiss = true))
            },
        )
        assertTrue(result is ErrorResult)
        assertEquals(4, seen.size)
        assertTrue(seen.all { !it.networkCachePolicy.readEnabled })
        assertEquals(anime, seen.last().data)
        assertEquals("anime;${anime.thumbnailUrl};${anime.coverLastModified}", seen.last().diskCacheKey)
        assertFalse(seen.last().extras.getOrDefault(AnimeImageFetcher.USE_CUSTOM_COVER_KEY))
    }

    @Test
    fun `preview and persisted original mode bypass both user file and external`() = runTest {
        Files.writeString(manual, "user bytes")
        interceptor.intercept(Chain(request().newBuilder().originalCover(true).build(), seen, ::success))
        assertEquals(1, seen.size)
        assertFalse(seen.single().extras.getOrDefault(AnimeImageFetcher.USE_CUSTOM_COVER_KEY))
        seen.clear()
        selection = selection.copy(originalOnly = true, external = emptyList())
        interceptor.intercept(Chain(request(), seen, ::success))
        assertEquals(1, seen.size)
        assertEquals(anime, seen.single().data)
        assertTrue(Files.exists(manual))
    }

    @Test
    fun `global off keeps manual first while disabled custom cover skips it`() = runTest {
        selection = selection.copy(external = emptyList())
        Files.writeString(manual, "user bytes")
        interceptor.intercept(Chain(request(), seen, ::success))
        assertTrue(seen.single().extras.getOrDefault(manualCoverAttemptKey))
        seen.clear()
        val start = request().newBuilder().apply { extras[AnimeImageFetcher.USE_CUSTOM_COVER_KEY] = false }.build()
        interceptor.intercept(Chain(start, seen, ::success))
        assertFalse(seen.single().extras.getOrDefault(AnimeImageFetcher.USE_CUSTOM_COVER_KEY))
    }

    @Test
    fun `cancellation stops fallback immediately`() = runTest {
        try {
            interceptor.intercept(Chain(request(), seen) { throw CancellationException() })
            fail<Unit>("Cancellation was swallowed")
        } catch (_: CancellationException) {
            assertEquals(1, seen.size)
        }
        assertTrue(removed.isEmpty())
    }

    @Test
    fun `clear during decode refuses publication and source uses a separate key`() = runTest {
        coEvery { repository.storeImage(any(), any(), any(), any(), any(), any()) } coAnswers {
            arg<suspend () -> Unit>(5).invoke()
            false
        }
        val result = interceptor.intercept(Chain(request(), seen, ::success))
        assertEquals(anime, result.request.data)
        assertEquals(2, seen.size)
        assertFalse(result.request.memoryCacheKey!!.startsWith("yume-metadata-v1:"))
    }

    @Test
    fun `revision change after memory hit refuses the previous image`() = runTest {
        val result = interceptor.intercept(
            Chain(request(), seen) {
                if (it.data is ExternalCover) selection = selection.copy(revision = 10, external = emptyList())
                success(it)
            },
        )
        assertEquals(anime, result.request.data)
        assertEquals(2, seen.size)
    }

    @Test
    fun `background thumbnails and bare URLs keep their existing chain`() = runTest {
        for (start in listOf(
            request().newBuilder().useBackground(true).build(),
            request("https://source.invalid/a.jpg"),
        )) {
            interceptor.intercept(Chain(start, seen, ::success))
            assertSame(start, seen.last())
        }
        coVerify(exactly = 0) { resolver.resolve(any()) }
        coVerify(exactly = 0) { repository.storeImage(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun `turning off while loading cancels the external attempt and returns original`() = runTest {
        val snapshots = MutableStateFlow(mapOf(1L to selection))
        every { resolver.observe(any()) } returns snapshots
        val started = CompletableDeferred<Unit>()
        var cancelled = false
        val pending = async {
            interceptor.intercept(
                Chain(request(), seen) {
                    if (it.data is ExternalCover) {
                        started.complete(Unit)
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled = true
                        }
                    }
                    success(it)
                },
            )
        }
        started.await()
        selection = selection.copy(epoch = selection.epoch + 1, external = emptyList())
        snapshots.value = mapOf(1L to selection)
        assertEquals(anime, pending.await().request.data)
        assertTrue(cancelled)
        assertEquals(2, seen.size)
    }

    @Test
    fun `same asset requests share a lock while different asset writes can proceed independently`() = runTest {
        val started = CompletableDeferred<Unit>()
        val resume = CompletableDeferred<Unit>()
        val chain = Chain(request(), seen) {
            if (!started.isCompleted) {
                started.complete(Unit)
                resume.await()
            }
            success(it)
        }
        val first = async { interceptor.intercept(chain) }
        started.await()
        val second = async { interceptor.intercept(chain) }
        runCurrent()
        assertEquals(1, seen.size)
        resume.complete(Unit)
        assertTrue(first.await() is SuccessResult)
        assertTrue(second.await() is SuccessResult)
        assertEquals(2, seen.size)
    }

    @Test
    fun `metadata storage failure still loads the source cover`() = runTest {
        coEvery { repository.storeImage(any(), any(), any(), any(), any(), any()) } throws
            IllegalStateException("storage")
        val result = interceptor.intercept(Chain(request(), seen, ::success))
        assertEquals(anime, result.request.data)
        assertEquals(1, seen.size)
    }
}
// <-- AM (Yume Coil chain contract tests)
