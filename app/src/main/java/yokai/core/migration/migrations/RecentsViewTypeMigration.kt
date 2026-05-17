package yokai.core.migration.migrations

import yokai.core.migration.Migration
import yokai.core.migration.MigrationContext
import yokai.domain.ui.UiPreferences

/**
 * Maps legacy RecentsViewType ordinals after dropping UngroupedAll.
 * Old: Grouped=0, UngroupedAll=1, History=2, Updates=3.
 * New: Grouped=0, History=1, Updates=2.
 * Migration: 1 -> 0 (UngroupedAll users land on Grouped), 2 -> 1, 3 -> 2.
 */
class RecentsViewTypeMigration : Migration {
    override val version: Float = 123f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val uiPreferences = migrationContext.get<UiPreferences>() ?: return false
        val pref = uiPreferences.recentsViewType()
        val newValue = when (pref.get()) {
            1 -> 0
            2 -> 1
            3 -> 2
            else -> return true
        }
        pref.set(newValue)
        return true
    }
}
