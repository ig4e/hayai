package hayai.novel.tts.ui

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
 * In-reader entry point to TTS settings. Mirrors the theme manager sheet pattern so the user
 * doesn't bounce out to a full-screen page from inside the reader.
 */
object TtsLaunchSheet {

    fun show(context: Context) {
        val dialog = BottomSheetDialog(context)
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setContent {
                YokaiTheme {
                    val screenModel = remember { TtsSettingsScreenModel() }
                    DisposableEffect(screenModel) {
                        onDispose { screenModel.screenModelScope.cancel() }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(640.dp),
                    ) {
                        TtsSettingsContent(screenModel = screenModel)
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
