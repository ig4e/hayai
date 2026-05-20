package eu.kanade.tachiyomi.data.preference

import yokai.util.koin.get
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import eu.kanade.tachiyomi.ui.library.LibraryPresenter
import java.util.concurrent.TimeUnit

class DelayedLibrarySuggestionsJob(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val preferences = get<PreferencesHelper>()
        if (!preferences.showLibrarySearchSuggestions().isSet()) {
            preferences.showLibrarySearchSuggestions().set(true)
            LibraryPresenter.setSearchSuggestion(preferences, get(), get())
        }
        return Result.success()
    }

    companion object {
        private const val TAG = "DelayedLibrarySuggestions"

        fun setupTask(context: Context, enabled: Boolean) {
            if (enabled) {
                val request = OneTimeWorkRequestBuilder<DelayedLibrarySuggestionsJob>()
                    .setInitialDelay(1, TimeUnit.DAYS)
                    .addTag(TAG)
                    .build()

                WorkManager.getInstance(context).enqueueUniqueWork(TAG, ExistingWorkPolicy.KEEP, request)
            } else {
                WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
            }
        }
    }
}
