package tachiyomi.domain.metadata.service

import dev.zacsweers.metro.Inject
import tachiyomi.core.common.preference.PreferenceStore

// AM (Yume metadata settings, 2026-10-20; new code assisted by Codex) -->
@Inject
class MetadataPreferences(preferenceStore: PreferenceStore) {
    val batchMobileData = preferenceStore.getBoolean("metadata_batch_mobile_data", false)
}
// <-- AM (Yume metadata settings)
