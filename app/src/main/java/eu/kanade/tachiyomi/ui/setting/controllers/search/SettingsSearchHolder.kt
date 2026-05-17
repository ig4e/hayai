package eu.kanade.tachiyomi.ui.setting.controllers.search

import android.view.View
import androidx.core.graphics.ColorUtils
import eu.davidea.viewholders.FlexibleViewHolder
import com.google.android.material.R as materialR
import eu.kanade.tachiyomi.R
import yokai.i18n.MR
import yokai.util.lang.getString
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.databinding.SettingsSearchControllerCardBinding
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.util.lang.highlightText
import eu.kanade.tachiyomi.util.system.getResourceColor
import kotlin.reflect.full.createInstance

/**
 * Holder that binds the [SettingsSearchItem] containing catalogue cards.
 *
 * @param view view of [SettingsSearchItem]
 * @param adapter instance of [SettingsSearchAdapter]
 */
class SettingsSearchHolder(view: View, val adapter: SettingsSearchAdapter) :
    FlexibleViewHolder(view, adapter) {

    private val binding = SettingsSearchControllerCardBinding.bind(view)
    init {
        binding.titleWrapper.setOnClickListener {
            adapter.getItem(bindingAdapterPosition)?.let {
                val ctrl = it.settingsSearchResult.searchController::class.createInstance()
                if (ctrl !is SettingsLegacyController) return@let

                ctrl.preferenceKey = it.settingsSearchResult.key

                // must pass a new Controller instance to avoid this error https://github.com/bluelinelabs/Conductor/issues/446
                adapter.titleClickListener.onTitleClick(ctrl)
            }
        }
    }

    /**
     * Show the loading of source search result.
     *
     * @param item item of card.
     */
    fun bind(item: SettingsSearchItem) {
        val color = ColorUtils.setAlphaComponent(itemView.context.getResourceColor(materialR.attr.colorSecondary), 75)
        binding.searchResultPrefTitle.text = item.settingsSearchResult.title.highlightText(item.searchResult, color)
        binding.searchResultPrefSummary.text = item.settingsSearchResult.summary.highlightText(item.searchResult, color)
        binding.searchResultPrefBreadcrumb.text = item.settingsSearchResult.breadcrumb.highlightText(item.searchResult, color)
    }
}
