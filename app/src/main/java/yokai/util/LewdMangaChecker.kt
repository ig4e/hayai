package yokai.util

import yokai.util.koin.get
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import java.util.*

fun Manga.isLewd(): Boolean {
    val sourceName = get<SourceManager>().get(source)?.name
    val tags = genre?.split(",")?.map { it.trim().lowercase(Locale.US) } ?: emptyList()

    if (!tags.none { it.isNonHentai() }) return false
    return (sourceName != null && sourceName.isFromHentaiSource()) || tags.any { it.isHentai() }
}

private fun String.isNonHentai() =
    contains("non-h", true) ||
    contains("non-erotic", true) ||
    contains("sfw", true)

private fun String.isFromHentaiSource() =
    contains("hentai", true) ||
    contains("adult", true)

private fun String.isHentai() =
    contains("hentai", true) ||
    contains("adult", true) ||
    contains("smut", true) ||
    contains("lewd", true) ||
    contains("nsfw", true) ||
    contains("erotic", true) ||
    contains("pornographic", true) ||
    contains("18+", true)
