package eu.kanade.tachiyomi.data.backup.restore.restorers

import yokai.util.koin.get
import android.content.Context
import eu.kanade.tachiyomi.core.preference.AndroidPreferenceStore
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.core.preference.getEnum
import eu.kanade.tachiyomi.data.backup.create.BackupCreatorJob
import eu.kanade.tachiyomi.data.backup.models.BackupPreference
import eu.kanade.tachiyomi.data.backup.models.BackupSourcePreferences
import eu.kanade.tachiyomi.data.backup.models.BooleanPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.FloatPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.IntPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.LongPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringPreferenceValue
import eu.kanade.tachiyomi.data.backup.models.StringSetPreferenceValue
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.extension.ExtensionUpdateJob
import eu.kanade.tachiyomi.source.sourcePreferences
import eu.kanade.tachiyomi.ui.library.LibrarySort
import yokai.domain.base.BasePreferences
import yokai.domain.ui.settings.ReaderPreferences

class PreferenceBackupRestorer(
    private val context: Context,
    private val preferenceStore: PreferenceStore = get(),
) {
    fun restoreAppPreferences(preferences: List<BackupPreference>, onComplete: () -> Unit) {
        restorePreferences(preferences, preferenceStore)

        ExtensionUpdateJob.setupTask(context)
        LibraryUpdateJob.setupTask(context)
        BackupCreatorJob.setupTask(context)

        onComplete()
    }

    fun restoreSourcePreferences(preferences: List<BackupSourcePreferences>, onComplete: () -> Unit) {
        preferences.forEach {
            val sourcePrefs = AndroidPreferenceStore(context, sourcePreferences(it.sourceKey))
            restorePreferences(it.prefs, sourcePrefs)
        }
    }

    private fun restorePreferences(
        toRestore: List<BackupPreference>,
        preferenceStore: PreferenceStore,
    ) {
        val prefs = preferenceStore.getAll()
        toRestore.forEach { (key, value) ->
            // j2k fork differences
            if (key == "library_sorting_mode" && value is StringPreferenceValue &&
                prefs[key] is Int?
            ) {
                val intValue = LibrarySort.deserialize(value.value)
                preferenceStore.getInt(key).set(intValue.mainValue)
                return@forEach
            }
            // end j2k fork differences

            // << Yokai-J2K compat
            if (key == "extension_installer" && value is IntPreferenceValue) {
                val enum = BasePreferences.ExtensionInstaller.migrate(value.value)
                preferenceStore.getEnum(key, enum).set(enum)
                return@forEach
            }

            if (key == PreferenceKeys.pagerCutoutBehavior && value is IntPreferenceValue) {
                val enum = ReaderPreferences.CutoutBehaviour.migrate(value.value)
                preferenceStore.getEnum(key, enum).set(enum)
                return@forEach
            }

            if (key == "landscape_cutout_behavior" && value is IntPreferenceValue) {
                val enum = ReaderPreferences.LandscapeCutoutBehaviour.migrate(value.value)
                preferenceStore.getEnum(key, enum).set(enum)
                return@forEach
            }
            // >> Yokai-J2K compat

            when (value) {
                is IntPreferenceValue -> {
                    if (prefs[key] is Int?) {
                        preferenceStore.getInt(key).set(value.value)
                    }
                }
                is LongPreferenceValue -> {
                    if (prefs[key] is Long?) {
                        preferenceStore.getLong(key).set(value.value)
                    }
                }
                is FloatPreferenceValue -> {
                    if (prefs[key] is Float?) {
                        preferenceStore.getFloat(key).set(value.value)
                    }
                }
                is StringPreferenceValue -> {
                    if (prefs[key] is String?) {
                        preferenceStore.getString(key).set(value.value)
                    }
                }
                is BooleanPreferenceValue -> {
                    if (prefs[key] is Boolean?) {
                        preferenceStore.getBoolean(key).set(value.value)
                    }
                }
                is StringSetPreferenceValue -> {
                    if (prefs[key] is Set<*>?) {
                        preferenceStore.getStringSet(key).set(value.value)
                    }
                }
            }
        }
    }
}
