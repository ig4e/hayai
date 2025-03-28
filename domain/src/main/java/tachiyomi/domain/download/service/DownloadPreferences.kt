package tachiyomi.domain.download.service

import tachiyomi.core.common.preference.PreferenceStore

class DownloadPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun downloadOnlyOverWifi() = preferenceStore.getBoolean(
        "pref_download_only_over_wifi_key",
        true,
    )

    fun saveChaptersAsCBZ() = preferenceStore.getBoolean("save_chapter_as_cbz", true)

    fun splitTallImages() = preferenceStore.getBoolean("split_tall_images", true)

    fun autoDownloadWhileReading() = preferenceStore.getInt("auto_download_while_reading", 0)

    fun removeAfterReadSlots() = preferenceStore.getInt("remove_after_read_slots", -1)

    fun removeAfterMarkedAsRead() = preferenceStore.getBoolean(
        "pref_remove_after_marked_as_read_key",
        false,
    )

    fun removeBookmarkedChapters() = preferenceStore.getBoolean("pref_remove_bookmarked", false)

    fun removeExcludeCategories() = preferenceStore.getStringSet(REMOVE_EXCLUDE_CATEGORIES_PREF_KEY, emptySet())

    fun downloadNewChapters() = preferenceStore.getBoolean("download_new", false)

    fun downloadNewChapterCategories() = preferenceStore.getStringSet(DOWNLOAD_NEW_CATEGORIES_PREF_KEY, emptySet())

    fun downloadNewChapterCategoriesExclude() =
        preferenceStore.getStringSet(DOWNLOAD_NEW_CATEGORIES_EXCLUDE_PREF_KEY, emptySet())

    fun downloadNewUnreadChaptersOnly() = preferenceStore.getBoolean("download_new_unread_chapters_only", false)

    /**
     * Returns the number of concurrent chapter downloaders per source.
     * This is a user-controlled preference.
     */
    fun numberOfDownloaders() = preferenceStore.getInt("pref_download_concurrent_chapters", 3)

    /**
     * Returns the maximum number of concurrent page downloads.
     * User can choose "Automatic" (0) or a specific value.
     */
    fun numberOfPageDownloaders() = preferenceStore.getInt("pref_download_concurrent_pages", 0)

    /**
     * Returns the maximum number of sources to download from concurrently.
     */
    fun numberOfConcurrentSources() = preferenceStore.getInt("pref_download_concurrent_sources", 4)

    /**
     * Returns whether to use dynamic concurrency that adapts to network and device capabilities.
     */
    fun useDynamicConcurrency() = preferenceStore.getBoolean("pref_download_dynamic_concurrency", true)

    companion object {
        private const val REMOVE_EXCLUDE_CATEGORIES_PREF_KEY = "remove_exclude_categories"
        private const val DOWNLOAD_NEW_CATEGORIES_PREF_KEY = "download_new_categories"
        private const val DOWNLOAD_NEW_CATEGORIES_EXCLUDE_PREF_KEY = "download_new_categories_exclude"
        val categoryPreferenceKeys = setOf(
            REMOVE_EXCLUDE_CATEGORIES_PREF_KEY,
            DOWNLOAD_NEW_CATEGORIES_PREF_KEY,
            DOWNLOAD_NEW_CATEGORIES_EXCLUDE_PREF_KEY,
        )
    }
}
