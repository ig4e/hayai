package hayai.novel.ui

import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible

/**
 * Adapter for the novel plugins list in the browse bottom sheet.
 * Follows the same pattern as ExtensionAdapter.
 */
class NovelPluginAdapter(val listener: OnButtonClickListener) :
    FlexibleAdapter<IFlexible<*>>(null, listener, true) {

    init {
        setDisplayHeadersAtStartUp(true)
    }

    interface OnButtonClickListener {
        fun onNovelPluginButtonClick(position: Int)
    }
}
