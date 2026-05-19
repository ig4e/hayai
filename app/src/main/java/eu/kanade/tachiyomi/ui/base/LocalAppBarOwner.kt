package eu.kanade.tachiyomi.ui.base

/**
 * Marker for a controller that hosts its own [ExpandedAppBarLayout] inside its layout
 * tree rather than reaching into the activity-global appBar. Implementing this lets
 * the `Controller.appBar()` extension route scroll-sync, alpha mutations, search-pill
 * queries, and tab-strip ops at the local instance — without which a controller's
 * chrome mutations would target the wrong appBar and leak across screens.
 *
 * Defined as a separate interface (not a method on [eu.kanade.tachiyomi.ui.base.controller.BaseController])
 * so that the `Controller.appBar()` extension can do the routing check without
 * forcing every controller through `BaseController` — `RouterPagerAdapter` children
 * and a few other Conductor leaves are plain `Controller`s.
 */
interface LocalAppBarOwner {
    fun localAppBar(): ExpandedAppBarLayout?
}
