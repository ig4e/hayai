package exh.util

import android.content.Context
import eu.kanade.tachiyomi.source.model.MangasPage
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
                // Return empty page; URL importing in the Rx path is best-effort
                // The suspend version below is the primary path
                MangasPage(emptyList(), false)
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
                    // The old Manga interface extends SManga, so it can be used directly
                    listOf(res.manga)
                } else {
                    emptyList()
                },
                false,
            )
        }
        else -> fail()
    }
