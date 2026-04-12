package exh.ui.smartsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.util.compose.LocalBackPress
import eu.kanade.tachiyomi.util.compose.LocalRouter
import eu.kanade.tachiyomi.util.compose.currentOrThrow
import eu.kanade.tachiyomi.util.system.toast
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import yokai.i18n.MR
import yokai.presentation.AppBarType
import yokai.presentation.YokaiScaffold
import yokai.util.Screen

class SmartSearchScreen(
    private val sourceId: Long,
    private val origTitle: String,
) : Screen() {

    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { SmartSearchScreenModel(sourceId, origTitle) }
        val onBackPress = LocalBackPress.currentOrThrow
        val context = LocalContext.current
        val router = LocalRouter.current
        val state by screenModel.state.collectAsState()

        LaunchedEffect(state) {
            val results = state
            if (results != null) {
                when (results) {
                    is SmartSearchScreenModel.SearchResults.Found -> {
                        val manga = results.manga
                        context.toast(MR.strings.entry_found)
                        if (router != null && manga.id != null) {
                            router.pushController(
                                MangaDetailsController(manga, fromCatalogue = true)
                                    .withFadeTransaction(),
                            )
                        }
                    }
                    is SmartSearchScreenModel.SearchResults.NotFound -> {
                        context.toast(MR.strings.could_not_find_entry)
                        onBackPress()
                    }
                    is SmartSearchScreenModel.SearchResults.Error -> {
                        context.toast(MR.strings.automatic_search_error)
                        onBackPress()
                    }
                }
            }
        }

        YokaiScaffold(
            onNavigationIconClicked = onBackPress,
            title = screenModel.source.name,
            appBarType = AppBarType.SMALL,
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
            ) {
                Text(
                    text = stringResource(MR.strings.searching_source),
                    style = MaterialTheme.typography.titleLarge,
                )
                CircularProgressIndicator(modifier = Modifier.size(56.dp))
            }
        }
    }
}
