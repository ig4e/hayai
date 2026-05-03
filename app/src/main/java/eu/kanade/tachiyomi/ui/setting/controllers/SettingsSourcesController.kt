package eu.kanade.tachiyomi.ui.setting.controllers

import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import coil3.asDrawable
import coil3.imageLoader
import coil3.request.ImageRequest
import eu.kanade.tachiyomi.R
import yokai.i18n.MR
import yokai.util.lang.getString
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.core.preference.minusAssign
import eu.kanade.tachiyomi.core.preference.plusAssign
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.icon
import eu.kanade.tachiyomi.ui.main.FloatingSearchInterface
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.ui.setting.onChange
import eu.kanade.tachiyomi.ui.setting.titleMRes as titleRes
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.view.activityBinding
import eu.kanade.tachiyomi.util.view.setOnQueryTextChangeListener
import eu.kanade.tachiyomi.widget.preference.SwitchPreferenceCategory
// NOVEL -->
import hayai.novel.source.NovelSource
// NOVEL <--
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.TreeMap

class SettingsSourcesController : SettingsLegacyController(), FloatingSearchInterface {
    init {
        setHasOptionsMenu(true)
    }

    // Catalogue (not just HttpSource) so novel TextSource entries are included alongside
    // manga extension sources. Both implement CatalogueSource and share the per-language
    // grouping / hide-source preference contract.
    private val catalogueSources by lazy { Injekt.get<SourceManager>().getVisibleCatalogueSources() }

    private var query = ""

    private var orderedLangs = listOf<String>()
    private var langPrefs = mutableListOf<Pair<String, SwitchPreferenceCategory>>()
    private var sourcesByLang: TreeMap<String, MutableList<CatalogueSource>> = TreeMap()
    private var sorting = SourcesSort.Alpha

    override fun getSearchTitle(): String? {
        return view?.context?.getString(MR.strings.search)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = MR.strings.filter
        sorting = SourcesSort.from(preferences.sourceSorting().get()) ?: SourcesSort.Alpha
        activity?.invalidateOptionsMenu()
        // Get the list of active language codes.
        val activeLangsCodes = preferences.enabledLanguages().get()

        // Get a map of sources grouped by language.
        sourcesByLang = catalogueSources.groupByTo(TreeMap()) { it.lang }

        // Order first by active languages, then inactive ones
        orderedLangs = sourcesByLang.keys.filter { it in activeLangsCodes } + sourcesByLang.keys
            .filterNot { it in activeLangsCodes }

        orderedLangs.forEach { lang ->
            // Create a preference group and set initial state and change listener
            langPrefs.add(
                Pair(
                    lang,
                    SwitchPreferenceCategory(context).apply {
                        preferenceScreen.addPreference(this)
                        title = LocaleHelper.getSourceDisplayName(lang, context)
                        isPersistent = false
                        if (lang in activeLangsCodes) {
                            setChecked(true)
                            addLanguageSources(this, sortedSources(sourcesByLang[lang]))
                        }

                        onChange { newValue ->
                            val checked = newValue as Boolean
                            if (!checked) {
                                preferences.enabledLanguages() -= lang
                                removeAll()
                            } else {
                                preferences.enabledLanguages() += lang
                                addLanguageSources(this, sortedSources(sourcesByLang[lang]))
                            }
                            true
                        }
                    },
                ),
            )
        }
    }

    override fun setDivider(divider: Drawable?) {
        super.setDivider(null)
    }

    /**
     * Adds the source list for the given group (language).
     *
     * @param group the language category.
     */
    private fun addLanguageSources(group: PreferenceGroup, sources: List<CatalogueSource>) {
        if (sources.isEmpty()) return
        val hiddenCatalogues = preferences.hiddenSources().get()

        // Bulk show/hide for every source in this language without disabling the language
        // itself (the parent SwitchPreferenceCategory does that). Useful when you have many
        // sources installed in one language but only want a few visible in browse. Renamed
        // to "Select all" + given a distinct check-box icon so it clearly reads as a UI
        // control instead of a phantom iconless source like the previous "All sources" row.
        val selectAllPreference = CheckBoxPreference(group.context).apply {
            title = context.getString(MR.strings.select_all)
            key = "select_all_${sources.first().lang}"
            isPersistent = false
            isChecked = sources.all { it.id.toString() !in hiddenCatalogues }
            isVisible = query.isEmpty()

            onChange { newValue ->
                val checked = newValue as Boolean
                val current = preferences.hiddenSources().get().toMutableSet()
                val ids = sources.map { it.id.toString() }
                if (checked) current.removeAll(ids) else current.addAll(ids)
                preferences.hiddenSources().set(current)
                group.removeAll()
                addLanguageSources(group, sortedSources(sources))
                true
            }
        }
        group.addPreference(selectAllPreference)

        sources.forEach { source ->
            val sourcePreference = CheckBoxPreference(group.context).apply {
                val id = source.id.toString()
                title = source.name
                key = getSourceKey(source.id)
                isPersistent = false
                isChecked = id !in hiddenCatalogues
                isVisible = query.isEmpty() || source.name.contains(query, ignoreCase = true)

                val sourceIcon = source.icon()
                when {
                    sourceIcon != null -> icon = sourceIcon
                    // NOVEL -->
                    // Novel sources aren't backed by an installed APK, so Source.icon() returns
                    // null and the row would render iconless. Mirror SourceHolder: try the hosted
                    // iconUrl via Coil, fall back to the generic book glyph if the URL is missing
                    // or fails to load.
                    //
                    // Visual alignment: manga sources come back as AdaptiveIconDrawable with a
                    // built-in ~16.7% safe-zone padding (visible content ~67% of the icon slot),
                    // while a raw BitmapDrawable from Coil fills the slot 100% and looks oversized
                    // next to manga rows. Wrap novel drawables in an InsetDrawable with the same
                    // safe-zone fraction so both source types render at consistent visual size.
                    source is NovelSource -> {
                        val ctx = group.context
                        icon = withAdaptiveIconInset(ContextCompat.getDrawable(ctx, R.drawable.ic_book_24dp))
                        val url = source.iconUrl
                        if (!url.isNullOrBlank()) {
                            val pref = this
                            val request = ImageRequest.Builder(ctx)
                                .data(url)
                                .target(
                                    onSuccess = { result ->
                                        pref.icon = withAdaptiveIconInset(result.asDrawable(ctx.resources))
                                    },
                                )
                                .build()
                            ctx.imageLoader.enqueue(request)
                        }
                    }
                    // NOVEL <--
                }

                onChange { newValue ->
                    val checked = newValue as Boolean
                    val current = preferences.hiddenSources().get()

                    preferences.hiddenSources().set(
                        if (checked) {
                            current - id
                        } else {
                            current + id
                        },
                    )

                    group.removeAll()
                    addLanguageSources(group, sortedSources(sources))
                    true
                }
            }

            group.addPreference(sourcePreference)
        }
    }

    private fun getSourceKey(sourceId: Long): String {
        return "source_$sourceId"
    }

    /**
     * Wrap a raw drawable in an [InsetDrawable] that mimics the launcher adaptive-icon
     * safe-zone (72/108 ≈ 0.667 visible, hence 0.167 inset on each side). Used for novel
     * source icons loaded via Coil so they sit at the same effective size as manga sources,
     * which come back as AdaptiveIconDrawable with the same safe-zone built in.
     */
    private fun withAdaptiveIconInset(drawable: Drawable?): Drawable? {
        return drawable?.let { InsetDrawable(it, ADAPTIVE_ICON_SAFE_ZONE_INSET) }
    }

    /**
     * Adds items to the options menu.
     *
     * @param menu menu containing options.
     * @param inflater used to load the menu xml.
     */
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.settings_sources, menu)
        if (sorting == SourcesSort.Alpha) {
            menu.findItem(R.id.action_sort_alpha).isChecked = true
        } else {
            menu.findItem(R.id.action_sort_enabled).isChecked = true
        }

        val useSearchTB = showFloatingBar()
        val searchItem = if (useSearchTB) {
            activityBinding?.searchToolbar?.searchItem
        } else {
            (menu.findItem(R.id.action_search))
        }
        val searchView = if (useSearchTB) {
            activityBinding?.searchToolbar?.searchView
        } else {
            searchItem?.actionView as? SearchView
        }
        if (!useSearchTB) {
            searchView?.maxWidth = Int.MAX_VALUE
        }

        activityBinding?.searchToolbar?.setQueryHint(getSearchTitle(), query.isEmpty())

        if (query.isNotEmpty()) {
            searchItem?.expandActionView()
            searchView?.setQuery(query, true)
            searchView?.clearFocus()
        }

        setOnQueryTextChangeListener(activityBinding?.searchToolbar?.searchView) {
            query = it ?: ""
            drawSources()
            true
        }

        setOnQueryTextChangeListener(searchView) {
            query = it ?: ""
            drawSources()
            true
        }

        if (useSearchTB) {
            // Fixes problem with the overflow icon showing up in lieu of search
            searchItem?.fixExpand(onExpand = { invalidateMenuOnExpand() })
        }
    }

    override fun showFloatingBar() = activityBinding?.appBar?.useLargeToolbar == true

    var expandActionViewFromInteraction = false
    private fun MenuItem.fixExpand(onExpand: ((MenuItem) -> Boolean)? = null, onCollapse: ((MenuItem) -> Boolean)? = null) {
        setOnActionExpandListener(
            object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return onExpand?.invoke(item) ?: true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    activity?.invalidateOptionsMenu()

                    return onCollapse?.invoke(item) ?: true
                }
            },
        )

        if (expandActionViewFromInteraction) {
            expandActionViewFromInteraction = false
            expandActionView()
        }
    }

    private fun invalidateMenuOnExpand(): Boolean {
        return if (expandActionViewFromInteraction) {
            activity?.invalidateOptionsMenu()
            false
        } else {
            true
        }
    }

    private fun drawSources() {
        val activeLangsCodes = preferences.enabledLanguages().get()
        langPrefs.forEach { group ->
            if (group.first in activeLangsCodes) {
                group.second.removeAll()
                addLanguageSources(group.second, sortedSources(sourcesByLang[group.first]))
            }
        }
    }

    private fun sortedSources(sources: List<CatalogueSource>?): List<CatalogueSource> {
        val sourceAlpha = sources.orEmpty().sortedBy { it.name }
        return if (sorting == SourcesSort.Enabled) {
            val hiddenCatalogues = preferences.hiddenSources().get()
            sourceAlpha.filter { it.id.toString() !in hiddenCatalogues } +
                sourceAlpha.filterNot { it.id.toString() !in hiddenCatalogues }
        } else {
            sourceAlpha
        }
    }

    /**
     * Called when an option menu item has been selected by the user.
     *
     * @param item The selected item.
     * @return True if this event has been consumed, false if it has not.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        sorting = when (item.itemId) {
            R.id.action_sort_alpha -> SourcesSort.Alpha
            R.id.action_sort_enabled -> SourcesSort.Enabled
            else -> return super.onOptionsItemSelected(item)
        }
        item.isChecked = true
        (activity as? MainActivity)?.let {
            val otherTB = if (it.currentToolbar == it.binding.searchToolbar) {
                it.binding.toolbar
            } else {
                it.binding.searchToolbar
            }
            otherTB.menu.findItem(item.itemId).isChecked = true
        }
        preferences.sourceSorting().set(sorting.value)
        drawSources()
        return true
    }

    enum class SourcesSort(val value: Int) {
        Alpha(0), Enabled(1);

        companion object {
            fun from(i: Int): SourcesSort? = entries.find { it.value == i }
        }
    }

    companion object {
        // Adaptive-icon safe-zone fraction: launcher icons reserve (108-72)/2/108 ≈ 0.167
        // of each side as transparent padding. We mirror that on raw novel bitmaps so they
        // visually align with the manga AdaptiveIconDrawables in the same list.
        private const val ADAPTIVE_ICON_SAFE_ZONE_INSET = 0.167f
    }
}
