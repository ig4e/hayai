package exh.ui.metadata

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.currentOrThrow
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.util.Screen

class MetadataViewScreen(
    private val mangaId: Long,
    private val sourceId: Long,
) : Screen() {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { MetadataViewScreenModel(mangaId, sourceId) }
        val onBackPress = LocalBackPress.currentOrThrow
        val context = LocalContext.current
        val clipboardManager = LocalClipboardManager.current

        val state by screenModel.state.collectAsState()
        val manga by screenModel.manga.collectAsState()

        YokaiScaffold(
            onNavigationIconClicked = onBackPress,
            title = manga?.title ?: "",
            appBarType = AppBarType.SMALL,
        ) { paddingValues ->
            when (
                @Suppress("NAME_SHADOWING")
                val state = state
            ) {
                MetadataViewState.Loading -> {
                    // Loading state - show nothing or a progress indicator
                }
                MetadataViewState.MetadataNotFound -> {
                    Text(
                        text = stringResource(MR.strings.no_results_found),
                        modifier = Modifier.padding(paddingValues).padding(16.dp),
                    )
                }
                MetadataViewState.SourceNotFound -> {
                    Text(
                        text = "Source not found",
                        modifier = Modifier.padding(paddingValues).padding(16.dp),
                    )
                }
                is MetadataViewState.Success -> {
                    val items = remember(state.meta) { state.meta.getExtraInfoPairs(context) }
                    LazyColumn(
                        contentPadding = paddingValues,
                    ) {
                        items(items) { (title, text) ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            clipboardManager.setText(AnnotatedString(text))
                                        },
                                    )
                                    .padding(vertical = 8.dp),
                            ) {
                                Text(
                                    title,
                                    modifier = Modifier
                                        .width(140.dp)
                                        .padding(start = 16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LocalContentColor.current.copy(alpha = 0.7F),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
