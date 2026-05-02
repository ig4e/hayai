package hayai.novel.reader

import android.graphics.Color
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.EdgeNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
import hayai.novel.preferences.NovelPreferences
import hayai.novel.theme.NovelTheme
import hayai.novel.theme.NovelThemeRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Configuration for the novel text reader.
 * Follows the same pattern as WebtoonConfig/PagerConfig.
 * Manages font, color, spacing, and navigation preferences.
 */
class NovelConfig(
    scope: CoroutineScope,
    preferences: PreferencesHelper = Injekt.get(),
    private val novelPreferences: NovelPreferences = Injekt.get(),
) : ViewerConfig(preferences, scope) {

    // Text rendering
    var fontSize: Int = 18
    var fontFamily: String = "serif"
    var fontWeight: Int = NovelPreferences.DEFAULT_FONT_WEIGHT
    var lineHeight: Float = 1.8f
    var textAlign: String = "start"
    var paddingHorizontal: Int = 16
    var paddingVertical: Int = 20
    var paragraphSpacing: Int = NovelPreferences.DEFAULT_PARAGRAPH_SPACING_DP

    // Theme — resolved from `selectedThemeId` against built-ins ∪ customThemes via NovelThemeRegistry.
    var selectedThemeId: String = NovelThemeRegistry.DEFAULT_ID
    var customThemes: List<NovelTheme> = emptyList()
    var menuThreshold: Int = 75

    // Listener for text property changes (triggers WebView reload)
    var textPropertyChangedListener: (() -> Unit)? = null

    data class ReaderColors(val textColor: String, val backgroundColor: String)

    /**
     * Returns the colors for the currently-selected theme, resolved across built-ins and custom
     * user-defined themes; falls back to the default built-in if the selected id no longer exists.
     */
    fun getColors(): ReaderColors {
        val theme = NovelThemeRegistry.resolve(selectedThemeId, customThemes)
        return ReaderColors(textColor = theme.textColor, backgroundColor = theme.backgroundColor)
    }

    /**
     * Legacy 0..4 theme index passed to [eu.kanade.tachiyomi.ui.reader.viewer.ReaderTransitionView].
     * Built-in themes map to their original index; custom themes pick the closest built-in based
     * on background-color luminance so the transition view's palette still picks readable colors.
     */
    val readerTheme: Int
        get() {
            val resolved = NovelThemeRegistry.resolve(selectedThemeId, customThemes)
            val builtInIndex = NovelThemeRegistry.builtIns.indexOfFirst { it.id == resolved.id }
            if (builtInIndex >= 0) return builtInIndex
            // Custom theme: pick dark (0) when the background is dark, otherwise light (1).
            return if (isDarkColor(resolved.backgroundColor)) 0 else 1
        }

    private fun isDarkColor(hex: String): Boolean {
        return runCatching {
            val c = Color.parseColor(hex)
            // Rec. 601 luma; threshold 0.5 maps "midnight blue" and similar to dark.
            val luma = (Color.red(c) * 0.299f + Color.green(c) * 0.587f + Color.blue(c) * 0.114f) / 255f
            luma < 0.5f
        }.getOrDefault(true)
    }

    companion object {
        val fontFamilies = arrayOf("serif", "sans-serif", "monospace", "system-ui")
        val textAligns = arrayOf("start", "center", "justify")
    }

    init {
        // Novel text preferences
        preferences.novelFontSize()
            .register({ fontSize = it }, { textPropertyChangedListener?.invoke() })
        preferences.novelFontFamily()
            .register({ fontFamily = fontFamilies.getOrElse(it) { "serif" } }, { textPropertyChangedListener?.invoke() })
        preferences.novelLineHeight()
            .register({ lineHeight = it / 10f }, { textPropertyChangedListener?.invoke() })
        preferences.novelTextAlign()
            .register({ textAlign = textAligns.getOrElse(it) { "start" } }, { textPropertyChangedListener?.invoke() })
        preferences.novelPadding()
            .register({ paddingHorizontal = it; paddingVertical = it }, { textPropertyChangedListener?.invoke() })
        novelPreferences.fontWeight()
            .register({ fontWeight = it }, { textPropertyChangedListener?.invoke() })
        novelPreferences.paragraphSpacing()
            .register({ paragraphSpacing = it }, { textPropertyChangedListener?.invoke() })
        novelPreferences.selectedThemeId()
            .register({ selectedThemeId = it }, { textPropertyChangedListener?.invoke() })
        novelPreferences.customThemes()
            .register({ customThemes = it }, { textPropertyChangedListener?.invoke() })

        // Navigation — reuse webtoon nav preferences (same scroll-based paradigm)
        preferences.navigationModeWebtoon()
            .register({ navigationMode = it }, { updateNavigation(it) })

        preferences.webtoonNavInverted()
            .register({ tappingInverted = it }, { navigator.invertMode = it })

        preferences.webtoonNavInverted().changes()
            .drop(1)
            .onEach { navigationModeInvertedListener?.invoke() }
            .launchIn(scope)

        // Navigation overlay for first-time users
        navigationOverlayForNewUser = preferences.showNavigationOverlayNewUserWebtoon().get()
        if (navigationOverlayForNewUser) {
            preferences.showNavigationOverlayNewUserWebtoon().set(false)
        }
    }

    override var navigator: ViewerNavigation = defaultNavigation()
        set(value) {
            field = value.also { it.invertMode = tappingInverted }
        }

    override fun defaultNavigation(): ViewerNavigation = LNavigation()

    override fun updateNavigation(navigationMode: Int) {
        navigator = when (navigationMode) {
            0 -> defaultNavigation()
            1 -> LNavigation()
            2 -> KindlishNavigation()
            3 -> EdgeNavigation()
            4 -> RightAndLeftNavigation()
            5 -> DisabledNavigation()
            else -> defaultNavigation()
        }
        navigationModeChangedListener?.invoke()
    }
}
