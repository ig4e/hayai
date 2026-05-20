package eu.kanade.tachiyomi.ui.base.controller

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.LocalDialogHostState
import eu.kanade.tachiyomi.util.compose.LocalRouter
import yokai.domain.DialogHostState
import yokai.presentation.theme.YokaiTheme

abstract class BaseComposeController(bundle: Bundle? = null) :
    BaseController(bundle) {

    override val shouldHideLegacyAppBar = true

    var navigator: cafe.adriel.voyager.navigator.Navigator? = null

    override fun handleBack(): Boolean {
        val nav = navigator
        return if (nav != null && nav.canPop) {
            nav.pop()
            true
        } else {
            super.handleBack()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup,
        savedViewState: Bundle?
    ): View {
        setAppBarVisibility()
        return ComposeView(container.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val dialogHostState = remember { DialogHostState() }
                YokaiTheme {
                    CompositionLocalProvider(
                        LocalDialogHostState provides dialogHostState,
                        // handleBack() only reports whether back was *consumed* (e.g. by
                        // popping a nested Voyager Navigator). For a root Compose screen it
                        // returns false and does NOT pop the controller — so a toolbar back
                        // button wired straight to ::handleBack would silently no-op. Route
                        // unconsumed presses through onBackPressedDispatcher so the activity
                        // tears down the controller the same way a system back gesture does.
                        LocalBackPress provides {
                            if (!handleBack()) {
                                (activity as? ComponentActivity)?.onBackPressedDispatcher?.onBackPressed()
                            }
                        },
                        LocalRouter provides router,
                    ) {
                        ScreenContent()
                    }
                }
            }
        }
    }

    @Composable
    abstract fun ScreenContent()

    override fun onDestroyView(view: View) {
        if (view is ComposeView) {
            view.disposeComposition()
        }
        navigator = null
        super.onDestroyView(view)
    }
}
