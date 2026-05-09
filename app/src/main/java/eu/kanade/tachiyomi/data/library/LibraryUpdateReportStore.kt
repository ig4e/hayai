package eu.kanade.tachiyomi.data.library

import android.content.Context
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.util.system.createFileInCacheDir
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Snapshot of the most recent library update run, persisted as a JSON sidecar in the cache dir
 * so the in-app "Library update report" screen can be opened after the run finishes — even
 * after the user dismisses the notification or restarts the app. Cleared whenever cache is
 * cleared, which mirrors the lifetime of the existing tachiyomi_update_*.txt log files.
 */
@Serializable
data class LibraryUpdateReport(
    val timestampMs: Long,
    val errors: List<LibraryUpdateReportEntry>,
    val skipped: List<LibraryUpdateReportEntry>,
)

@Serializable
data class LibraryUpdateReportEntry(
    val mangaId: Long,
    val mangaTitle: String,
    val mangaThumbnailUrl: String?,
    val mangaCoverLastModified: Long,
    val mangaInLibrary: Boolean,
    val sourceId: Long,
    val sourceName: String,
    /** Error message or skip reason — already localized when generated. */
    val message: String,
)

class LibraryUpdateReportStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun save(report: LibraryUpdateReport) {
        try {
            val file = context.createFileInCacheDir(REPORT_FILE_NAME)
            file.writeText(json.encodeToString(report))
        } catch (e: Exception) {
            Logger.e(e) { "Failed to persist library update report" }
        }
    }

    fun read(): LibraryUpdateReport? {
        return try {
            val file = File(context.cacheDir, REPORT_FILE_NAME)
            if (!file.exists() || file.length() == 0L) return null
            json.decodeFromString<LibraryUpdateReport>(file.readText())
        } catch (e: Exception) {
            Logger.e(e) { "Failed to read library update report" }
            null
        }
    }

    fun clear() {
        runCatching { File(context.cacheDir, REPORT_FILE_NAME).delete() }
    }

    /** Path to the human-readable .txt log written alongside the JSON, if it still exists. */
    fun errorLogFile(): File? = File(context.cacheDir, ERROR_LOG_FILE_NAME).takeIf { it.exists() }

    fun skippedLogFile(): File? = File(context.cacheDir, SKIPPED_LOG_FILE_NAME).takeIf { it.exists() }

    companion object {
        const val REPORT_FILE_NAME = "library_update_report.json"
        // Must mirror the names written by LibraryUpdateJob.writeErrorFile.
        const val ERROR_LOG_FILE_NAME = "tachiyomi_update_errors.txt"
        const val SKIPPED_LOG_FILE_NAME = "tachiyomi_update_skipped.txt"
    }
}
