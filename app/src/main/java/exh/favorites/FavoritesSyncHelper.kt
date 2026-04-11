package exh.favorites

import android.content.Context
import android.net.wifi.WifiManager
import android.os.PowerManager
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.online.all.EHentai
import exh.GalleryAddEvent
import exh.GalleryAdder
import exh.eh.EHentaiUpdateWorker
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.ExhPreferences
import exh.util.ThrottleManager
import exh.util.createPartialWakeLock
import exh.util.createWifiLock
import exh.util.ignore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import okhttp3.FormBody
import okhttp3.Request
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.time.Duration.Companion.seconds

// TODO only apply database changes after sync
class FavoritesSyncHelper(val context: Context) {
    private val exhPreferences: ExhPreferences by injectLazy()

    private val exh by lazy {
        // TODO: Wire up SourceManager when available
        EHentai(EXH_SOURCE_ID, true, context)
    }

    private val storage by lazy { LocalFavoritesStorage() }

    private val galleryAdder by lazy { GalleryAdder() }

    private val throttleManager by lazy { ThrottleManager() }

    private var wifiLock: WifiManager.WifiLock? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val logger = Logger.withTag("FavoritesSyncHelper")

    val status: MutableStateFlow<FavoritesSyncStatus> = MutableStateFlow(FavoritesSyncStatus.Idle)

    @Synchronized
    fun runSync(scope: CoroutineScope) {
        if (status.value !is FavoritesSyncStatus.Idle) {
            return
        }

        status.value = FavoritesSyncStatus.Initializing

        scope.launch(Dispatchers.IO) { beginSync() }
    }

    private suspend fun beginSync() {
        // Check if logged in
        if (!exhPreferences.enableExhentai.get()) {
            status.value = FavoritesSyncStatus.SyncError.NotLoggedInSyncError
            return
        }

        val errorList = mutableListOf<FavoritesSyncStatus.SyncError.GallerySyncError>()

        try {
            // Take wake + wifi locks
            ignore { wakeLock?.release() }
            wakeLock = ignore { context.createPartialWakeLock("teh:ExhFavoritesSyncWakelock") }
            ignore { wifiLock?.release() }
            wifiLock = ignore { context.createWifiLock("teh:ExhFavoritesSyncWifi") }

            // Do not update galleries while syncing favorites
            EHentaiUpdateWorker.cancelBackground(context)

            // TODO: Implement full sync logic when repositories are available
            status.value = FavoritesSyncStatus.Processing.CleaningUp

        } catch (e: Exception) {
            status.value = FavoritesSyncStatus.SyncError.UnknownSyncError(e.message.orEmpty())
            logger.e(e) { "Favorites sync error" }
            return
        } finally {
            // Release wake + wifi locks
            ignore {
                wakeLock?.release()
                wakeLock = null
            }
            ignore {
                wifiLock?.release()
                wifiLock = null
            }

            // Update galleries again!
            EHentaiUpdateWorker.scheduleBackground(context)
        }

        if (errorList.isEmpty()) {
            status.value = FavoritesSyncStatus.Idle
        } else {
            status.value = FavoritesSyncStatus.CompleteWithErrors(errorList)
        }
    }

    private fun needWarnThrottle() =
        throttleManager.throttleTime >= THROTTLE_WARN

    class IgnoredException(message: FavoritesSyncStatus.SyncError.GallerySyncError) : RuntimeException(message.toString())

    companion object {
        private val THROTTLE_WARN = 1.seconds
    }
}

@Serializable
sealed class FavoritesSyncStatus {
    @Serializable
    sealed class SyncError : FavoritesSyncStatus() {
        @Serializable
        data object NotLoggedInSyncError : SyncError()

        @Serializable
        data object FailedToFetchFavorites : SyncError()

        @Serializable
        data class UnknownSyncError(val message: String) : SyncError()

        @Serializable
        sealed class GallerySyncError : SyncError() {
            @Serializable
            data class UnableToAddGalleryToRemote(val title: String, val gid: String) : GallerySyncError()

            @Serializable
            data object UnableToDeleteFromRemote : GallerySyncError()

            @Serializable
            data class GalleryAddFail(val title: String, val reason: String) : GallerySyncError()

            @Serializable
            data class InvalidGalleryFail(val title: String, val url: String) : GallerySyncError()
        }
    }

    @Serializable
    data object Idle : FavoritesSyncStatus()

    @Serializable
    sealed class BadLibraryState : FavoritesSyncStatus() {
        @Serializable
        data class MangaInMultipleCategories(
            val mangaId: Long,
            val mangaTitle: String,
            val categories: List<String>,
        ) : BadLibraryState()
    }

    @Serializable
    data object Initializing : FavoritesSyncStatus()

    @Serializable
    sealed class Processing : FavoritesSyncStatus() {
        data object VerifyingLibrary : Processing()
        data object DownloadingFavorites : Processing()
        data object CalculatingRemoteChanges : Processing()
        data object CalculatingLocalChanges : Processing()
        data object SyncingCategoryNames : Processing()
        data class RemovingRemoteGalleries(val galleryCount: Int) : Processing()
        data class AddingGalleryToRemote(
            val index: Int,
            val total: Int,
            val isThrottling: Boolean,
            val title: String,
        ) : Processing()
        data class RemovingGalleryFromLocal(
            val index: Int,
            val total: Int,
        ) : Processing()
        data class AddingGalleryToLocal(
            val index: Int,
            val total: Int,
            val isThrottling: Boolean,
            val title: String,
        ) : Processing()
        data object CleaningUp : Processing()
    }

    @Serializable
    data class CompleteWithErrors(val messages: List<SyncError.GallerySyncError>) : FavoritesSyncStatus()
}
