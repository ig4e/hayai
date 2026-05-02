package hayai.novel.theme

import kotlinx.serialization.Serializable

/**
 * A reader theme — a (text color, background color) pair plus a stable id and display name.
 *
 * Themes come in two flavours: built-in (immutable, shipped in code via [NovelThemeRegistry]) and
 * custom (user-defined, persisted as JSON via the `novel_custom_themes` preference). Selection is
 * stored as the theme's [id] in the `novel_selected_theme_id` preference.
 *
 * Color strings are 7-character `#RRGGBB` form so they can be parsed by `Color.parseColor` without
 * additional handling and round-trip through serialization unchanged.
 */
@Serializable
data class NovelTheme(
    val id: String,
    val name: String,
    val textColor: String,
    val backgroundColor: String,
    val builtIn: Boolean,
)
