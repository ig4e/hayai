package exh.util

import android.content.Context
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import rx.Observable

// TODO: Integrate GalleryAdder for full URL import support

/**
 * A version of fetchSearchManga that supports URL importing
 */
fun UrlImportableSource.urlImportFetchSearchManga(
    context: Context,
    query: String,
    fail: () -> Observable<MangasPage>,
): Observable<MangasPage> = fail()

/**
 * A version of fetchSearchManga that supports URL importing
 */
suspend fun UrlImportableSource.urlImportFetchSearchMangaSuspend(
    context: Context,
    query: String,
    fail: suspend () -> MangasPage,
): MangasPage = fail()
