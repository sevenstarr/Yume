package eu.kanade.tachiyomi.ui.anime.metadata

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import dev.zacsweers.metrox.viewmodel.assistedMetroViewModel
import eu.kanade.presentation.anime.components.AnimeCover
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import tachiyomi.domain.metadata.model.MatchInput
import tachiyomi.domain.metadata.model.MetadataFailureKind
import tachiyomi.domain.metadata.model.MetadataFormat
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.service.AnimeMatcher
import tachiyomi.domain.metadata.service.CoverPolicy
import tachiyomi.domain.metadata.service.ExternalCover
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import java.text.DateFormat
import java.util.Date

// AM (Yume manual metadata UI, 2026-10-20; new code assisted by Codex) -->
class MetadataReviewScreen(private val animeId: Long) : Screen() {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = assistedMetroViewModel<MetadataReviewModel, MetadataReviewModel.Factory> { create(animeId) }
        val state by model.state.collectAsStateWithLifecycle()
        DisposableEffect(model) {
            onDispose { model.cancel() }
        }
        var query by rememberSaveable(state.anime?.ogTitle) { mutableStateOf(state.anime?.ogTitle.orEmpty()) }
        var remove by remember { mutableStateOf(false) }
        val record = state.record?.state
        val idle = !state.busy && state.anime != null
        Scaffold(
            topBar = { AppBar(title = stringResource(MR.strings.metadata_settings), navigateUp = navigator::pop) },
        ) { padding ->
            LazyColumn(contentPadding = padding, modifier = Modifier.imePadding().padding(horizontal = 16.dp)) {
                item {
                    Text(state.anime?.title.orEmpty(), style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { navigator.push(SettingsScreen(SettingsScreen.Destination.Metadata)) }) {
                        Text(stringResource(MR.strings.metadata_settings))
                    }
                    if (!state.enabled) Text(stringResource(MR.strings.metadata_disabled))
                    val association = record?.association
                    val requiresReview = record?.requiresRevalidation == true || record?.needsReview == true
                    Text(
                        stringResource(
                            if (record?.originalOnly == true) {
                                MR.strings.metadata_state_original
                            } else {
                                MR.strings.metadata_state_normal
                            },
                        ),
                    )
                    Text(
                        stringResource(
                            MR.strings.metadata_origin,
                            association?.id?.provider ?: "—",
                            association?.id?.value ?: "—",
                        ),
                    )
                    Text(
                        stringResource(
                            MR.strings.metadata_last_update,
                            record?.lastCheckedAt?.let { DateFormat.getDateTimeInstance().format(Date(it)) } ?: "—",
                        ),
                    )
                    Text(
                        stringResource(
                            when {
                                requiresReview -> MR.strings.metadata_state_review
                                record?.outcome == "UNAVAILABLE" -> MR.strings.metadata_state_unavailable
                                association?.manual == true -> MR.strings.metadata_state_pinned
                                association != null -> MR.strings.metadata_state_associated
                                else -> MR.strings.metadata_state_pending
                            },
                        ),
                    )
                    record?.failure?.let { Text(stringResource(failureString(it))) }
                    if (state.busy) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(
                            stringResource(MR.strings.metadata_loading),
                            Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        TextButton(onClick = model::cancel) { Text(stringResource(MR.strings.action_cancel)) }
                    }
                    val message = when {
                        state.localError -> MR.strings.metadata_local_error
                        state.conflict -> MR.strings.metadata_conflict
                        state.failure != null -> failureString(state.failure!!)
                        state.saved -> MR.strings.metadata_saved
                        else -> null
                    }
                    message?.let {
                        Text(stringResource(it), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    }
                    TextButton(onClick = model::refresh, enabled = idle && state.enabled) {
                        Text(stringResource(MR.strings.metadata_refresh))
                    }
                    TextButton(onClick = { model.original(record?.originalOnly != true) }, enabled = idle) {
                        val label = if (record?.originalOnly == true) {
                            MR.strings.metadata_use_selected
                        } else {
                            MR.strings.metadata_use_original
                        }
                        Text(stringResource(label))
                    }
                    Text(stringResource(MR.strings.metadata_original_help))
                    TextButton(onClick = { remove = true }, enabled = idle && association != null) {
                        Text(stringResource(MR.strings.metadata_remove))
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { if (it.length <= 300) query = it },
                        label = { Text(stringResource(MR.strings.metadata_query)) },
                        singleLine = true,
                        enabled = idle && state.enabled,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val canSearch = idle && state.enabled && query.isNotBlank()
                    Button(onClick = { model.search(query) }, enabled = canSearch) {
                        Text(stringResource(MR.strings.action_search))
                    }
                    if (state.searched) {
                        Text(stringResource(MR.strings.metadata_results_for, state.query))
                        if (state.incomplete) Text(stringResource(MR.strings.metadata_incomplete))
                        if (state.candidates.isEmpty()) Text(stringResource(MR.strings.metadata_no_results))
                    }
                }
                items(state.candidates, key = { it.id.value }) { media ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Candidate(media, state.enabled)
                        val rejected = media.id in record?.rejected.orEmpty()
                        if (rejected) Text(stringResource(MR.strings.metadata_rejected))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { model.select(media.id) }, enabled = idle && state.enabled) {
                                Text(stringResource(MR.strings.metadata_review_candidate))
                            }
                            val canReject = idle && !rejected && state.record != null
                            TextButton(onClick = { model.reject(media.id) }, enabled = canReject) {
                                Text(stringResource(MR.strings.metadata_reject))
                            }
                        }
                    }
                }
                if (state.hasNext) {
                    item {
                        TextButton(
                            onClick = { model.search(state.query, next = true) },
                            enabled = idle && state.enabled,
                        ) {
                            Text(stringResource(MR.strings.metadata_next_page))
                        }
                    }
                }
            }
        }
        val proposal = state.proposal
        if (proposal != null) {
            var acknowledged by remember(proposal) { mutableStateOf(false) }
            val evidence = remember(proposal) {
                val input = MatchInput(proposal.identity, listOf(proposal.verified.value), true)
                AnimeMatcher().evaluate(input).candidates.single()
            }
            AlertDialog(
                onDismissRequest = model::dismissProposal,
                title = { Text(stringResource(MR.strings.metadata_confirm_selection)) },
                text = {
                    Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                        Candidate(proposal.verified.value, state.enabled)
                        Text(stringResource(MR.strings.metadata_local_title, proposal.identity.title))
                        val titleEvidence = if (evidence.exactTitle) {
                            MR.strings.metadata_title_agrees
                        } else {
                            MR.strings.metadata_title_differs
                        }
                        Text(stringResource(titleEvidence))
                        Text(stringResource(MR.strings.metadata_missing_context))
                        if (evidence.vetoes.isNotEmpty()) {
                            Text(stringResource(MR.strings.metadata_candidate_conflict))
                        }
                        if (!proposal.verified.complete) Text(stringResource(MR.strings.metadata_incomplete))
                        Row(
                            Modifier.fillMaxWidth().toggleable(
                                value = acknowledged,
                                role = Role.Checkbox,
                                onValueChange = { acknowledged = it },
                            ).padding(vertical = 12.dp),
                        ) {
                            Checkbox(acknowledged, onCheckedChange = null)
                            Text(stringResource(MR.strings.metadata_acknowledge), Modifier.padding(start = 8.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = model::confirm, enabled = acknowledged && !state.busy && state.enabled) {
                        Text(stringResource(MR.strings.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = model::dismissProposal) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
        }
        if (remove) {
            AlertDialog(
                onDismissRequest = { remove = false },
                title = { Text(stringResource(MR.strings.metadata_remove)) },
                text = { Text(stringResource(MR.strings.metadata_remove_explanation)) },
                confirmButton = {
                    TextButton(onClick = {
                        remove = false
                        model.remove()
                    }) { Text(stringResource(MR.strings.action_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { remove = false }) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
        }
    }
}

@Composable
private fun Candidate(media: MetadataMedia, enabled: Boolean) {
    val context = LocalContext.current
    val title = media.titles.romaji ?: media.titles.english ?: media.titles.native.orEmpty()
    val url = media.covers.firstOrNull { it.size == "medium" && CoverPolicy.safeUrl(it.url) }?.url
    val request = remember(media, enabled) {
        if (enabled && url != null) {
            ImageRequest.Builder(context)
                .data(ExternalCover(media.id, url, "metadata-preview:${media.id.value}:$url"))
                // Preview bytes are ephemeral; selective durable cache ownership remains with accepted covers.
                .memoryCachePolicy(CachePolicy.DISABLED).diskCachePolicy(CachePolicy.DISABLED).build()
        } else {
            null
        }
    }
    val format = stringResource(
        when (media.format) {
            MetadataFormat.TV -> MR.strings.metadata_format_tv
            MetadataFormat.TV_SHORT -> MR.strings.metadata_format_tv_short
            MetadataFormat.MOVIE -> MR.strings.metadata_format_movie
            MetadataFormat.SPECIAL -> MR.strings.metadata_format_special
            MetadataFormat.OVA -> MR.strings.metadata_format_ova
            MetadataFormat.ONA -> MR.strings.metadata_format_ona
            MetadataFormat.MUSIC -> MR.strings.metadata_format_music
            null -> MR.strings.metadata_unknown
        },
    )
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        AnimeCover.Book(data = request, modifier = Modifier.width(56.dp), contentDescription = title)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            val alternatives = listOfNotNull(media.titles.english, media.titles.native)
                .filter { it != title }.distinct()
            alternatives.forEach { Text(it) }
            Text(
                stringResource(
                    MR.strings.metadata_candidate_details,
                    (media.startDate?.year ?: media.seasonYear)?.toString() ?: "—",
                    format,
                    media.id.value,
                ),
            )
            if (url == null) Text(stringResource(MR.strings.metadata_no_preview))
        }
    }
}

private fun failureString(kind: MetadataFailureKind) = when (kind) {
    MetadataFailureKind.DISABLED -> MR.strings.metadata_disabled
    MetadataFailureKind.INVALID_INPUT -> MR.strings.metadata_invalid_input
    MetadataFailureKind.RATE_LIMITED, MetadataFailureKind.CIRCUIT_OPEN -> MR.strings.metadata_rate_limited
    MetadataFailureKind.UNAVAILABLE -> MR.strings.metadata_unavailable
    MetadataFailureKind.CONTRACT -> MR.strings.metadata_contract_error
    MetadataFailureKind.NETWORK, MetadataFailureKind.TIMEOUT -> MR.strings.metadata_network_error
}
// <-- AM (Yume manual metadata UI)
