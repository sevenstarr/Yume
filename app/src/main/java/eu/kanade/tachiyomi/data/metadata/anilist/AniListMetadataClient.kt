package eu.kanade.tachiyomi.data.metadata.anilist

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import kotlinx.serialization.json.Json
import tachiyomi.domain.metadata.service.MetadataProvider

// AM (Yume metadata, 2026-10-20; new code assisted by Codex) -->
@Inject
@SingleIn(AppScope::class)
class AniListMetadataClient(network: NetworkHelper, private val json: Json) {
    private val transport = OkHttpAniListTransport(network.client)
    private val backgroundTransport = OkHttpAniListTransport(network.client, background = true)
    private val budget = AniListBudget(
        onObserved = AniListHostInterceptor::observe,
        onGraphqlFailure = AniListHostInterceptor::transientFailure,
        onSuccess = AniListHostInterceptor::success,
    )

    /** Callers supply opt-in policy; review/coordinator also observe SQL invalidation and cancel transport. */
    fun provider(
        background: Boolean = false,
        isEnabled: () -> Boolean = { false },
    ): MetadataProvider = AniListProvider(if (background) backgroundTransport else transport, json, budget, isEnabled)
}
// <-- AM (Yume metadata)
