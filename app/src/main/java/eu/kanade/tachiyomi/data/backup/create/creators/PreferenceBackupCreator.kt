package eu.kanade.tachiyomi.data.backup.create.creators

import yokai.util.koin.get
import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.data.backup.create.BackupOptions
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.preferenceKey
import eu.kanade.tachiyomi.source.sourcePreferences
import eu.kanade.tachiyomi.ui.library.LibrarySort

class PreferenceBackupCreator(
    private val sourceManager: SourceManager = get(),
    private val preferenceStore: PreferenceStore = get(),
) {
    fun createApp(includePrivatePreferences: Boolean): List<BackupPreference> {
        return preferenceStore.getAll().toBackupPreferences()
            .withPrivatePreferences(includePrivatePreferences)
    }

    fun createSource(includePrivatePreferences: Boolean): List<BackupSourcePreferences> {
        return sourceManager.getOnlineSources()
            .filterIsInstance<ConfigurableSource>()
            .map {
                BackupSourcePreferences(
                    it.preferenceKey(),
                    it.sourcePreferences().all.toBackupPreferences()
                        .withPrivatePreferences(includePrivatePreferences),
                )
            }
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, *>.toBackupPreferences(): List<BackupPreference> {
        return this.filterKeys { !Preference.isAppState(it) }
            .mapNotNull { (key, value) ->
                // j2k fork differences
                if (key == "library_sorting_mode" && value is Int) {
                    val stringValue = (LibrarySort.valueOf(value) ?: LibrarySort.Title).serialize()
                    return@mapNotNull BackupPreference(key, StringPreferenceValue(stringValue))
                }
                // end j2k fork differences
                when (value) {
                    is Int -> BackupPreference(key, IntPreferenceValue(value))
                    is Long -> BackupPreference(key, LongPreferenceValue(value))
                    is Float -> BackupPreference(key, FloatPreferenceValue(value))
                    is String -> BackupPreference(key, StringPreferenceValue(value))
                    is Boolean -> BackupPreference(key, BooleanPreferenceValue(value))
                    is Set<*> -> (value as? Set<String>)?.let {
                        BackupPreference(key, StringSetPreferenceValue(it))
                    }
                    else -> null
                }
            }
    }

    private fun List<BackupPreference>.withPrivatePreferences(include: Boolean) =
        if (include) {
            this
        } else {
            this.filter { !Preference.isPrivate(it.key) }
        }
}
