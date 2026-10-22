package tachiyomi.source.local

import com.hippo.unifile.UniFile
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tachiyomi.core.metadata.tachiyomi.AnimeDetails
import java.io.ByteArrayOutputStream

class LocalAnimeMetadataTest {
    private val storage = LocalAnimeMetadata(Json)
    private val files = linkedMapOf<String, UniFile>()
    private val contents = mutableMapOf<String, ByteArray>()
    private val directory = mockk<UniFile> {
        every { listFiles() } answers { files.values.toTypedArray() }
        every { createFile(any()) } answers { file(firstArg()) }
    }

    private fun file(name: String, text: String = "{}") = mockk<UniFile> {
        every { this@mockk.name } returns name
        contents[name] = text.toByteArray()
        every { openInputStream() } answers { contents.getValue(name).inputStream() }
        every { openOutputStream() } answers {
            object : ByteArrayOutputStream() {
                override fun close() {
                    contents[name] = toByteArray()
                    super.close()
                }
            }
        }
    }.also { files[name] = it }

    @Test
    fun `new local edits can be read after reopening the metadata store`() {
        val details = AnimeDetails("Edited", "Author", "Artist", "Description", listOf("Drama"), 2)
        storage.write(directory, details)
        assertEquals(setOf("details.json"), files.keys)
        val restored = LocalAnimeMetadata(Json).read(files.values.toList())!!
        assertEquals(details.title, restored.title)
        assertEquals(details.author, restored.author)
        assertEquals(details.artist, restored.artist)
        assertEquals(details.description, restored.description)
        assertEquals(details.genre, restored.genre)
        assertEquals(details.status, restored.status)
    }

    @Test
    fun `legacy info json remains readable and is updated in place`() {
        file("info.json", """{"title":"Legacy"}""")
        assertEquals("Legacy", storage.read(files.values.toList())?.title)
        storage.write(directory, AnimeDetails(title = "Updated"))
        assertEquals(setOf("info.json"), files.keys)
        assertEquals("Updated", storage.read(files.values.toList())?.title)
    }

    @Test
    fun `details takes precedence and editing preserves the legacy file`() {
        file("info.json", """{"title":"Legacy"}""")
        file("details.json", """{"title":"Canonical"}""")
        val legacy = contents.getValue("info.json").toList()
        assertEquals("Canonical", storage.read(files.values.toList())?.title)
        storage.write(directory, AnimeDetails(title = "Edited"))
        assertEquals("Edited", storage.read(files.values.toList())?.title)
        assertEquals(legacy, contents.getValue("info.json").toList())
    }

    @Test
    fun `invalid canonical metadata does not silently restore a stale legacy title`() {
        file("info.json", """{"title":"Stale"}""")
        file("details.json", "invalid")
        assertThrows(kotlinx.serialization.SerializationException::class.java) {
            storage.read(files.values.toList())
        }
    }
}
