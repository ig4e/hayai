package yokai.core.migration.migrations

import android.app.Application
import androidx.preference.PreferenceManager
import yokai.core.migration.Migration
import yokai.core.migration.MigrationContext
import yokai.domain.ui.settings.ReaderPreferences

/**
 * Migrates Hayai's pre-port novel reader preferences (six unprefixed keys on
 * PreferencesHelper, e.g. `novel_font_size`) to the new Tsundoku-compatible
 * keys (`pref_novel_*`) on [ReaderPreferences].
 *
 * Runs once. Old keys are not deleted by the migration itself; the legacy
 * accessors and constants are removed in a later phase.
 */
class NovelReaderPrefsMigration : Migration {
    override val version: Float = 122f

    override suspend fun invoke(migrationContext: MigrationContext): Boolean {
        val context = migrationContext.get<Application>() ?: return false
        val readerPreferences = migrationContext.get<ReaderPreferences>() ?: return false
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        runCatching {
            if (prefs.contains(LEGACY_FONT_SIZE)) {
                readerPreferences.novelFontSize.set(prefs.getInt(LEGACY_FONT_SIZE, 18))
            }
        }

        runCatching {
            if (prefs.contains(LEGACY_FONT_FAMILY)) {
                val idx = prefs.getInt(LEGACY_FONT_FAMILY, 0)
                readerPreferences.novelFontFamily.set(LEGACY_FONT_FAMILIES.getOrElse(idx) { "sans-serif" })
            }
        }

        runCatching {
            if (prefs.contains(LEGACY_LINE_HEIGHT)) {
                val tenths = prefs.getInt(LEGACY_LINE_HEIGHT, 18)
                readerPreferences.novelLineHeight.set(tenths / 10f)
            }
        }

        runCatching {
            if (prefs.contains(LEGACY_TEXT_ALIGN)) {
                val idx = prefs.getInt(LEGACY_TEXT_ALIGN, 0)
                readerPreferences.novelTextAlign.set(LEGACY_TEXT_ALIGNS.getOrElse(idx) { "left" })
            }
        }

        runCatching {
            if (prefs.contains(LEGACY_PADDING)) {
                val pad = prefs.getInt(LEGACY_PADDING, 16)
                readerPreferences.novelMarginLeft.set(pad)
                readerPreferences.novelMarginRight.set(pad)
            }
        }

        runCatching {
            if (prefs.contains(LEGACY_THEME)) {
                val theme = LEGACY_THEMES.getOrElse(prefs.getInt(LEGACY_THEME, 0)) { "app" }
                readerPreferences.novelTheme.set(theme)
            }
        }

        return true
    }

    companion object {
        private const val LEGACY_FONT_SIZE = "novel_font_size"
        private const val LEGACY_FONT_FAMILY = "novel_font_family"
        private const val LEGACY_LINE_HEIGHT = "novel_line_height"
        private const val LEGACY_TEXT_ALIGN = "novel_text_align"
        private const val LEGACY_PADDING = "novel_padding"
        private const val LEGACY_THEME = "novel_reader_theme"

        // Legacy index → CSS family-name mapping. Order must match Hayai's
        // pre-port `NovelConfig.fontFamilies` (serif/sans-serif/monospace/system-ui).
        private val LEGACY_FONT_FAMILIES = arrayOf("serif", "sans-serif", "monospace", "system-ui")

        // Legacy index → Tsundoku string. Old indices were 0=start, 1=center, 2=justify.
        // "start" maps to the new "left" (LTR locales).
        private val LEGACY_TEXT_ALIGNS = arrayOf("left", "center", "justify")

        // Legacy index → Tsundoku theme key. Old indices: 0=dark, 1=light, 2=sepia, 3=amoled, 4=cool.
        // amoled→black (closest match), cool→grey (closest match).
        private val LEGACY_THEMES = arrayOf("dark", "light", "sepia", "black", "grey")
    }
}
