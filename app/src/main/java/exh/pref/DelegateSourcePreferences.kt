package exh.pref

import eu.kanade.tachiyomi.core.preference.PreferenceStore

/**
 * Preferences for delegated source configuration.
 */
class DelegateSourcePreferences(private val preferenceStore: PreferenceStore) {
    fun delegatedSourcesEnabled() = preferenceStore.getBoolean("eh_delegateSources", true)

    fun sourceBlacklistEnabled() = preferenceStore.getBoolean("eh_enableSourceBlacklist", true)
}
