package tachiyomi.domain.backup.service

import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore

class BackupPreferences(
    private val preferenceStore: PreferenceStore,
) {

    fun backupInterval() = preferenceStore.getInt("backup_interval", 12)

    fun numberOfBackups() = preferenceStore.getInt("backup_slots", 2)

    fun lastAutoBackupTimestamp() = preferenceStore.getLong(Preference.appStateKey("last_auto_backup_timestamp"), 0L)
}
