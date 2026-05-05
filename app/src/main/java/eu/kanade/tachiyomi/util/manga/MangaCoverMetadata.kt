package eu.kanade.tachiyomi.util.manga

import android.graphics.BitmapFactory
import androidx.annotation.ColorInt
import androidx.palette.graphics.Palette
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.coil.getBestColor
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.domain.manga.models.Manga
import java.util.concurrent.ConcurrentHashMap
import uy.kohesive.injekt.injectLazy

/** Object that holds info about a covers size ratio + dominant colors */
object MangaCoverMetadata {
    private val coverRatioMap = ConcurrentHashMap<Long, Float>()
    private val coverColorMap = ConcurrentHashMap<Long, Pair<Int, Int>>()
    private val vibrantCoverColorMap = ConcurrentHashMap<Long, Int>()
    private val preferences by injectLazy<PreferencesHelper>()
    private val coverCache by injectLazy<CoverCache>()

    /**
     * True once [load] has finished hydrating the in-memory maps from prefs. [savePrefs]
     * must short-circuit until this flips to avoid persisting an empty/partial map and
     * wiping the user's saved cover data — relevant because [load] runs on Dispatchers.IO
     * (App.onCreate dispatches it via `scope.launchIO`) and MainActivity.onPause can fire
     * `savePrefs` before the load coroutine finishes on slow devices.
     */
    @Volatile
    private var loaded = false

    fun load() {
        val ratios = preferences.coverRatios().get()
        ratios.forEach { entry ->
            val splits = entry.split("|")
            val id = splits.firstOrNull()?.toLongOrNull()
            val ratio = splits.lastOrNull()?.toFloatOrNull()
            if (id != null && ratio != null) {
                // Merge into the existing map rather than replacing it. setRatioAndColors
                // may have populated entries between app start and this IO load completing,
                // so on-disk values must NOT clobber fresher in-memory values.
                coverRatioMap.putIfAbsent(id, ratio)
            }
        }
        val colors = preferences.coverColors().get()
        colors.forEach { entry ->
            val splits = entry.split("|")
            val id = splits.firstOrNull()?.toLongOrNull()
            val color = splits.getOrNull(1)?.toIntOrNull()
            val textColor = splits.getOrNull(2)?.toIntOrNull()
            if (id != null && color != null) {
                coverColorMap.putIfAbsent(id, color to (textColor ?: 0))
            }
        }
        loaded = true
    }

    fun setRatioAndColors(mangaId: Long?, mangaThumbnailUrl: String?, isInLibrary: Boolean, ogFile: UniFile? = null, force: Boolean = false) {
        if (!isInLibrary) {
            remove(mangaId)
        }
        if (getVibrantColor(mangaId) != null && !isInLibrary) return
        val file = ogFile
            ?: UniFile.fromFile(coverCache.getCustomCoverFile(mangaId))?.takeIf { it.exists() }
            ?: UniFile.fromFile(coverCache.getCoverFile(mangaThumbnailUrl, !isInLibrary))
        // if the file exists and the there was still an error then the file is corrupted
        if (file?.exists() == true) {
            val options = BitmapFactory.Options()
            val hasVibrantColor = if (isInLibrary) vibrantCoverColorMap[mangaId] != null else true
            if (getColors(mangaId) != null && hasVibrantColor && !force) {
                options.inJustDecodeBounds = true
            } else {
                options.inSampleSize = 4
            }
            val bitmap = try {
                val stream = file.openInputStream()
                BitmapFactory.decodeStream(stream, null, options)
            } catch (_: Throwable) {
                null
            }
            if (bitmap != null) {
                Palette.from(bitmap).generate { palette ->
                    if (isInLibrary) {
                        palette?.dominantSwatch?.let { swatch ->
                            addCoverColor(mangaId, swatch.rgb, swatch.titleTextColor)
                        }
                    }
                    val color = palette?.getBestColor() ?: return@generate
                    setVibrantColor(mangaId, color)
                }
            }
            if (isInLibrary && !(options.outWidth == -1 || options.outHeight == -1)) {
                addCoverRatio(mangaId, options.outWidth / options.outHeight.toFloat())
            }
        }
    }

    fun remove(manga: Manga) {
        remove(manga.id)
    }

    fun remove(mangaId: Long?) {
        mangaId ?: return
        coverRatioMap.remove(mangaId)
        coverColorMap.remove(mangaId)
    }

    fun addCoverRatio(manga: Manga, ratio: Float) {
        addCoverRatio(manga.id, ratio)
    }

    fun addCoverRatio(mangaId: Long?, ratio: Float) {
        mangaId ?: return
        coverRatioMap[mangaId] = ratio
    }

    fun addCoverColor(manga: Manga, @ColorInt color: Int, @ColorInt textColor: Int) {
        addCoverColor(manga.id, color, textColor)
    }

    fun addCoverColor(mangaId: Long?, @ColorInt color: Int, @ColorInt textColor: Int) {
        mangaId ?: return
        coverColorMap[mangaId] = color to textColor
    }

    fun getColors(manga: Manga): Pair<Int, Int>? = getColors(manga.id)

    fun getColors(mangaId: Long?): Pair<Int, Int>? {
        return coverColorMap[mangaId]
    }

    fun getRatio(manga: Manga): Float? {
        return coverRatioMap[manga.id]
    }

    fun setVibrantColor(mangaId: Long?, @ColorInt color: Int?) {
        mangaId ?: return

        if (color == null) {
            vibrantCoverColorMap.remove(mangaId)
            return
        }

        vibrantCoverColorMap[mangaId] = color
    }

    fun getVibrantColor(mangaId: Long?): Int? {
        return vibrantCoverColorMap[mangaId]
    }

    fun savePrefs() {
        // No-op until load() has hydrated the in-memory maps. Otherwise the IO load racing
        // against onPause would let us serialize an empty / partially-populated map and
        // overwrite the user's persisted cover_ratios / cover_colors StringSets.
        if (!loaded) return
        val mapCopy = coverRatioMap.toMap()
        preferences.coverRatios().set(mapCopy.map { "${it.key}|${it.value}" }.toSet())
        val mapColorCopy = coverColorMap.toMap()
        preferences.coverColors().set(mapColorCopy.map { "${it.key}|${it.value.first}|${it.value.second}" }.toSet())
    }
}
