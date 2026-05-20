package eu.kanade.tachiyomi.ui.more.stats

import yokai.util.koin.get
import eu.kanade.tachiyomi.data.database.models.LibraryManga
import eu.kanade.tachiyomi.data.database.models.Track
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.data.preference.MANGA_HAS_UNREAD
import eu.kanade.tachiyomi.data.preference.MANGA_NON_COMPLETED
import eu.kanade.tachiyomi.data.preference.MANGA_NON_READ
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.data.track.TrackService
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.ui.more.stats.StatsHelper.getReadDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yokai.util.koin.injectLazy
import yokai.data.DatabaseHandler
import yokai.domain.manga.interactor.GetLibraryManga
import yokai.domain.track.interactor.GetTrack
import yokai.i18n.MR
import yokai.util.lang.getString

/**
 * Presenter of [StatsController].
 */
class StatsPresenter(
    private val prefs: PreferencesHelper = get(),
    private val trackManager: TrackManager = get(),
    private val downloadManager: DownloadManager = get(),
    private val sourceManager: SourceManager = get(),
): BaseCoroutinePresenter<StatsController>() {
    private val handler: DatabaseHandler by injectLazy()
    private val getLibraryManga: GetLibraryManga by injectLazy()
    private val getTrack: GetTrack by injectLazy()

    // Empty until load() resolves. The controller awaits load() in viewScope before reading
    // any of these so the screen mounts immediately rather than blocking on a synchronous
    // library + per-manga track scan at construction.
    private var libraryMangas: List<LibraryManga> = emptyList()
    var mangaDistinct: List<LibraryManga> = emptyList()
        private set

    private var tracksByManga: Map<Long, List<Track>> = emptyMap()
    private var totalReadDurationMs: Long? = null

    /**
     * Loads the library snapshot, per-manga tracks, and total read duration on IO.
     * Must be awaited before [getTracks], [getReadDuration], [mangaDistinct], or
     * [getGlobalUpdateManga] return meaningful values.
     */
    suspend fun load() {
        libraryMangas = withContext(Dispatchers.IO) { getLibraryManga.await() }
        mangaDistinct = libraryMangas.distinct()
        tracksByManga = withContext(Dispatchers.IO) {
            mangaDistinct.associate { lm ->
                val id = lm.manga.id!!
                id to getTrack.awaitAllByMangaId(id)
            }
        }
        totalReadDurationMs = withContext(Dispatchers.IO) {
            handler.awaitOneOrNull { historyQueries.getTotalReadDuration() }?.sum?.toLong()
        }
    }

    fun getTracks(manga: Manga): List<Track> = tracksByManga[manga.id] ?: emptyList()

    fun getLoggedTrackers(): List<TrackService> {
        return trackManager.services.filter { it.isLogged }
    }

    fun getSources(): List<CatalogueSource> {
        val languages = prefs.enabledLanguages().get()
        val hiddenCatalogues = prefs.hiddenSources().get()
        return sourceManager.getCatalogueSources()
            .filter { it.lang in languages }
            .filterNot { it.id.toString() in hiddenCatalogues }
    }

    fun getGlobalUpdateManga(): Map<Long?, List<LibraryManga>> {
        val includedCategories = prefs.libraryUpdateCategories().get().map(String::toInt)
        val excludedCategories = prefs.libraryUpdateCategoriesExclude().get().map(String::toInt)
        val restrictions = prefs.libraryUpdateMangaRestriction().get()
        return libraryMangas.groupBy { it.manga.id }
            .filterNot { it.value.any { manga -> manga.category in excludedCategories } }
            .filter { includedCategories.isEmpty() || it.value.any { manga -> manga.category in includedCategories } }
            .filterNot {
                val manga = it.value.first()
                (MANGA_NON_COMPLETED in restrictions && manga.manga.status == SManga.COMPLETED) ||
                    (MANGA_HAS_UNREAD in restrictions && manga.unread != 0) ||
                    (MANGA_NON_READ in restrictions && manga.totalChapters > 0 && !manga.hasRead)
            }
    }

    fun getDownloadCount(manga: LibraryManga): Int {
        return downloadManager.getDownloadCount(manga.manga)
    }

    fun get10PointScore(track: Track): Float? {
        val service = trackManager.getService(track.sync_id)
        return service?.get10PointScore(track.score)
    }

    fun getReadDuration(): String =
        totalReadDurationMs.getReadDuration(prefs.context.getString(MR.strings.none))
}
