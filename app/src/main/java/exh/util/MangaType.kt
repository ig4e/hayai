package exh.util

import yokai.domain.manga.models.Manga

/**
 * The type of comic the manga is (ie. manga, manhwa, manhua)
 */
fun Manga.mangaType(sourceName: String? = null): MangaType {
    val currentTags = genres.orEmpty()
    return when {
        currentTags.any { tag -> isMangaTag(tag) } -> {
            MangaType.TYPE_MANGA
        }
        currentTags.any { tag -> isWebtoonTag(tag) } || sourceName?.let { isWebtoonSource(it) } == true -> {
            MangaType.TYPE_WEBTOON
        }
        currentTags.any { tag -> isComicTag(tag) } || sourceName?.let { isComicSource(it) } == true -> {
            MangaType.TYPE_COMIC
        }
        currentTags.any { tag -> isManhuaTag(tag) } || sourceName?.let { isManhuaSource(it) } == true -> {
            MangaType.TYPE_MANHUA
        }
        currentTags.any { tag -> isManhwaTag(tag) } || sourceName?.let { isManhwaSource(it) } == true -> {
            MangaType.TYPE_MANHWA
        }
        else -> {
            MangaType.TYPE_MANGA
        }
    }
}

private fun isMangaTag(tag: String): Boolean {
    return tag.contains("manga", true) ||
        tag.contains("\u043C\u0430\u043D\u0433\u0430", true) // манга
}

private fun isManhuaTag(tag: String): Boolean {
    return tag.contains("manhua", true) ||
        tag.contains("\u043C\u0430\u043D\u044C\u0445\u0443\u0430", true) // маньхуа
}

private fun isManhwaTag(tag: String): Boolean {
    return tag.contains("manhwa", true) ||
        tag.contains("\u043C\u0430\u043D\u0445\u0432\u0430", true) // манхва
}

private fun isComicTag(tag: String): Boolean {
    return tag.contains("comic", true) ||
        tag.contains("\u043A\u043E\u043C\u0438\u043A\u0441", true) // комикс
}

private fun isWebtoonTag(tag: String): Boolean {
    return tag.contains("long strip", true) ||
        tag.contains("webtoon", true)
}

private fun isManhwaSource(sourceName: String): Boolean {
    return sourceName.contains("manhwa", true) ||
        sourceName.contains("toonily", true)
}

private fun isWebtoonSource(sourceName: String): Boolean {
    return sourceName.contains("webtoon", true) ||
        sourceName.contains("toomics", true)
}

private fun isComicSource(sourceName: String): Boolean {
    return sourceName.contains("readcomiconline", true) ||
        sourceName.contains("comicextra", true)
}

private fun isManhuaSource(sourceName: String): Boolean {
    return sourceName.contains("manhua", true)
}

enum class MangaType {
    TYPE_MANGA,
    TYPE_MANHWA,
    TYPE_MANHUA,
    TYPE_COMIC,
    TYPE_WEBTOON,
}
