package tachiyomi.domain.metadata.service

import tachiyomi.domain.metadata.model.MetadataFormat
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataPage
import tachiyomi.domain.metadata.model.MetadataResult
import tachiyomi.domain.metadata.model.MetadataSeason

// AM (Yume metadata, 2026-10-20; new code assisted by Codex) -->
data class MetadataSearch(
    val query: String,
    val page: Int = 1,
    val format: MetadataFormat? = null,
    val season: MetadataSeason? = null,
    val year: Int? = null,
)

data class MetadataCapabilities(val provider: String, val pageSize: Int, val formats: Set<MetadataFormat>)

interface MetadataProvider {
    val capabilities: MetadataCapabilities
    suspend fun search(search: MetadataSearch): MetadataResult<MetadataPage>
    suspend fun getById(id: MetadataId): MetadataResult<MetadataMedia>
}
// <-- AM (Yume metadata)
