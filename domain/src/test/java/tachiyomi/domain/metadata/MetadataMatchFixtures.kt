package tachiyomi.domain.metadata

import tachiyomi.domain.metadata.model.LocalAnimeIdentity
import tachiyomi.domain.metadata.model.MatchInput
import tachiyomi.domain.metadata.model.MetadataDate
import tachiyomi.domain.metadata.model.MetadataFormat
import tachiyomi.domain.metadata.model.MetadataId
import tachiyomi.domain.metadata.model.MetadataMedia
import tachiyomi.domain.metadata.model.MetadataScope
import tachiyomi.domain.metadata.model.MetadataTitles

// AM (Yume metadata matching tests, 2026-10-20; new synthetic fixtures assisted by Codex) -->
/** TSV labels are editorial synthetic scenarios, not claims about real catalog records. '-' is unknown. */
data class MatchFixture(val name: String, val expectedId: MetadataId?, val input: MatchInput)

object MetadataMatchFixtures {
    fun read(split: String): List<MatchFixture> {
        val text = checkNotNull(javaClass.getResource("/metadata/$split.tsv")).readText()
        return text.lineSequence().filter { it.isNotBlank() && !it.startsWith('#') }.mapIndexed { index, line ->
            val fields = line.split('\t')
            require(fields.size == 10) { "$split:${index + 1}" }
            val candidates = if (fields[9] == "-") {
                emptyList()
            } else {
                fields[9].split(';').map { raw ->
                    val parts = raw.split('~')
                    require(parts.size == 5)
                    media(parts[0], parts[1].split('|'), parts[2].toIntOrNull(), format(parts[3]))
                }
            }
            val rejected = if (fields[9] == "-") {
                emptySet()
            } else {
                fields[9].split(';').filter {
                    it.endsWith("~rejected")
                }.map { MetadataId("anilist", it.substringBefore('~')) }.toSet()
            }
            MatchFixture(
                fields[0],
                fields[1].takeUnless { it == "-" }?.let { MetadataId("anilist", it) },
                MatchInput(
                    LocalAnimeIdentity(
                        index.toLong(),
                        7,
                        "/fixture/$split/$index",
                        fields[2],
                        fields[3].toIntOrNull(),
                        format(fields[4]),
                        MetadataScope.valueOf(fields[5]),
                        seasonNumber = fields[6].toIntOrNull(),
                    ),
                    candidates,
                    fields[7] == "complete",
                    rejected,
                    automaticBlocked = fields[8] == "blocked",
                ),
            )
        }.toList()
    }

    fun media(id: String, titles: List<String>, year: Int?, format: MetadataFormat?) = MetadataMedia(
        MetadataId("anilist", id), null, MetadataTitles(titles.first(), null, null), titles.drop(1), format,
        year?.let { MetadataDate(it, null, null) }, null, null, null, null, emptyList(),
    )

    private fun format(value: String) = value.takeUnless { it == "-" }?.let(MetadataFormat::valueOf)
}
// <-- AM (Yume metadata matching tests)
