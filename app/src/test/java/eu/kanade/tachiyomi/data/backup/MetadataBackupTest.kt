package eu.kanade.tachiyomi.data.backup

import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.models.Backup
import eu.kanade.tachiyomi.data.backup.models.BackupAnime
import eu.kanade.tachiyomi.data.backup.restore.RestoreOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tachiyomi.domain.metadata.model.MetadataBackup
import tachiyomi.domain.metadata.model.MetadataBackupDecision

// AM (Yume backup wire compatibility tests, 2026-10-20; assisted by Codex) -->
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
class MetadataBackupTest {
    /** Previous reader's fields used by this fixture; protobuf skips all other top-level fields. */
    @Serializable
    private data class PreviousReader(
        @ProtoNumber(500) val isLegacy: Boolean = true,
        @ProtoNumber(501) val backupAnime: List<BackupAnime>,
    )

    private val anime = BackupAnime(
        source = 42, url = "/anime", title = "Original", customTitle = "Custom",
        customArtist = "Artist", customAuthor = "Author", customDescription = "Description",
        customGenre = listOf("Drama"), customStatus = 2,
    )

    @Test
    fun `previous wire fixture decodes without metadata`() {
        val bytes = ProtoBuf.encodeToByteArray(PreviousReader.serializer(), PreviousReader(false, listOf(anime)))
        val decoded = ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes)
        assertNull(decoded.metadataDecisions)
        assertEquals("Original", decoded.backupAnime.single().title)
        assertEquals("Custom", decoded.backupAnime.single().customTitle)
    }

    @Test
    fun `new optional field is skipped by previous reader and round trips decisions`() {
        val decisions = MetadataBackup(
            decisions = listOf(
                MetadataBackupDecision(
                    42,
                    "/anime",
                    provider = "anilist",
                    remoteId = "1",
                    manual = true,
                ),
            ),

        )
        val current = Backup(isLegacy = false, backupAnime = listOf(anime), metadataDecisions = decisions)
        val bytes = ProtoBuf.encodeToByteArray(Backup.serializer(), current)
        val previous = ProtoBuf.decodeFromByteArray(PreviousReader.serializer(), bytes)
        assertEquals("Original", previous.backupAnime.single().title)
        assertEquals(decisions, ProtoBuf.decodeFromByteArray(Backup.serializer(), bytes).metadataDecisions)
    }

    @Test
    fun `custom backup values survive wire and projection into original and custom fields`() {
        val bytes = ProtoBuf.encodeToByteArray(BackupAnime.serializer(), anime)
        val restored = ProtoBuf.decodeFromByteArray(BackupAnime.serializer(), bytes).getAnimeImpl()
        assertEquals("Original", restored.ogTitle)
        assertEquals("Custom", restored.customTitle)
        assertEquals("Artist", restored.customArtist)
        assertEquals("Author", restored.customAuthor)
        assertEquals("Description", restored.customDescription)
        assertEquals(listOf("Drama"), restored.customGenre)
        assertEquals(2L, restored.customStatus)
        assertNull(BackupAnime(42, "/old").getAnimeImpl().customStatus)
    }

    @Test
    fun `old saved option arrays remain valid and metadata is opt in`() {
        assertFalse(BackupOptions.fromBooleanArray(BooleanArray(12) { true }).metadataDecisions)
        assertFalse(RestoreOptions.fromBooleanArray(BooleanArray(7) { true }).metadataDecisions)
        val backup = BackupOptions(metadataDecisions = true).asBooleanArray()
        val restore = RestoreOptions(metadataDecisions = true).asBooleanArray()
        assertTrue(BackupOptions.fromBooleanArray(backup).metadataDecisions)
        assertTrue(RestoreOptions.fromBooleanArray(restore).metadataDecisions)
    }
}
// <-- AM (Yume backup wire compatibility tests)
