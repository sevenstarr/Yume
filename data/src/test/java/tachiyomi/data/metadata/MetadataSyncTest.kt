package tachiyomi.data.metadata

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.DatabaseBindings
import tachiyomi.data.anime.AnimeRepositoryImpl
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MetadataCachePolicy
import tachiyomi.domain.metadata.model.MetadataFailureKind
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataPage
import tachiyomi.domain.metadata.model.MetadataResult
import tachiyomi.domain.metadata.model.MetadataTitles
import tachiyomi.domain.metadata.service.MetadataCapabilities
import tachiyomi.domain.metadata.service.MetadataImageCache
import tachiyomi.domain.metadata.service.MetadataProvider
import tachiyomi.domain.metadata.service.MetadataSearch
import tachiyomi.domain.metadata.service.MetadataSyncCoordinator
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

// AM (Yume background sync integration tests, 2026-10-20; new code assisted by Codex)
class MetadataSyncTest {
    private var now = 1_800_000_000_000L
    private val media = MetadataMedia(
        MetadataId("anilist", "1"), null, MetadataTitles("Test anime", null, null), emptyList(),
        null, null, null, null, null, null, emptyList(),
    )

    private inner class Store(val driver: JdbcSqliteDriver) : AutoCloseable {
        val db = DatabaseBindings.providesDatabase(driver)
        val repo = MetadataRepositoryImpl(db, MetadataImageCache { })
        val queue = MetadataSyncQueueImpl(db)
        var calls = 0
        var requested = emptyList<MetadataId>()
        var response: MetadataResult<MetadataPage> = MetadataResult.Success(MetadataPage(listOf(media), 1, false))
        var lookup: suspend () -> MetadataResult<MetadataPage> = { response }
        val provider = object : MetadataProvider {
            override val capabilities = MetadataCapabilities("anilist", 20, emptySet())
            override suspend fun search(search: MetadataSearch): MetadataResult<MetadataPage> {
                calls++
                return lookup()
            }
            override suspend fun getById(id: MetadataId): MetadataResult<MetadataMedia> {
                calls++
                requested += id
                return MetadataResult.Success(media.copy(id = id))
            }
        }
        val coordinator = MetadataSyncCoordinator(queue, repo, provider) { now }
        override fun close() = driver.close()
    }

    private suspend fun fresh(count: Int = 1): Store = Store(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)).also {
        Database.Schema.create(it.driver).await()
        it.driver.execute(null, "PRAGMA foreign_keys=ON", 0).await()
        seed(it, count)
    }

    private suspend fun seed(store: Store, count: Int) {
        store.driver.execute(
            null,
            """WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM n WHERE x < $count)
                INSERT INTO animes(_id,source,url,title,status,initialized,viewer,episode_flags,
                cover_last_modified,season_flags,season_number,season_source_order,background_last_modified,
                favorite_at,thumbnail_url,custom_title)
                SELECT x,42,'/'||x,'Test anime',2,1,0,0,123,0,-1,0,0,1000,
                'https://source.invalid/original.jpg','Local title' FROM n
            """.trimIndent(),
            0,
        ).await()
    }

    @Test
    fun `off never queues or emits and enabling alone does not scan`() = runTest {
        fresh().use {
            assertFalse(it.queue.start())
            assertFalse(it.coordinator.runSlice())
            it.repo.setEnabled(true)
            assertFalse(it.coordinator.runSlice())
            assertEquals(0, it.calls)
        }
    }

    @Test
    fun `large library slices are bounded and resume without repeating completed items`() = runTest {
        fresh(2000).use {
            it.repo.setEnabled(true)
            assertTrue(it.queue.start())
            assertFalse(it.queue.start())
            assertTrue(it.coordinator.runSlice(20))
            assertEquals(20, it.calls)
            assertEquals(2000, it.queue.status().total)
            assertEquals(20, it.queue.status().ambiguous)
            it.queue.transition("PAUSED")
            assertFalse(it.coordinator.runSlice())
            it.queue.transition("RUNNING")
            it.coordinator.runSlice(20)
            assertEquals(40, it.calls)
            assertEquals(40, it.queue.status().completed)
            while (it.coordinator.runSlice(100)) {
                // No real HTTP/sleep; drive all persisted slices to completion.
            }
            assertEquals(2000, it.calls)
            assertEquals(2000, it.queue.status().completed)
            assertEquals("COMPLETED", it.queue.status().status)
        }
    }

    @Test
    fun `manual pin refreshes only its exact ID and originals stay equal`() = runTest {
        fresh().use {
            val original = it.db.animesQueries.getAnimeById(1).awaitAsOne()
            it.repo.setEnabled(true)
            val identity = LocalAnimeIdentity(1, 42, "/1", "Test anime")
            val ticket = it.repo.begin(identity, now - MetadataCachePolicy.FRESH - 1)!!
            assertTrue(
                it.repo.selectManual(
                    ticket,
                    media.id,
                    MetadataResult.Success(media),
                    true,
                    now - MetadataCachePolicy.FRESH - 1,
                ),
            )
            it.queue.start()
            assertFalse(it.coordinator.runSlice())
            assertEquals(listOf(media.id), it.requested)
            assertTrue(it.repo.get(1)!!.state.association!!.manual)
            assertEquals(original, it.db.animesQueries.getAnimeById(1).awaitAsOne())
            it.queue.start()
            it.coordinator.runSlice()
            assertEquals(1, it.calls)
            assertEquals(1, it.queue.status().skipped)
            it.repo.clearCache()
            it.queue.start()
            it.coordinator.runSlice()
            assertEquals(2, it.calls)
            assertEquals(media.id, it.repo.get(1)!!.state.association!!.id)
        }
    }

    @Test
    fun `search never pins and repeated recent batch uses durable negative result`() = runTest {
        fresh().use {
            it.response = MetadataResult.Success(MetadataPage(emptyList(), 1, false))
            it.repo.setEnabled(true)
            it.queue.start()
            it.coordinator.runSlice()
            assertEquals(null, it.repo.get(1)!!.state.association)
            assertEquals("NOT_FOUND", it.repo.get(1)!!.state.outcome)
            it.queue.start()
            it.coordinator.runSlice()
            assertEquals(1, it.calls)
        }
    }

    @Test
    fun `original only and removed favorites are skipped without network`() = runTest {
        fresh(2).use {
            it.repo.setOriginalOnly(1, null, true)
            it.repo.setEnabled(true)
            it.queue.start()
            it.driver.execute(null, "UPDATE animes SET favorite_at=NULL WHERE _id=2", 0).await()
            it.coordinator.runSlice()
            assertEquals(0, it.calls)
            assertEquals(2, it.queue.status().skipped)
        }
    }

    @Test
    fun `pause cancels transport releases lease and resume continues`() = runTest {
        fresh().use { store ->
            store.repo.setEnabled(true)
            store.queue.start()
            val started = CompletableDeferred<Unit>()
            var cancelled = false
            store.lookup = {
                started.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
            val work = async { store.coordinator.runSlice() }
            started.await()
            store.queue.transition("PAUSED")
            runCurrent()
            assertTrue(work.isCancelled)
            assertTrue(cancelled)
            assertEquals(0, store.queue.status().completed)
            assertTrue(store.repo.get(1)!!.state.outcome != "SEARCHING")
            store.lookup = { store.response }
            store.queue.transition("RUNNING")
            assertFalse(store.coordinator.runSlice())
            assertEquals(1, store.queue.status().ambiguous)
        }
    }

    @Test
    fun `transient failures have finite retries and honor durable due time`() = runTest {
        fresh().use {
            it.response = MetadataResult.Failure(MetadataFailureKind.NETWORK, retryable = true)
            it.repo.setEnabled(true)
            it.queue.start()
            assertTrue(it.coordinator.runSlice())
            assertTrue(it.coordinator.runSlice())
            assertEquals(1, it.calls)
            now += 60_000
            it.coordinator.runSlice()
            now += 120_000
            assertFalse(it.coordinator.runSlice())
            assertEquals(3, it.calls)
            assertEquals(1, it.queue.status().errors)
        }
    }

    @Test
    fun `quota cooldown yields without another provider call`() = runTest {
        fresh(2).use {
            it.response = MetadataResult.Failure(MetadataFailureKind.RATE_LIMITED, retryAtMillis = now + 120_000)
            it.repo.setEnabled(true)
            it.queue.start()
            assertTrue(it.coordinator.runSlice())
            assertEquals(1, it.calls)
            assertTrue(it.coordinator.runSlice())
            assertEquals(1, it.calls)
            assertEquals(now + 120_000, it.repo.control().hostNotBefore)
        }
    }

    @Test
    fun `old generation cannot complete an item after cancel and new batch`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            it.queue.start()
            val generation = it.queue.status().generation
            it.queue.transition("CANCELLED")
            it.queue.start()
            it.queue.finish(generation, 1, "SUCCESS")
            assertEquals(0, it.queue.status().success)
        }
    }

    @Test
    fun `queue and attempts survive real database close and reopen`() = runTest {
        val file = Files.createTempFile("metadata-sync-restart", ".db")
        Store(JdbcSqliteDriver("jdbc:sqlite:$file")).use {
            Database.Schema.create(it.driver).await()
            seed(it, 3)
            it.repo.setEnabled(true)
            it.queue.start()
            it.coordinator.runSlice(1)
        }
        Store(JdbcSqliteDriver("jdbc:sqlite:$file")).use {
            assertEquals(3, it.queue.status().total)
            assertEquals(1, it.queue.status().completed)
            assertFalse(it.coordinator.runSlice())
            assertEquals(2, it.calls)
            assertEquals("COMPLETED", it.queue.status().status)
        }
        Files.delete(file)
    }

    @Test
    fun `favorite addition is transactional opt in and duplicate updates do not requeue`() = runTest {
        fresh().use {
            val animeRepo = AnimeRepositoryImpl(it.db)
            animeRepo.update(AnimeUpdate(1) { favoriteAt = null })
            it.repo.setEnabled(true)
            it.queue.setOnAdd(true)
            val emission = async { it.queue.observe().first { status -> status.total == 1L } }
            runCurrent()
            animeRepo.update(AnimeUpdate(1) { favoriteAt = 1234 })
            assertEquals("RUNNING", emission.await().status)
            it.coordinator.runSlice()
            animeRepo.update(AnimeUpdate(1) { favoriteAt = 1235 })
            assertEquals("COMPLETED", it.queue.status().status)
            assertEquals(1, it.calls)
            animeRepo.update(AnimeUpdate(1) { favoriteAt = null })
            now += MetadataCachePolicy.LOOKUP + 1
            animeRepo.update(AnimeUpdate(1) { favoriteAt = 1236 })
            assertEquals("RUNNING", it.queue.status().status)
            it.coordinator.runSlice()
            assertEquals(2, it.calls)
        }
    }

    @Test
    fun `off invalidates batch and stale queue results`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            it.queue.start()
            val generation = it.queue.status().generation
            it.repo.setEnabled(false)
            it.queue.finish(generation, 1, "SUCCESS")
            assertFalse(it.coordinator.runSlice())
            assertEquals("CANCELLED", it.queue.status().status)
            assertEquals(0, it.queue.status().total)
        }
    }

    @Test
    fun `migration 149 preserves decisions and library and initializes no work`() = runTest {
        val file = Files.createTempFile("metadata-sync-migration", ".db")
        Files.copy(Path.of("src/main/sqldelight/149.db"), file, StandardCopyOption.REPLACE_EXISTING)
        Store(JdbcSqliteDriver("jdbc:sqlite:$file")).use {
            seed(it, 1)
            val original = it.db.animesQueries.getAnimeById(1).awaitAsOne()
            it.db.metadataQueries.insertDecision(1, "synthetic persisted decision")
            val decision = it.db.metadataQueries.get(1).awaitAsOne()
            Database.Schema.migrate(it.driver, 149, 150).await()
            assertEquals(original, it.db.animesQueries.getAnimeById(1).awaitAsOne())
            assertEquals(decision, it.db.metadataQueries.get(1).awaitAsOne())
            assertEquals("IDLE", it.queue.status().status)
            assertFalse(it.queue.status().onAdd)
            assertEquals(0, it.queue.status().total)
        }
        Files.delete(file)
    }

    @Test
    fun `abandoned exhausted lease recovers after restart but queue attempt cap stays durable`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            it.queue.start()
            val batch = it.queue.status()
            val identity = it.queue.next(batch.generation)!!.identity!!
            val ticket = it.repo.beginSync(identity, null, now, batch.generation)!!
            assertTrue(it.queue.claim(batch.generation, 1))
            repeat(3) { _ -> assertTrue(it.repo.recordEmission(ticket, now)) }
            // Simulate process death: no finally/cancelAttempt. The next slice must wait for lease expiry.
            assertTrue(it.coordinator.runSlice())
            assertEquals(0, it.calls)
            now += MetadataCachePolicy.LEASE + 1
            assertFalse(it.coordinator.runSlice())
            assertEquals(1, it.calls)
            assertEquals(1, it.queue.status().ambiguous)
        }
    }

    @Test
    fun `pause atomically rejects late repository result before watcher delivery`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            it.queue.start()
            val batch = it.queue.status()
            val identity = it.queue.next(batch.generation)!!.identity!!
            val ticket = it.repo.beginSync(identity, null, now, batch.generation)!!
            assertTrue(it.queue.claim(batch.generation, 1))
            it.queue.transition("PAUSED")
            assertFalse(it.repo.applyMatch(ticket, listOf(media), true, now))
            assertFalse(it.repo.recordEmission(ticket, now))
            assertTrue(it.repo.cancelAttempt(ticket))
            assertEquals(0, it.queue.status().completed)
        }
    }

    @Test
    fun `scheduled charging requirement survives pause resume and manual batches reset it`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            it.queue.start(scheduled = true)
            it.queue.transition("PAUSED")
            it.queue.transition("RUNNING")
            assertTrue(it.queue.status().charging)
            it.queue.transition("CANCELLED")
            it.queue.start()
            assertFalse(it.queue.status().charging)
        }
    }

    @Test
    fun `three abandoned attempts cannot reset provider budget after recovery`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            it.queue.start()
            val batch = it.queue.status()
            val identity = it.queue.next(batch.generation)!!.identity!!
            repeat(3) { _ ->
                val ticket = it.repo.beginSync(identity, it.repo.get(1)?.revision, now, batch.generation)!!
                assertTrue(it.queue.claim(batch.generation, 1))
                repeat(3) { _ -> assertTrue(it.repo.recordEmission(ticket, now)) }
                now += MetadataCachePolicy.LEASE + 1
            }
            assertFalse(it.coordinator.runSlice())
            assertEquals(0, it.calls)
            assertEquals(1, it.queue.status().errors)
        }
    }

    @Test
    fun `new favorite after cancel never revives the cancelled pending items`() = runTest {
        fresh(2).use {
            it.repo.setEnabled(true)
            it.queue.start()
            it.queue.transition("CANCELLED")
            assertEquals(2, it.queue.status().skipped)
            it.queue.setOnAdd(true)
            val animeRepo = AnimeRepositoryImpl(it.db)
            animeRepo.update(AnimeUpdate(2) { favoriteAt = null })
            animeRepo.update(AnimeUpdate(2) { favoriteAt = 1236 })
            it.coordinator.runSlice()
            assertEquals(1, it.calls)
            assertEquals(1, it.queue.status().skipped)
            assertEquals(1, it.queue.status().ambiguous)
        }
    }
}
