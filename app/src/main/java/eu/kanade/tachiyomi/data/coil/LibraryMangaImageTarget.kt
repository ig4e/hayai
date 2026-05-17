package eu.kanade.tachiyomi.data.coil

import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.palette.graphics.Palette
import coil3.Image
import coil3.target.ImageViewTarget
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.database.models.updateCoverLastModified
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.util.system.launchIO
import uy.kohesive.injekt.injectLazy

class LibraryMangaImageTarget(
    override val view: ImageView,
    private val libraryManga: Manga,
) : ImageViewTarget(view) {

    private val coverCache: CoverCache by injectLazy()

    override fun onError(error: Image?) {
        super.onError(error)
        if (libraryManga.favorite) {
            launchIO {
                val file = coverCache.getCoverFile(libraryManga.thumbnail_url, false)
                // if the file exists and the there was still an error then the file is corrupted
                if (file != null && file.exists()) {
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeFile(file.path, options)
                    if (options.outWidth == -1 || options.outHeight == -1) {
                        libraryManga.updateCoverLastModified()
                        file.delete()
                    }
                }
            }
        }
    }
}

fun Palette.getBestColor(defaultColor: Int) = getBestColor() ?: defaultColor

fun Palette.getBestColor(): Int? {
    // Prefer the most expressive swatch. The previous algorithm picked the dominant swatch
    // whenever its saturation was ≥0.25 — but on many covers the dominant is a background
    // plate (sky, paper, large flat shading) that's slightly tinted, while the cover's actual
    // accent lives in the vibrant swatch. Pick dominant only when it clearly dwarfs vibrant
    // AND is well-saturated; otherwise trust vibrant.
    val vibrant = vibrantSwatch
    val muted = mutedSwatch
    val dominant = dominantSwatch
    val vibPop = vibrant?.population ?: -1
    val mutedPop = muted?.population ?: -1
    val domPop = dominant?.population ?: -1
    val domSat = dominant?.hsl?.get(1) ?: 0f
    val domLum = dominant?.hsl?.get(2) ?: -1f
    val mutedSaturationLimit = if (mutedPop > vibPop * 3f) 0.1f else 0.25f
    return when {
        // Dominant only wins when well-saturated, in a usable luminance band, and clearly
        // out-populates vibrant. Without the population guard, faintly-tinted backgrounds
        // beat strong character accents.
        domSat >= 0.35f && domLum in 0.2f..0.8f && domPop > vibPop * 2.5f -> dominant?.rgb
        vibPop >= mutedPop * 0.75f -> vibrant?.rgb
        mutedPop > vibPop * 1.5f &&
            (muted?.hsl?.get(1) ?: 0f) > mutedSaturationLimit -> muted?.rgb
        else -> arrayListOf(vibrant, lightVibrantSwatch, darkVibrantSwatch).maxByOrNull {
            if (it === vibrant) (it?.population ?: -1) * 3 else it?.population ?: -1
        }?.rgb
    }
}
