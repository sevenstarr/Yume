package tachiyomi.data.metadata

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import tachiyomi.data.Database
import tachiyomi.data.DatabaseBindings
import tachiyomi.data.anime.AnimeRepositoryImpl
import tachiyomi.data.backup.RestoreRepositoryImpl
import tachiyomi.data.episode.EpisodeRepositoryImpl
import tachiyomi.data.track.TrackRepositoryImpl
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeUpdate
import tachiyomi.domain.backup.model.RestoredAnime
import tachiyomi.domain.metadata.model.CoverVariant
import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MetadataBackup
import tachiyomi.domain.metadata.model.MetadataBackupDecision
import tachiyomi.domain.metadata.model.MetadataCachePolicy
import tachiyomi.domain.metadata.model.MetadataDate
import tachiyomi.domain.metadata.model.MetadataFailureKind
import tachiyomi.domain.metadata.model.MetadataFormat
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataLookupKey
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataPage
import tachiyomi.domain.metadata.model.MetadataRestoreOutcome
import tachiyomi.domain.metadata.model.MetadataResult
import tachiyomi.domain.metadata.model.MetadataSeason
import tachiyomi.domain.metadata.model.MetadataTitles
import tachiyomi.domain.metadata.service.MetadataImageCache
import java.nio.file.Files
import java.nio.file.Path

// AM (Yume metadata SQL integration tests, 2026-10-20; new code assisted by Codex) -->
class MetadataRepositoryTest {
    private val now = 1_800_000_000_000L
    private val identity = LocalAnimeIdentity(1, 42, "/1", "Cowboy Bebop")
    private val remote = MetadataId("anilist", "1")
    private val media = MetadataMedia(
        remote, null, MetadataTitles("Cowboy Bebop", null, null), emptyList(), MetadataFormat.TV,
        MetadataDate(1998, 4, null), MetadataSeason.SPRING, 1998, 26, null,
        listOf(CoverVariant("extraLarge", "https://s4.anilist.co/file/anilistcdn/media/anime/cover/large/test.jpg")),
    )

    private class Store(val driver: JdbcSqliteDriver) : AutoCloseable {
        val db = DatabaseBindings.providesDatabase(driver)
        val repo = MetadataRepositoryImpl(db, MetadataImageCache { })
        override fun close() = driver.close()
    }

    private suspend fun fresh(): Store = Store(JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)).also {
        Database.Schema.create(it.driver).await()
        it.driver.execute(null, "PRAGMA foreign_keys=ON", 0).await()
        addAnime(it)
    }

    private suspend fun addAnime(store: Store, id: Long = 1, url: String = "/1") {
        store.driver.execute(
            null,
            """INSERT INTO animes(_id, source, url, title, status, initialized, viewer, episode_flags,
                cover_last_modified, season_flags, season_number, season_source_order, background_last_modified,
                favorite_at, thumbnail_url, custom_title)
                VALUES($id, 42, '$url', 'Cowboy Bebop', 2, 1, 0, 0, 123, 0, -1, 0, 0,
                1000, 'https://source.invalid/original.jpg', 'Local title')
            """.trimIndent(),
            0,
        ).await()
    }

    private suspend fun pin(store: Store, id: LocalAnimeIdentity = identity, time: Long = now) {
        store.repo.setEnabled(true)
        val ticket = checkNotNull(store.repo.begin(id, time))
        assertTrue(store.repo.selectManual(ticket, remote, MetadataResult.Success(media), true, time))
    }

    @Test
    fun `migration from 148 preserves legacy rows and starts disabled`() = runTest {
        val file = Files.createTempFile("metadata-migration", ".db")
        Files.copy(Path.of("src/main/sqldelight/148.db"), file, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        Store(JdbcSqliteDriver("jdbc:sqlite:$file")).use { store ->
            addAnime(store)
            val original = store.db.animesQueries.getAnimeById(1).awaitAsOne()
            Database.Schema.migrate(store.driver, 148, 149).await()
            assertEquals(149L, Database.Schema.version)
            assertEquals(original, store.db.animesQueries.getAnimeById(1).awaitAsOne())
            assertFalse(store.repo.control().enabled)
            assertNull(store.repo.get(1))
        }
        Files.delete(file)
    }

    @Test
    fun `pin and cooldown survive close and reopen without network`() = runTest {
        val file = Files.createTempFile("metadata-restart", ".db")
        Store(JdbcSqliteDriver("jdbc:sqlite:$file")).use {
            Database.Schema.create(it.driver).await()
            addAnime(it)
            pin(it)
            it.repo.deferHost(now + 90_000)
        }
        Store(JdbcSqliteDriver("jdbc:sqlite:$file")).use {
            assertTrue(it.repo.get(1)!!.state.association!!.manual)
            assertEquals(remote, it.repo.get(1)!!.state.association!!.id)
            assertEquals(now + 90_000, it.repo.control().hostNotBefore)
            assertNotNull(it.repo.readMedia(remote, now + 1))
            assertNull(it.repo.begin(identity, now + 1))
        }
        Files.delete(file)
    }

    @Test
    fun `manual pin invalidates concurrent result and duplicate lease`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            val ticket = it.repo.begin(identity, now)!!
            assertNull(it.repo.begin(identity, now))
            val first = async { it.repo.selectManual(ticket, remote, MetadataResult.Success(media), true, now) }
            val late = async { it.repo.applyMatch(ticket, emptyList(), true, now) }
            assertTrue(first.await())
            assertFalse(late.await())
            assertEquals(remote, it.repo.get(1)!!.state.association!!.id)
        }
    }

    @Test
    fun `off clear removal and source mutation reject stale writes`() = runTest {
        fresh().use { store ->
            store.repo.setEnabled(true)
            var ticket = store.repo.begin(identity, now)!!
            store.repo.setEnabled(false)
            store.repo.setEnabled(true)
            assertFalse(store.repo.selectManual(ticket, remote, MetadataResult.Success(media), true, now))
            ticket = store.repo.begin(identity, now)!!
            store.repo.clearCache()
            assertFalse(
                store.repo.cacheLookup(
                    ticket,
                    MetadataLookupKey("test"),
                    MetadataResult.Success(
                        MetadataPage(
                            emptyList(),
                            1,
                            false,
                        ),
                    ),
                    now,
                ),
            )
            ticket = store.repo.begin(identity, now)!!
            store.repo.removeAssociation(1, ticket.revision)
            assertFalse(store.repo.selectManual(ticket, remote, MetadataResult.Success(media), true, now))
            store.repo.setOriginalOnly(1, store.repo.get(1)!!.revision, false)
            ticket = store.repo.begin(identity, now)!!
            store.driver.execute(null, "UPDATE animes SET title='Changed' WHERE _id=1", 0).await()
            assertFalse(store.repo.selectManual(ticket, remote, MetadataResult.Success(media), true, now))
        }
    }

    @Test
    fun `expired lease retains emission budget through repository recreation`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            val ticket = it.repo.begin(identity, now)!!
            repeat(3) { _ -> assertTrue(it.repo.recordEmission(ticket, now)) }
            assertFalse(it.repo.recordEmission(ticket, now))
            val restarted = MetadataRepositoryImpl(it.db, MetadataImageCache { })
            assertNull(restarted.begin(identity, now + MetadataCachePolicy.LEASE))
            assertFalse(
                restarted.selectManual(
                    ticket,
                    remote,
                    MetadataResult.Success(media),
                    true,
                    now + MetadataCachePolicy.LEASE,
                ),
            )
        }
    }

    @Test
    fun `expired unfinished lease gets new generation without reviving old ticket`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            val old = it.repo.begin(identity, now)!!
            assertTrue(it.repo.recordEmission(old, now))
            val resumed = it.repo.begin(identity, now + MetadataCachePolicy.LEASE)!!
            assertTrue(resumed.generation > old.generation)
            assertFalse(
                it.repo.selectManual(
                    old,
                    remote,
                    MetadataResult.Success(media),
                    true,
                    now + MetadataCachePolicy.LEASE,
                ),
            )
            assertTrue(it.repo.recordEmission(resumed, now + MetadataCachePolicy.LEASE))
            assertTrue(it.repo.recordEmission(resumed, now + MetadataCachePolicy.LEASE))
            assertFalse(it.repo.recordEmission(resumed, now + MetadataCachePolicy.LEASE))
        }
    }

    @Test
    fun `cache freshness retention and failures never renew checked date`() = runTest {
        fresh().use {
            pin(it)
            assertTrue(it.repo.readMedia(remote, now + MetadataCachePolicy.FRESH - 1)!!.fresh)
            assertFalse(it.repo.readMedia(remote, now + MetadataCachePolicy.FRESH)!!.fresh)
            val ticket = it.repo.begin(identity, now + 1)!!
            assertTrue(it.repo.refresh(ticket, MetadataResult.Failure(MetadataFailureKind.NETWORK, true), now + 1))
            assertEquals(now, it.repo.get(1)!!.state.lastCheckedAt)
            assertEquals(now, it.repo.readMedia(remote, now + 2)!!.fetchedAt)
            assertNull(it.repo.readMedia(remote, now - 1))
            assertNull(it.repo.readMedia(remote, now + MetadataCachePolicy.RETAIN))
            it.repo.prune(now + MetadataCachePolicy.RETAIN)
            assertNotNull(it.repo.get(1)!!.state.association)
        }
    }

    @Test
    fun `only complete empty first page becomes negative with fingerprint and ttl`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            val ticket = it.repo.begin(identity, now)!!
            val key = MetadataLookupKey("Cowboy Bebop")
            assertFalse(it.repo.cacheLookup(ticket, key, MetadataResult.Failure(MetadataFailureKind.NETWORK), now))
            assertFalse(
                it.repo.cacheLookup(
                    ticket,
                    key,
                    MetadataResult.Success(
                        MetadataPage(
                            emptyList(),
                            1,
                            null,
                        ),
                    ),
                    now,
                ),
            )
            assertFalse(
                it.repo.cacheLookup(
                    ticket,
                    key,
                    MetadataResult.Success(
                        MetadataPage(
                            emptyList(),
                            1,
                            false,
                        ),
                        identityComplete = false,
                    ),
                    now,
                ),
            )
            assertTrue(
                it.repo.cacheLookup(
                    ticket,
                    key,
                    MetadataResult.Success(
                        MetadataPage(
                            emptyList(),
                            1,
                            false,
                        ),
                    ),
                    now,
                ),
            )
            assertNotNull(it.repo.readLookup(key, ticket.fingerprint, now + MetadataCachePolicy.DAY - 1))
            assertNull(it.repo.readLookup(key, "different", now))
            assertNull(it.repo.readLookup(key, ticket.fingerprint, now + MetadataCachePolicy.DAY))
            assertNull(it.repo.readLookup(key.copy(matcherVersion = "next"), ticket.fingerprint, now))
        }
    }

    @Test
    fun `lookup LRU stays bounded and reads do not prolong expiry`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            val ticket = it.repo.begin(identity, now)!!
            repeat(201) { index ->
                assertTrue(
                    it.repo.cacheLookup(
                        ticket,
                        MetadataLookupKey("q$index"),
                        MetadataResult.Success(
                            MetadataPage(
                                emptyList(),
                                1,
                                false,
                            ),
                        ),
                        now + index,
                    ),
                )
            }
            assertNull(it.repo.readLookup(MetadataLookupKey("q0"), ticket.fingerprint, now + 201))
            assertNotNull(it.repo.readLookup(MetadataLookupKey("q200"), ticket.fingerprint, now + 201))
            assertNull(
                it.repo.readLookup(
                    MetadataLookupKey("q200"),
                    ticket.fingerprint,
                    now + 200 + MetadataCachePolicy.DAY,
                ),
            )
        }
    }

    @Test
    fun `cache clear preserves decisions rejections originals while full clear disables`() = runTest {
        fresh().use {
            val original = it.db.animesQueries.getAnimeById(1).awaitAsOne()
            pin(it)
            it.repo.reject(1, it.repo.get(1)!!.revision, MetadataId("anilist", "2"))
            it.repo.deferHost(now + 100_000)
            it.repo.clearCache()
            assertNotNull(it.repo.get(1)!!.state.association)
            assertEquals(1, it.repo.get(1)!!.state.rejected.size)
            assertNull(it.repo.readMedia(remote, now))
            assertTrue(it.repo.control().enabled)
            it.repo.clearAll()
            assertFalse(it.repo.control().enabled)
            assertEquals(now + 100_000, it.repo.control().hostNotBefore)
            assertNull(it.repo.get(1))
            assertEquals(original, it.db.animesQueries.getAnimeById(1).awaitAsOne())
        }
    }

    @Test
    fun `deleted anime cascades and batch observation publishes committed state`() = runTest {
        fresh().use {
            pin(it)
            assertEquals(remote, it.repo.observe(listOf(1, 2)).first().single().state.association!!.id)
            val ticket = it.repo.begin(identity, now + 1)!!
            it.driver.execute(null, "DELETE FROM animes WHERE _id=1", 0).await()
            assertNull(it.repo.get(1))
            assertFalse(it.repo.selectManual(ticket, remote, MetadataResult.Success(media), true, now + 1))
        }
    }

    @Test
    fun `oversized media rolls back pin and keeps lease usable`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            val ticket = it.repo.begin(identity, now)!!
            val oversizedUrl = "https://s4.anilist.co/" + "x".repeat(1000)
            val oversized = media.copy(covers = List(100) { CoverVariant("large", oversizedUrl) })
            try {
                it.repo.selectManual(
                    ticket,
                    remote,
                    MetadataResult.Success(oversized),
                    true,
                    now,
                )
                fail<Unit>("Expected bounded payload")
            } catch (_: IllegalArgumentException) {
                assertNull(it.repo.get(1)!!.state.association)
                assertTrue(it.repo.selectManual(ticket, remote, MetadataResult.Success(media), true, now))
            }
        }
    }

    @Test
    fun `restore remaps source url preserves destination pin and records conflicts offline`() = runTest {
        fresh().use { source ->
            pin(source)
            val backup = source.repo.exportDecisions(listOf(1))
            fresh().use { destination ->
                destination.driver.execute(null, "UPDATE animes SET _id=99 WHERE _id=1", 0).await()
                assertEquals(MetadataRestoreOutcome.APPLIED, destination.repo.restoreDecisions(backup).single().outcome)
                assertFalse(destination.repo.control().enabled)
                assertEquals(remote, destination.repo.get(99)!!.state.association!!.id)
                assertTrue(destination.repo.get(99)!!.state.needsReview)
                assertNull(destination.repo.readMedia(remote, now))
                assertEquals(
                    MetadataRestoreOutcome.CONFLICT,
                    destination.repo.restoreDecisions(backup).single().outcome,
                )
                assertEquals(1, destination.repo.pendingRestores().size)
                destination.repo.setEnabled(true)
                val restoredIdentity = identity.copy(animeId = 99)
                val ticket = destination.repo.begin(restoredIdentity, now)!!
                assertTrue(destination.repo.get(99)!!.state.requiresRevalidation)
                assertTrue(destination.repo.refresh(ticket, MetadataResult.Success(media), now))
                assertFalse(destination.repo.get(99)!!.state.requiresRevalidation)
            }
        }
    }

    @Test
    fun `duplicate targets duplicate backup pins unknown providers and old empty backup are conservative`() = runTest {
        fresh().use {
            val decision = MetadataBackupDecision(42, "/1", provider = "anilist", remoteId = "1", manual = true)
            assertTrue(it.repo.restoreDecisions(MetadataBackup()).isEmpty())
            val conflict = it.repo.restoreDecisions(
                MetadataBackup(
                    decisions = listOf(
                        decision,
                        decision.copy(remoteId = "2"),
                    ),
                ),
            )
            assertTrue(conflict.all { result -> result.outcome == MetadataRestoreOutcome.CONFLICT })
            assertNull(it.repo.get(1))
            addAnime(it, 2)
            assertEquals(
                MetadataRestoreOutcome.UNMAPPED,
                it.repo.restoreDecisions(MetadataBackup(decisions = listOf(decision))).single().outcome,
            )
            assertEquals(
                MetadataRestoreOutcome.INACTIVE,
                it.repo.restoreDecisions(
                    MetadataBackup(decisions = listOf(decision.copy(provider = "FUTURE"))),
                ).single().outcome,
            )
            assertEquals(
                MetadataRestoreOutcome.INVALID,
                it.repo.restoreDecisions(
                    MetadataBackup(
                        version = 2,
                        decisions = listOf(decision),
                    ),
                ).single().outcome,
            )
        }
    }

    @Test
    fun `restore custom fields preserves originals and destination overrides`() = runTest {
        fresh().use {
            val animeRepo = AnimeRepositoryImpl(it.db)
            val restore = RestoreRepositoryImpl(
                it.db,
                animeRepo,
                EpisodeRepositoryImpl(it.db),
                TrackRepositoryImpl(it.db),
            )
            val backupAnime = Anime.create().copy(
                source = 42, url = "/new", ogTitle = "Original title", ogAuthor = "Author", ogArtist = "Artist",
                customTitle = "Backup title", customAuthor = "Custom author", customArtist = "Custom artist",
                customDescription = "Custom description", customGenre = listOf("Drama"), customStatus = 2,
            )
            suspend fun restoreOne(anime: Anime) = restore.restoreAnime(
                listOf(RestoredAnime(anime, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null)),
            ) { current -> AnimeUpdate(current.id) {} }
            restoreOne(backupAnime)
            val restored = animeRepo.getAnimeByUrlAndSourceId("/new", 42)!!
            assertEquals("Original title", restored.ogTitle)
            assertEquals("Backup title", restored.customTitle)
            assertEquals("Custom author", restored.customAuthor)
            assertEquals("Custom artist", restored.customArtist)
            assertEquals("Custom description", restored.customDescription)
            assertEquals(listOf("Drama"), restored.customGenre)
            assertEquals(2L, restored.customStatus)
            restoreOne(backupAnime.copy(customTitle = "Other title"))
            assertEquals("Backup title", animeRepo.getAnimeByUrlAndSourceId("/new", 42)!!.customTitle)
            assertEquals("Author", animeRepo.getAnimeByUrlAndSourceId("/new", 42)!!.ogAuthor)
        }
    }

    @Test
    fun `image barrier reuses cache keys and clear evicts only external assets after stale write`() = runTest {
        fresh().use { store ->
            val removed = mutableSetOf<String>()
            val repository = MetadataRepositoryImpl(store.db, MetadataImageCache { removed.addAll(it) })
            pin(store)
            val row = repository.get(1)!!
            val epoch = repository.control().epoch
            val key = "yume-metadata-v1:anilist:1:${"a".repeat(64)}:1"
            val started = CompletableDeferred<Unit>()
            val resume = CompletableDeferred<Unit>()
            val writing = async {
                repository.storeImage(1, row.revision, epoch, key, now) {
                    started.complete(Unit)
                    resume.await()
                }
            }
            started.await()
            val clearing = async { repository.clearCache() }
            runCurrent()
            assertFalse(clearing.isCompleted)
            resume.complete(Unit)
            assertFalse(writing.await())
            clearing.await()
            assertEquals(setOf(key), removed)
            assertFalse(repository.storeImage(1, row.revision, epoch, key, now) { fail<Unit>("Stale write") })
        }
    }

    @Test
    fun `failed search persists typed error without negative or checked date and cooldown survives clear`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            val ticket = it.repo.begin(identity, now)!!
            val failure = MetadataResult.Failure(MetadataFailureKind.RATE_LIMITED, true)
            assertTrue(it.repo.failAttempt(ticket, failure, now))
            assertEquals(MetadataFailureKind.RATE_LIMITED, it.repo.get(1)!!.state.failure)
            assertNull(it.repo.get(1)!!.state.lastCheckedAt)
            assertNull(it.repo.readLookup(MetadataLookupKey("query"), ticket.fingerprint, now))
            it.repo.clearAll()
            it.repo.setEnabled(true)
            assertNull(it.repo.begin(identity, now + 59_999))
            assertNotNull(it.repo.begin(identity, now + 60_000))
        }
    }

    @Test
    fun `off and cache clear invalidate searching status as well as lease`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            it.repo.begin(identity, now)
            it.repo.clearCache()
            assertEquals("PENDING", it.repo.get(1)!!.state.outcome)
            it.repo.begin(identity, now)
            it.repo.setEnabled(false)
            assertEquals("PENDING", it.repo.get(1)!!.state.outcome)
        }
    }

    @Test
    fun `reusing local id and identical clock cannot revive deleted item ticket`() = runTest {
        fresh().use {
            it.repo.setEnabled(true)
            val old = it.repo.begin(identity, now)!!
            it.driver.execute(null, "DELETE FROM animes WHERE _id=1", 0).await()
            addAnime(it)
            val current = it.repo.begin(identity, now)!!
            assertEquals(old.revision, current.revision)
            assertEquals(old.generation, current.generation)
            assertEquals(old.leaseUntil, current.leaseUntil)
            assertNotEquals(old.leaseId, current.leaseId)
            assertFalse(it.repo.selectManual(old, remote, MetadataResult.Success(media), true, now))
            assertTrue(it.repo.selectManual(current, remote, MetadataResult.Success(media), true, now))
        }
    }

    @Test
    fun `different images load concurrently and clear drains every writer`() = runTest {
        fresh().use { store ->
            pin(store)
            val record = store.repo.get(1)!!
            val epoch = store.repo.control().epoch
            val resume = CompletableDeferred<Unit>()
            val firstStarted = CompletableDeferred<Unit>()
            val secondStarted = CompletableDeferred<Unit>()
            fun key(char: String) = "yume-metadata-v1:anilist:1:${char.repeat(64)}:1"
            val first = async {
                store.repo.storeImage(1, record.revision, epoch, key("a"), now) {
                    firstStarted.complete(Unit)
                    resume.await()
                }
            }
            val second = async {
                store.repo.storeImage(1, record.revision, epoch, key("b"), now) {
                    secondStarted.complete(Unit)
                    resume.await()
                }
            }
            runCurrent()
            assertTrue(firstStarted.isCompleted)
            assertTrue(secondStarted.isCompleted)
            val clear = async { store.repo.clearCache() }
            runCurrent()
            assertFalse(clear.isCompleted)
            resume.complete(Unit)
            assertFalse(first.await())
            assertFalse(second.await())
            clear.await()
        }
    }

    @Test
    fun `image publication refuses source identity changes even for a manual pin`() = runTest {
        fresh().use { store ->
            pin(store)
            val record = store.repo.get(1)!!
            val epoch = store.repo.control().epoch
            val key = "yume-metadata-v1:anilist:1:${"a".repeat(64)}:1"
            assertFalse(
                store.repo.storeImage(1, record.revision, epoch, key, now) {
                    store.driver.execute(null, "UPDATE animes SET url='/different' WHERE _id=1", 0).await()
                },
            )
            assertTrue(store.repo.setOriginalOnly(1, record.revision, true))
            val resolver = CoverResolverImpl(store.db, store.repo)
            try {
                withContext(Dispatchers.IO) {
                    withTimeout(5_000) { assertTrue(resolver.resolve(1).originalOnly) }
                }
            } finally {
                resolver.close()
            }
        }
    }

    @Test
    fun `shared cover index observes requested IDs and original mode without modifying source data`() = runTest {
        fresh().use { store ->
            val clock = System.currentTimeMillis()
            pin(store, time = clock)
            addAnime(store, 2, "/2")
            pin(store, identity.copy(animeId = 2, sourceUrl = "/2"), clock)
            val original = store.db.animesQueries.getAnimeById(1).awaitAsOne()
            val resolver = CoverResolverImpl(store.db, store.repo)
            try {
                withContext(Dispatchers.IO) {
                    withTimeout(5_000) {
                        val both = resolver.observe(listOf(1, 2)).first {
                            it.values.all { cover -> cover.external.isNotEmpty() }
                        }
                        assertEquals(setOf(1L, 2L), both.keys)
                        assertEquals(both.getValue(1).external, both.getValue(2).external)
                        val row = store.repo.get(1)!!
                        assertTrue(store.repo.setOriginalOnly(1, row.revision, true))
                        val source = resolver.observe(listOf(1)).first { it.getValue(1).originalOnly }.getValue(1)
                        assertTrue(source.external.isEmpty())
                        assertTrue(store.repo.setOriginalOnly(1, store.repo.get(1)!!.revision, false))
                        assertTrue(
                            resolver.observe(listOf(1)).first { it.getValue(1).external.isNotEmpty() }
                                .getValue(1).external.isNotEmpty(),
                        )
                        store.repo.setEnabled(false)
                        val off = resolver.observe(listOf(1)).first { it.getValue(1).external.isEmpty() }.getValue(1)
                        assertFalse(off.originalOnly)
                    }
                }
            } finally {
                resolver.close()
            }
            assertEquals(original, store.db.animesQueries.getAnimeById(1).awaitAsOne())
        }
    }

    @Test
    fun `original only works before a lookup with enrichment disabled and remains reversible`() = runTest {
        fresh().use { store ->
            val original = store.db.animesQueries.getAnimeById(1).awaitAsOne()
            assertFalse(store.repo.control().enabled)
            assertNull(store.repo.get(1))
            assertTrue(store.repo.setOriginalOnly(1, null, true))
            val record = store.repo.get(1)!!
            assertTrue(record.state.originalOnly)
            assertNull(record.state.association)
            assertFalse(store.repo.control().enabled)
            assertFalse(store.repo.setOriginalOnly(1, null, true))
            assertTrue(store.repo.setOriginalOnly(1, record.revision, false))
            assertFalse(store.repo.get(1)!!.state.originalOnly)
            assertEquals(original, store.db.animesQueries.getAnimeById(1).awaitAsOne())
        }
    }
}
// <-- AM (Yume metadata SQL integration tests)
