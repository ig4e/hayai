package eu.kanade.tachiyomi.ui.library.compose

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.view.isGone
import androidx.core.view.isVisible
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import kotlinx.collections.immutable.toImmutableList
import eu.kanade.tachiyomi.databinding.LibraryControllerBinding
import eu.kanade.tachiyomi.ui.base.controller.BaseCoroutineController
import eu.kanade.tachiyomi.ui.library.models.LibraryItem
import eu.kanade.tachiyomi.ui.main.BottomSheetController
import eu.kanade.tachiyomi.ui.main.FloatingSearchInterface
import eu.kanade.tachiyomi.ui.main.RootSearchInterface
import java.util.Locale
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.ui.UiPreferences
import yokai.i18n.MR
import yokai.presentation.library.LibraryContent
import yokai.presentation.theme.YokaiTheme
import yokai.util.lang.getString

class LibraryComposeController(
    bundle: Bundle? = null,
    val uiPreferences: UiPreferences = Injekt.get(),
    val preferences: PreferencesHelper = Injekt.get(),
) : BaseCoroutineController<LibraryControllerBinding, LibraryComposePresenter>(bundle) ,
    BottomSheetController,
    RootSearchInterface,
    FloatingSearchInterface {

    override fun getTitle(): String? {
        return view?.context?.getString(MR.strings.library)
    }

    override fun getSearchTitle(): String? {
        val searchSuggestion by lazy { preferences.librarySearchSuggestion().get() }

        return searchTitle(
            if (preferences.showLibrarySearchSuggestions().get() && searchSuggestion.isNotBlank()) {
                "\"$searchSuggestion\""
            } else {
                view?.context?.getString(MR.strings.your_library)?.lowercase(Locale.ROOT)
            },
        )
    }

    override val presenter = LibraryComposePresenter()

    override fun createBinding(inflater: LayoutInflater) = LibraryControllerBinding.inflate(inflater)

    override fun onViewCreated(view: View) {
        super.onViewCreated(view)

        binding.composeView.isVisible = true
        binding.swipeRefresh.isGone = true
        binding.fastScroller.isGone = true

        binding.composeView.setContent {
            YokaiTheme {
                ScreenContent()
            }
        }
    }

    @Composable
    fun ScreenContent() {
        val nestedScrollInterop = rememberNestedScrollInteropConnection()

        val state by presenter.state.collectAsState()
        LibraryContent(
            modifier = Modifier.nestedScroll(nestedScrollInterop),
            items = (0..50).map { LibraryItem.Blank(it) }.toImmutableList(),
            columns = 3,
        )
    }

    override fun showSheet() {
    }

    override fun hideSheet() {
    }

    override fun toggleSheet() {
    }
}
