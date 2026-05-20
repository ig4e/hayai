package eu.kanade.tachiyomi.widget

import android.content.Context
import android.util.AttributeSet
import androidx.core.widget.NestedScrollView
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.library.LibraryController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.recents.RecentsController
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import yokai.util.koin.injectLazy
import yokai.domain.recents.RecentsPreferences
import yokai.domain.ui.UiPreferences
import yokai.domain.ui.settings.ReaderPreferences

abstract class BaseTabbedScrollView<VB : ViewBinding> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    NestedScrollView(context, attrs) {

    lateinit var binding: VB
        private set
    init {
        clipToPadding = false
    }
    internal val preferences: PreferencesHelper by injectLazy()
    internal val uiPreferences: UiPreferences by injectLazy()

    abstract fun initGeneralPreferences()
    abstract fun inflateBinding(): VB

    override fun onFinishInflate() {
        super.onFinishInflate()
        binding = inflateBinding()
        setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)
        initGeneralPreferences()
    }
}

abstract class BaseRecentsDisplayView<VB : ViewBinding> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseTabbedScrollView<VB>(context, attrs) {
    var controller: RecentsController? = null

    internal val recentsPreferences: RecentsPreferences by injectLazy()
}

abstract class BaseLibraryDisplayView<VB : ViewBinding> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseTabbedScrollView<VB>(context, attrs) {
    var controller: LibraryController? = null
}

abstract class BaseReaderSettingsView<VB : ViewBinding> @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseTabbedScrollView<VB>(context, attrs) {
    lateinit var activity: ReaderActivity

    internal val readerPreferences: ReaderPreferences by injectLazy()
}
