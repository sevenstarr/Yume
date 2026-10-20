package tachiyomi.domain.metadata.service

import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

// AM (Yume metadata matching, 2026-10-20; new code assisted by Codex) -->
data class NormalizedMetadataTitle(
    val raw: String,
    val primary: String,
    val comparison: String,
    val compatibility: String,
    val accentFolded: String,
    val searchVariants: List<String>,
)

object MetadataTitleNormalizer {
    const val VERSION = "title-nfc-v1"
    const val FINGERPRINT_VERSION = "identity-v1"

    private val punctuation = Regex("[^\\p{L}\\p{M}\\p{N}]+")
    private val suffix = Regex(
        "(?:\\s+|\\s*[-–—|]\\s*|\\s*[\\[(]\\s*)(?:legendado|dublado|subbed|dubbed|hd|(?:480|720|1080|2160)p)" +
            "\\s*[\\])]?$",
        RegexOption.IGNORE_CASE,
    )

    fun normalize(raw: String): NormalizedMetadataTitle {
        require(raw.length <= 500)
        val nfc = Normalizer.normalize(raw, Normalizer.Form.NFC).replace(Regex("[\\p{Z}\\s]+"), " ").trim()
        var stripped = nfc
        while (true) {
            val next = stripped.replace(suffix, "").trim()
            if (next.isEmpty() || next == stripped) break
            stripped = next
        }
        val primary = tokens(nfc)
        val comparison = tokens(stripped)
        val compatibility = tokens(Normalizer.normalize(stripped, Normalizer.Form.NFKC))
        val accentFolded = Normalizer.normalize(compatibility, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        return NormalizedMetadataTitle(
            raw,
            primary,
            comparison,
            compatibility,
            accentFolded,
            listOf(primary, comparison).filter(String::isNotBlank).distinct(),
        )
    }

    /** Length-prefixed UTF-8 fields avoid delimiter/null collisions. Raw origin fields remain local. */
    fun fingerprint(identity: LocalAnimeIdentity): String {
        val fields = listOf(
            FINGERPRINT_VERSION, VERSION, identity.animeId.toString(), identity.sourceId.toString(),
            identity.sourceUrl, identity.title, identity.year?.toString(), identity.format?.name,
            identity.scope.name, identity.parentId?.toString(), identity.seasonNumber?.toString(),
        )
        val digest = MessageDigest.getInstance("SHA-256")
        fields.forEach { field ->
            val bytes = field?.toByteArray(Charsets.UTF_8)
            digest.update("${bytes?.size ?: -1}:".toByteArray(Charsets.US_ASCII))
            if (bytes != null) digest.update(bytes)
        }
        return "$FINGERPRINT_VERSION:" + digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun tokens(value: String) = value.lowercase(Locale.ROOT).replace(punctuation, " ").trim()
}
// <-- AM (Yume metadata matching)
