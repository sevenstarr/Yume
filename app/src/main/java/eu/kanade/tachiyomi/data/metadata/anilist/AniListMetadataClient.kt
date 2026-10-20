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
    private val budget = AniListBudget()

    /** No consumer is wired yet. Future coordinator supplies its current opt-in policy. */
    fun provider(
        isEnabled: () -> Boolean = { false },
    ): MetadataProvider = AniListProvider(transport, json, budget, isEnabled)
}
// <-- AM (Yume metadata)
