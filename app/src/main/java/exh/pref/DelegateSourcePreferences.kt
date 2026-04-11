package exh.pref

import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.core.preference.PreferenceStore

class DelegateSourcePreferences(
    private val preferenceStore: PreferenceStore,
) {

    val delegateSources: Preference<Boolean> = preferenceStore.getBoolean("eh_delegate_sources", true)

    val useJapaneseTitle: Preference<Boolean> = preferenceStore.getBoolean("use_jp_title", false)
}
