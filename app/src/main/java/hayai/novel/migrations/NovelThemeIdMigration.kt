package hayai.novel.migrations

import android.app.Application
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import hayai.novel.theme.NovelThemeRegistry
import yokai.core.migration.Migration
import yokai.core.migration.MigrationContext

/**
 * Migrates the legacy integer theme index (`novel_reader_theme: Int`, where 0=dark, 1=light,
 * 2=sepia, 3=amoled, 4=cool) to the new string-based id (`novel_selected_theme_id: String`)
 * introduced for user-defined custom themes in Phase 1B.
 *
 * The integer-to-id mapping table here is the authoritative source for that translation; the
 * built-in id constants in [NovelThemeRegistry] must stay aligned with the order below or this
 * migration would assign the wrong theme to existing users.
 */
class NovelThemeIdMigration : Migration {
    override val version: Float = 131f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        // Use a sentinel that can't collide with a real value so we can distinguish "missing
        // legacy key" (fresh install) from "set to 0 / dark" (migrating existing user).
        val legacy = prefs.getInt(LEGACY_KEY, SENTINEL)
        if (legacy == SENTINEL) return true

        val mappedId = LEGACY_INT_TO_ID.getOrNull(legacy) ?: NovelThemeRegistry.DEFAULT_ID
        prefs.edit {
            putString("novel_selected_theme_id", mappedId)
            remove(LEGACY_KEY)
        }
        return true
    }

    private companion object {
        const val LEGACY_KEY = "novel_reader_theme"
        const val SENTINEL = Int.MIN_VALUE

        // Index → built-in id. Order MUST match the legacy NovelConfig.themes map.
        val LEGACY_INT_TO_ID = listOf(
            NovelThemeRegistry.ID_DARK,
            NovelThemeRegistry.ID_LIGHT,
            NovelThemeRegistry.ID_SEPIA,
            NovelThemeRegistry.ID_AMOLED,
            NovelThemeRegistry.ID_COOL,
        )
    }
}
