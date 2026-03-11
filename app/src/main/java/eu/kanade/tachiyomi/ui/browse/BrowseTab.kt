package eu.kanade.tachiyomi.ui.browse

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.core.preference.asState
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.TabContent
import eu.kanade.presentation.browse.components.BrowseTabbedScreen
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.extension.ExtensionsScreenModel
import eu.kanade.tachiyomi.ui.browse.extension.extensionsTab
import eu.kanade.tachiyomi.ui.browse.feed.feedTab
import eu.kanade.tachiyomi.ui.browse.migration.sources.migrateSourceTab
import eu.kanade.tachiyomi.ui.browse.source.sourcesTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

private enum class BrowseRootDestination {
    Feed,
    Sources,
    Extensions,
    Migration,
}

data object BrowseTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_browse_enter)
            return TabOptions(
                index = 2u,
                title = stringResource(MR.strings.browse),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(GlobalSearchScreen())
    }

    private val switchToExtensionTabChannel = Channel<Unit>(1, BufferOverflow.DROP_OLDEST)

    fun showExtension() {
        switchToExtensionTabChannel.trySend(Unit)
    }

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        // SY -->
        val hideFeedTab by remember { Injekt.get<UiPreferences>().hideFeedTab().asState(scope) }
        val feedTabInFront by remember { Injekt.get<UiPreferences>().feedTabInFront().asState(scope) }
        // SY <--

        // Hoisted for extensions tab's search bar
        val extensionsScreenModel = rememberScreenModel { ExtensionsScreenModel() }
        val extensionsState by extensionsScreenModel.state.collectAsState()
        var selectedDestination by rememberSaveable { mutableStateOf(BrowseRootDestination.Sources) }

        val tabs = buildBrowseTabs(
            screen = this,
            hideFeedTab = hideFeedTab,
            feedTabInFront = feedTabInFront,
            extensionsScreenModel = extensionsScreenModel,
        )
        val initialPage = tabs.indexOfFirst { it.destination == selectedDestination }
            .takeIf { it >= 0 }
            ?: 0

        val state = rememberPagerState(initialPage = initialPage) { tabs.size }
        val currentDestination = tabs.getOrNull(state.currentPage)?.destination ?: BrowseRootDestination.Sources
        val extensionIndex = tabs.indexOfFirst { it.destination == BrowseRootDestination.Extensions }

        LaunchedEffect(currentDestination) {
            selectedDestination = currentDestination
        }

        LaunchedEffect(tabs) {
            val targetDestination = selectedDestination
                .takeIf { destination -> tabs.any { it.destination == destination } }
                ?: BrowseRootDestination.Sources
            val targetIndex = tabs.indexOfFirst { it.destination == targetDestination }.coerceAtLeast(0)
            if (state.currentPage != targetIndex) {
                state.scrollToPage(targetIndex)
            }
        }

        BrowseTabbedScreen(
            titleRes = MR.strings.browse,
            tabs = tabs.map { it.content }.toPersistentList(),
            state = state,
            rootActions = persistentListOf(
                AppBar.Action(
                    title = stringResource(MR.strings.action_settings),
                    icon = Icons.Outlined.Settings,
                    onClick = { navigator.push(SettingsScreen()) },
                ),
            ),
        )
        LaunchedEffect(extensionIndex) {
            switchToExtensionTabChannel.receiveAsFlow()
                .collectLatest {
                    if (extensionIndex >= 0) {
                        selectedDestination = BrowseRootDestination.Extensions
                        state.scrollToPage(extensionIndex)
                    }
                }
        }

        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}

private data class BrowseRootTab(
    val destination: BrowseRootDestination,
    val content: TabContent,
)

@Composable
private fun buildBrowseTabs(
    screen: Screen,
    hideFeedTab: Boolean,
    feedTabInFront: Boolean,
    extensionsScreenModel: ExtensionsScreenModel,
): kotlinx.collections.immutable.ImmutableList<BrowseRootTab> {
    return when {
        hideFeedTab -> persistentListOf(
            BrowseRootTab(BrowseRootDestination.Sources, screen.sourcesTab()),
            BrowseRootTab(BrowseRootDestination.Extensions, extensionsTab(extensionsScreenModel)),
            BrowseRootTab(BrowseRootDestination.Migration, screen.migrateSourceTab()),
        )

        feedTabInFront -> persistentListOf(
            BrowseRootTab(BrowseRootDestination.Feed, screen.feedTab()),
            BrowseRootTab(BrowseRootDestination.Sources, screen.sourcesTab()),
            BrowseRootTab(BrowseRootDestination.Extensions, extensionsTab(extensionsScreenModel)),
            BrowseRootTab(BrowseRootDestination.Migration, screen.migrateSourceTab()),
        )

        else -> persistentListOf(
            BrowseRootTab(BrowseRootDestination.Sources, screen.sourcesTab()),
            BrowseRootTab(BrowseRootDestination.Feed, screen.feedTab()),
            BrowseRootTab(BrowseRootDestination.Extensions, extensionsTab(extensionsScreenModel)),
            BrowseRootTab(BrowseRootDestination.Migration, screen.migrateSourceTab()),
        )
    }
}
