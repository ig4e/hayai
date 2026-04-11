package exh.util

import android.content.Context
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import exh.GalleryAddEvent
import exh.GalleryAdder
import rx.Observable

private val galleryAdder by lazy {
    GalleryAdder()
}

/**
 * A version of fetchSearchManga that supports URL importing
 */
fun UrlImportableSource.urlImportFetchSearchManga(
    context: Context,
    query: String,
    fail: () -> Observable<MangasPage>,
): Observable<MangasPage> =
    when {
        query.startsWith("http://") || query.startsWith("https://") -> {
            Observable.fromCallable {
                // TODO: This needs proper coroutine-to-rx bridging
                throw UnsupportedOperationException("Use urlImportFetchSearchMangaSuspend instead")
            }
        }
        else -> fail()
    }

/**
 * A version of fetchSearchManga that supports URL importing
 */
suspend fun UrlImportableSource.urlImportFetchSearchMangaSuspend(
    context: Context,
    query: String,
    fail: suspend () -> MangasPage,
): MangasPage =
    when {
        query.startsWith("http://") || query.startsWith("https://") -> {
            val res = galleryAdder.addGallery(
                context = context,
                url = query,
                fav = false,
                forceSource = this,
            )

            MangasPage(
                if (res is GalleryAddEvent.Success) {
                    listOf(
                        SManga.create().apply {
                            title = res.manga.title
                            url = res.manga.url
                            thumbnail_url = res.manga.thumbnailUrl
                        },
                    )
                } else {
                    emptyList()
                },
                false,
            )
        }
        else -> fail()
    }
