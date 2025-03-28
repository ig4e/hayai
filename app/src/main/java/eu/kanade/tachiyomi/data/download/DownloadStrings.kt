package eu.kanade.tachiyomi.data.download

import android.content.Context
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR

/**
 * String constants for use in the Downloader settings
 */
object DownloadStrings {

    // Add these string getters to the SYMR namespace temporarily
    fun getDownloaderStrings(context: Context): Map<Int, String> {
        return mapOf(
            // SYMR.strings.concurrent_chapter_downloads to "Concurrent chapter downloads",
            // SYMR.strings.concurrent_chapter_downloads_summary to "Maximum number of chapters to download simultaneously per source",
            // SYMR.strings.concurrent_page_downloads to "Concurrent page downloads",
            // SYMR.strings.concurrent_page_downloads_summary to "Maximum number of concurrent page downloads per chapter",
            // SYMR.strings.use_dynamic_concurrency to "Use dynamic concurrency",
            // SYMR.strings.use_dynamic_concurrency_summary to "Automatically adjust download concurrency based on network and device performance"
        )
    }
}
