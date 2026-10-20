package tachiyomi.domain.metadata.service

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import tachiyomi.domain.metadata.model.CoverVariant
import tachiyomi.domain.metadata.model.MetadataCachePolicy
import tachiyomi.domain.metadata.model.MetadataCachedMedia
import tachiyomi.domain.metadata.model.MetadataControl
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataRecord
import java.net.URI
import java.security.MessageDigest

// AM (Yume cover resolution, 2026-10-20; new code assisted by Codex) -->
data class ExternalCover(val id: MetadataId, val url: String, val key: String)

data class CoverSelection(
    val originalOnly: Boolean = false,
    val revision: Long = 0,
    val epoch: Long = 0,
    val sourceId: Long? = null,
    val external: List<ExternalCover> = emptyList(),
)

/** Observes only requested local IDs. Resolution is local; it never searches a provider. */
interface CoverResolver {
    fun observe(animeIds: List<Long>): Flow<Map<Long, CoverSelection>>
    suspend fun resolve(animeId: Long): CoverSelection = observe(listOf(animeId)).first().getValue(animeId)
}

object CoverPolicy {
    fun resolve(
        control: MetadataControl,
        record: MetadataRecord?,
        cached: MetadataCachedMedia?,
        now: Long,
    ): CoverSelection {
        val state = record?.state
        val base = CoverSelection(
            originalOnly = state?.originalOnly == true,
            revision = record?.revision ?: 0,
            epoch = control.epoch,
            sourceId = state?.identity?.sourceId,
        )
        val id = state?.association?.id
        if (!control.enabled || base.originalOnly || state == null || id == null ||
            state.needsReview || state.requiresRevalidation || state.outcome == "UNAVAILABLE" ||
            cached == null || cached.media.id != id || !AnimeMatcher.validId(id) ||
            now < cached.fetchedAt || now - cached.fetchedAt >= MetadataCachePolicy.RETAIN
        ) {
            return base
        }
        val variants = listOf("extraLarge", "large", "medium")
            .flatMap { size -> cached.media.covers.filter { it.size == size } }
            .filter { safeUrl(it.url) }
            .distinctBy(CoverVariant::url)
            .take(3)
            .map {
                val hash = MessageDigest.getInstance("SHA-256").digest(it.url.toByteArray(Charsets.UTF_8))
                    .joinToString("") { byte -> "%02x".format(byte) }
                ExternalCover(id, it.url, "yume-metadata-v1:${id.provider}:${id.value}:$hash:${cached.fetchedAt}")
            }
        return base.copy(external = variants)
    }

    /** Only the CDN/path verified by the AniList client is enabled; redirects are not inferred. */
    fun safeUrl(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme == "https" && uri.host == "s4.anilist.co" && uri.port in listOf(-1, 443) &&
            uri.rawUserInfo == null && uri.rawFragment == null && uri.normalize() == uri &&
            uri.rawPath.startsWith("/file/anilistcdn/media/anime/cover/") &&
            uri.path.split('/').none { it == "." || it == ".." } &&
            !uri.path.contains('\\') &&
            !uri.rawPath.contains('\\') && url.length <= 4096
    }.getOrDefault(false)
}
// <-- AM (Yume cover resolution)
