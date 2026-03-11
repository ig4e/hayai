package eu.kanade.presentation.more.settings.screen

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.base.BasePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.tachiyomi.data.cache.ChapterCache
import eu.kanade.tachiyomi.data.download.DownloadCache
import eu.kanade.tachiyomi.data.library.LibraryUpdateJob
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.NetworkPreferences
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.system.isOutdated
import eu.kanade.tachiyomi.util.system.powerManager
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import tachiyomi.core.common.i18n.stringResource as coreStringResource
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.history.interactor.RemoveHistory
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.manga.interactor.ResetViewerFlags
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Advanced settings screen for technical and diagnostic preferences.
 * Includes data cleanup, network diagnostics, and beta channel enrollment.
 */
object SettingsAdvancedScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = tachiyomi.i18n.MR.strings.pref_category_advanced

    @Composable
    override fun getPreferences(): List<Preference> {
        val scope = rememberCoroutineScope()
        val context = LocalContext.current

        val basePreferences = remember { Injekt.get<BasePreferences>() }
        val networkPreferences = remember { Injekt.get<NetworkPreferences>() }
        val libraryPreferences = remember { Injekt.get<LibraryPreferences>() }
        val downloadPreferences = remember { Injekt.get<DownloadPreferences>() }

        return listOf(
            Preference.PreferenceItem.SwitchPreference(
                preference = basePreferences.crashReport(),
                title = stringResource(tachiyomi.i18n.MR.strings.send_crash_report),
                subtitle = stringResource(tachiyomi.i18n.MR.strings.helps_fix_bugs),
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(tachiyomi.i18n.MR.strings.pref_dump_crash_logs),
                subtitle = stringResource(tachiyomi.i18n.MR.strings.pref_dump_crash_logs_summary),
                onClick = {
                    scope.launch {
                        CrashLogUtil(context).dumpLogs()
                    }
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(tachiyomi.i18n.MR.strings.pref_clear_chapter_cache),
                subtitle = stringResource(tachiyomi.i18n.MR.strings.used_cache),
                onClick = {
                    val chapterCache = Injekt.get<ChapterCache>()
                    val filesCleared = chapterCache.clear()
                    context.toast(context.coreStringResource(tachiyomi.i18n.MR.strings.cache_deleted, filesCleared))
                },
            ),
            Preference.PreferenceItem.TextPreference(
                title = stringResource(tachiyomi.i18n.MR.strings.pref_invalidate_download_cache),
                subtitle = stringResource(tachiyomi.i18n.MR.strings.pref_invalidate_download_cache_summary),
                onClick = {
                    val downloadCache = Injekt.get<DownloadCache>()
                    scope.launchIO {
                        downloadCache.invalidateCache()
                        withUIContext {
                            context.toast(tachiyomi.i18n.MR.strings.download_cache_invalidated)
                        }
                    }
                },
            ),
            getCheckForBeta(basePreferences),
            getBackgroundActivityGroup(),
            getDataGroup(scope),
            getNetworkGroup(networkPreferences = networkPreferences),
            getLibraryGroup(libraryPreferences = libraryPreferences),
            getDownloadsGroup(downloadPreferences = downloadPreferences),
        )
    }

    /**
     * Beta enrollment group with warning dialog.
     */
    @Composable
    private fun getCheckForBeta(basePreferences: BasePreferences): Preference {
        val pref = basePreferences.checkForBetas()
        var showWarningDialog by remember { mutableStateOf<Boolean?>(null) }

        if (showWarningDialog != null) {
            val enroll = showWarningDialog!!
            AlertDialog(
                onDismissRequest = { showWarningDialog = null },
                title = { Text(text = stringResource(tachiyomi.i18n.MR.strings.label_warning)) },
                text = {
                    Text(
                        text = stringResource(
                            if (enroll) tachiyomi.i18n.MR.strings.warning_enroll_into_beta else tachiyomi.i18n.MR.strings.warning_unenroll_from_beta,
                        ),
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pref.set(enroll)
                            showWarningDialog = null
                        },
                    ) {
                        Text(text = stringResource(tachiyomi.i18n.MR.strings.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWarningDialog = null }) {
                        Text(text = stringResource(tachiyomi.i18n.MR.strings.action_cancel))
                    }
                },
            )
        }

        return Preference.PreferenceItem.SwitchPreference(
            preference = pref,
            title = stringResource(tachiyomi.i18n.MR.strings.check_for_beta_releases),
            subtitle = stringResource(tachiyomi.i18n.MR.strings.try_new_features),
            onValueChanged = {
                if (it != eu.kanade.tachiyomi.BuildConfig.IS_BETA) {
                    showWarningDialog = it
                    false
                } else {
                    true
                }
            },
        )
    }

    /**
     * Background activity management group.
     */
    @Composable
    private fun getBackgroundActivityGroup(): Preference.PreferenceGroup {
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current

        return Preference.PreferenceGroup(
            title = stringResource(tachiyomi.i18n.MR.strings.label_background_activity),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(tachiyomi.i18n.MR.strings.pref_disable_battery_optimization),
                    subtitle = stringResource(tachiyomi.i18n.MR.strings.pref_disable_battery_optimization_summary),
                    onClick = {
                        val packageName = context.packageName
                        val intent = Intent().apply {
                            action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                            data = android.net.Uri.parse("package:$packageName")
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            context.toast(tachiyomi.i18n.MR.strings.battery_optimization_setting_activity_not_found)
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(tachiyomi.i18n.MR.strings.onboarding_permission_ignore_battery_opts),
                    subtitle = stringResource(tachiyomi.i18n.MR.strings.onboarding_permission_ignore_battery_opts_description),
                    onClick = { uriHandler.openUri("https://dontkillmyapp.com/") },
                ),
            ),
        )
    }

    /**
     * Data management group for clearing flags and cache.
     */
    @Composable
    private fun getDataGroup(scope: kotlinx.coroutines.CoroutineScope): Preference.PreferenceGroup {
        val context = LocalContext.current

        return Preference.PreferenceGroup(
            title = stringResource(tachiyomi.i18n.MR.strings.label_data),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(tachiyomi.i18n.MR.strings.pref_invalidate_download_cache),
                    subtitle = stringResource(tachiyomi.i18n.MR.strings.pref_invalidate_download_cache_summary),
                    onClick = {
                        val downloadCache = Injekt.get<DownloadCache>()
                        scope.launchIO {
                            downloadCache.invalidateCache()
                            withUIContext {
                                context.toast(tachiyomi.i18n.MR.strings.download_cache_invalidated)
                            }
                        }
                    },
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(tachiyomi.i18n.MR.strings.pref_clear_history),
                    subtitle = stringResource(tachiyomi.i18n.MR.strings.clear_history_confirmation),
                    onClick = {
                        scope.launchIO {
                            val removeHistory = Injekt.get<RemoveHistory>()
                            val success = removeHistory.awaitAll()
                            withUIContext {
                                if (success) {
                                    context.toast(tachiyomi.i18n.MR.strings.clear_history_completed)
                                }
                            }
                        }
                    },
                ),
            ),
        )
    }

    /**
     * Network group for cookie management and logging.
     */
    @Composable
    private fun getNetworkGroup(networkPreferences: NetworkPreferences): Preference.PreferenceGroup {
        val context = LocalContext.current
        val networkHelper = remember { Injekt.get<NetworkHelper>() }

        return Preference.PreferenceGroup(
            title = stringResource(tachiyomi.i18n.MR.strings.label_network),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(tachiyomi.i18n.MR.strings.pref_clear_cookies),
                    onClick = {
                        networkHelper.cookieJar.removeAll()
                        context.toast(tachiyomi.i18n.MR.strings.cookies_cleared)
                    },
                ),
                Preference.PreferenceItem.SwitchPreference(
                    preference = networkPreferences.verboseLogging(),
                    title = stringResource(tachiyomi.i18n.MR.strings.pref_verbose_logging),
                    subtitle = stringResource(tachiyomi.i18n.MR.strings.pref_verbose_logging_summary),
                    onValueChanged = {
                        context.toast(tachiyomi.i18n.MR.strings.requires_app_restart)
                        true
                    },
                ),
            ),
        )
    }

    /**
     * Library group for metadata refresh.
     */
    @Composable
    private fun getLibraryGroup(libraryPreferences: LibraryPreferences): Preference.PreferenceGroup {
        val context = LocalContext.current

        return Preference.PreferenceGroup(
            title = stringResource(tachiyomi.i18n.MR.strings.label_library),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(tachiyomi.i18n.MR.strings.pref_library_update_refresh_metadata),
                    subtitle = stringResource(tachiyomi.i18n.MR.strings.pref_library_update_refresh_metadata_summary),
                    onClick = {
                        LibraryUpdateJob.startNow(context, target = LibraryUpdateJob.Target.CHAPTERS)
                    },
                ),
            ),
        )
    }

    /**
     * Downloads group for connectivity restrictions.
     */
    @Composable
    private fun getDownloadsGroup(downloadPreferences: DownloadPreferences): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(tachiyomi.i18n.MR.strings.pref_category_downloads),
            preferenceItems = persistentListOf(
                Preference.PreferenceItem.SwitchPreference(
                    preference = downloadPreferences.downloadOnlyOverWifi(),
                    title = stringResource(tachiyomi.i18n.MR.strings.connected_to_wifi),
                ),
            ),
        )
    }
}
