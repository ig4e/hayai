package eu.kanade.tachiyomi.ui.source

import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.LocalSource
import eu.kanade.tachiyomi.source.SourceManager
import exh.source.BlacklistedSources
import eu.kanade.tachiyomi.util.system.withUIContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.TreeMap

/**
 * Presenter of [BrowseController].
 *
 * Source listing is partitioned by [BrowseSourceType]: only sources whose
 * [browseType] matches [currentType] are shown, and the pinned / last-used
 * state is read from the per-type preferences.
 */
class SourcePresenter(
    val controller: BrowseController,
    val sourceManager: SourceManager = Injekt.get(),
    val extensionManager: ExtensionManager = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
) {

    private var scope = CoroutineScope(Job() + Dispatchers.Default)

    /** Active tab's source family. Persisted via [PreferencesHelper.lastBrowseSourceType]. */
    var currentType: BrowseSourceType = BrowseSourceType.fromOrdinal(preferences.lastBrowseSourceType().get())
        private set

    /** All enabled catalogue sources across both types (filter applied at [loadSources]). */
    var sources = getEnabledSources()
    private var sourcesJob: Job? = null

    var sourceItems = emptyList<SourceItem>()
    var lastUsedItem: SourceItem? = null

    var lastUsedJob: Job? = null

    fun onCreate() {
        if (lastSources != null) {
            if (sourceItems.isEmpty()) {
                sourceItems = lastSources ?: emptyList()
            }
            lastUsedItem = lastUsedItemRem
            lastSources = null
            lastUsedItemRem = null
        }

        maybeMigrateLegacyPinned()
        observeSources()
        loadSources()
    }

    /** Switch the visible source family and reload. Persists the choice across app restarts. */
    fun setCurrentType(type: BrowseSourceType) {
        if (type == currentType) return
        currentType = type
        preferences.lastBrowseSourceType().set(type.ordinal)
        loadSources()
    }

    private fun pinnedPrefFor(type: BrowseSourceType) = when (type) {
        BrowseSourceType.Manga -> preferences.pinnedMangaCatalogues()
        BrowseSourceType.Novel -> preferences.pinnedNovelCatalogues()
    }

    private fun lastUsedPrefFor(type: BrowseSourceType) = when (type) {
        BrowseSourceType.Manga -> preferences.lastUsedMangaSource()
        BrowseSourceType.Novel -> preferences.lastUsedNovelSource()
    }

    /**
     * One-shot split of the legacy [PreferencesHelper.pinnedCatalogues] set into the
     * per-type sets on first Browse load. The legacy set is left in place so cross-type
     * features (GlobalSearch, Migration) continue to read from it. We only run the split
     * when BOTH per-type sets are empty — once the user has touched either, we treat the
     * per-type sets as authoritative and never re-apply the legacy values on top.
     */
    private fun maybeMigrateLegacyPinned() {
        val mangaSet = preferences.pinnedMangaCatalogues().get()
        val novelSet = preferences.pinnedNovelCatalogues().get()
        if (mangaSet.isNotEmpty() || novelSet.isNotEmpty()) return
        val legacy = preferences.pinnedCatalogues().get()
        if (legacy.isEmpty()) return

        val newManga = mutableSetOf<String>()
        val newNovel = mutableSetOf<String>()
        legacy.forEach { idStr ->
            val id = idStr.toLongOrNull() ?: return@forEach
            val src = sourceManager.get(id) as? CatalogueSource ?: return@forEach
            when (src.browseType) {
                BrowseSourceType.Manga -> newManga.add(idStr)
                BrowseSourceType.Novel -> newNovel.add(idStr)
            }
        }
        if (newManga.isNotEmpty()) preferences.pinnedMangaCatalogues().set(newManga)
        if (newNovel.isNotEmpty()) preferences.pinnedNovelCatalogues().set(newNovel)
    }

    /**
     * Build the visible source list for [currentType]: pinned-of-type rows first (under a
     * PINNED langheader), then the remaining sources grouped by language.
     */
    private fun loadSources() {
        scope.launch {
            val pinnedSources = mutableListOf<SourceItem>()
            val pinnedCatalogues = pinnedPrefFor(currentType).get()
            val typeSources = sources.filter { it.browseType == currentType }

            val map = TreeMap<String, MutableList<CatalogueSource>> { d1, d2 ->
                // Catalogues without a lang defined will be placed at the end
                when {
                    d1 == "" && d2 != "" -> 1
                    d2 == "" && d1 != "" -> -1
                    else -> d1.compareTo(d2)
                }
            }
            val byLang = typeSources.groupByTo(map) { it.lang }
            sourceItems = byLang.flatMap {
                val langItem = LangItem(it.key)
                it.value.map { source ->
                    val isPinned = source.id.toString() in pinnedCatalogues
                    if (isPinned) {
                        pinnedSources.add(SourceItem(source, LangItem(PINNED_KEY)))
                    }

                    SourceItem(source, langItem, isPinned)
                }
            }

            if (pinnedSources.isNotEmpty()) {
                sourceItems = pinnedSources + sourceItems
            }

            lastUsedItem = getLastUsedSource(lastUsedPrefFor(currentType).get())
            withUIContext {
                controller.setSources(sourceItems, lastUsedItem)
                loadLastUsedSource()
            }
        }
    }

    private fun loadLastUsedSource() {
        lastUsedJob?.cancel()
        lastUsedJob = lastUsedPrefFor(currentType).changes()
            .drop(1)
            .onEach {
                lastUsedItem = getLastUsedSource(it)
                withUIContext {
                    controller.setLastUsedSource(lastUsedItem)
                }
            }.launchIn(scope)
    }

    private fun getLastUsedSource(value: Long): SourceItem? {
        return (sourceManager.get(value) as? CatalogueSource)?.let { source ->
            // Only surface the last-used row if its type matches the active tab AND it's
            // not already in the pinned section (which would duplicate the entry).
            if (source.browseType != currentType) return@let null
            val pinnedCatalogues = pinnedPrefFor(currentType).get()
            val isPinned = source.id.toString() in pinnedCatalogues
            if (isPinned) {
                null
            } else {
                SourceItem(source, LangItem(LAST_USED_KEY), isPinned)
            }
        }
    }

    fun updateSources() {
        sources = getEnabledSources()
        loadSources()
    }

    fun onDestroy() {
        // observeSources's combine wraps onStart lambdas that read preferences.X(), capturing
        // SourcePresenter (and therefore BrowseController). Without this cancel, the suspended
        // collector inside StateFlowSlot keeps the controller — and its scrollViewWith lifecycle
        // listener that captures the source recycler — alive past onDestroy, leaking MainActivity.
        // Cancelling the scope ends sourcesJob + lastUsedJob + any pending loadSources launch,
        // freeing the upstream chain so Conductor's normal listener cleanup can release the rest.
        scope.cancel()
        lastSources = sourceItems
        lastUsedItemRem = lastUsedItem
    }

    /**
     * Returns a list of enabled sources ordered by language and name.
     *
     * @return list containing enabled sources.
     */
    private fun getEnabledSources(): List<CatalogueSource> {
        return getEnabledSources(sourceManager.getVisibleCatalogueSources())
    }

    private fun getEnabledSources(catalogueSources: List<CatalogueSource>): List<CatalogueSource> {
        val languages = preferences.enabledLanguages().get()
        val hiddenCatalogues = preferences.hiddenSources().get()

        return catalogueSources
            .filter { it.lang in languages || it.id == LocalSource.ID }
            .filterNot { it.id.toString() in hiddenCatalogues }
            .filterNot { it.id in BlacklistedSources.HIDDEN_SOURCES }
            .sortedBy { "(${it.lang}) ${it.name}" }
    }

    private fun observeSources() {
        sourcesJob?.cancel()
        sourcesJob = combine(
            sourceManager.catalogueSources,
            preferences.enabledLanguages().changes().onStart { emit(preferences.enabledLanguages().get()) },
            preferences.hiddenSources().changes().onStart { emit(preferences.hiddenSources().get()) },
            preferences.pinnedMangaCatalogues().changes().onStart { emit(preferences.pinnedMangaCatalogues().get()) },
            preferences.pinnedNovelCatalogues().changes().onStart { emit(preferences.pinnedNovelCatalogues().get()) },
        ) { catalogueSources, _, _, _, _ ->
            // The flow doesn't pre-filter HIDDEN_SOURCES so we keep that filter inside
            // getEnabledSources rather than calling the visible-sources helper here.
            getEnabledSources(catalogueSources)
        }.onEach {
            sources = it
            loadSources()
        }.launchIn(scope)
    }

    companion object {
        const val PINNED_KEY = "pinned"
        const val LAST_USED_KEY = "last_used"

        private var lastSources: List<SourceItem>? = null
        private var lastUsedItemRem: SourceItem? = null

        fun onLowMemory() {
            lastSources = null
            lastUsedItemRem = null
        }
    }
}
