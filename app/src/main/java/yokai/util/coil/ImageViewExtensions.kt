package yokai.util.coil

import android.view.View
import android.widget.ImageView
import coil3.ImageLoader
import coil3.imageLoader
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.maxBitmapSize
import coil3.size.Precision
import coil3.size.Size
import coil3.target.ImageViewTarget
import eu.kanade.tachiyomi.data.coil.CoverViewTarget
import eu.kanade.tachiyomi.data.coil.LibraryMangaImageTarget
import eu.kanade.tachiyomi.domain.manga.models.Manga
import yokai.domain.manga.models.MangaCover
import yokai.domain.manga.models.cover

private const val MAX_BITMAP_SIZE = 2048

fun ImageView.loadManga(
    manga: Manga,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {},
): Disposable {
    val request = ImageRequest.Builder(context)
        .data(manga.cover())
        .target(LibraryMangaImageTarget(this, manga))
        .precision(Precision.INEXACT)
        // Default ViewSizeResolver decodes at view bounds; callers needing original opt in via builder.
        .maxBitmapSize(Size(MAX_BITMAP_SIZE, MAX_BITMAP_SIZE))
        .apply(builder)
        .build()
    return imageLoader.enqueue(request)
}

fun ImageView.asTarget() = ImageViewTarget(this)

fun ImageView.loadManga(
    cover: MangaCover,
    progress: View? = null,
    target: ImageViewTarget? = null,
    imageLoader: ImageLoader = context.imageLoader,
    builder: ImageRequest.Builder.() -> Unit = {},
): Disposable {
    val request = ImageRequest.Builder(context)
        .data(cover)
        .target(target ?: CoverViewTarget(this, progress))
        .precision(Precision.INEXACT)
        .maxBitmapSize(Size(MAX_BITMAP_SIZE, MAX_BITMAP_SIZE))
        .apply(builder)
        .build()
    return imageLoader.enqueue(request)
}
