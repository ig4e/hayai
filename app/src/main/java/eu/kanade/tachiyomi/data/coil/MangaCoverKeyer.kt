package eu.kanade.tachiyomi.data.coil

import yokai.util.koin.get
import coil3.key.Keyer
import coil3.request.Options
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.hasCustomCover
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.util.storage.DiskUtil
import yokai.domain.manga.models.MangaCover

class MangaKeyer : Keyer<Manga> {
    override fun key(data: Manga, options: Options): String {
        val key = when {
            data.hasCustomCover() -> data.id
            data.favorite -> data.thumbnail_url?.let { DiskUtil.hashKeyForDisk(it) }
            else -> data.thumbnail_url
        }

        return "${key};${data.cover_last_modified}"
    }
}

class MangaCoverKeyer(private val coverCache: CoverCache = get()) : Keyer<MangaCover> {
    override fun key(data: MangaCover, options: Options): String {
        val key = when {
            coverCache.getCustomCoverFile(data.mangaId).exists() -> data.mangaId
            data.inLibrary -> DiskUtil.hashKeyForDisk(data.url)
            else -> data.url
        }

        return "${key};${data.lastModified}"
    }
}
