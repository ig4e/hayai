package eu.kanade.presentation.more.settings.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ChromeReaderMode
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import cafe.adriel.voyager.core.screen.Screen as VoyagerScreen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarActions
import eu.kanade.presentation.more.settings.screen.about.AboutScreen
import eu.kanade.presentation.more.settings.widget.PreferenceGroupHeader
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.presentation.util.Screen
import exh.assets.EhAssets
import exh.assets.ehassets.EhLogo
import exh.assets.ehassets.MangadexLogo
import kotlinx.collections.immutable.persistentListOf
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource

object SettingsMainScreen : Screen() {

    @Composable
    override fun Content() {
        Content(twoPane = false)
    }

    @Composable
    private fun getPalerSurface(): Color {
        val surface = MaterialTheme.colorScheme.surface
        val dark = isSystemInDarkTheme()
        return remember(surface, dark) {
            val arr = FloatArray(3)
            ColorUtils.colorToHSL(surface.toArgb(), arr)
            arr[2] = if (dark) {
                arr[2] - 0.05f
            } else {
                arr[2] + 0.02f
            }.coerceIn(0f, 1f)
            Color.hsl(arr[0], arr[1], arr[2])
        }
    }

    @Composable
    fun Content(twoPane: Boolean) {
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.currentOrThrow
        val containerColor = if (twoPane) getPalerSurface() else MaterialTheme.colorScheme.surface
        val topBarState = rememberTopAppBarState()

        Scaffold(
            topBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topBarState),
            topBar = { scrollBehavior ->
                AppBar(
                    title = stringResource(MR.strings.label_settings),
                    navigateUp = backPress::invoke,
                    actions = {
                        AppBarActions(
                            persistentListOf(
                                AppBar.Action(
                                    title = stringResource(MR.strings.action_search),
                                    icon = Icons.Outlined.Search,
                                    onClick = { navigator.navigate(SettingsSearchScreen(), twoPane) },
                                ),
                            ),
                        )
                    },
                    scrollBehavior = scrollBehavior,
                )
            },
            containerColor = containerColor,
            content = { contentPadding ->
                val state = rememberLazyListState()
                val groups = settingsGroups.filterAndNormalize()
                val flattenedItems = groups.flatMap { it.items }
                val indexSelected = if (twoPane) {
                    flattenedItems.indexOfFirst { it.screen::class == navigator.items.first()::class }
                        .also {
                            LaunchedEffect(Unit) {
                                if (it >= 0) {
                                    state.animateScrollToItem(it + 2)
                                }
                                if (it > 0) {
                                    // Lift scroll
                                    topBarState.contentOffset = topBarState.heightOffsetLimit
                                }
                            }
                        }
                } else {
                    null
                }

                LazyColumn(
                    state = state,
                    contentPadding = contentPadding,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    item {
                        SettingsOverviewCard()
                    }

                    groups.forEach { group ->
                        item(key = group.titleRes.resourceId) {
                            PreferenceGroupHeader(title = stringResource(group.titleRes))
                        }
                        items(
                            count = group.items.size,
                            key = { index -> group.items[index].hashCode() },
                        ) { index ->
                            val item = group.items[index]
                            val selected = indexSelected == flattenedItems.indexOf(item)
                            SettingsCard(
                                item = item,
                                selected = selected,
                                onClick = { navigator.navigate(item.screen, twoPane) },
                            )
                        }
                    }
                }
            },
        )
    }

    @Composable
    private fun SettingsCard(
        item: Item,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        val shape = RoundedCornerShape(24.dp)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(shape)
                .clickable(onClick = onClick),
            shape = shape,
            color = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            tonalElevation = if (selected) 0.dp else 2.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (selected) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Column(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(item.titleRes),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = item.formatSubtitle(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }

    @Composable
    private fun SettingsOverviewCard() {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(MR.strings.label_customize_hayai),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(MR.strings.label_customize_hayai_summary),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    private fun Navigator.navigate(screen: VoyagerScreen, twoPane: Boolean) {
        if (twoPane) replaceAll(screen) else push(screen)
    }

    private fun List<Group>.filterAndNormalize(): List<Group> {
        return mapNotNull { group ->
            val filteredItems = group.items.filter { it.screen !is SearchableSettings || it.screen.isEnabled() }
            if (filteredItems.isNotEmpty()) {
                group.copy(items = filteredItems)
            } else {
                null
            }
        }
    }

    private data class Item(
        val titleRes: StringResource,
        val subtitleRes: StringResource,
        val formatSubtitle: @Composable () -> String = { stringResource(subtitleRes) },
        val icon: ImageVector,
        val screen: VoyagerScreen,
    )

    private data class Group(
        val titleRes: StringResource,
        val items: List<Item>,
    )

    private val settingsGroups = listOf(
        Group(
            titleRes = MR.strings.label_settings,
            items = listOf(
                Item(
                    titleRes = MR.strings.pref_category_general,
                    subtitleRes = MR.strings.pressing_back_to_start,
                    icon = Icons.Outlined.Tune,
                    screen = SettingsGeneralScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_appearance,
                    subtitleRes = MR.strings.pref_appearance_summary,
                    icon = Icons.Outlined.Palette,
                    screen = SettingsAppearanceScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_library,
                    subtitleRes = MR.strings.pref_library_summary,
                    icon = Icons.Outlined.CollectionsBookmark,
                    screen = SettingsLibraryScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_reader,
                    subtitleRes = MR.strings.pref_reader_summary,
                    icon = Icons.AutoMirrored.Outlined.ChromeReaderMode,
                    screen = SettingsReaderScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_downloads,
                    subtitleRes = MR.strings.pref_downloads_summary,
                    icon = Icons.Outlined.GetApp,
                    screen = SettingsDownloadScreen,
                ),
                Item(
                    titleRes = MR.strings.browse,
                    subtitleRes = MR.strings.pref_browse_summary,
                    icon = Icons.Outlined.Explore,
                    screen = SettingsBrowseScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_tracking,
                    subtitleRes = MR.strings.pref_tracking_summary,
                    icon = Icons.Outlined.Sync,
                    screen = SettingsTrackingScreen,
                ),
                Item(
                    titleRes = MR.strings.label_data_storage,
                    subtitleRes = MR.strings.pref_backup_summary,
                    icon = Icons.Outlined.Storage,
                    screen = SettingsDataScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_security,
                    subtitleRes = MR.strings.pref_security_summary,
                    icon = Icons.Outlined.Security,
                    screen = SettingsSecurityScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_advanced,
                    subtitleRes = MR.strings.pref_advanced_summary,
                    icon = Icons.Outlined.Code,
                    screen = SettingsAdvancedScreen,
                ),
                Item(
                    titleRes = MR.strings.pref_category_about,
                    subtitleRes = StringResource(0),
                    formatSubtitle = {
                        "${stringResource(MR.strings.app_name)} ${AboutScreen.getVersionName(withBuildDate = false)}"
                    },
                    icon = Icons.Outlined.Info,
                    screen = AboutScreen,
                ),
            ),
        ),
        Group(
            titleRes = MR.strings.services,
            items = listOf(
                Item(
                    titleRes = SYMR.strings.pref_category_eh,
                    subtitleRes = SYMR.strings.pref_ehentai_summary,
                    icon = EhAssets.EhLogo,
                    screen = SettingsEhScreen,
                ),
                Item(
                    titleRes = SYMR.strings.pref_category_mangadex,
                    subtitleRes = SYMR.strings.pref_mangadex_summary,
                    icon = EhAssets.MangadexLogo,
                    screen = SettingsMangadexScreen,
                ),
            ),
        ),
    )
}
