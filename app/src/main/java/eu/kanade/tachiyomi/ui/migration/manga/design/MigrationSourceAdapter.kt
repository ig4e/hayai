package eu.kanade.tachiyomi.ui.migration.manga.design

import yokai.util.koin.get
import android.os.Bundle
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.system.getParcelableArrayListCompat
import yokai.util.koin.injectLazy

class MigrationSourceAdapter(
    var items: List<MigrationSourceItem>,
    controllerPre: PreMigrationController,
) : FlexibleAdapter<MigrationSourceItem>(
    items,
    controllerPre,
    true,
) {

    val extensionManager: ExtensionManager = get()
    val enabledLanguages = get<PreferencesHelper>().enabledLanguages().get()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putParcelableArrayList(
            SELECTED_SOURCES_KEY,
            ArrayList(
                currentItems.map {
                    it.asParcelable()
                },
            ),
        )
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        val sourceManager: SourceManager by injectLazy()
        savedInstanceState.getParcelableArrayListCompat(
            SELECTED_SOURCES_KEY,
            MigrationSourceItem.ParcelableSI::class.java
        )?.let {
            updateDataSet(it.map { MigrationSourceItem.fromParcelable(sourceManager, it) })
        }

        super.onRestoreInstanceState(savedInstanceState)
    }

    fun updateItems() {
        items = currentItems
    }

    companion object {
        private const val SELECTED_SOURCES_KEY = "selected_sources"
    }
}
