package tachiyomi.source.local

import com.hippo.unifile.UniFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import tachiyomi.core.metadata.tachiyomi.AnimeDetails

/** Shares the same filename policy between editing and loading local anime details. */
internal class LocalAnimeMetadata(private val json: Json) {
    fun read(files: List<UniFile>): AnimeDetails? = find(files)?.openInputStream()?.use {
        json.decodeFromStream<AnimeDetails>(it)
    }

    fun write(directory: UniFile, details: AnimeDetails) {
        val file = find(directory.listFiles().orEmpty().toList())
            ?: directory.createFile("details.json")
            ?: return
        file.openOutputStream().use { json.encodeToStream(details, it) }
    }

    private fun find(files: List<UniFile>): UniFile? =
        files.firstOrNull { it.name == "details.json" }
            ?: files.firstOrNull { it.name == "info.json" }
}
