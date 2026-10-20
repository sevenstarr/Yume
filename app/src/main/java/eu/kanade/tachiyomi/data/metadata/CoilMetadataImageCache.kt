package eu.kanade.tachiyomi.data.metadata

import android.content.Context
import coil3.imageLoader
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tachiyomi.domain.metadata.service.MetadataImageCache

// AM (Yume metadata image cache, 2026-10-20; new code assisted by Codex) -->
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class CoilMetadataImageCache(private val context: Context) : MetadataImageCache {
    override suspend fun remove(keys: Set<String>) = withContext(Dispatchers.IO) {
        require(keys.all { it.startsWith("yume-metadata-v1:") })
        val loader = context.imageLoader
        loader.memoryCache?.let { memory ->
            memory.keys.filter { it.key in keys }.forEach { memory.remove(it) }
        }
        keys.forEach { loader.diskCache?.remove(it) }
    }
}
// <-- AM (Yume metadata image cache)
