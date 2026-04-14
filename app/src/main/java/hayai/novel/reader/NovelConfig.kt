package hayai.novel.reader

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerConfig
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.DisabledNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.EdgeNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.KindlishNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.LNavigation
import eu.kanade.tachiyomi.ui.reader.viewer.navigation.RightAndLeftNavigation
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
) : ViewerConfig(preferences, scope) {

    // Text rendering
    var fontSize: Int = 18
    var fontFamily: String = "serif"
    var lineHeight: Float = 1.8f
    var textAlign: String = "start"
    var paddingHorizontal: Int = 16
    var paddingVertical: Int = 20

    // Theme (0=dark, 1=light, 2=sepia, 3=amoled, 4=cool)
    var readerTheme: Int = 0
    var menuThreshold: Int = 75

    // Listener for text property changes (triggers WebView reload)
    var textPropertyChangedListener: (() -> Unit)? = null

    data class ReaderColors(val textColor: String, val backgroundColor: String)

    val themes = mapOf(
        0 to ReaderColors("#E0E0E0", "#1A1A1A"),    // Dark
        1 to ReaderColors("#212121", "#FFFFFF"),      // Light
        2 to ReaderColors("#5B4636", "#F4ECD8"),      // Sepia
        3 to ReaderColors("#C8C8C8", "#0A0A0A"),      // AMOLED
        4 to ReaderColors("#000000", "#DCE5E2"),      // Cool
    )

    fun getColors(): ReaderColors = themes[readerTheme] ?: themes[0]!!

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
        preferences.novelReaderTheme()
            .register({ readerTheme = it }, { textPropertyChangedListener?.invoke() })

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
