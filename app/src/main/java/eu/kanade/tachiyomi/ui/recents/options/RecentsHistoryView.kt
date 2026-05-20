package eu.kanade.tachiyomi.ui.recents.options

import yokai.util.koin.get
import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.databinding.RecentsHistoryViewBinding
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.util.system.materialAlertDialog
import eu.kanade.tachiyomi.util.view.setMessage
import eu.kanade.tachiyomi.util.view.setNegativeButton
import eu.kanade.tachiyomi.util.view.setPositiveButton
import eu.kanade.tachiyomi.util.view.setTitle
import eu.kanade.tachiyomi.widget.BaseRecentsDisplayView
import yokai.i18n.MR
import yokai.util.lang.getString
import android.R as AR

class RecentsHistoryView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseRecentsDisplayView<RecentsHistoryViewBinding>(context, attrs) {

    override fun inflateBinding() = RecentsHistoryViewBinding.bind(this)
    override fun initGeneralPreferences() {
        binding.groupChapters.bindToPreference(preferences.groupChaptersHistory())
        binding.collapseGroupedChapters.bindToPreference(preferences.collapseGroupedHistory()) {
            controller?.presenter?.expandedSectionsMap?.clear()
        }
        binding.showHidden.bindToPreference(recentsPreferences.showHiddenInHistory())
        bindHiddenSourcesRow()
        binding.clearHistory.setOnClickListener {
            val activity = controller?.activity ?: return@setOnClickListener
            activity.materialAlertDialog()
                .setMessage(MR.strings.clear_history_confirmation)
                .setPositiveButton(MR.strings.clear) { _, _ ->
                    controller?.presenter?.deleteAllHistory()
                }
                .setNegativeButton(AR.string.cancel, null)
                .show()
        }
    }

    private fun bindHiddenSourcesRow() {
        updateHiddenSourcesLabel()
        binding.hiddenSources.setOnClickListener { showHiddenSourcesDialog() }
    }

    private fun updateHiddenSourcesLabel() {
        val count = recentsPreferences.hiddenSourcesInHistory().get().size
        val base = context.getString(MR.strings.recents_hidden_sources)
        binding.hiddenSources.text = if (count > 0) "$base ($count)" else base
    }

    private fun showHiddenSourcesDialog() {
        val activity = controller?.activity ?: return
        val sourceManager = get<SourceManager>()
        // Group catalogue sources by display name so multi-lang/factory variants don't
        // surface as duplicate rows; toggling one row hides every id sharing that name.
        val groups = sourceManager.getCatalogueSources()
            .groupBy { it.name }
            .entries
            .map { (name, list) -> name to list.map { it.id.toString() } }
            .sortedBy { it.first.lowercase() }
        if (groups.isEmpty()) {
            activity.materialAlertDialog()
                .setTitle(MR.strings.recents_hidden_sources)
                .setMessage(MR.strings.recents_no_hidden_sources)
                .setPositiveButton(AR.string.ok, null)
                .show()
            return
        }
        val currentlyHidden = recentsPreferences.hiddenSourcesInHistory().get().toMutableSet()
        val checked = BooleanArray(groups.size) { i -> groups[i].second.any { it in currentlyHidden } }
        val labels = groups.map { it.first }.toTypedArray()
        activity.materialAlertDialog()
            .setTitle(MR.strings.recents_hidden_sources)
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                val ids = groups[which].second
                if (isChecked) currentlyHidden.addAll(ids) else currentlyHidden.removeAll(ids.toSet())
            }
            .setPositiveButton(AR.string.ok) { _, _ ->
                recentsPreferences.hiddenSourcesInHistory().set(currentlyHidden)
                updateHiddenSourcesLabel()
            }
            .setNegativeButton(AR.string.cancel, null)
            .show()
    }
}
