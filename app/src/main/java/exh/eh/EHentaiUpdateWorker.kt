package exh.eh

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkerParameters
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.data.database.models.Chapter
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.util.chapter.syncChaptersWithSource
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.workManager
import exh.debug.DebugToggles
import exh.eh.EHentaiUpdateWorkerConstants.UPDATES_PER_ITERATION
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.ExhPreferences
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import yokai.domain.chapter.interactor.GetChapter
import yokai.domain.manga.interactor.GetManga
import yokai.domain.manga.interactor.UpdateManga
import yokai.domain.manga.metadata.MangaMetadataRepository
import yokai.domain.manga.models.MangaUpdate
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

class EHentaiUpdateWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val exhPreferences: ExhPreferences by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val updateHelper: EHentaiUpdateHelper by injectLazy()
    private val logger = Logger.withTag("EHentaiUpdateWorker")

    private val getManga: GetManga by injectLazy()
    private val getChapter: GetChapter by injectLazy()
    private val updateManga: UpdateManga by injectLazy()
    private val metadataRepository: MangaMetadataRepository by injectLazy()

    private val updateNotifier by lazy { EHentaiUpdateNotifier(context) }

    override suspend fun doWork(): Result {
        return try {
            if (requiresWifiConnection(exhPreferences) && !context.isConnectedToWifi()) {
                Result.success() // retry again later
            } else {
                setForeground(getForegroundInfo())
                startUpdating()
                logger.d { "Update job completed!" }
                Result.success()
            }
        } catch (e: Exception) {
            Result.success() // retry again later
        } finally {
            updateNotifier.cancelProgressNotification()
        }
    }

    override suspend fun getForegroundInfo(): ForegroundInfo {
        return ForegroundInfo(
            EHentaiUpdateNotifier.NOTIFICATION_ID_PROGRESS,
            updateNotifier.progressNotificationBuilder.build(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
    }

    private suspend fun startUpdating() {
        logger.d { "Update job started!" }
        val startTime = System.currentTimeMillis()

        logger.d { "Finding manga with metadata..." }
        // Get all favorite EH/EXH manga
        val favorites = getManga.awaitFavorites()
        val metadataManga = favorites.filter { manga ->
            manga.source == EH_SOURCE_ID || manga.source == EXH_SOURCE_ID
        }

        logger.d { "Filtering manga and raising metadata..." }
        val curTime = System.currentTimeMillis()
        val allMeta = metadataManga.asFlow().mapNotNull { manga ->
            val mangaId = manga.id ?: return@mapNotNull null
            val searchMeta = metadataRepository.getMetadataById(mangaId) ?: return@mapNotNull null
            val tags = metadataRepository.getTagsById(mangaId)
            val titles = metadataRepository.getTitlesById(mangaId)
            val flatMeta = exh.metadata.metadata.base.FlatMetadata(searchMeta, tags, titles)

            val raisedMeta = try {
                flatMeta.raise<EHentaiSearchMetadata>()
            } catch (e: Exception) {
                return@mapNotNull null
            }

            // Don't update galleries too frequently
            if (raisedMeta.aged ||
                (
                    curTime - raisedMeta.lastUpdateCheck < MIN_BACKGROUND_UPDATE_FREQ &&
                        DebugToggles.RESTRICT_EXH_GALLERY_UPDATE_CHECK_FREQUENCY.enabled
                    )
            ) {
                return@mapNotNull null
            }

            val chapter = getChapter.awaitAll(mangaId, false).minByOrNull {
                it.date_upload
            }

            UpdateEntry(manga, raisedMeta, chapter)
        }.toList().sortedBy { it.meta.lastUpdateCheck }

        logger.d { "Found ${allMeta.size} manga to update, starting updates!" }
        val mangaMetaToUpdateThisIter = allMeta.take(UPDATES_PER_ITERATION)

        var failuresThisIteration = 0
        var updatedThisIteration = 0
        val modifiedThisIteration = mutableSetOf<Long>()

        try {
            for ((index, entry) in mangaMetaToUpdateThisIter.withIndex()) {
                val (manga, meta, _) = entry
                if (failuresThisIteration > MAX_UPDATE_FAILURES) {
                    logger.w { "Too many update failures, aborting..." }
                    break
                }

                logger.d {
                    "Updating gallery (index: $index, manga.id: ${manga.id}, " +
                        "meta.gId: ${meta.gId}, meta.gToken: ${meta.gToken}, " +
                        "failures-so-far: $failuresThisIteration, " +
                        "modifiedThisIteration.size: ${modifiedThisIteration.size})..."
                }

                if (manga.id in modifiedThisIteration) {
                    // We already processed this manga!
                    logger.w { "Gallery already updated this iteration, skipping..." }
                    updatedThisIteration++
                    continue
                }

                val (newChapters, allChapters) = try {
                    updateNotifier.showProgressNotification(
                        manga,
                        updatedThisIteration + failuresThisIteration,
                        mangaMetaToUpdateThisIter.size,
                    )
                    updateEntryAndGetChapters(manga)
                } catch (e: GalleryNotUpdatedException) {
                    if (e.network) {
                        failuresThisIteration++

                        logger.e(e) {
                            "Network error while updating gallery! " +
                                "(manga.id: ${manga.id}, meta.gId: ${meta.gId}, " +
                                "meta.gToken: ${meta.gToken}, failures-so-far: $failuresThisIteration)"
                        }
                    }

                    continue
                }

                if (allChapters.isEmpty()) {
                    logger.e {
                        "No chapters found for gallery (manga.id: ${manga.id}, " +
                            "meta.gId: ${meta.gId}, meta.gToken: ${meta.gToken}, " +
                            "failures-so-far: $failuresThisIteration)!"
                    }

                    continue
                }

                // Find accepted root and discard others
                val (acceptedRoot, discardedRoots, _) =
                    updateHelper.findAcceptedRootAndDiscardOthers(manga.source, allChapters)

                modifiedThisIteration += acceptedRoot.manga.id ?: continue
                modifiedThisIteration += discardedRoots.mapNotNull { it.manga.id }
                updatedThisIteration++
            }
        } finally {
            exhPreferences.exhAutoUpdateStats.set(
                Json.encodeToString(
                    EHentaiUpdaterStats.serializer(),
                    EHentaiUpdaterStats(
                        startTime,
                        allMeta.size,
                        updatedThisIteration,
                    ),
                ),
            )

            updateNotifier.cancelProgressNotification()
        }
    }

    // Returns Pair(new chapters, all current chapters)
    private suspend fun updateEntryAndGetChapters(manga: Manga): Pair<List<Chapter>, List<Chapter>> {
        val source = sourceManager.get(manga.source) as? EHentai
            ?: throw GalleryNotUpdatedException(false, IllegalStateException("Missing EH-based source (${manga.source})!"))

        try {
            val updatedManga = source.getMangaDetails(manga.copy())
            manga.copyFrom(updatedManga)
            updateManga.await(manga.toMangaUpdate())

            val newChapters = source.getChapterList(manga.copy())

            val (added, _) = syncChaptersWithSource(newChapters, manga, source)
            return added to getChapter.awaitAll(manga.id!!, false)
        } catch (t: Throwable) {
            if (t is EHentai.GalleryNotFoundException) {
                // Age dead galleries
                val mangaId = manga.id
                if (mangaId != null) {
                    val searchMeta = metadataRepository.getMetadataById(mangaId)
                    if (searchMeta != null) {
                        val tags = metadataRepository.getTagsById(mangaId)
                        val titles = metadataRepository.getTitlesById(mangaId)
                        val flatMeta = exh.metadata.metadata.base.FlatMetadata(searchMeta, tags, titles)
                        val meta = try {
                            flatMeta.raise<EHentaiSearchMetadata>()
                        } catch (e: Exception) {
                            null
                        }
                        if (meta != null) {
                            logger.d { "Aged ${manga.id} - notfound" }
                            meta.aged = true
                            meta.mangaId = mangaId
                            metadataRepository.insertMetadata(meta)
                        }
                    }
                }
                throw GalleryNotUpdatedException(false, t)
            }
            throw GalleryNotUpdatedException(true, t)
        }
    }

    companion object {
        private const val MAX_UPDATE_FAILURES = 5

        private val MIN_BACKGROUND_UPDATE_FREQ = 1.days.inWholeMilliseconds

        private const val TAG = "EHBackgroundUpdater"

        private val logger = Logger.withTag("EHUpdaterScheduler")

        fun launchBackgroundTest(context: Context) {
            context.workManager.enqueue(
                OneTimeWorkRequestBuilder<EHentaiUpdateWorker>()
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .addTag(TAG)
                    .build(),
            )
        }

        fun scheduleBackground(context: Context, prefInterval: Int? = null, prefRestrictions: Set<String>? = null) {
            val exhPreferences = Injekt.get<ExhPreferences>()
            val interval = prefInterval ?: exhPreferences.exhAutoUpdateFrequency.get()
            if (interval > 0) {
                val restrictions = prefRestrictions ?: exhPreferences.exhAutoUpdateRequirements.get()
                val acRestriction = "ac" in restrictions

                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresCharging(acRestriction)
                    .build()

                val request = PeriodicWorkRequestBuilder<EHentaiUpdateWorker>(
                    interval.toLong(),
                    TimeUnit.HOURS,
                    10,
                    TimeUnit.MINUTES,
                )
                    .addTag(TAG)
                    .setConstraints(constraints)
                    .build()

                context.workManager.enqueueUniquePeriodicWork(TAG, ExistingPeriodicWorkPolicy.UPDATE, request)
                logger.d { "Successfully scheduled background update job!" }
            } else {
                cancelBackground(context)
            }
        }

        fun cancelBackground(context: Context) {
            context.workManager.cancelAllWorkByTag(TAG)
        }
    }

    fun requiresWifiConnection(exhPreferences: ExhPreferences): Boolean {
        val restrictions = exhPreferences.exhAutoUpdateRequirements.get()
        return "wifi" in restrictions
    }
}

data class UpdateEntry(val manga: Manga, val meta: EHentaiSearchMetadata, val rootChapter: Chapter?)

object EHentaiUpdateWorkerConstants {
    const val UPDATES_PER_ITERATION = 50

    val GALLERY_AGE_TIME = 365.days.inWholeMilliseconds
}
