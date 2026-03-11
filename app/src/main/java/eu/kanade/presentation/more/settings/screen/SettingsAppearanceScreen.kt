package eu.kanade.presentation.more.settings.screen

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.TabletUiMode
import eu.kanade.domain.ui.model.ThemeMode
import eu.kanade.domain.ui.model.setAppCompatDelegateThemeMode
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.widget.AppThemeModePreferenceWidget
import eu.kanade.presentation.more.settings.widget.AppThemePreferenceWidget
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableMap
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

object SettingsAppearanceScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.pref_category_appearance

    @Composable
    override fun getPreferences(): List<Preference> {
        val uiPreferences = remember { Injekt.get<UiPreferences>() }

        return listOf(
            getThemeGroup(uiPreferences = uiPreferences),
            getToolbarGroup(uiPreferences = uiPreferences),
            getDetailsGroup(uiPreferences = uiPreferences),
            getNavigationGroup(uiPreferences = uiPreferences),
            // SY -->
            getForkGroup(uiPreferences = uiPreferences),
            // SY <--
        )
    }

    @Composable
    private fun getThemeGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current

        val themeModePref = uiPreferences.themeMode()
        val themeMode by themeModePref.collectAsState()

        val appThemePref = uiPreferences.appTheme()
        val appTheme by appThemePref.collectAsState()

        val amoledPref = uiPreferences.themeDarkAmoled()
        val amoled by amoledPref.collectAsState()

        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.pref_category_theme),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.CustomPreference(
                    title = stringResource(MR.strings.pref_app_theme),
                ) {
                    Column {
                        AppThemeModePreferenceWidget(
                            value = themeMode,
                            onItemClick = {
                                themeModePref.set(it)
                                setAppCompatDelegateThemeMode(it)
                            },
                        )

                        AppThemePreferenceWidget(
                            value = appTheme,
                            amoled = amoled,
                            onItemClick = { appThemePref.set(it) },
                        )
                    }
                },
                Preference.PreferenceItem.SwitchPreference(
                    preference = amoledPref,
                    title = stringResource(MR.strings.pref_dark_theme_pure_black),
                    enabled = themeMode != ThemeMode.LIGHT,
                    onValueChanged = {
                        (context as? Activity)?.let { ActivityCompat.recreate(it) }
                        true
                    },
                ),
            ),
        )
    }

    @Composable
    private fun getToolbarGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.toolbar),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.expandedAppBars(),
                    title = stringResource(MR.strings.pref_expanded_app_bars),
                    subtitle = stringResource(MR.strings.pref_expanded_app_bars_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.floatingSearchBars(),
                    title = stringResource(MR.strings.pref_floating_search_bars),
                    subtitle = stringResource(MR.strings.pref_floating_search_bars_summary),
                ),
            ),
        )
    }

    @Composable
    private fun getDetailsGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.manga),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.coverThemedMangaDetails(),
                    title = stringResource(MR.strings.pref_cover_themed_manga_details),
                    subtitle = stringResource(MR.strings.pref_cover_themed_manga_details_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.imagesInDescription(),
                    title = stringResource(MR.strings.pref_display_images_description),
                ),
            ),
        )
    }

    @Composable
    private fun getNavigationGroup(
        uiPreferences: UiPreferences,
    ): Preference.PreferenceGroup {
        val context = LocalContext.current
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.navigation),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.hideBottomNavOnScroll(),
                    title = stringResource(MR.strings.hide_bottom_nav),
                    subtitle = stringResource(MR.strings.hides_on_scroll),
                ),
                Preference.PreferenceItem.ListPreference(
                    preference = uiPreferences.tabletUiMode(),
                    entries = TabletUiMode.entries
                        .associateWith { stringResource(it.titleRes) }
                        .toImmutableMap(),
                    title = stringResource(MR.strings.pref_tablet_ui_mode),
                    onValueChanged = {
                        context.toast(MR.strings.requires_app_restart)
                        true
                    },
                ),
                // SY -->
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.bottomBarLabels(),
                    title = stringResource(SYMR.strings.pref_show_bottom_bar_labels),
                ),
                // SY <--
            ),
        )
    }

    // SY -->
    @Composable
    fun getForkGroup(uiPreferences: UiPreferences): Preference.PreferenceGroup {
        val previewsRowCount by uiPreferences.previewsRowCount().collectAsState()

        return Preference.PreferenceGroup(
            stringResource(SYMR.strings.pref_category_fork),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.expandFilters(),
                    title = stringResource(SYMR.strings.toggle_expand_search_filters),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.recommendsInOverflow(),
                    title = stringResource(SYMR.strings.put_recommends_in_overflow),
                    subtitle = stringResource(SYMR.strings.put_recommends_in_overflow_summary),
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = uiPreferences.mergeInOverflow(),
                    title = stringResource(SYMR.strings.put_merge_in_overflow),
                    subtitle = stringResource(SYMR.strings.put_merge_in_overflow_summary),
                ),
                Preference.PreferenceItem.SliderPreference(
                    value = previewsRowCount,
                    title = stringResource(SYMR.strings.pref_previews_row_count),
                    subtitle = if (previewsRowCount > 0) {
                        pluralStringResource(
                            SYMR.plurals.row_count,
                            previewsRowCount,
                            previewsRowCount,
                        )
                    } else {
                        stringResource(MR.strings.disabled)
                    },
                    valueRange = 0..10,
                    onValueChanged = {
                        uiPreferences.previewsRowCount().set(it)
                        true
                    },
                ),
            ),
        )
    }
    // SY <--
}
