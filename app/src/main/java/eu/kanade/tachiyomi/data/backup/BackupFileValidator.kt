package eu.kanade.tachiyomi.data.backup

import yokai.util.koin.get
import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.BackupUtil
import yokai.util.lang.getString

class BackupFileValidator(
    private val sourceManager: SourceManager = get(),
    private val trackManager: TrackManager = get(),
) {

    /**
     * Checks for critical backup file data.
     *
     * @throws Exception if manga cannot be found.
     * @return List of missing sources or missing trackers.
     */
    fun validate(context: Context, uri: Uri): Results {
        val backup = try {
            BackupUtil.decodeBackup(context, uri)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }

        val sources = backup.backupSources.associate { it.sourceId to it.name }
        val missingSources = sources
            .filter { sourceManager.get(it.key) == null }
            .map { sourceManager.getOrStub(it.key).name }
            .sorted()

        val trackers = backup.backupManga
            .flatMap { it.tracking }
            .map { it.syncId.toLong() }
            .distinct()
        val missingTrackers = trackers
            .mapNotNull { trackManager.getService(it) }
            .filter { !it.isLogged }
            .map { context.getString(it.nameRes()) }
            .sorted()

        return Results(missingSources, missingTrackers)
    }

    data class Results(val missingSources: List<String>, val missingTrackers: List<String>)
}
