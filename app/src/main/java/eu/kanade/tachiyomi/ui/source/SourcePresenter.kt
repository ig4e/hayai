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
 * Presenter of [BrowseController]
 * Function calls should be done from here. UI calls should be done from the controller.
 *
 * @param sourceManager manages the different sources.
 * @param preferences application preferences.
 */
class SourcePresenter(
    val controller: BrowseController,
    val sourceManager: SourceManager = Injekt.get(),
    val extensionManager: ExtensionManager = Injekt.get(),
    private val preferences: PreferencesHelper = Injekt.get(),
) {

    private var scope = CoroutineScope(Job() + Dispatchers.Default)
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

        observeSources()
        loadSources()
    }

    /**
     * Unsubscribe and create a new subscription to fetch enabled sources.
     */
    private fun loadSources() {
        scope.launch {
            val pinnedSources = mutableListOf<SourceItem>()
            val pinnedCatalogues = preferences.pinnedCatalogues().get()

            val map = TreeMap<String, MutableList<CatalogueSource>> { d1, d2 ->
                // Catalogues without a lang defined will be placed at the end
                when {
                    d1 == "" && d2 != "" -> 1
                    d2 == "" && d1 != "" -> -1
                    else -> d1.compareTo(d2)
                }
            }
            val byLang = sources.groupByTo(map) { it.lang }
            sourceItems = byLang.flatMap {
                val langItem = LangItem(it.key)
                it.value.map { source ->
                    val isPinned = source.id.toString() in pinnedCatalogues
                    if (source.id.toString() in pinnedCatalogues) {
                        pinnedSources.add(SourceItem(source, LangItem(PINNED_KEY)))
                    }

                    SourceItem(source, langItem, isPinned)
                }
            }

            if (pinnedSources.isNotEmpty()) {
                sourceItems = pinnedSources + sourceItems
            }

            lastUsedItem = getLastUsedSource(preferences.lastUsedCatalogueSource().get())
            withUIContext {
                controller.setSources(sourceItems, lastUsedItem)
                loadLastUsedSource()
            }
        }
    }

    private fun loadLastUsedSource() {
        lastUsedJob?.cancel()
        lastUsedJob = preferences.lastUsedCatalogueSource().changes()
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
            val pinnedCatalogues = preferences.pinnedCatalogues().get()
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
            preferences.pinnedCatalogues().changes().onStart { emit(preferences.pinnedCatalogues().get()) },
        ) { catalogueSources, _, _, _ ->
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
