package eu.kanade.presentation.more.settings.screen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import eu.kanade.presentation.more.settings.Preference
import kotlinx.coroutines.CancellationException
import mihon.app.di.appGraph
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.metadata.model.MetadataControl
import tachiyomi.domain.metadata.model.MetadataRestoreResult
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

// AM (Yume metadata settings, 2026-10-20; new code assisted by Codex) -->
object SettingsMetadataScreen : SearchableSettings {
    @Composable
    override fun getTitleRes() = MR.strings.metadata_settings

    @Composable
    override fun getPreferences(): List<Preference> {
        val graph = LocalContext.current.appGraph
        val repository = graph.metadataRepository
        val scheduler = graph.metadataSyncScheduler
        val scope = rememberCoroutineScope()
        val controlFlow = remember(repository) { repository.observeControl() }
        val control by controlFlow.collectAsState(MetadataControl(false, 0, 0))
        val batch by remember(graph) { graph.metadataSyncQueue.observe() }
            .collectAsState(tachiyomi.domain.metadata.service.MetadataSyncStatus())
        var confirmation by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }
        var failed by remember { mutableStateOf(false) }
        var pending by remember { mutableStateOf<List<MetadataRestoreResult>>(emptyList()) }
        LaunchedEffect(control.epoch) {
            try {
                pending = repository.pendingRestores()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                failed = true
            }
        }
        fun execute(block: suspend () -> Unit) {
            busy = true
            scope.launchIO {
                try {
                    block()
                    failed = false
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    failed = true
                } finally {
                    busy = false
                }
            }
        }
        val action = confirmation
        if (action != null) {
            AlertDialog(
                onDismissRequest = { confirmation = null },
                title = { Text(stringResource(MR.strings.metadata_settings)) },
                text = {
                    Text(
                        stringResource(
                            when (action) {
                                "enable" -> MR.strings.metadata_enable_explanation
                                "cache" -> MR.strings.metadata_clear_cache_explanation
                                else -> MR.strings.metadata_clear_all_explanation
                            },
                        ),
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        confirmation = null
                        execute {
                            when (action) {
                                "enable" -> repository.setEnabled(true)
                                "cache" -> repository.clearCache()
                                else -> repository.clearAll()
                            }
                        }
                    }) { Text(stringResource(MR.strings.action_ok)) }
                },
                dismissButton = {
                    TextButton(onClick = { confirmation = null }) { Text(stringResource(MR.strings.action_cancel)) }
                },
            )
        }
        val enableLabel = stringResource(MR.strings.metadata_enable)
        val toggleEnabled = {
            if (!busy) {
                if (control.enabled) execute { repository.setEnabled(false) } else confirmation = "enable"
            }
        }
        return buildList {
            add(
                Preference.PreferenceItem.TextPreference(
                    title = enableLabel,
                    subtitle = stringResource(MR.strings.metadata_enable_explanation),
                    enabled = !busy,
                    widget = {
                        Switch(
                            checked = control.enabled,
                            onCheckedChange = { toggleEnabled() },
                            modifier = Modifier.semantics { contentDescription = enableLabel },
                        )
                    },
                    onClick = toggleEnabled,
                ),
            )
            add(
                Preference.PreferenceItem.SwitchPreference(
                    preference = graph.metadataPreferences.batchMobileData,
                    title = stringResource(MR.strings.metadata_mobile_data),
                    subtitle = stringResource(MR.strings.metadata_mobile_data_explanation),
                    enabled = !busy,
                ),
            )
            val onAddLabel = stringResource(MR.strings.metadata_sync_on_add)
            add(
                Preference.PreferenceItem.TextPreference(
                    title = onAddLabel,
                    subtitle = stringResource(MR.strings.metadata_sync_explanation),
                    enabled = !busy,
                    widget = {
                        Switch(
                            checked = batch.onAdd,
                            enabled = !busy,
                            modifier = Modifier.semantics { contentDescription = onAddLabel },
                            onCheckedChange = { value -> execute { graph.metadataSyncQueue.setOnAdd(value) } },
                        )
                    },
                    onClick = { execute { graph.metadataSyncQueue.setOnAdd(!batch.onAdd) } },
                ),
            )
            add(
                Preference.PreferenceItem.SwitchPreference(
                    preference = graph.metadataPreferences.scheduled,
                    title = stringResource(MR.strings.metadata_sync_daily),
                    subtitle = stringResource(MR.strings.metadata_sync_explanation),
                    enabled = !busy,
                ),
            )
            val batchState = stringResource(
                when (batch.status) {
                    "RUNNING" -> MR.strings.metadata_sync_running
                    "PAUSED" -> MR.strings.metadata_sync_paused
                    "CANCELLED" -> MR.strings.metadata_sync_cancelled
                    "COMPLETED" -> MR.strings.metadata_sync_completed
                    else -> MR.strings.metadata_sync_idle
                },
            )
            add(
                Preference.PreferenceItem.TextPreference(
                    title = batchState,
                    subtitle = stringResource(
                        MR.strings.metadata_sync_progress,
                        batch.completed,
                        batch.total,
                        batch.success,
                        batch.ambiguous,
                        batch.errors,
                        batch.skipped,
                    ),
                ),
            )
            add(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.metadata_sync_start),
                    subtitle = stringResource(MR.strings.metadata_sync_explanation),
                    enabled = !busy && control.enabled && batch.status !in setOf("RUNNING", "PAUSED"),
                    onClick = { execute { scheduler.start() } },
                ),
            )
            if (batch.status in setOf("RUNNING", "PAUSED")) {
                add(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(
                            if (batch.status ==
                                "PAUSED"
                            ) {
                                MR.strings.metadata_sync_resume
                            } else {
                                MR.strings.metadata_sync_pause
                            },
                        ),
                        enabled = !busy && control.enabled,
                        onClick = {
                            execute { if (batch.status == "PAUSED") scheduler.resume() else scheduler.pause() }
                        },
                    ),
                )
                add(
                    Preference.PreferenceItem.TextPreference(
                        title = stringResource(MR.strings.metadata_sync_cancel),
                        enabled = !busy,
                        onClick = { execute { scheduler.cancel() } },
                    ),
                )
            }
            if (busy) add(Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.metadata_loading)))
            if (failed) add(Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.metadata_local_error)))
            add(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.metadata_clear_cache),
                    subtitle = stringResource(MR.strings.metadata_clear_cache_explanation),
                    enabled = !busy,
                    onClick = { confirmation = "cache" },
                ),
            )
            add(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.metadata_clear_all),
                    subtitle = stringResource(MR.strings.metadata_clear_all_explanation),
                    enabled = !busy,
                    onClick = { confirmation = "all" },
                ),
            )
            if (pending.isNotEmpty()) {
                add(Preference.PreferenceItem.InfoPreference(stringResource(MR.strings.metadata_restore_help)))
                pending.forEach { result ->
                    add(
                        Preference.PreferenceItem.TextPreference(
                            title = stringResource(MR.strings.metadata_restore_pending),
                            subtitle = stringResource(
                                MR.strings.metadata_restore_detail,
                                result.decision.provider.orEmpty(),
                                result.decision.remoteId.orEmpty(),
                                result.decision.source.toString(),
                                result.decision.url,
                            ),
                        ),
                    )
                }
            }
        }
    }
}
// <-- AM (Yume metadata settings)
