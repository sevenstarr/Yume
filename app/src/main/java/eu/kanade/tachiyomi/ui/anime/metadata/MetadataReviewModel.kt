package eu.kanade.tachiyomi.ui.anime.metadata

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactory
import dev.zacsweers.metrox.viewmodel.ManualViewModelAssistedFactoryKey
import eu.kanade.tachiyomi.animesource.model.FetchType
import eu.kanade.tachiyomi.data.metadata.anilist.AniListMetadataClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.anime.interactor.GetAnime
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MetadataFailureKind
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataRecord
import tachiyomi.domain.metadata.model.MetadataScope
import tachiyomi.domain.metadata.service.MetadataManualInput
import tachiyomi.domain.metadata.service.MetadataRepository
import tachiyomi.domain.metadata.service.MetadataReviewActions
import java.util.concurrent.atomic.AtomicBoolean

// AM (Yume manual metadata UI, 2026-10-20; new code assisted by Codex) -->
@AssistedInject
class MetadataReviewModel(
    @Assisted private val animeId: Long,
    getAnime: GetAnime,
    private val repository: MetadataRepository,
    client: AniListMetadataClient,
) : ViewModel() {
    private val enabled = AtomicBoolean(false)
    private val actions = MetadataReviewActions(repository, client.provider { enabled.get() })
    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State> = mutableState
    private var operation: Job? = null

    init {
        viewModelScope.launchIO {
            try {
                combine(
                    getAnime.subscribe(animeId),
                    repository.observe(listOf(animeId)),
                    repository.observeControl(),
                ) { anime, records, control -> Triple(anime, records.firstOrNull(), control) }
                    .collect { (anime, record, control) ->
                        enabled.set(control.enabled)
                        mutableState.update {
                            val invalidated = it.epoch != control.epoch || !control.enabled
                            it.copy(
                                anime = anime,
                                record = record,
                                enabled = control.enabled,
                                epoch = control.epoch,
                                candidates = if (invalidated) emptyList() else it.candidates,
                                searched = it.searched && !invalidated,
                                hasNext = it.hasNext && !invalidated,
                                incomplete = it.incomplete && !invalidated,
                                proposal = it.proposal?.takeIf { proposal ->
                                    control.enabled && proposal.epoch == control.epoch
                                },
                            )
                        }
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(localError = true) }
            }
        }
    }

    fun search(query: String, next: Boolean = false) = run {
        val snapshot = state.value
        val identity = snapshot.anime?.identity() ?: return@run invalid()
        val text = if (next) snapshot.query else query.trim()
        val page = if (next) snapshot.page + 1 else 1
        val id = MetadataManualInput.id(text)
        if (id != null) {
            showProposal(actions.verify(identity, snapshot.record?.revision, id))
            return@run
        }
        // Link-like and numeric input must never silently become an unrelated title search.
        if (text.contains("://") || text.all(Char::isDigit)) return@run invalid()
        when (val result = actions.search(identity, snapshot.record?.revision, text, page)) {
            is MetadataReviewActions.Result.Value -> mutableState.update {
                if (!it.enabled || it.epoch != snapshot.epoch) return@update it
                it.copy(
                    candidates = if (next) {
                        (it.candidates + result.value.media).distinctBy(MetadataMedia::id)
                    } else {
                        result.value.media
                    },
                    query = text,
                    page = page,
                    hasNext = result.value.hasNextPage == true && page < 20,
                    searched = true,
                    incomplete = result.value.hasNextPage == null,
                )
            }
            else -> message(result)
        }
    }

    fun select(id: MetadataId) = run {
        val snapshot = state.value
        val identity = snapshot.anime?.identity() ?: return@run invalid()
        showProposal(actions.verify(identity, snapshot.record?.revision, id))
    }

    private fun showProposal(result: MetadataReviewActions.Result<MetadataReviewActions.Proposal>) {
        if (result is MetadataReviewActions.Result.Value) {
            mutableState.update {
                if (it.enabled && it.epoch == result.value.epoch) it.copy(proposal = result.value) else it
            }
        } else {
            message(result)
        }
    }

    fun confirm() = run {
        val proposal = state.value.proposal ?: return@run
        val result = actions.confirm(proposal, true)
        mutableState.update { it.copy(proposal = null) }
        message(result)
    }

    fun dismissProposal() {
        mutableState.update { it.copy(proposal = null) }
    }

    fun refresh() {
        if (state.value.record?.state?.association == null) {
            search(state.value.anime?.ogTitle.orEmpty())
            return
        }
        run {
            val snapshot = state.value
            val identity = snapshot.anime?.identity() ?: return@run invalid()
            message(actions.refresh(identity, snapshot.record?.revision))
        }
    }

    fun original(value: Boolean) = run {
        val revision = state.value.record?.revision
        localResult(repository.setOriginalOnly(animeId, revision, value))
    }

    fun remove() = run {
        val revision = state.value.record?.revision ?: return@run
        localResult(repository.removeAssociation(animeId, revision))
    }

    fun reject(id: MetadataId) = run {
        val record = state.value.record ?: return@run
        if (record.state.rejected.size >= 100 && id !in record.state.rejected) return@run invalid()
        localResult(repository.reject(animeId, record.revision, id))
    }

    fun cancel() {
        operation?.cancel()
    }

    private fun run(block: suspend () -> Unit) {
        if (state.value.busy) return
        mutableState.update {
            it.copy(busy = true, failure = null, conflict = false, localError = false, saved = false)
        }
        operation = viewModelScope.launchIO {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutableState.update { it.copy(localError = true) }
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    private fun localResult(applied: Boolean) = message(
        if (applied) MetadataReviewActions.Result.Value(Unit) else MetadataReviewActions.Result.Conflict,
    )

    private fun message(result: MetadataReviewActions.Result<*>) {
        mutableState.update {
            it.copy(
                failure = (result as? MetadataReviewActions.Result.Failed)?.kind,
                conflict = result is MetadataReviewActions.Result.Conflict,
                saved = result is MetadataReviewActions.Result.Value,
            )
        }
    }

    private fun invalid() = message(MetadataReviewActions.Result.Failed(MetadataFailureKind.INVALID_INPUT))

    private fun Anime.identity(): LocalAnimeIdentity? {
        if (ogTitle.isBlank() || ogTitle.length > 500 || url.length > 4096) return null
        return LocalAnimeIdentity(
            id,
            source,
            url,
            ogTitle,
            scope = when {
                fetchType == FetchType.Seasons -> MetadataScope.COLLECTION
                parentId != null -> MetadataScope.CHILD_SEASON
                else -> MetadataScope.UNKNOWN
            },
            parentId = parentId,
            seasonNumber = seasonNumber.toInt().takeIf { it > 0 && it.toDouble() == seasonNumber },
        )
    }

    data class State(
        val anime: Anime? = null,
        val record: MetadataRecord? = null,
        val enabled: Boolean = false,
        val epoch: Long = 0,
        val busy: Boolean = false,
        val candidates: List<MetadataMedia> = emptyList(),
        val query: String = "",
        val page: Int = 0,
        val hasNext: Boolean = false,
        val searched: Boolean = false,
        val incomplete: Boolean = false,
        val proposal: MetadataReviewActions.Proposal? = null,
        val failure: MetadataFailureKind? = null,
        val conflict: Boolean = false,
        val localError: Boolean = false,
        val saved: Boolean = false,
    )

    @AssistedFactory
    @ManualViewModelAssistedFactoryKey
    @ContributesIntoMap(AppScope::class)
    interface Factory : ManualViewModelAssistedFactory {
        fun create(animeId: Long): MetadataReviewModel
    }
}
// <-- AM (Yume manual metadata UI)
