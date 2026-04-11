package exh.util

import exh.source.isEhBasedManga
import yokai.domain.manga.models.Manga

fun Manga.isLewd(): Boolean {
    if (isEhBasedManga()) {
        return genres.orEmpty().none { tag -> isNonHentaiTag(tag) }
    }

    return genres.orEmpty().any { tag -> isHentaiTag(tag) }
}

private fun isNonHentaiTag(tag: String): Boolean {
    return tag.contains("non-h", true)
}

private fun isHentaiTag(tag: String): Boolean {
    return tag.contains("hentai", true) ||
        tag.contains("adult", true) ||
        tag.contains("smut", true) ||
        tag.contains("lewd", true) ||
        tag.contains("nsfw", true) ||
        tag.contains("erotica", true) ||
        tag.contains("pornographic", true) ||
        tag.contains("mature", true) ||
        tag.contains("18+", true)
}
