package eu.kanade.tachiyomi.ui.reader.viewer.navigation

import android.graphics.RectF
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation

/**
 * Visualization of default state without any inversion. Three horizontal stripes:
 * +---+---+---+
 * | P | P | P |   P: Previous (top third — scrolls up)
 * +---+---+---+
 * | M | M | M |   M: Menu (middle third — toggles reader chrome)
 * +---+---+---+
 * | N | N | N |   N: Next (bottom third — scrolls down)
 * +---+---+---+
 *
 * Used by the novel reader to drive tap-to-scroll: NovelWebViewViewer maps PREV → scroll
 * up by ~80% viewport, NEXT → scroll down by ~80% viewport, MENU → toggleMenu(). The
 * default DisabledNavigation has empty regions so every tap resolves to MENU; this class
 * is what makes tap-to-scroll actually fire.
 */
class VerticalNavigation : ViewerNavigation() {

    override var regions: List<Region> = listOf(
        Region(
            rectF = RectF(0f, 0f, 1f, 0.33f),
            type = NavigationRegion.PREV,
        ),
        Region(
            rectF = RectF(0f, 0.66f, 1f, 1f),
            type = NavigationRegion.NEXT,
        ),
    )
}
