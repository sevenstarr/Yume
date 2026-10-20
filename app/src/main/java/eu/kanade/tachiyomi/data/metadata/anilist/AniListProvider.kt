package eu.kanade.tachiyomi.data.metadata.anilist

import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import tachiyomi.domain.metadata.model.CoverVariant
import tachiyomi.domain.metadata.model.MetadataDate
import tachiyomi.domain.metadata.model.MetadataFailureKind
import tachiyomi.domain.metadata.model.MetadataFieldError
import tachiyomi.domain.metadata.model.MetadataFormat
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataPage
import tachiyomi.domain.metadata.model.MetadataResult
import tachiyomi.domain.metadata.model.MetadataSeason
import tachiyomi.domain.metadata.model.MetadataTitles
import tachiyomi.domain.metadata.service.MetadataCapabilities
import tachiyomi.domain.metadata.service.MetadataProvider
import tachiyomi.domain.metadata.service.MetadataSearch
import java.io.IOException
import kotlin.coroutines.coroutineContext

// AM (Yume metadata, 2026-10-20; new code assisted by Codex) -->
class AniListProvider internal constructor(
    private val transport: AniListTransport,
    private val json: Json,
    private val budget: AniListBudget,
    private val enabled: () -> Boolean,
) : MetadataProvider {
    override val capabilities = MetadataCapabilities(PROVIDER, 20, MetadataFormat.entries.toSet())

    // Only collapse concurrent/repeated requests briefly; durable cache/TTL belongs to the repository.
    private val recent = linkedMapOf<String, Pair<Long, MetadataResult.Success<AniListData>>>()

    override suspend fun search(search: MetadataSearch): MetadataResult<MetadataPage> {
        val query = search.query.trim()
        if (query.isEmpty() || query.length > 300 || search.page !in 1..50 ||
            search.year?.let { it !in 1..9999 } == true
        ) {
            return failure(MetadataFailureKind.INVALID_INPUT)
        }
        val result = request(
            SEARCH_QUERY,
            buildJsonObject {
                put("search", query)
                put("page", search.page)
                search.format?.let { put("format", it.name) }
                search.season?.let { put("season", it.name) }
                search.year?.let { put("year", it) }
            },
            "Page",
        )
        if (result !is MetadataResult.Success) return result.asFailure()
        val page = result.value.page ?: return failure(MetadataFailureKind.CONTRACT)
        val errors = result.errors.toMutableList()
        val media = page.media?.mapIndexedNotNull { index, dto ->
            val path = listOf("Page", "media", index.toString())
            val fieldErrors = result.errors.filter { it.path.take(path.size) == path }
            val mapped = dto?.toDomain(fieldErrors.map { it.path.drop(path.size) })
            if (mapped == null) errors.add(MetadataFieldError(path, null))
            mapped
        }?.distinctBy { it.id } ?: run {
            errors.add(MetadataFieldError(listOf("Page", "media"), null))
            emptyList()
        }
        val info = page.pageInfo
        if (info?.currentPage != search.page || info.hasNextPage == null) {
            errors.add(MetadataFieldError(listOf("Page", "pageInfo"), null))
        }
        return MetadataResult.Success(
            MetadataPage(media, search.page, info?.hasNextPage),
            errors.distinct(),
            identityComplete = errors.all { "coverImage" in it.path },
        )
    }

    override suspend fun getById(id: MetadataId): MetadataResult<MetadataMedia> {
        val number = id.value.toIntOrNull()
        if (id.provider != PROVIDER || number == null || number <= 0 || number.toString() != id.value) {
            return failure(MetadataFailureKind.INVALID_INPUT)
        }
        val result = request(DETAIL_QUERY, buildJsonObject { put("id", number) }, "Media")
        if (result !is MetadataResult.Success) return result.asFailure()
        val dto = result.value.media
        if (dto == null) return if (result.complete) MetadataResult.NotFound else failure(MetadataFailureKind.CONTRACT)
        if (dto.id != number) return failure(MetadataFailureKind.CONTRACT)
        val media = dto.toDomain(result.errors.map { it.path.drop(1) }) ?: return failure(MetadataFailureKind.CONTRACT)
        return MetadataResult.Success(
            media,
            result.errors,
            identityComplete = result.errors.all {
                "coverImage" in
                    it.path
            },
        )
    }

    private suspend fun request(query: String, variables: JsonObject, root: String): MetadataResult<AniListData> {
        if (!enabled()) return failure(MetadataFailureKind.DISABLED)
        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") { variables.forEach { (key, value) -> put(key, value) } }
        }.toString()
        val deadline = budget.clock.monotonicMillis() + 45_000
        return withTimeoutOrNull(45_000) {
            budget.mutex.withLock {
                if (!enabled()) return@withLock failure(MetadataFailureKind.DISABLED)
                recent[payload]?.takeIf { budget.clock.monotonicMillis() - it.first < 5_000 }?.let {
                    return@withLock it.second
                }
                repeat(3) { attempt ->
                    coroutineContext.ensureActive()
                    if (!enabled()) return@withLock failure(MetadataFailureKind.DISABLED)
                    val deferredUntil = budget.acquire(deadline)
                    if (deferredUntil != null) {
                        return@withLock MetadataResult.Failure(
                            if (budget.isCircuitOpen()) {
                                MetadataFailureKind.CIRCUIT_OPEN
                            } else {
                                MetadataFailureKind.RATE_LIMITED
                            },
                            retryable = true,
                            retryAtMillis = deferredUntil,
                        )
                    }
                    if (!enabled()) return@withLock failure(MetadataFailureKind.DISABLED)
                    val response = try {
                        transport.execute(payload)
                    } catch (_: AniListBodyTooLarge) {
                        return@withLock failure(MetadataFailureKind.CONTRACT)
                    } catch (_: IOException) {
                        budget.transientFailure()
                        if (attempt == 2) return@withLock failure(MetadataFailureKind.NETWORK, true)
                        backoff(attempt)
                        return@repeat
                    }
                    val envelope = try {
                        json.decodeFromString<AniListEnvelope>(response.body)
                    } catch (_: SerializationException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                    val status = response.code.takeUnless { it in 200..299 }
                        ?: envelope?.errors?.firstNotNullOfOrNull { it.status?.takeIf { code -> code >= 400 } }
                    val retryAt = budget.observe(response.headers, status == 429)
                    if (status == 429) {
                        return@withLock MetadataResult.Failure(
                            MetadataFailureKind.RATE_LIMITED,
                            true,
                            429,
                            retryAt,
                        )
                    }
                    if (status != null && status >= 500) {
                        budget.transientFailure()
                        if (attempt == 2) {
                            return@withLock MetadataResult.Failure(MetadataFailureKind.NETWORK, true, status)
                        }
                        backoff(attempt)
                        return@repeat
                    }
                    if (status != null) {
                        return@withLock MetadataResult.Failure(
                            if (status in listOf(401, 403, 404, 410)) {
                                MetadataFailureKind.UNAVAILABLE
                            } else {
                                MetadataFailureKind.CONTRACT
                            },
                            status = status,
                        )
                    }
                    if (envelope?.data == null) return@withLock failure(MetadataFailureKind.CONTRACT)
                    val rawData = try {
                        json.parseToJsonElement(response.body).jsonObject["data"]?.jsonObject
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                    if (rawData?.containsKey(root) != true) return@withLock failure(MetadataFailureKind.CONTRACT)
                    val errors = envelope.errors.map { error ->
                        val path = error.path.orEmpty().map { element ->
                            val value = (element as? kotlinx.serialization.json.JsonPrimitive)?.content.orEmpty()
                            value.takeIf {
                                it in PATH_FIELDS ||
                                    it.toIntOrNull()?.let { index -> index in 0..19 } == true
                            }.orEmpty()
                        }
                        MetadataFieldError(path, error.status)
                    }
                    if (errors.any { !validErrorPath(it.path, root) }) {
                        return@withLock failure(MetadataFailureKind.CONTRACT)
                    }
                    val result = MetadataResult.Success(envelope.data, errors)
                    budget.success()
                    if (result.complete) {
                        recent[payload] = budget.clock.monotonicMillis() to result
                        if (recent.size > 32) recent.remove(recent.keys.first())
                    }
                    return@withLock result
                }
                failure(MetadataFailureKind.NETWORK, true)
            }
        } ?: failure(MetadataFailureKind.TIMEOUT, true)
    }

    private suspend fun backoff(attempt: Int) {
        budget.clock.sleep((2_000L shl attempt) + budget.jitter().coerceIn(0, 500))
    }

    private fun validErrorPath(path: List<String>, root: String): Boolean {
        if (path.firstOrNull() != root || path.size < 2 || path.any(String::isEmpty)) return false
        return if (root == "Media") {
            path[1] in MEDIA_FIELDS
        } else {
            path[1] == "pageInfo" ||
                (path[1] == "media" && path.size >= 3 && path[2].toIntOrNull()?.let { it in 0..19 } == true)
        }
    }

    private fun AniListMedia.toDomain(errors: List<List<String>>): MetadataMedia? {
        val fields = errors.mapNotNull { it.firstOrNull() }.toSet()
        if (errors.any { it.isEmpty() } || id == null || id <= 0 || type != "ANIME" || title == null ||
            fields.any { it in listOf("id", "type", "title", "synonyms") } ||
            listOf(title.romaji, title.english, title.native).all { it.isNullOrBlank() }
        ) {
            return null
        }
        val covers = if ("coverImage" in fields) {
            emptyList()
        } else {
            listOfNotNull(
                safeCover("extraLarge", coverImage?.extraLarge),
                safeCover("large", coverImage?.large),
                safeCover("medium", coverImage?.medium),
            ).distinctBy { it.url }
        }
        return MetadataMedia(
            id = MetadataId(PROVIDER, id.toString()),
            idMal = idMal?.takeIf { it > 0 && "idMal" !in fields },
            titles = MetadataTitles(title.romaji, title.english, title.native),
            synonyms = synonyms.orEmpty().filterNotNull().filter { it.isNotBlank() }.distinct(),
            format = MetadataFormat.entries.find { it.name == format && "format" !in fields },
            startDate = startDate?.takeUnless { "startDate" in fields }?.let {
                MetadataDate(
                    year = it.year?.takeIf { year -> year > 0 },
                    month = it.month?.takeIf { month -> month in 1..12 },
                    day = it.day?.takeIf { day -> day in 1..31 },
                )
            },
            season = MetadataSeason.entries.find { it.name == season && "season" !in fields },
            seasonYear = seasonYear?.takeIf { it > 0 && "seasonYear" !in fields },
            episodes = episodes?.takeIf { it >= 0 && "episodes" !in fields },
            siteUrl = siteUrl?.takeIf { it == "https://anilist.co/anime/$id" || it == "https://anilist.co/anime/$id/" }
                ?.takeUnless { "siteUrl" in fields },
            covers = covers,
        )
    }

    private fun safeCover(size: String, url: String?): CoverVariant? {
        val parsed = url?.toHttpUrlOrNull() ?: return null
        return if (parsed.scheme == "https" && parsed.host == "s4.anilist.co" && parsed.port == 443 &&
            parsed.username.isEmpty() && parsed.password.isEmpty() && parsed.fragment == null &&
            parsed.encodedPath.startsWith("/file/anilistcdn/media/anime/cover/")
        ) {
            CoverVariant(size, parsed.toString())
        } else {
            null
        }
    }

    private fun failure(kind: MetadataFailureKind, retryable: Boolean = false) = MetadataResult.Failure(kind, retryable)

    private fun MetadataResult<AniListData>.asFailure(): MetadataResult.Failure = this as MetadataResult.Failure

    companion object {
        const val PROVIDER = "anilist"
        private val MEDIA_FIELDS = setOf(
            "id", "idMal", "type", "title", "synonyms", "format", "startDate", "season",
            "seasonYear", "episodes", "siteUrl", "coverImage",
        )
        private val PATH_FIELDS = setOf(
            "Page", "Media", "media", "pageInfo", "currentPage", "hasNextPage", "id", "idMal", "type",
            "title", "romaji", "english", "native", "synonyms", "format", "startDate", "year", "month", "day",
            "season", "seasonYear", "episodes", "siteUrl", "coverImage", "extraLarge", "large", "medium",
        )
        private const val FIELDS = """
            id idMal type title { romaji english native } synonyms format
            startDate { year month day } season seasonYear episodes siteUrl
            coverImage { extraLarge large medium }
        """
        internal val SEARCH_QUERY = $$"""
            query YumeSearch($search: String!, $page: Int!, $format: MediaFormat, $season: MediaSeason, $year: Int) {
                Page(page: $page, perPage: 20) {
                    pageInfo { currentPage hasNextPage }
                    media(search: $search, type: ANIME, format: $format, season: $season, seasonYear: $year) { {{FIELDS}} }
                }
            }
        """.replace("{{FIELDS}}", FIELDS)
        internal val DETAIL_QUERY = $$"""
            query YumeMedia($id: Int!) { Media(id: $id, type: ANIME) { {{FIELDS}} } }
        """.replace("{{FIELDS}}", FIELDS)
    }
}
// <-- AM (Yume metadata)
