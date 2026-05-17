package yokai.util.coil

import android.content.Context
import coil3.ImageLoader
import coil3.imageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size
import okhttp3.OkHttpClient

/**
 * Centralised helpers for getting a Coil [ImageLoader] in app code.
 *
 * Why this file exists: the app singleton loader is built in
 * `App.newImageLoader` with a carefully tuned component stack — OkHttp
 * fetcher, JXL/AVIF decoder, GIF decoder, BufferedSource fetcher,
 * MangaCover fetcher chain, a sized memory cache, RGB565 on low-RAM
 * devices, and `Dispatchers.IO.limitedParallelism(8 / 3)` pools for
 * fetcher / decoder. Building a fresh `ImageLoader.Builder(context)` in
 * a screen throws all of that away and gets you a default Coil loader.
 *
 * Use [appImageLoader] for any screen that just needs to load an image.
 * Use [loaderForSource] when you specifically need to route the request
 * through a source's OkHttp client (cookies / source-specific headers)
 * — the resulting loader inherits everything else from the singleton.
 */

/**
 * The app's singleton [ImageLoader]. Equivalent to `context.imageLoader`;
 * exists as a wrapper so callsites have one obvious import and we can swap
 * the implementation later without touching every screen.
 */
fun appImageLoader(context: Context): ImageLoader = context.imageLoader

/**
 * A loader that fetches with [sourceClient] (e.g. for an E/Ex/NHentai page
 * preview that needs the source's auth cookies) but shares the singleton's
 * memory cache, disk cache, decoder stack, and dispatcher pools.
 *
 * Internally uses `singleton.newBuilder()` which preserves all of the
 * singleton's configuration; we add an additional [OkHttpNetworkFetcherFactory]
 * for [sourceClient]. Coil tries factories in reverse-registration order,
 * so the source-specific factory wins for requests it can handle.
 *
 * Cheap to call; safe inside `remember(sourceClient) { ... }`. Do NOT build
 * `ImageLoader.Builder(context)` from scratch in screen code — use this.
 */
fun loaderForSource(context: Context, sourceClient: OkHttpClient): ImageLoader {
    val clientLazy = lazy { sourceClient }
    return context.imageLoader.newBuilder()
        .components {
            add(OkHttpNetworkFetcherFactory(clientLazy::value))
        }
        .build()
}

/**
 * Request defaults for source page-preview thumbnails (~108–120dp cells).
 *
 * - [maxBitmapSize] caps decode RAM so `ContentScale.FillWidth` doesn't
 *   blow a 4k page image into RAM for a 350px-wide cell.
 * - [Precision.INEXACT] lets Coil reuse a slightly-different-size cached
 *   bitmap instead of forcing a re-decode at exact target size.
 * - [crossfade] off matches the singleton (per [eu.kanade.tachiyomi.App]);
 *   restating it here keeps previews aligned even if a future per-request
 *   builder accidentally turns it on.
 */
fun ImageRequest.Builder.hayaiPagePreviewDefaults(): ImageRequest.Builder = this
    .precision(Precision.INEXACT)
    .maxBitmapSize(Size(PAGE_PREVIEW_MAX_BITMAP, PAGE_PREVIEW_MAX_BITMAP))
    .crossfade(false)

private const val PAGE_PREVIEW_MAX_BITMAP = 1024
