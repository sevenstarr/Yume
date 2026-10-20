package tachiyomi.domain.metadata.service

// AM (Yume metadata image cache boundary, 2026-10-20; assisted by Codex) -->
/** Evicts only keys registered by this layer; implementations reuse the app's image cache. */
fun interface MetadataImageCache {
    suspend fun remove(keys: Set<String>)
}
// <-- AM (Yume metadata image cache boundary)
