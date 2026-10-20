package eu.kanade.presentation.anime.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import coil3.Extras
import coil3.getOrDefault
import coil3.request.ImageRequest
import eu.kanade.tachiyomi.data.coil.useBackgroundKey
import mihon.app.di.AppGraph
import mihon.core.metro.metroGraph
import tachiyomi.domain.anime.model.Anime
import tachiyomi.domain.anime.model.AnimeCover
import tachiyomi.domain.metadata.service.CoverSelection

// AM (Yume reactive cover requests, 2026-10-20; new code assisted by Codex) -->
private val coverSelectionKey = Extras.Key(CoverSelection())

@Composable
fun rememberMetadataCover(data: Any?): Any? {
    if (LocalInspectionMode.current) return data
    val request = data as? ImageRequest
    if (request?.extras?.getOrDefault(useBackgroundKey) == true) return data
    val id = when (val model = request?.data ?: data) {
        is Anime -> model.id
        is AnimeCover -> model.animeId
        else -> return data
    }
    val context = LocalContext.current
    val resolver = remember(context) { context.metroGraph<AppGraph>().coverResolver }
    val flow = remember(resolver, id) { resolver.observe(listOf(id)) }
    val selections by flow.collectAsState(initial = emptyMap())
    val selection = selections[id] ?: CoverSelection()
    return remember(data, selection) {
        (request?.newBuilder() ?: ImageRequest.Builder(context).data(data))
            .apply { extras[coverSelectionKey] = selection }.build()
    }
}
// <-- AM (Yume reactive cover requests)
