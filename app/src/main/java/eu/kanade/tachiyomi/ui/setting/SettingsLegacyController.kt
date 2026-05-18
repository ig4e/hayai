package eu.kanade.tachiyomi.ui.setting

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceController
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceScreen
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import androidx.preference.R as preferenceR
import eu.kanade.tachiyomi.R
import yokai.i18n.MR
import yokai.util.lang.getString
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.changesIn
import eu.kanade.tachiyomi.ui.base.controller.BaseControllerPreferenceControllerCommonInterface
import eu.kanade.tachiyomi.ui.main.FloatingSearchInterface
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.view.BackHandlerControllerInterface
import eu.kanade.tachiyomi.util.view.backgroundColor
import eu.kanade.tachiyomi.util.view.isControllerVisible
import eu.kanade.tachiyomi.util.view.scrollViewWith
import eu.kanade.tachiyomi.widget.LinearLayoutManagerAccurateOffset
import kotlinx.coroutines.MainScope
import uy.kohesive.injekt.injectLazy
import yokai.domain.base.BasePreferences
import java.util.*

abstract class SettingsLegacyController : PreferenceController(), SettingsControllerInterface, BackHandlerControllerInterface, BaseControllerPreferenceControllerCommonInterface, eu.kanade.tachiyomi.ui.main.chrome.ChromeAware {

    var preferenceKey: String? = null
    val basePreferences: BasePreferences by injectLazy()
    val preferences: PreferencesHelper by injectLazy()
    val viewScope = MainScope()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.layoutManager = LinearLayoutManagerAccurateOffset(view.context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.backgroundColor = view.context.getResourceColor(android.R.attr.colorBackground)
        scrollViewWith(listView, padBottom = true)
        return view
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        preferenceKey?.let { prefKey ->
            val adapter = listView.adapter
            scrollToPreference(prefKey)

            listView.post {
                if (adapter is PreferenceGroup.PreferencePositionCallback) {
                    val pos = adapter.getPreferenceAdapterPosition(prefKey)
                    listView.findViewHolderForAdapterPosition(pos)?.let {
                        animatePreferenceHighlight(it.itemView)
                    }
                    preferenceKey = null
                }
            }
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(getThemedContext())
        preferenceScreen = screen
        setupPreferenceScreen(screen)
    }

    abstract fun setupPreferenceScreen(screen: PreferenceScreen): PreferenceScreen

    private fun getThemedContext(): Context {
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(preferenceR.attr.preferenceTheme, tv, true)
        return ContextThemeWrapper(activity, tv.resourceId)
    }

    private fun animatePreferenceHighlight(view: View) {
        ValueAnimator
            .ofObject(ArgbEvaluator(), Color.TRANSPARENT, view.context.getResourceColor(android.R.attr.colorControlHighlight))
            .apply {
                duration = 500L
                repeatCount = 2
                addUpdateListener { animator -> view.setBackgroundColor(animator.animatedValue as Int) }
                reverse()
            }
    }

    override fun getTitle(): String? = __getTitle() ?: preferenceScreen?.title?.toString()

    override fun getSearchTitle(): String? {
        return if (this is FloatingSearchInterface) {
            searchTitle(preferenceScreen?.title?.toString()?.lowercase(Locale.ROOT))
        } else {
            null
        }
    }

    fun setTitle() = __setTitle()

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        if (type.isEnter && isControllerVisible) {
            setTitle()
        }
        // super.onChangeStarted (BaseController) handles the chrome bind via the hoisted
        // ChromeAware path, as well as setHasOptionsMenu + alpha gating for off-screen entries.
        super.onChangeStarted(handler, type)
    }

    override fun describeChrome(): eu.kanade.tachiyomi.ui.main.chrome.ChromeSpec =
        eu.kanade.tachiyomi.ui.main.chrome.ChromeSpec(
            appBarVisible = true,
            scrollSource = if (view != null) listView else null,
            useSmallToolbar = true,
            tabs = null,
        )

    inline fun <T> Preference.visibleIf(preference: eu.kanade.tachiyomi.core.preference.Preference<T>, crossinline block: (T) -> Boolean) {
        preference.changesIn(viewScope) { isVisible = block(it) }
    }
}
