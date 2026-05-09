package eu.kanade.tachiyomi.ui.library.update

import android.os.Bundle
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.CrossfadeTransition
import yokai.presentation.theme.LocalReducedMotion
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import yokai.presentation.library.update.LibraryUpdateReportScreen
import yokai.presentation.library.update.LibraryUpdateReportScreenModel

/**
 * Conductor controller that hosts the Voyager [LibraryUpdateReportScreen]. Pushed both from
 * the library-update notification (via MainActivity.handleIntentAction) and from in-app entry
 * points (Recents overflow, library settings).
 */
class LibraryUpdateReportController(bundle: Bundle? = null) : BaseComposeController(bundle) {

    constructor(initialTab: LibraryUpdateReportScreenModel.ReportTab) : this(
        Bundle().apply { putString(EXTRA_INITIAL_TAB, initialTab.name) },
    )

    private val initialTab: LibraryUpdateReportScreenModel.ReportTab
        get() = args.getString(EXTRA_INITIAL_TAB)
            ?.let { runCatching { LibraryUpdateReportScreenModel.ReportTab.valueOf(it) }.getOrNull() }
            ?: LibraryUpdateReportScreenModel.ReportTab.ERRORS

    @Composable
    override fun ScreenContent() {
        Navigator(
            screen = LibraryUpdateReportScreen(initialTab),
            content = { navigator ->
                if (LocalReducedMotion.current) CurrentScreen()
                else CrossfadeTransition(navigator = navigator)
            },
        )
    }

    companion object {
        const val EXTRA_INITIAL_TAB = "initial_tab"
    }
}
