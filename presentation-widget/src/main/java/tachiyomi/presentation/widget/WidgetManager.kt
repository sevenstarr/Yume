@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package tachiyomi.presentation.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.LifecycleCoroutineScope
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.metadata.service.CoverResolver
import tachiyomi.domain.updates.interactor.GetUpdates

@Inject
class WidgetManager(
    private val getUpdates: GetUpdates,
    private val securityPreferences: SecurityPreferences,
    private val coverResolver: CoverResolver,
) {

    context(context: Context)
    fun init(scope: LifecycleCoroutineScope) {
        getUpdates.subscribe(seen = false, after = BaseUpdatesGridGlanceWidget.DateLimit.toEpochMilliseconds())
            .flatMapLatest { updates ->
                combine(
                    securityPreferences.useAuthenticator.changes(),
                    coverResolver.observe(updates.map { it.animeId }),
                ) { locked, selections ->
                    Triple(updates, locked, selections)
                }
            }
            .distinctUntilChanged { old, new ->
                old.second == new.second && old.third == new.third &&
                    old.first.map { it.episodeId }.toSet() == new.first.map { it.episodeId }.toSet()
            }
            .onEach {
                try {
                    UpdatesGridGlanceWidget().updateAll(context)
                    UpdatesGridCoverScreenGlanceWidget().updateAll(context)
                } catch (e: Exception) {
                    this.logcat(LogPriority.ERROR, e) { "Failed to update widget" }
                }
            }
            .flowOn(Dispatchers.Default)
            .launchIn(scope)
    }
}
