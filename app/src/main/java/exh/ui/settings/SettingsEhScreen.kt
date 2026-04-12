package exh.ui.settings

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.core.storage.preference.collectAsState
import eu.kanade.tachiyomi.ui.webview.WebViewActivity
import eu.kanade.tachiyomi.util.system.toast
import exh.eh.EHentaiUpdateWorker
import exh.eh.EHentaiUpdateWorkerConstants
import exh.eh.EHentaiUpdaterStats
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.source.EXH_SOURCE_ID
import exh.pref.DelegateSourcePreferences
import exh.source.ExhPreferences
import exh.ui.login.EhLoginActivity
import exh.util.nullIfBlank
import exh.util.toAnnotatedString
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.i18n.MR
import yokai.presentation.component.preference.Preference
import yokai.util.lang.getString
import yokai.presentation.settings.ComposableSettings
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object SettingsEhScreen : ComposableSettings() {

    private fun readResolve(): Any = SettingsEhScreen

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes(): StringResource = MR.strings.pref_category_eh

    @Composable
    override fun getPreferences(): List<Preference> {
        val exhPreferences: ExhPreferences = remember { Injekt.get() }
        val exhentaiEnabled by exhPreferences.enableExhentai.collectAsState()
        var runConfigureDialog by remember { mutableStateOf(false) }
        val openWarnConfigureDialogController = { runConfigureDialog = true }

        Reconfigure(exhPreferences, openWarnConfigureDialogController)

        ConfigureExhDialog(run = runConfigureDialog, onRunning = { runConfigureDialog = false })

        return listOf(
            getAccountSettingsGroup(exhPreferences, exhentaiEnabled, openWarnConfigureDialogController),
            getFavoritesSyncGroup(exhPreferences),
            getGalleryUpdateCheckerGroup(exhPreferences),
        )
    }

    @Composable
    private fun Reconfigure(
        exhPreferences: ExhPreferences,
        openWarnConfigureDialogController: () -> Unit,
    ) {
        val delegateSourcePreferences: DelegateSourcePreferences = remember { Injekt.get() }
        var initialLoadGuard by remember { mutableStateOf(false) }
        val useHentaiAtHome by exhPreferences.useHentaiAtHome.collectAsState()
        val useJapaneseTitle by delegateSourcePreferences.useJapaneseTitle.collectAsState()
        val useOriginalImages by exhPreferences.exhUseOriginalImages.collectAsState()
        val ehTagFilterValue by exhPreferences.ehTagFilterValue.collectAsState()
        val ehTagWatchingValue by exhPreferences.ehTagWatchingValue.collectAsState()
        val settingsLanguages by exhPreferences.exhSettingsLanguages.collectAsState()
        val enabledCategories by exhPreferences.exhEnabledCategories.collectAsState()
        val imageQuality by exhPreferences.imageQuality.collectAsState()
        DisposableEffect(
            useHentaiAtHome,
            useJapaneseTitle,
            useOriginalImages,
            ehTagFilterValue,
            ehTagWatchingValue,
            settingsLanguages,
            enabledCategories,
            imageQuality,
        ) {
            if (initialLoadGuard) {
                openWarnConfigureDialogController()
            }
            initialLoadGuard = true
            onDispose {}
        }
    }

    // region Account Settings Group

    @Composable
    private fun getAccountSettingsGroup(
        exhPreferences: ExhPreferences,
        exhentaiEnabled: Boolean,
        openWarnConfigureDialogController: () -> Unit,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.ehentai_prefs_account_settings),
            preferenceItems = persistentListOf(
                getLoginPreference(exhPreferences, openWarnConfigureDialogController),
                getUseHentaiAtHome(exhentaiEnabled, exhPreferences),
                getUseJapaneseTitle(exhentaiEnabled),
                getUseOriginalImages(exhentaiEnabled, exhPreferences),
                getWatchedTags(exhentaiEnabled),
                getTagFilterThreshold(exhentaiEnabled, exhPreferences),
                getTagWatchingThreshold(exhentaiEnabled, exhPreferences),
                getSettingsLanguages(exhentaiEnabled, exhPreferences),
                getEnabledCategories(exhentaiEnabled, exhPreferences),
                getWatchedListDefaultState(exhentaiEnabled, exhPreferences),
                getImageQuality(exhentaiEnabled, exhPreferences),
                getEnhancedEhentaiView(exhPreferences),
            ),
        )
    }

    @Composable
    private fun getLoginPreference(
        exhPreferences: ExhPreferences,
        openWarnConfigureDialogController: () -> Unit,
    ): Preference.PreferenceItem.SwitchPreference {
        val activityResultContract =
            rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    openWarnConfigureDialogController()
                }
            }
        val context = LocalContext.current
        val value by exhPreferences.enableExhentai.collectAsState()
        return Preference.PreferenceItem.SwitchPreference(
            pref = exhPreferences.enableExhentai,
            title = stringResource(MR.strings.enable_exhentai),
            subtitle = if (!value) {
                stringResource(MR.strings.requires_login)
            } else {
                null
            },
            onValueChanged = { newVal ->
                if (!newVal) {
                    exhPreferences.enableExhentai.set(false)
                    true
                } else {
                    activityResultContract.launch(EhLoginActivity.newIntent(context))
                    false
                }
            },
        )
    }

    @Composable
    private fun getUseHentaiAtHome(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.ListPreference<Int> {
        return Preference.PreferenceItem.ListPreference(
            pref = exhPreferences.useHentaiAtHome,
            title = stringResource(MR.strings.use_hentai_at_home),
            subtitle = stringResource(MR.strings.use_hentai_at_home_summary),
            entries = persistentMapOf(
                0 to stringResource(MR.strings.use_hentai_at_home_option_1),
                1 to stringResource(MR.strings.use_hentai_at_home_option_2),
            ),
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    private fun getUseJapaneseTitle(
        exhentaiEnabled: Boolean,
    ): Preference.PreferenceItem.SwitchPreference {
        val delegateSourcePreferences: DelegateSourcePreferences = remember { Injekt.get() }
        val value by delegateSourcePreferences.useJapaneseTitle.collectAsState()
        return Preference.PreferenceItem.SwitchPreference(
            pref = delegateSourcePreferences.useJapaneseTitle,
            title = stringResource(MR.strings.show_japanese_titles),
            subtitle = if (value) {
                stringResource(MR.strings.show_japanese_titles_option_1)
            } else {
                stringResource(MR.strings.show_japanese_titles_option_2)
            },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    private fun getUseOriginalImages(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        val value by exhPreferences.exhUseOriginalImages.collectAsState()
        return Preference.PreferenceItem.SwitchPreference(
            pref = exhPreferences.exhUseOriginalImages,
            title = stringResource(MR.strings.use_original_images),
            subtitle = if (value) {
                stringResource(MR.strings.use_original_images_on)
            } else {
                stringResource(MR.strings.use_original_images_off)
            },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    private fun getWatchedTags(exhentaiEnabled: Boolean): Preference.PreferenceItem.TextPreference {
        val context = LocalContext.current
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.watched_tags),
            subtitle = stringResource(MR.strings.watched_tags_summary),
            onClick = {
                context.startActivity(
                    WebViewActivity.newIntent(
                        context,
                        url = "https://exhentai.org/mytags",
                        sourceId = EXH_SOURCE_ID,
                        title = context.getString(MR.strings.watched_tags_exh),
                    ),
                )
            },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    private fun getTagFilterThreshold(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.TextPreference {
        val value by exhPreferences.ehTagFilterValue.collectAsState()
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            TagThresholdDialog(
                onDismissRequest = { dialogOpen = false },
                title = stringResource(MR.strings.tag_filtering_threshold),
                initialValue = value,
                valueRange = -9999..0,
                outsideRangeError = stringResource(MR.strings.tag_filtering_threshhold_error),
                onValueChange = {
                    dialogOpen = false
                    exhPreferences.ehTagFilterValue.set(it)
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.tag_filtering_threshold),
            subtitle = stringResource(MR.strings.tag_filtering_threshhold_summary),
            onClick = { dialogOpen = true },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    private fun getTagWatchingThreshold(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.TextPreference {
        val value by exhPreferences.ehTagWatchingValue.collectAsState()
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            TagThresholdDialog(
                onDismissRequest = { dialogOpen = false },
                title = stringResource(MR.strings.tag_watching_threshhold),
                initialValue = value,
                valueRange = 0..9999,
                outsideRangeError = stringResource(MR.strings.tag_watching_threshhold_error),
                onValueChange = {
                    dialogOpen = false
                    exhPreferences.ehTagWatchingValue.set(it)
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.tag_watching_threshhold),
            subtitle = stringResource(MR.strings.tag_watching_threshhold_summary),
            onClick = { dialogOpen = true },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    private fun getSettingsLanguages(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.TextPreference {
        val value by exhPreferences.exhSettingsLanguages.collectAsState()
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            LanguagesDialog(
                onDismissRequest = { dialogOpen = false },
                initialValue = value,
                onValueChange = {
                    dialogOpen = false
                    exhPreferences.exhSettingsLanguages.set(it)
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.language_filtering),
            subtitle = stringResource(MR.strings.language_filtering_summary),
            onClick = { dialogOpen = true },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    private fun getEnabledCategories(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.TextPreference {
        val value by exhPreferences.exhEnabledCategories.collectAsState()
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            FrontPageCategoriesDialog(
                onDismissRequest = { dialogOpen = false },
                initialValue = value,
                onValueChange = {
                    dialogOpen = false
                    exhPreferences.exhEnabledCategories.set(it)
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.front_page_categories),
            subtitle = stringResource(MR.strings.front_page_categories_summary),
            onClick = { dialogOpen = true },
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    private fun getWatchedListDefaultState(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        return Preference.PreferenceItem.SwitchPreference(
            pref = exhPreferences.exhWatchedListDefaultState,
            title = stringResource(MR.strings.watched_list_default),
            subtitle = stringResource(MR.strings.watched_list_state_summary),
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    private fun getImageQuality(
        exhentaiEnabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.ListPreference<String> {
        return Preference.PreferenceItem.ListPreference(
            pref = exhPreferences.imageQuality,
            title = stringResource(MR.strings.eh_image_quality),
            subtitle = stringResource(MR.strings.eh_image_quality_summary),
            entries = persistentMapOf(
                "auto" to stringResource(MR.strings.eh_image_quality_auto),
                "ovrs_2400" to stringResource(MR.strings.eh_image_quality_2400),
                "ovrs_1600" to stringResource(MR.strings.eh_image_quality_1600),
                "high" to stringResource(MR.strings.eh_image_quality_1280),
                "med" to stringResource(MR.strings.eh_image_quality_980),
                "low" to stringResource(MR.strings.eh_image_quality_780),
            ),
            enabled = exhentaiEnabled,
        )
    }

    @Composable
    private fun getEnhancedEhentaiView(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        return Preference.PreferenceItem.SwitchPreference(
            pref = exhPreferences.enhancedEHentaiView,
            title = stringResource(MR.strings.pref_enhanced_e_hentai_view),
            subtitle = stringResource(MR.strings.pref_enhanced_e_hentai_view_summary),
        )
    }

    // endregion

    // region Favorites Sync Group

    @Composable
    private fun getFavoritesSyncGroup(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.favorites_sync),
            preferenceItems = persistentListOf(
                getReadOnlySync(exhPreferences),
                getSyncFavoriteNotes(),
                getLenientSync(exhPreferences),
                getForceSyncReset(),
            ),
        )
    }

    @Composable
    private fun getReadOnlySync(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        return Preference.PreferenceItem.SwitchPreference(
            pref = exhPreferences.exhReadOnlySync,
            title = stringResource(MR.strings.disable_favorites_uploading),
            subtitle = stringResource(MR.strings.disable_favorites_uploading_summary),
        )
    }

    @Composable
    private fun getSyncFavoriteNotes(): Preference.PreferenceItem.TextPreference {
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            SyncFavoritesWarningDialog(
                onDismissRequest = { dialogOpen = false },
                onAccept = { dialogOpen = false },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.show_favorite_sync_notes),
            subtitle = stringResource(MR.strings.show_favorite_sync_notes_summary),
            onClick = { dialogOpen = true },
        )
    }

    @Composable
    private fun SyncFavoritesWarningDialog(
        onDismissRequest: () -> Unit,
        onAccept: () -> Unit,
    ) {
        val context = LocalContext.current
        val text = remember {
            androidx.core.text.HtmlCompat.fromHtml(
                context.getString(MR.strings.favorites_sync_notes_message),
                androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY,
            ).toAnnotatedString()
        }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(onClick = onAccept) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            title = {
                Text(stringResource(MR.strings.favorites_sync_notes))
            },
            text = {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                ) {
                    Text(text = text)
                }
            },
            properties = DialogProperties(dismissOnClickOutside = false),
        )
    }

    @Composable
    private fun getLenientSync(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        return Preference.PreferenceItem.SwitchPreference(
            pref = exhPreferences.exhLenientSync,
            title = stringResource(MR.strings.ignore_sync_errors),
            subtitle = stringResource(MR.strings.ignore_sync_errors_summary),
        )
    }

    @Composable
    private fun getForceSyncReset(): Preference.PreferenceItem.TextPreference {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val ehFavoritesRepository: yokai.domain.manga.EhFavoritesRepository = remember { Injekt.get() }
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            SyncResetDialog(
                onDismissRequest = { dialogOpen = false },
                onStartReset = {
                    dialogOpen = false
                    scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            ehFavoritesRepository.deleteAll()
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                context.toast(MR.strings.sync_state_reset, Toast.LENGTH_LONG)
                            }
                        } catch (e: Exception) {
                            co.touchlab.kermit.Logger.withTag("SettingsEhScreen")
                                .e(e) { "Failed to reset sync state" }
                        }
                    }
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.force_sync_state_reset),
            subtitle = stringResource(MR.strings.force_sync_state_reset_summary),
            onClick = { dialogOpen = true },
        )
    }

    // endregion

    // region Gallery Update Checker Group

    @Composable
    private fun getGalleryUpdateCheckerGroup(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceGroup {
        return Preference.PreferenceGroup(
            title = stringResource(MR.strings.gallery_update_checker),
            preferenceItems = persistentListOf(
                getUpdateCheckerFrequency(exhPreferences),
                getAutoUpdateRequirements(exhPreferences),
                getUpdaterStatistics(exhPreferences),
            ),
        )
    }

    @Composable
    private fun getUpdateCheckerFrequency(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.ListPreference<Int> {
        val value by exhPreferences.exhAutoUpdateFrequency.collectAsState()
        val context = LocalContext.current
        return Preference.PreferenceItem.ListPreference(
            pref = exhPreferences.exhAutoUpdateFrequency,
            title = stringResource(MR.strings.time_between_batches),
            subtitle = if (value == 0) {
                stringResource(MR.strings.time_between_batches_summary_1, stringResource(MR.strings.app_name))
            } else {
                stringResource(
                    MR.strings.time_between_batches_summary_2,
                    stringResource(MR.strings.app_name),
                    value,
                    EHentaiUpdateWorkerConstants.UPDATES_PER_ITERATION,
                )
            },
            entries = persistentMapOf(
                0 to stringResource(MR.strings.time_between_batches_never),
                1 to stringResource(MR.strings.time_between_batches_1_hour),
                2 to stringResource(MR.strings.time_between_batches_2_hours),
                3 to stringResource(MR.strings.time_between_batches_3_hours),
                6 to stringResource(MR.strings.time_between_batches_6_hours),
                12 to stringResource(MR.strings.time_between_batches_12_hours),
                24 to stringResource(MR.strings.time_between_batches_24_hours),
                48 to stringResource(MR.strings.time_between_batches_48_hours),
            ),
            onValueChanged = { interval ->
                EHentaiUpdateWorker.scheduleBackground(context, prefInterval = interval)
                true
            },
        )
    }

    @Composable
    private fun getAutoUpdateRequirements(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.MultiSelectListPreference {
        return Preference.PreferenceItem.MultiSelectListPreference(
            pref = exhPreferences.exhAutoUpdateRequirements,
            title = stringResource(MR.strings.auto_update_restrictions),
            subtitle = stringResource(MR.strings.auto_update_restrictions_summary),
            entries = persistentMapOf(
                "wifi" to stringResource(MR.strings.connected_to_wifi),
                "ac" to stringResource(MR.strings.charging),
            ),
        )
    }

    @Composable
    private fun getUpdaterStatistics(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.TextPreference {
        var dialogOpen by remember { mutableStateOf(false) }
        if (dialogOpen) {
            AlertDialog(
                onDismissRequest = { dialogOpen = false },
                title = { Text(text = stringResource(MR.strings.gallery_updater_statistics)) },
                text = { Text(text = stringResource(MR.strings.gallery_updater_not_ran_yet)) },
                confirmButton = {
                    TextButton(onClick = { dialogOpen = false }) {
                        Text(text = stringResource(MR.strings.action_ok))
                    }
                },
            )
        }
        return Preference.PreferenceItem.TextPreference(
            title = stringResource(MR.strings.show_updater_statistics),
            subtitle = stringResource(MR.strings.show_updater_statistics_summary),
            onClick = { dialogOpen = true },
        )
    }

    // endregion

    // region Dialogs

    @Composable
    private fun TagThresholdDialog(
        onDismissRequest: () -> Unit,
        title: String,
        initialValue: Int,
        valueRange: IntRange,
        outsideRangeError: String,
        onValueChange: (Int) -> Unit,
    ) {
        var value by remember(initialValue) { mutableStateOf(initialValue.toString()) }
        val isValid = remember(value) { value.toIntOrNull().let { it != null && it in valueRange } }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                TextButton(
                    onClick = { onValueChange(value.toIntOrNull() ?: return@TextButton) },
                    enabled = isValid,
                ) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
            title = { Text(text = title) },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        maxLines = 1,
                        singleLine = true,
                        isError = !isValid,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = if (!isValid) {
                            { Icon(Icons.Outlined.Error, outsideRangeError) }
                        } else {
                            null
                        },
                    )
                    if (!isValid) {
                        Text(
                            text = outsideRangeError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    }
                }
            },
        )
    }

    @Composable
    private fun SyncResetDialog(
        onDismissRequest: () -> Unit,
        onStartReset: () -> Unit,
    ) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = stringResource(MR.strings.favorites_sync_reset)) },
            text = { Text(text = stringResource(MR.strings.favorites_sync_reset_message)) },
            confirmButton = {
                TextButton(onClick = onStartReset) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
            ),
        )
    }

    // region Language Dialog

    class LanguageDialogState(preference: String) {
        class RowState(original: ColumnState, translated: ColumnState, rewrite: ColumnState) {
            var original by mutableStateOf(original)
            var translated by mutableStateOf(translated)
            var rewrite by mutableStateOf(rewrite)

            fun toPreference() = "${original.value}*${translated.value}*${rewrite.value}"
        }

        enum class ColumnState(val value: String) {
            Unavailable("false"),
            Enabled("true"),
            Disabled("false"),
        }

        private fun String.toRowState(disableFirst: Boolean = false) = split("*")
            .map {
                if (it.toBoolean()) ColumnState.Enabled else ColumnState.Disabled
            }
            .let {
                if (disableFirst) {
                    RowState(ColumnState.Unavailable, it[1], it[2])
                } else {
                    RowState(it[0], it[1], it[2])
                }
            }

        val japanese: RowState
        val english: RowState
        val chinese: RowState
        val dutch: RowState
        val french: RowState
        val german: RowState
        val hungarian: RowState
        val italian: RowState
        val korean: RowState
        val polish: RowState
        val portuguese: RowState
        val russian: RowState
        val spanish: RowState
        val thai: RowState
        val vietnamese: RowState
        val notAvailable: RowState
        val other: RowState

        init {
            val settingsLanguages = preference.split("\n")
            japanese = settingsLanguages[0].toRowState(true)
            english = settingsLanguages[1].toRowState()
            chinese = settingsLanguages[2].toRowState()
            dutch = settingsLanguages[3].toRowState()
            french = settingsLanguages[4].toRowState()
            german = settingsLanguages[5].toRowState()
            hungarian = settingsLanguages[6].toRowState()
            italian = settingsLanguages[7].toRowState()
            korean = settingsLanguages[8].toRowState()
            polish = settingsLanguages[9].toRowState()
            portuguese = settingsLanguages[10].toRowState()
            russian = settingsLanguages[11].toRowState()
            spanish = settingsLanguages[12].toRowState()
            thai = settingsLanguages[13].toRowState()
            vietnamese = settingsLanguages[14].toRowState()
            notAvailable = settingsLanguages[15].toRowState()
            other = settingsLanguages[16].toRowState()
        }

        fun toPreference() = listOf(
            japanese, english, chinese, dutch, french, german, hungarian,
            italian, korean, polish, portuguese, russian, spanish,
            thai, vietnamese, notAvailable, other,
        ).joinToString("\n") { it.toPreference() }
    }

    @Composable
    private fun LanguageDialogRowCheckbox(
        columnState: LanguageDialogState.ColumnState,
        onStateChange: (LanguageDialogState.ColumnState) -> Unit,
    ) {
        if (columnState != LanguageDialogState.ColumnState.Unavailable) {
            Checkbox(
                checked = columnState == LanguageDialogState.ColumnState.Enabled,
                onCheckedChange = {
                    onStateChange(
                        if (it) LanguageDialogState.ColumnState.Enabled
                        else LanguageDialogState.ColumnState.Disabled,
                    )
                },
            )
        } else {
            Box(modifier = Modifier.size(48.dp))
        }
    }

    @Composable
    private fun LanguageDialogRow(
        language: String,
        row: LanguageDialogState.RowState,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = language,
                modifier = Modifier
                    .padding(4.dp)
                    .width(80.dp),
                maxLines = 1,
            )
            LanguageDialogRowCheckbox(row.original, onStateChange = { row.original = it })
            LanguageDialogRowCheckbox(row.translated, onStateChange = { row.translated = it })
            LanguageDialogRowCheckbox(row.rewrite, onStateChange = { row.rewrite = it })
        }
    }

    @Composable
    private fun LanguagesDialog(
        onDismissRequest: () -> Unit,
        initialValue: String,
        onValueChange: (String) -> Unit,
    ) {
        val state = remember(initialValue) { LanguageDialogState(initialValue) }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(MR.strings.language_filtering)) },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(stringResource(MR.strings.language_filtering_summary))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = stringResource(MR.strings.language), modifier = Modifier.padding(4.dp))
                        Text(text = stringResource(MR.strings.original), modifier = Modifier.padding(4.dp))
                        Text(text = stringResource(MR.strings.translated), modifier = Modifier.padding(4.dp))
                        Text(text = stringResource(MR.strings.rewrite), modifier = Modifier.padding(4.dp))
                    }
                    LanguageDialogRow("Japanese", state.japanese)
                    LanguageDialogRow("English", state.english)
                    LanguageDialogRow("Chinese", state.chinese)
                    LanguageDialogRow("Dutch", state.dutch)
                    LanguageDialogRow("French", state.french)
                    LanguageDialogRow("German", state.german)
                    LanguageDialogRow("Hungarian", state.hungarian)
                    LanguageDialogRow("Italian", state.italian)
                    LanguageDialogRow("Korean", state.korean)
                    LanguageDialogRow("Polish", state.polish)
                    LanguageDialogRow("Portuguese", state.portuguese)
                    LanguageDialogRow("Russian", state.russian)
                    LanguageDialogRow("Spanish", state.spanish)
                    LanguageDialogRow("Thai", state.thai)
                    LanguageDialogRow("Vietnamese", state.vietnamese)
                    LanguageDialogRow("N/A", state.notAvailable)
                    LanguageDialogRow("Other", state.other)
                }
            },
            confirmButton = {
                TextButton(onClick = { onValueChange(state.toPreference()) }) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
        )
    }

    // endregion

    // region Front Page Categories Dialog

    class FrontPageCategoriesDialogState(preference: String) {
        private val enabledCategories = preference.split(",").map { !it.toBoolean() }
        var doujinshi by mutableStateOf(enabledCategories[0])
        var manga by mutableStateOf(enabledCategories[1])
        var artistCg by mutableStateOf(enabledCategories[2])
        var gameCg by mutableStateOf(enabledCategories[3])
        var western by mutableStateOf(enabledCategories[4])
        var nonH by mutableStateOf(enabledCategories[5])
        var imageSet by mutableStateOf(enabledCategories[6])
        var cosplay by mutableStateOf(enabledCategories[7])
        var asianPorn by mutableStateOf(enabledCategories[8])
        var misc by mutableStateOf(enabledCategories[9])

        fun toPreference() = listOf(
            doujinshi, manga, artistCg, gameCg, western,
            nonH, imageSet, cosplay, asianPorn, misc,
        ).joinToString(separator = ",") { (!it).toString() }
    }

    @Composable
    private fun FrontPageCategoriesDialogRow(
        title: String,
        value: Boolean,
        onValueChange: (Boolean) -> Unit,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { onValueChange(!value) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = title)
            Switch(checked = value, onCheckedChange = null)
        }
    }

    @Composable
    private fun FrontPageCategoriesDialog(
        onDismissRequest: () -> Unit,
        initialValue: String,
        onValueChange: (String) -> Unit,
    ) {
        val state = remember(initialValue) { FrontPageCategoriesDialogState(initialValue) }
        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(stringResource(MR.strings.front_page_categories)) },
            text = {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(stringResource(MR.strings.front_page_categories_summary))
                    FrontPageCategoriesDialogRow("Doujinshi", state.doujinshi) { state.doujinshi = it }
                    FrontPageCategoriesDialogRow("Manga", state.manga) { state.manga = it }
                    FrontPageCategoriesDialogRow("Artist CG", state.artistCg) { state.artistCg = it }
                    FrontPageCategoriesDialogRow("Game CG", state.gameCg) { state.gameCg = it }
                    FrontPageCategoriesDialogRow("Western", state.western) { state.western = it }
                    FrontPageCategoriesDialogRow("Non-H", state.nonH) { state.nonH = it }
                    FrontPageCategoriesDialogRow("Image Set", state.imageSet) { state.imageSet = it }
                    FrontPageCategoriesDialogRow("Cosplay", state.cosplay) { state.cosplay = it }
                    FrontPageCategoriesDialogRow("Asian Porn", state.asianPorn) { state.asianPorn = it }
                    FrontPageCategoriesDialogRow("Misc", state.misc) { state.misc = it }
                }
            },
            confirmButton = {
                TextButton(onClick = { onValueChange(state.toPreference()) }) {
                    Text(text = stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(MR.strings.action_cancel))
                }
            },
        )
    }

    // endregion
}
