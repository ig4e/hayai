package hayai.novel.theme

/**
 * Catalog of built-in novel reader themes plus the resolution helper that finds the
 * currently-selected theme across built-ins and the user's custom list.
 *
 * Built-in IDs are stable strings — they're persisted to SharedPreferences and migrated from the
 * legacy integer index by [hayai.novel.migrations.NovelThemeIdMigration]. Do not rename or
 * reorder them without updating the migration's mapping table.
 */
object NovelThemeRegistry {

    /**
     * Stable IDs for the built-in themes. Each maps 1:1 to a position in the legacy
     * `novel_reader_theme: Int` preference (Dark=0, Light=1, Sepia=2, AMOLED=3, Cool=4) which the
     * migration uses to translate the old value to the new string id.
     */
    const val ID_DARK = "dark"
    const val ID_LIGHT = "light"
    const val ID_SEPIA = "sepia"
    const val ID_AMOLED = "amoled"
    const val ID_COOL = "cool"

    const val DEFAULT_ID = ID_DARK

    /**
     * Built-in themes in their canonical display order.
     */
    val builtIns: List<NovelTheme> = listOf(
        NovelTheme(id = ID_DARK,   name = "Dark",   textColor = "#E0E0E0", backgroundColor = "#1A1A1A", builtIn = true),
        NovelTheme(id = ID_LIGHT,  name = "Light",  textColor = "#212121", backgroundColor = "#FFFFFF", builtIn = true),
        NovelTheme(id = ID_SEPIA,  name = "Sepia",  textColor = "#5B4636", backgroundColor = "#F4ECD8", builtIn = true),
        NovelTheme(id = ID_AMOLED, name = "AMOLED", textColor = "#C8C8C8", backgroundColor = "#0A0A0A", builtIn = true),
        NovelTheme(id = ID_COOL,   name = "Cool",   textColor = "#000000", backgroundColor = "#DCE5E2", builtIn = true),
    )

    /**
     * Resolves the active theme by [selectedId], searching built-ins first then [customs]. Falls
     * back to the default built-in if nothing matches (e.g. user deleted the custom theme that was
     * previously selected).
     */
    fun resolve(selectedId: String, customs: List<NovelTheme>): NovelTheme {
        builtIns.firstOrNull { it.id == selectedId }?.let { return it }
        customs.firstOrNull { it.id == selectedId }?.let { return it }
        return builtIns.first { it.id == DEFAULT_ID }
    }

    /**
     * The full ordered list shown in the theme manager: built-ins first, then customs in their
     * stored order.
     */
    fun all(customs: List<NovelTheme>): List<NovelTheme> = builtIns + customs
}
