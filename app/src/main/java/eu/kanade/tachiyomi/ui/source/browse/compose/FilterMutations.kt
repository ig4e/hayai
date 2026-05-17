package eu.kanade.tachiyomi.ui.source.browse.compose

import eu.kanade.tachiyomi.source.model.Filter

/**
 * Pure state-mutation helpers for every [Filter] type rendered in the new sheet.
 *
 * Extracted from the composables so the same logic can be exercised by unit tests on the JVM
 * without standing up a Compose harness. The composables in this package wire user gestures to
 * these functions; they MUST NOT mutate `Filter.X.state` directly anywhere else.
 *
 * Every function mutates the supplied filter IN PLACE — this is what
 * `BrowseSourceController.showFilters()` relies on for its `oldFilters` vs `presenter.sourceFilters`
 * snapshot comparison. Never replace the [Filter] instance.
 */
internal object FilterMutations {

    fun toggleCheckbox(filter: Filter.CheckBox) {
        filter.state = !filter.state
    }

    /**
     * Direct-select TriState: tapping the include chip toggles between [Filter.TriState.STATE_INCLUDE]
     * and [Filter.TriState.STATE_IGNORE]; the exclude chip does the same with `STATE_EXCLUDE`.
     * This is a UX upgrade over the legacy single-tap-cycles-three-states gesture but reaches every
     * end state the old code could reach, so the applied query is identical.
     */
    fun setTriState(filter: Filter.TriState, target: Int) {
        require(
            target == Filter.TriState.STATE_INCLUDE ||
                target == Filter.TriState.STATE_EXCLUDE ||
                target == Filter.TriState.STATE_IGNORE,
        ) { "Invalid TriState target: $target" }
        filter.state = if (filter.state == target) Filter.TriState.STATE_IGNORE else target
    }

    fun setText(filter: Filter.Text, text: String) {
        filter.state = text
    }

    fun setSelect(filter: Filter.Select<*>, index: Int) {
        filter.state = index
    }

    /**
     * Sort-row tap, replicating [eu.kanade.tachiyomi.widget.SortTextView]'s rule:
     * - currently unselected → selected, ascending=false (shown as descending arrow)
     * - currently selected → toggle ascending
     *
     * Matches legacy `SortItem.bindViewHolder` so the resulting `Filter.Sort.Selection`
     * payload sent to the source is identical.
     */
    fun toggleSort(filter: Filter.Sort, index: Int) {
        val current = filter.state
        filter.state = when {
            current == null -> Filter.Sort.Selection(index, ascending = false)
            current.index != index -> Filter.Sort.Selection(index, ascending = false)
            else -> Filter.Sort.Selection(index, ascending = !current.ascending)
        }
    }

    /**
     * Adds [tag] to the autocomplete state unless the (prefix-stripped) tag is in
     * [Filter.AutoComplete.skipAutoFillTags]. Returns `true` if added.
     *
     * Matches legacy `AutoCompleteContent.onSubmit` so the rejection semantics around
     * "skip" tags is preserved verbatim.
     */
    fun addAutoCompleteTag(filter: Filter.AutoComplete, tag: String): Boolean {
        val prefix = filter.validPrefixes.find { tag.startsWith(it) }
        val tagNoPrefix = if (prefix != null) tag.removePrefix(prefix).trim() else tag
        if (tagNoPrefix in filter.skipAutoFillTags) return false
        filter.state = filter.state + tag
        return true
    }

    fun removeAutoCompleteTag(filter: Filter.AutoComplete, tag: String) {
        filter.state = filter.state - tag
    }
}
