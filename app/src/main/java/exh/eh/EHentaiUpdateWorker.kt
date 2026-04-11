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
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.util.system.isConnectedToWifi
import eu.kanade.tachiyomi.util.system.workManager
import exh.eh.EHentaiUpdateWorkerConstants.UPDATES_PER_ITERATION
import exh.source.ExhPreferences
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days

class EHentaiUpdateWorker(private val context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {
    private val exhPreferences: ExhPreferences by injectLazy()
    private val updateHelper: EHentaiUpdateHelper by injectLazy()
    private val logger = Logger.withTag("EHentaiUpdateWorker")

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

        // TODO: Implement full update logic when manga/metadata repositories are wired up
        // This is a skeleton that will be completed once the domain layer is ready
        val updatedThisIteration = 0
        val possibleUpdates = 0

        exhPreferences.exhAutoUpdateStats.set(
            Json.encodeToString(
                EHentaiUpdaterStats.serializer(),
                EHentaiUpdaterStats(
                    startTime,
                    possibleUpdates,
                    updatedThisIteration,
                ),
            ),
        )
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

data class UpdateEntry(val manga: yokai.domain.manga.models.Manga, val meta: exh.metadata.metadata.EHentaiSearchMetadata, val rootChapter: ChapterData?)

object EHentaiUpdateWorkerConstants {
    const val UPDATES_PER_ITERATION = 50

    val GALLERY_AGE_TIME = 365.days.inWholeMilliseconds
}
