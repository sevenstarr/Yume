package eu.kanade.tachiyomi.data.coil

import android.content.Context
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.request.CachePolicy
import coil3.request.Options
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okio.ByteString.Companion.encodeUtf8
import okio.Path.Companion.toOkioPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.service.ExternalCover
import java.nio.file.Files

// AM (Yume physical Coil cache tests, 2026-10-20; new code assisted by Codex) -->
class ExternalCoverFetcherTest {
    @Test
    fun `Coil disk bytes survive reopen and cache only never invokes transport`() = runTest {
        val directory = Files.createTempDirectory("yume-coil-cache")
        val cover = ExternalCover(
            MetadataId("anilist", "1"),
            "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/test.jpg",
            "yume-metadata-v1:anilist:1:${"a".repeat(64)}:1",
        )
        val bytes = "synthetic image bytes; decode is exercised by separate chain contract".encodeUtf8()
        val transport = mockk<CoverTransport>()
        coEvery { transport.read(cover.url) } returns bytes
        val loader = mockk<ImageLoader>()
        val context = mockk<Context>(relaxed = true)
        fun cache() = DiskCache.Builder().directory(directory.toFile().toOkioPath()).maxSizeBytes(1024 * 1024).build()
        var disk = cache()
        every { loader.diskCache } answers { disk }
        try {
            val fetched = ExternalCoverFetcher(cover, Options(context), loader, transport).fetch()
            assertEquals(bytes, fetched.source.source().readByteString())
            fetched.source.close()
            disk.openSnapshot(cover.key).use { assertTrue(it != null) }
            disk.shutdown()
            disk = cache()
            val offline = Options(context, networkCachePolicy = CachePolicy.DISABLED)
            val cached = ExternalCoverFetcher(cover, offline, loader, transport).fetch()
            assertEquals(bytes, cached.source.source().readByteString())
            cached.source.close()
            coVerify(exactly = 1) { transport.read(any()) }
            disk.remove(cover.key)
            var cacheMiss = false
            try {
                ExternalCoverFetcher(cover, offline, loader, transport).fetch()
            } catch (
                e: CoverFetchFailure,
            ) {
                cacheMiss = e.cacheMiss
            }
            assertTrue(cacheMiss)
            coVerify(exactly = 1) { transport.read(any()) }
        } finally {
            disk.shutdown()
            Files.walk(directory).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach { Files.delete(it) } }
        }
    }
}
// <-- AM (Yume physical Coil cache tests)
