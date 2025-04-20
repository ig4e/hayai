package eu.kanade.tachiyomi.ui.browse.migration.advanced.process

import android.content.Context
import android.widget.Toast
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.domain.chapter.interactor.SyncChaptersWithSource
import eu.kanade.domain.manga.interactor.UpdateManga
import eu.kanade.domain.manga.model.hasCustomCover
import eu.kanade.domain.manga.model.toSManga
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.data.download.DownloadManager
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.getNameForMangaInfo
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.ui.browse.migration.MigrationFlags
import eu.kanade.tachiyomi.ui.browse.migration.advanced.design.MigrationType
import eu.kanade.tachiyomi.ui.browse.migration.advanced.process.MigratingManga.SearchResult
import eu.kanade.tachiyomi.util.system.toast
import exh.smartsearch.SmartSourceSearchEngine
import exh.source.MERGED_SOURCE_ID
import exh.util.ThrottleManager
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.UnsortedPreferences
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.SetMangaCategories
import tachiyomi.domain.chapter.interactor.GetChaptersByMangaId
import tachiyomi.domain.chapter.interactor.UpdateChapter
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.model.ChapterUpdate
import tachiyomi.domain.history.interactor.GetHistoryByMangaId
import tachiyomi.domain.history.interactor.UpsertHistory
import tachiyomi.domain.history.model.HistoryUpdate
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.GetMergedReferencesById
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.DeleteTrack
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.interactor.InsertTrack
import tachiyomi.i18n.sy.SYMR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class MigrationListScreenModel(
    private val config: MigrationProcedureConfig,
    private val preferences: UnsortedPreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val downloadManager: DownloadManager = Injekt.get(),
    private val coverCache: CoverCache = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val updateManga: UpdateManga = Injekt.get(),
    private val syncChaptersWithSource: SyncChaptersWithSource = Injekt.get(),
    private val updateChapter: UpdateChapter = Injekt.get(),
    private val getChaptersByMangaId: GetChaptersByMangaId = Injekt.get(),
    private val getMergedReferencesById: GetMergedReferencesById = Injekt.get(),
    private val getHistoryByMangaId: GetHistoryByMangaId = Injekt.get(),
    private val upsertHistory: UpsertHistory = Injekt.get(),
    private val getCategories: GetCategories = Injekt.get(),
    private val setMangaCategories: SetMangaCategories = Injekt.get(),
    private val getTracks: GetTracks = Injekt.get(),
    private val insertTrack: InsertTrack = Injekt.get(),
    private val deleteTrack: DeleteTrack = Injekt.get(),
) : ScreenModel {

    private val smartSearchEngine = SmartSourceSearchEngine(config.extraSearchParams)
    private val throttleManager = ThrottleManager()

    // Add caching of already searched manga titles to avoid duplicate searches
    private val searchedMangaCache = ConcurrentHashMap<String, Manga?>()

    // Use a more permissive semaphore to allow more concurrent operations
    private val migrationSemaphore = Semaphore(8) // Increased from 5

    val migratingItems = MutableStateFlow<ImmutableList<MigratingManga>?>(null)
    val migrationDone = MutableStateFlow(false)
    val unfinishedCount = MutableStateFlow(0)

    val manualMigrations = MutableStateFlow(0)

    val hideNotFound = preferences.hideNotFoundMigration().get()
    val showOnlyUpdates = preferences.showOnlyUpdatesMigration().get()

    val navigateOut = MutableSharedFlow<Unit>()

    val dialog = MutableStateFlow<Dialog?>(null)

    val migratingProgress = MutableStateFlow(Float.MAX_VALUE)

    private var migrateJob: Job? = null

    init {
        screenModelScope.launchIO {
            val mangaIds = when (val migration = config.migration) {
                is MigrationType.MangaList -> {
                    migration.mangaIds
                }
                is MigrationType.MangaSingle -> listOf(migration.fromMangaId)
            }
            runMigrations(
                mangaIds
                    .map { mangaId ->
                        coroutineScope {
                            async {
                                val manga = getManga.await(mangaId) ?: return@async null
                                MigratingManga(
                                    manga = manga,
                                    chapterInfo = getChapterInfo(mangaId),
                                    sourcesString = sourceManager.getOrStub(manga.source).getNameForMangaInfo(
                                        if (manga.source == MERGED_SOURCE_ID) {
                                            getMergedReferencesById.await(manga.id)
                                                .map { sourceManager.getOrStub(it.mangaSourceId) }
                                        } else {
                                            null
                                        },
                                    ),
                                    parentContext = screenModelScope.coroutineContext,
                                )
                            }
                        }
                    }
                    .awaitAll()
                    .filterNotNull()
                    .also {
                        migratingItems.value = it.toImmutableList()
                    },
            )
        }
    }

    suspend fun getManga(result: SearchResult.Result) = getManga(result.id)
    suspend fun getManga(id: Long) = getManga.await(id)
    suspend fun getChapterInfo(result: SearchResult.Result) = getChapterInfo(result.id)
    suspend fun getChapterInfo(id: Long) = getChaptersByMangaId.await(id).let { chapters ->
        MigratingManga.ChapterInfo(
            latestChapter = chapters.maxOfOrNull { it.chapterNumber },
            chapterCount = chapters.size,
        )
    }
    fun getSourceName(manga: Manga) = sourceManager.getOrStub(manga.source).getNameForMangaInfo(null)

    fun getMigrationSources() = preferences.migrationSources().get().split("/").mapNotNull {
        val value = it.toLongOrNull() ?: return@mapNotNull null
        sourceManager.get(value) as? CatalogueSource
    }

    private suspend fun runMigrations(mangas: List<MigratingManga>) {
        throttleManager.resetThrottle()
        unfinishedCount.value = mangas.size
        val useSourceWithMost = preferences.useSourceWithMost().get()
        val useSmartSearch = preferences.smartMigration().get()

        // Pre-load all sources in parallel to save time during search
        val sources = getMigrationSources()

        // Create a batch processing system for faster migrations
        val mangaBatches = mangas.chunked(5)

        for (mangaBatch in mangaBatches) {
            if (!currentCoroutineContext().isActive) {
                break
            }

            val batchJobs = mangaBatch.map { manga ->
                coroutineScope {
                    async {
                        // Skip if already processed or removed
                        when (val migration = config.migration) {
                            is MigrationType.MangaList -> if (manga.manga.id !in migration.mangaIds) {
                                return@async
                            }
                            else -> Unit
                        }

                        if (manga.searchResult.value == SearchResult.Searching && manga.migrationScope.isActive) {
                            val mangaObj = manga.manga
                            val mangaSource = sourceManager.getOrStub(mangaObj.source)

                            try {
                                val result = manga.migrationScope.async<Manga?> {
                                    val validSources = if (sources.size == 1) {
                                        sources
                                    } else {
                                        sources.filter { it.id != mangaSource.id }
                                    }
                                    when (val migration = config.migration) {
                                        is MigrationType.MangaSingle -> if (migration.toManga != null) {
                                            val localManga = getManga.await(migration.toManga)
                                            if (localManga != null) {
                                                val source = sourceManager.get(localManga.source) as? CatalogueSource
                                                if (source != null) {
                                                    val chapters = if (source is EHentai) {
                                                        source.getChapterList(localManga.toSManga(), throttleManager::throttle)
                                                    } else {
                                                        source.getChapterList(localManga.toSManga())
                                                    }
                                                    try {
                                                        syncChaptersWithSource.await(chapters, localManga, source)
                                                    } catch (_: Exception) {
                                                    }
                                                    manga.progress.value = validSources.size to validSources.size
                                                    return@async localManga
                                                }
                                            }
                                        }
                                        else -> Unit
                                    }
                                    if (useSourceWithMost) {
                                        // Check cache first to avoid redundant searches
                                        val cacheKey = "${mangaObj.ogTitle}|${mangaObj.source}"
                                        val cached = searchedMangaCache[cacheKey]
                                        if (cached != null) {
                                            return@async cached
                                        }

                                        val processedSources = AtomicInteger()

                                        // Process sources in parallel with limited concurrency
                                        val searchResults = validSources.map { source ->
                                            coroutineScope {
                                                async<Pair<Manga, Int>?> {
                                                    migrationSemaphore.withPermit {
                                                        try {
                                                            // Use cached search results when possible
                                                            val searchCacheKey = "${mangaObj.ogTitle}|${source.id}"
                                                            val cachedSearch = searchedMangaCache[searchCacheKey]

                                                            val searchResult = cachedSearch ?: if (useSmartSearch) {
                                                                smartSearchEngine.smartSearch(source, mangaObj.ogTitle)
                                                            } else {
                                                                smartSearchEngine.normalSearch(source, mangaObj.ogTitle)
                                                            }

                                                            // Cache the search result
                                                            if (cachedSearch == null && searchResult != null) {
                                                                searchedMangaCache[searchCacheKey] = searchResult
                                                            }

                                                            if (searchResult != null &&
                                                                !(searchResult.url == mangaObj.url && source.id == mangaObj.source)
                                                            ) {
                                                                val localManga = networkToLocalManga.await(searchResult)

                                                                var chapterCount = 0
                                                                try {
                                                                    val chapters = if (source is EHentai) {
                                                                        source.getChapterList(localManga.toSManga(), throttleManager::throttle)
                                                                    } else {
                                                                        source.getChapterList(localManga.toSManga())
                                                                    }
                                                                    chapterCount = chapters.size

                                                                    // Only sync chapters if we found a good match
                                                                    if (chapterCount > 0) {
                                                                        syncChaptersWithSource.await(chapters, localManga, source)
                                                                    }
                                                                } catch (e: Exception) {
                                                                    if (e is CancellationException) throw e
                                                                    return@withPermit null
                                                                }

                                                                manga.progress.value = validSources.size to processedSources.incrementAndGet()
                                                                localManga to chapterCount
                                                            } else {
                                                                manga.progress.value = validSources.size to processedSources.incrementAndGet()
                                                                null
                                                            }
                                                        } catch (e: CancellationException) {
                                                            // Ignore cancellations
                                                            throw e
                                                        } catch (e: Exception) {
                                                            manga.progress.value = validSources.size to processedSources.incrementAndGet()
                                                            null
                                                        }
                                                    }
                                                }
                                            }
                                        }.mapNotNull { it.await() }

                                        // Clean up cache periodically
                                        if (searchedMangaCache.size > 1000) {
                                            searchedMangaCache.clear()
                                        }

                                        // Reset the smart search engine caches periodically
                                        smartSearchEngine.clearCaches()

                                        val maxChaptersManga = searchResults.maxByOrNull { it.second }
                                        if (maxChaptersManga != null) {
                                            // Cache this result for future use
                                            searchedMangaCache[cacheKey] = maxChaptersManga.first
                                            return@async maxChaptersManga.first
                                        }

                                        // Cache a null result to avoid researching
                                        searchedMangaCache[cacheKey] = null
                                        null
                                    } else {
                                        null
                                    }
                                }.await()

                                when {
                                    result != null -> {
                                        try {
                                            if (result.thumbnailUrl.isNullOrEmpty()) {
                                                val newManga = sourceManager.getOrStub(result.source).getMangaDetails(result.toSManga())
                                                updateManga.awaitUpdateFromSource(result, newManga, true)
                                            }
                                        } catch (e: CancellationException) {
                                            // Ignore cancellations
                                            throw e
                                        } catch (e: Exception) {
                                            // Ignore other exceptions
                                        }

                                        manga.searchResult.value = SearchResult.Result(result.id)
                                    }
                                    else -> manga.searchResult.value = SearchResult.NotFound
                                }
                            } catch (e: CancellationException) {
                                // Ignore cancellations
                                throw e
                            } catch (e: Exception) {
                                logcat(LogPriority.ERROR, e) { "Error migrating manga" }
                                manga.searchResult.value = SearchResult.NotFound
                            }
                        }

                        // Update progress
                        sourceFinished()
                    }
                }
            }

            // Wait for the current batch to complete before moving to the next
            batchJobs.awaitAll()
        }

        // Perform final updates
        migrationDone.value = true
    }

    private suspend fun sourceFinished() {
        unfinishedCount.value = migratingItems.value.orEmpty().count {
            it.searchResult.value == SearchResult.Searching
        }

        // Update manual migration counter properly for UI
        manualMigrations.value = migratingItems.value.orEmpty().count {
            it.searchResult.value is SearchResult.Result
        }

        if (allMangasDone()) {
            migrationDone.value = true
        }
        if (migratingItems.value?.isEmpty() == true) {
            navigateOut()
        }
    }

    fun allMangasDone() = migratingItems.value.orEmpty().all { it.searchResult.value != SearchResult.Searching } &&
        migratingItems.value.orEmpty().any { it.searchResult.value is SearchResult.Result }

    fun mangasSkipped() = migratingItems.value.orEmpty().count { it.searchResult.value == SearchResult.NotFound }

    private suspend fun migrateMangaInternal(
        prevManga: Manga,
        manga: Manga,
        replace: Boolean,
    ) {
        if (prevManga.id == manga.id) return // Nothing to migrate

        val flags = preferences.migrateFlags().get()
        try {
            // Copy categories
            if (MigrationFlags.hasCategories(flags)) {
                val categoryIds = getCategories.await(prevManga.id).map { it.id }
                setMangaCategories.await(manga.id, categoryIds)
            }

            // Update chapters
            if (replace && MigrationFlags.hasChapters(flags)) {
                // Process all chapters
                val prevMangaChapters = getChaptersByMangaId.await(prevManga.id)

                // Update chapters read status
                if (prevMangaChapters.isNotEmpty()) {
                    val sourceChapters = sourceManager.getOrStub(manga.source).getChapterList(manga.toSManga())
                    val dbChapters = getChaptersByMangaId.await(manga.id)
                    val sourceChaptersMap = sourceChapters.associateBy { it.url }

                    // Use maps for faster lookups
                    val prevMangaChaptersMap = prevMangaChapters.associateBy { it.url }
                    val prevMangaChaptersByName = prevMangaChapters.groupBy { it.name }
                    val prevMangaChaptersByNumber = prevMangaChapters.groupBy { it.chapterNumber }

                    // Create batch update for chapters
                    val toUpdate = mutableListOf<ChapterUpdate>()

                    // Process all chapters
                    withContext(Dispatchers.Default) {
                        val processed = dbChapters.map { chapter ->
                            async<ChapterUpdate?> {
                                // Try to find the chapter using different matching strategies
                                val prevChapter = sourceChaptersMap[chapter.url]?.let { sourceChapter ->
                                    prevMangaChaptersMap[sourceChapter.url]
                                } ?: prevMangaChaptersByName[chapter.name]?.firstOrNull()
                                ?: prevMangaChaptersByNumber[chapter.chapterNumber]?.firstOrNull()

                                if (prevChapter != null && prevChapter.read) {
                                    // Copy read status and bookmark status
                                    ChapterUpdate(
                                        id = chapter.id,
                                        read = true,
                                        bookmark = prevChapter.bookmark,
                                        lastPageRead = prevChapter.lastPageRead,
                                    )
                                } else null
                            }
                        }.mapNotNull { it.await() }

                        toUpdate.addAll(processed)
                    }

                    // Perform update in a single batch operation
                    if (toUpdate.isNotEmpty()) {
                        updateChapter.awaitAll(toUpdate)
                    }

                    // Update history
                    val history = getHistoryByMangaId.await(prevManga.id).mapNotNull { history ->
                        val chapter = prevMangaChapters.find { it.id == history.chapterId }
                        val dbChapter = dbChapters.find { dbChapter ->
                            val prevChapter = prevMangaChaptersMap[dbChapter.url]
                            ?: prevMangaChaptersByName[dbChapter.name]?.firstOrNull()
                            ?: prevMangaChaptersByNumber[dbChapter.chapterNumber]?.firstOrNull()

                            prevChapter?.id == chapter?.id
                        }

                        if (dbChapter != null && history.readAt != null) {
                            HistoryUpdate(
                                chapterId = dbChapter.id,
                                readAt = history.readAt!!,
                                sessionReadDuration = history.readDuration
                            )
                        } else null
                    }

                    if (history.isNotEmpty()) {
                        upsertHistory.awaitAll(history)
                    }
                }
            }

            // Copy tracking data
            if (MigrationFlags.hasTracks(flags)) {
                val tracks = getTracks.await(prevManga.id)
                for (track in tracks) {
                    insertTrack.await(
                        track.copy(
                            id = 0,
                            mangaId = manga.id,
                        ),
                    )
                }
            }

            // Update custom cover
            if (MigrationFlags.hasCustomCover(flags) && prevManga.hasCustomCover(coverCache)) {
                coverCache.setCustomCoverToCache(manga, coverCache.getCustomCoverFile(prevManga.id).inputStream())
            }

            // SY --> Ensure the new manga is marked as favorite and has a valid dateAdded
            updateManga.await(
                MangaUpdate(
                    id = manga.id,
                    favorite = true,
                    // Optionally inherit dateAdded or set to current time
                    // dateAdded = prevManga.dateAdded // Inherit from previous manga
                    dateAdded = Date().time, // Set to current time
                )
            )
            // SY <--

            // Finally, delete the manga if needed
            if (replace) {
                // Remove covers in cache
                if (prevManga.hasCustomCover()) {
                    coverCache.deleteCustomCover(prevManga.id)
                }
                coverCache.deleteFromCache(prevManga)

                // Delete downloaded chapters
                downloadManager.deleteManga(prevManga, sourceManager.getOrStub(prevManga.source))

                // Delete manga
                updateManga.await(MangaUpdate(id = prevManga.id, favorite = false, dateAdded = 0))
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e)
        }
    }

    fun useMangaForMigration(context: Context, newMangaId: Long, selectedMangaId: Long) {
        val migratingManga = migratingItems.value.orEmpty().find { it.manga.id == selectedMangaId }
            ?: return
        migratingManga.searchResult.value = SearchResult.Searching
        screenModelScope.launchIO {
            val result = migratingManga.migrationScope.async<Manga?> {
                val manga = getManga.await(newMangaId) ?: return@async null
                val localManga = networkToLocalManga.await(manga)
                try {
                    val source = sourceManager.get(manga.source)!!
                    val chapters = source.getChapterList(localManga.toSManga())
                    syncChaptersWithSource.await(chapters, localManga, source)
                } catch (e: Exception) {
                    return@async null
                }
                localManga
            }.await()

            if (result != null) {
                try {
                    val newManga = sourceManager.getOrStub(result.source).getMangaDetails(result.toSManga())
                    updateManga.awaitUpdateFromSource(result, newManga, true)
                } catch (e: CancellationException) {
                    // Ignore cancellations
                    throw e
                } catch (e: Exception) {
                }

                migratingManga.searchResult.value = SearchResult.Result(result.id)
            } else {
                migratingManga.searchResult.value = SearchResult.NotFound
                withUIContext {
                    context.toast(SYMR.strings.no_chapters_found_for_migration, Toast.LENGTH_LONG)
                }
            }
        }
    }

    fun migrateMangas() {
        migrateMangas(true)
    }

    fun copyMangas() {
        migrateMangas(false)
    }

    private fun migrateMangas(replace: Boolean) {
        dialog.value = null
        migrateJob = screenModelScope.launchIO {
            migratingProgress.value = 0f
            val items = migratingItems.value.orEmpty()
            try {
                items.forEachIndexed { index, manga ->
                    try {
                        ensureActive()
                        val toMangaObj = manga.searchResult.value.let {
                            if (it is SearchResult.Result) {
                                getManga.await(it.id)
                            } else {
                                null
                            }
                        }
                        if (toMangaObj != null) {
                            migrateMangaInternal(
                                manga.manga,
                                toMangaObj,
                                replace,
                            )
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        logcat(LogPriority.WARN, throwable = e)
                    }
                    migratingProgress.value = index.toFloat() / items.size
                }

                navigateOut()
            } finally {
                migratingProgress.value = Float.MAX_VALUE
                migrateJob = null
            }
        }
    }

    fun cancelMigrate() {
        migrateJob?.cancel()
        migrateJob = null
    }

    private suspend fun navigateOut() {
        navigateOut.emit(Unit)
    }

    fun migrateManga(mangaId: Long, copy: Boolean) {
        screenModelScope.launchIO {
            val manga = migratingItems.value.orEmpty().find { it.manga.id == mangaId }
                ?: return@launchIO

            val toMangaObj = getManga.await((manga.searchResult.value as? SearchResult.Result)?.id ?: return@launchIO)
                ?: return@launchIO
            migrateMangaInternal(
                manga.manga,
                toMangaObj,
                !copy,
            )

            removeManga(mangaId)
        }
    }

    fun removeManga(mangaId: Long) {
        screenModelScope.launchIO {
            val item = migratingItems.value.orEmpty().find { it.manga.id == mangaId }
                ?: return@launchIO
            removeManga(item)
            item.migrationScope.cancel()
            sourceFinished()
        }
    }

    fun removeManga(item: MigratingManga) {
        when (val migration = config.migration) {
            is MigrationType.MangaList -> {
                val ids = migration.mangaIds.toMutableList()
                val index = ids.indexOf(item.manga.id)
                if (index > -1) {
                    ids.removeAt(index)
                    config.migration = MigrationType.MangaList(ids)
                    val index2 = migratingItems.value.orEmpty().indexOf(item)
                    if (index2 > -1) migratingItems.value = (migratingItems.value.orEmpty() - item).toImmutableList()
                }
            }
            is MigrationType.MangaSingle -> Unit
        }
    }

    override fun onDispose() {
        super.onDispose()
        migratingItems.value.orEmpty().forEach {
            it.migrationScope.cancel()
        }
    }

    fun openMigrateDialog(
        copy: Boolean,
    ) {
        dialog.value = Dialog.MigrateMangaDialog(
            copy,
            migratingItems.value.orEmpty().size,
            mangasSkipped(),
        )
    }

    sealed class Dialog {
        data class MigrateMangaDialog(val copy: Boolean, val mangaSet: Int, val mangaSkipped: Int) : Dialog()
        object MigrationExitDialog : Dialog()
    }
}
