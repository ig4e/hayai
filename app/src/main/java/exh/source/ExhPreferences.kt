package exh.source

import eu.kanade.tachiyomi.core.preference.PreferenceStore

/**
 * EXH-specific preferences for controlling EH/EXH features.
 */
class ExhPreferences(private val preferenceStore: PreferenceStore) {
    val enableExhentai = preferenceStore.getBoolean("enable_exhentai", false)

    fun autoUpdateFrequency() = preferenceStore.getInt("eh_autoUpdateFrequency", 1)

    fun autoUpdateRequirements() = preferenceStore.getStringSet("eh_autoUpdateRestrictions", emptySet())

    fun useHentaiAtHome() = preferenceStore.getInt("eh_useHentaiAtHome", 0)

    fun useJapaneseTitle() = preferenceStore.getBoolean("use_jp_title", false)

    fun useOriginalImages() = preferenceStore.getBoolean("eh_useOrigImages", false)

    fun ehTagFilterValue() = preferenceStore.getInt("eh_tag_filtering_value", 0)

    fun ehTagWatchingValue() = preferenceStore.getInt("eh_tag_watching_value", 0)

    fun exhWatchedListDefaultState() = preferenceStore.getBoolean("eh_watched_list_default", false)

    fun enhancedEHentaiView() = preferenceStore.getBoolean("enhanced_e_hentai_view", true)

    fun enableDelegatedSources() = preferenceStore.getBoolean("eh_delegateSources", true)

    fun enableSourceBlacklist() = preferenceStore.getBoolean("eh_enableSourceBlacklist", true)

    fun ehLastUpdatedCheck() = preferenceStore.getLong("eh_lastUpdatedCheck", 0)
}
