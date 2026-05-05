package hayai.novel.reader.settings

import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.ui.settings.ReaderPreferences
import yokai.presentation.theme.YokaiTheme

/**
 * Standalone bottom sheet that hosts only the regex find-and-replace editor.
 *
 * Triggered from the in-reader action bar's "Find & Replace" button so users can edit
 * rules without first navigating into the main settings sheet's Advanced tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelFindReplaceSheet(
    preferences: ReaderPreferences,
    onDismissRequest: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        // RegexReplacementSection already provides its own header row; suppress the
        // drag handle and any default top padding so the title sits flush with the top.
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
        ) {
            RegexReplacementSection(preferences)
        }
    }
}

fun showNovelFindReplaceSheet(activity: ReaderActivity) {
    val preferences: ReaderPreferences = Injekt.get()
    val rootView = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
    val composeView = ComposeView(activity)
    composeView.setContent {
        YokaiTheme {
            NovelFindReplaceSheet(
                preferences = preferences,
                onDismissRequest = {
                    (composeView.parent as? ViewGroup)?.removeView(composeView)
                },
            )
        }
    }
    rootView.addView(composeView)
}
