package eu.kanade.tachiyomi.ui.source

import android.content.res.Resources
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.util.system.launchIO
import org.xmlpull.v1.XmlPullParser
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background prefetch for the Browse cold path.
 *
 * Browse is the heaviest of the three root tabs to cold-enter: it inflates the
 * source list AND the bottom-sheet ViewPager (extensions / novel plugins /
 * migration) into the same frame the cross-fade is animating. We can't move
 * the XML inflation itself off the UI thread (AsyncLayoutInflater crashes on
 * any layout containing a View whose constructor touches `Animator`, which
 * burned us on `recent_sub_chapter_item`), but we can pay the *parsing* cost
 * in advance from a background thread so the on-frame inflate skips it.
 *
 * `Resources.getXml` returns an `XmlResourceParser` backed by an `XmlBlock`
 * that the AssetManager caches per (resourceId, configuration). Iterating the
 * parser once forces the binary XML to be decoded and stored — subsequent
 * `LayoutInflater.inflate` / `MenuInflater.inflate` calls reuse the cached
 * block and skip the parse step (~5–15ms saved per layout).
 *
 * We also `Class.forName` the Browse-side classes that the cold path would
 * load lazily (adapters, presenters, custom views). This pulls dexopt + class
 * verification + static-init work off the cold frame.
 *
 * Idempotent — only ever runs once per process. Failures are swallowed (we
 * don't want a warmup miss to crash the activity).
 */
object BrowseWarmup {

    private val primed = AtomicBoolean(false)

    /**
     * Kick off the warmup on a background coroutine. Safe to call multiple times.
     * Call from [eu.kanade.tachiyomi.ui.main.MainActivity.onCreate] after `super.onCreate`.
     */
    fun primeAsync(resources: Resources) {
        if (!primed.compareAndSet(false, true)) return
        launchIO {
            primeXmlCache(resources)
            primeClassCache()
        }
    }

    /**
     * Walks each XML resource end-to-end so the AssetManager parses + caches the
     * binary XML. `runCatching` because a missing resource (e.g. removed in a
     * future refactor) shouldn't take down the cold path.
     */
    private fun primeXmlCache(resources: Resources) {
        XML_IDS.forEach { id ->
            runCatching {
                resources.getXml(id).use { parser ->
                    while (parser.next() != XmlPullParser.END_DOCUMENT) {
                        // Drain the parser to force the AssetManager to fully
                        // decode the binary XML into its cache.
                    }
                }
            }
        }
    }

    private fun primeClassCache() {
        CLASS_NAMES.forEach { name ->
            runCatching { Class.forName(name) }
        }
    }

    // Layouts + menus on the Browse cold path. Each layout includes / row that
    // would land in a long first-attach frame is listed so the parse cost is
    // paid here instead.
    private val XML_IDS = intArrayOf(
        R.layout.browse_controller,
        R.layout.extensions_bottom_sheet,
        R.layout.recycler_with_scroller,
        R.layout.source_item,
        R.layout.source_header_item,
        R.layout.extension_card_item,
        R.layout.extension_card_header,
        R.menu.extension_main,
        R.menu.migration_main,
        R.menu.catalogue_main,
    )

    // Classes the cold path lazy-loads. Class.forName here triggers dexopt +
    // verification on the IO thread so the UI thread doesn't pay it.
    private val CLASS_NAMES = listOf(
        "eu.kanade.tachiyomi.ui.extension.ExtensionBottomSheet",
        "eu.kanade.tachiyomi.ui.extension.ExtensionAdapter",
        "eu.kanade.tachiyomi.ui.extension.ExtensionBottomPresenter",
        "eu.kanade.tachiyomi.ui.extension.RecyclerWithScrollerView",
        "eu.kanade.tachiyomi.ui.source.SourceAdapter",
        "eu.kanade.tachiyomi.ui.source.SourcePresenter",
        "eu.kanade.tachiyomi.ui.migration.SourceAdapter",
        "eu.kanade.tachiyomi.ui.migration.MangaAdapter",
        "hayai.novel.ui.NovelPluginAdapter",
    )
}
