package tachiyomi.domain.metadata.model

// AM (Yume metadata, 2026-10-20; new code assisted by Codex) -->
data class MetadataId(val provider: String, val value: String)

data class MetadataTitles(val romaji: String?, val english: String?, val native: String?)

data class MetadataDate(val year: Int?, val month: Int?, val day: Int?)

enum class MetadataFormat { TV, TV_SHORT, MOVIE, SPECIAL, OVA, ONA, MUSIC }

enum class MetadataSeason { WINTER, SPRING, SUMMER, FALL }

data class CoverVariant(val size: String, val url: String)

data class MetadataMedia(
    val id: MetadataId,
    val idMal: Int?,
    val titles: MetadataTitles,
    val synonyms: List<String>,
    val format: MetadataFormat?,
    val startDate: MetadataDate?,
    val season: MetadataSeason?,
    val seasonYear: Int?,
    val episodes: Int?,
    val siteUrl: String?,
    val covers: List<CoverVariant>,
)

/** Paths are field names/indices only; server messages and private query text are never exposed. */
data class MetadataFieldError(val path: List<String>, val status: Int?)

data class MetadataPage(val media: List<MetadataMedia>, val page: Int, val hasNextPage: Boolean?)

enum class MetadataFailureKind {
    DISABLED,
    INVALID_INPUT,
    NETWORK,
    TIMEOUT,
    RATE_LIMITED,
    UNAVAILABLE,
    CONTRACT,
    CIRCUIT_OPEN,
}

sealed interface MetadataResult<out T> {
    data class Success<T>(
        val value: T,
        val errors: List<MetadataFieldError> = emptyList(),
        val identityComplete: Boolean = errors.isEmpty(),
    ) : MetadataResult<T> {
        val complete: Boolean get() = errors.isEmpty()
    }

    data object NotFound : MetadataResult<Nothing>

    data class Failure(
        val kind: MetadataFailureKind,
        val retryable: Boolean = false,
        val status: Int? = null,
        val retryAtMillis: Long? = null,
    ) : MetadataResult<Nothing>
}
// <-- AM (Yume metadata)
