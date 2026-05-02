package hayai.novel.theme.ui

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.screenModelScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.cancel
import yokai.presentation.theme.YokaiTheme

/**
 * In-reader entry point to the theme manager. Presents [NovelThemeManagerContent] inside a
 * [BottomSheetDialog] so it can sit on top of the reader's existing settings sheet without
 * cross-activity navigation.
 *
 * The sheet pre-expands to the full screen height so the LazyColumn + FAB are visible without
 * forcing the user to drag the sheet up first.
 */
object NovelThemeManagerSheet {

    fun show(context: Context) {
        val dialog = BottomSheetDialog(context)
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                YokaiTheme {
                    val screenModel = remember { NovelThemeManagerScreenModel() }
                    DisposableEffect(screenModel) {
                        onDispose {
                            // The Voyager ScreenModelStore is what normally cancels these scopes;
                            // outside Voyager we do it ourselves so the preference-flow collector
                            // started in init {} doesn't leak past the sheet's dismissal.
                            screenModel.screenModelScope.cancel()
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(560.dp),
                    ) {
                        NovelThemeManagerContent(screenModel = screenModel)
                    }
                }
            }
        }
        dialog.setContentView(composeView)
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.behavior.skipCollapsed = true
        dialog.show()
    }
}
