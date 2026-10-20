package eu.kanade.tachiyomi.data.metadata.anilist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// AM (Yume metadata, 2026-10-20; new code assisted by Codex) -->
@Serializable
internal data class AniListEnvelope(val data: AniListData? = null, val errors: List<AniListError> = emptyList())

@Serializable
internal data class AniListData(
    @SerialName("Page") val page: AniListPage? = null,
    @SerialName("Media") val media: AniListMedia? = null,
)

@Serializable
internal data class AniListError(val path: List<JsonElement>? = null, val status: Int? = null)

@Serializable
internal data class AniListPage(val media: List<AniListMedia?>? = null, val pageInfo: AniListPageInfo? = null)

@Serializable
internal data class AniListPageInfo(val currentPage: Int? = null, val hasNextPage: Boolean? = null)

@Serializable
internal data class AniListMedia(
    val id: Int? = null,
    val idMal: Int? = null,
    val type: String? = null,
    val title: AniListTitles? = null,
    val synonyms: List<String?>? = null,
    val format: String? = null,
    val startDate: AniListDate? = null,
    val season: String? = null,
    val seasonYear: Int? = null,
    val episodes: Int? = null,
    val siteUrl: String? = null,
    val coverImage: AniListCovers? = null,
)

@Serializable
internal data class AniListTitles(val romaji: String? = null, val english: String? = null, val native: String? = null)

@Serializable
internal data class AniListDate(val year: Int? = null, val month: Int? = null, val day: Int? = null)

@Serializable
internal data class AniListCovers(val extraLarge: String? = null, val large: String? = null, val medium: String? = null)
// <-- AM (Yume metadata)
