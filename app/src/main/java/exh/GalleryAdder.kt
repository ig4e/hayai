package exh

import android.content.Context
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.source.getMainSource
import yokai.domain.manga.models.Manga

class GalleryAdder {
    private val logger = Logger.withTag("GalleryAdder")

    fun pickSource(url: String): List<UrlImportableSource> {
        val uri = url.toUri()
        // TODO: Wire up SourceManager when available
        return emptyList()
    }

    suspend fun addGallery(
        context: Context,
        url: String,
        fav: Boolean = false,
        forceSource: UrlImportableSource? = null,
        throttleFunc: suspend () -> Unit = {},
        retry: Int = 1,
    ): GalleryAddEvent {
        logger.d { "Importing gallery: $url, fav=$fav, forceSource=${forceSource?.toString().orEmpty()}" }
        try {
            val uri = url.toUri()

            // Find matching source
            val source = if (forceSource != null) {
                try {
                    if (forceSource.matchesUri(uri)) {
                        forceSource
                    } else {
                        return GalleryAddEvent.Fail.UnknownSource(url)
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Source URI must match" }
                    return GalleryAddEvent.Fail.UnknownType(url)
                }
            } else {
                return GalleryAddEvent.Fail.UnknownSource(url)
            }

            // Map URL to manga URL
            val realMangaUrl = try {
                source.mapUrlToMangaUrl(uri)
            } catch (e: Exception) {
                logger.e(e) { "URI map to gallery error" }
                null
            } ?: return GalleryAddEvent.Fail.UnknownType(url)

            // Clean URL
            val cleanedMangaUrl = try {
                source.cleanMangaUrl(realMangaUrl)
            } catch (e: Exception) {
                logger.e(e) { "URI clean error" }
                null
            } ?: return GalleryAddEvent.Fail.UnknownType(url)

            // TODO: Wire up manga fetching and DB operations when repositories are available
            // For now, create a minimal manga object
            val manga = Manga(
                id = null,
                url = cleanedMangaUrl,
                title = "",
                artist = null,
                author = null,
                description = null,
                genres = null,
                status = 0,
                thumbnailUrl = null,
                updateStrategy = eu.kanade.tachiyomi.source.model.UpdateStrategy.ALWAYS_UPDATE,
                initialized = false,
                source = source.id,
                favorite = fav,
                lastUpdate = 0L,
                dateAdded = System.currentTimeMillis(),
                viewerFlags = 0,
                chapterFlags = 0,
                hideTitle = false,
                filteredScanlators = null,
                coverLastModified = 0L,
            )

            return GalleryAddEvent.Success(url, manga)
        } catch (e: Exception) {
            logger.w(e) { "Could not add gallery: $url" }

            if (e is EHentai.GalleryNotFoundException) {
                return GalleryAddEvent.Fail.NotFound(url)
            }

            return GalleryAddEvent.Fail.Error(
                url,
                ((e.message ?: "Unknown error!") + " (Gallery: $url)").trim(),
            )
        }
    }
}

sealed class GalleryAddEvent {
    abstract val logMessage: String
    abstract val galleryUrl: String
    open val galleryTitle: String? = null

    class Success(
        override val galleryUrl: String,
        val manga: Manga,
    ) : GalleryAddEvent() {
        override val galleryTitle = manga.title
        override val logMessage = "Successfully added gallery: $galleryTitle"
    }

    sealed class Fail : GalleryAddEvent() {
        class UnknownType(override val galleryUrl: String) : Fail() {
            override val logMessage = "Unknown gallery type: $galleryUrl"
        }

        open class Error(
            override val galleryUrl: String,
            override val logMessage: String,
        ) : Fail()

        class NotFound(galleryUrl: String) :
            Error(galleryUrl, "Gallery does not exist: $galleryUrl")

        class UnknownSource(override val galleryUrl: String) : Fail() {
            override val logMessage = "Unknown source for gallery: $galleryUrl"
        }
    }
}
