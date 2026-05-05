package eu.kanade.tachiyomi.util.view

import com.google.android.material.tabs.TabLayout

fun TabLayout.bindStringTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onReselected: ((Int) -> Unit)? = null,
) {
    removeAllTabs()
    clearOnTabSelectedListeners()
    val safeIndex = selectedIndex.coerceIn(0, (labels.size - 1).coerceAtLeast(0))
    labels.forEachIndexed { index, label ->
        addTab(
            newTab().setText(label).also { tab -> tab.view.compatToolTipText = null },
            index == safeIndex,
        )
    }
    addOnTabSelectedListener(
        object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab ?: return
                onSelected(tab.position)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {
                tab ?: return
                onReselected?.invoke(tab.position)
            }
        },
    )
}
