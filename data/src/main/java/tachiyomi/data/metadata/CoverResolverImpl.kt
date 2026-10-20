@file:OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package tachiyomi.data.metadata

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.serialization.json.Json
import tachiyomi.data.Database
import tachiyomi.data.subscribeToList
import tachiyomi.domain.metadata.model.MetadataCachePolicy
import tachiyomi.domain.metadata.model.MetadataCachedMedia
import tachiyomi.domain.metadata.model.MetadataDecisionState
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataRecord
import tachiyomi.domain.metadata.service.CoverPolicy
import tachiyomi.domain.metadata.service.CoverResolver
import tachiyomi.domain.metadata.service.CoverSelection
import tachiyomi.domain.metadata.service.MetadataRepository

// AM (Yume shared cover index, 2026-10-20; new code assisted by Codex) -->
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CoverResolverImpl(
    private val database: Database,
    private val repository: MetadataRepository,
) : CoverResolver {
    private val json = Json { ignoreUnknownKeys = true }
    private val registrations = mutableMapOf<Long, Int>()
    private val active = MutableStateFlow<List<Long>>(emptyList())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val selections = active.debounce(16).flatMapLatest { ids ->
        if (ids.isEmpty()) return@flatMapLatest flowOf(emptyMap())
        combine(
            database.metadataQueries.artworkRecords(ids).subscribeToList(),
            repository.observeControl(),
        ) { rows, control -> rows to control }.flatMapLatest { (rows, control) ->
            val records = rows.mapNotNull { row ->
                val state = runCatching { json.decodeFromString<MetadataDecisionState>(row.payload) }.getOrNull()
                    ?: return@mapNotNull null
                val changedSource = state.identity.sourceId != row.current_source ||
                    state.identity.sourceUrl != row.current_url
                val changed = changedSource || (
                    state.association?.manual == false &&
                        (state.identity.title != row.current_title || state.identity.parentId != row.current_parent)
                    )
                MetadataRecord(
                    state.copy(
                        association = state.association.takeUnless { changedSource },
                        needsReview = state.needsReview || changed,
                    ),
                    row.revision,
                    row.generation,
                )
            }.associateBy { it.state.identity.animeId }
            val remoteIds = records.values.mapNotNull { it.state.association?.id?.value }.distinct()
            val mediaFlow = if (remoteIds.isEmpty()) {
                flowOf(emptyList())
            } else {
                database.metadataQueries.artworkMedia(remoteIds).subscribeToList()
            }
            mediaFlow.transformLatest { mediaRows ->
                val media = mediaRows.mapNotNull { row ->
                    runCatching {
                        MetadataCachedMedia(json.decodeFromString<MetadataMedia>(row.payload), row.fetched_at, false)
                    }.getOrNull()
                }.associateBy { it.media.id }
                while (true) {
                    val now = System.currentTimeMillis()
                    emit(
                        ids.associateWith { id ->
                            val record = records[id]
                            CoverPolicy.resolve(control, record, media[record?.state?.association?.id], now)
                        },
                    )
                    val nextExpiry = media.values.map { it.fetchedAt + MetadataCachePolicy.RETAIN }
                        .filter { it > now }.minOrNull() ?: awaitCancellation()
                    delay((nextExpiry - now).coerceAtLeast(1))
                }
            }
        }.catch { emit(ids.associateWith { CoverSelection() }) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000, 0), emptyMap())

    override fun observe(animeIds: List<Long>): Flow<Map<Long, CoverSelection>> = flow {
        val ids = animeIds.distinct()
        if (ids.isEmpty()) {
            emit(emptyMap())
            return@flow
        }
        synchronized(registrations) {
            ids.forEach { registrations[it] = (registrations[it] ?: 0) + 1 }
            active.value = registrations.keys.sorted()
        }
        try {
            emitAll(
                selections.filter { snapshot -> ids.all { it in snapshot } }
                    .map { snapshot -> ids.associateWith { snapshot.getValue(it) } }.distinctUntilChanged(),
            )
        } finally {
            synchronized(registrations) {
                ids.forEach { id ->
                    val remaining = registrations.getValue(id) - 1
                    if (remaining == 0) registrations.remove(id) else registrations[id] = remaining
                }
                active.value = registrations.keys.sorted()
            }
        }
    }

    internal fun close() = scope.cancel()
}
// <-- AM (Yume shared cover index)
