package exh.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.core.storage.preference.collectAsState
import exh.source.ExhPreferences
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.i18n.MR
import yokai.presentation.component.preference.Preference
import yokai.presentation.settings.ComposableSettings

object SettingsDataSaverScreen : ComposableSettings() {

    private fun readResolve(): Any = SettingsDataSaverScreen

    @Composable
    @ReadOnlyComposable
    override fun getTitleRes(): StringResource = MR.strings.data_saver

    @Composable
    override fun getPreferences(): List<Preference> {
        val exhPreferences: ExhPreferences = remember { Injekt.get() }
        val dataSaverMode by exhPreferences.dataSaverMode.collectAsState()
        val dataSaverEnabled = dataSaverMode != 0
        val isCustomOrProxy = dataSaverMode == 2 || dataSaverMode == 3

        return listOf(
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.data_saver),
                preferenceItems = persistentListOf(
                    getDataSaverMode(exhPreferences),
                    getDataSaverQuality(dataSaverEnabled, exhPreferences),
                    getDataSaverFormat(dataSaverEnabled, exhPreferences),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.data_saver_resize),
                preferenceItems = persistentListOf(
                    getDataSaverMaxWidth(dataSaverEnabled, exhPreferences),
                    getDataSaverMaxHeight(dataSaverEnabled, exhPreferences),
                    getDataSaverFitMode(dataSaverEnabled, exhPreferences),
                    getDataSaverNoUpscale(dataSaverEnabled, exhPreferences),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.data_saver_adjustments),
                preferenceItems = persistentListOf(
                    getDataSaverBrightness(dataSaverEnabled, exhPreferences),
                    getDataSaverContrast(dataSaverEnabled, exhPreferences),
                    getDataSaverSaturation(dataSaverEnabled, exhPreferences),
                    getDataSaverSharpen(dataSaverEnabled, exhPreferences),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.data_saver_effects),
                preferenceItems = persistentListOf(
                    getDataSaverBlur(dataSaverEnabled, exhPreferences),
                    getDataSaverFilter(dataSaverEnabled, exhPreferences),
                ),
            ),
            Preference.PreferenceGroup(
                title = stringResource(MR.strings.data_saver_server),
                preferenceItems = persistentListOf(
                    getDataSaverServerUrl(isCustomOrProxy, exhPreferences),
                    getDataSaverApiKey(isCustomOrProxy, exhPreferences),
                ),
            ),
        )
    }

    @Composable
    private fun getDataSaverMode(
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.ListPreference<Int> {
        return Preference.PreferenceItem.ListPreference(
            pref = exhPreferences.dataSaverMode,
            title = stringResource(MR.strings.data_saver),
            entries = persistentMapOf(
                0 to stringResource(MR.strings.data_saver_off),
                1 to stringResource(MR.strings.data_saver_wsrv),
                2 to stringResource(MR.strings.data_saver_custom),
                3 to stringResource(MR.strings.data_saver_proxy),
            ),
        )
    }

    @Composable
    private fun getDataSaverQuality(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SliderPreference {
        val quality by exhPreferences.dataSaverQuality.collectAsState()
        return Preference.PreferenceItem.SliderPreference(
            value = quality,
            min = 1,
            max = 100,
            title = stringResource(MR.strings.data_saver_quality),
            subtitle = stringResource(MR.strings.data_saver_quality_summary, quality),
            enabled = enabled,
            onValueChanged = {
                exhPreferences.dataSaverQuality.set(it)
                true
            },
        )
    }

    @Composable
    private fun getDataSaverFormat(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.ListPreference<String> {
        return Preference.PreferenceItem.ListPreference(
            pref = exhPreferences.dataSaverFormat,
            title = stringResource(MR.strings.data_saver_format),
            entries = persistentMapOf(
                "webp" to "WebP",
                "avif" to "AVIF",
                "jpeg" to "JPEG",
                "png" to "PNG",
                "original" to stringResource(MR.strings.data_saver_filter_none),
            ),
            enabled = enabled,
        )
    }

    @Composable
    private fun getDataSaverMaxWidth(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.EditTextPreference {
        return Preference.PreferenceItem.EditTextPreference(
            pref = exhPreferences.dataSaverMaxWidth,
            title = stringResource(MR.strings.data_saver_max_width),
            enabled = enabled,
        )
    }

    @Composable
    private fun getDataSaverMaxHeight(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.EditTextPreference {
        return Preference.PreferenceItem.EditTextPreference(
            pref = exhPreferences.dataSaverMaxHeight,
            title = stringResource(MR.strings.data_saver_max_height),
            enabled = enabled,
        )
    }

    @Composable
    private fun getDataSaverFitMode(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.ListPreference<String> {
        return Preference.PreferenceItem.ListPreference(
            pref = exhPreferences.dataSaverFitMode,
            title = stringResource(MR.strings.data_saver_fit_mode),
            entries = persistentMapOf(
                "inside" to stringResource(MR.strings.data_saver_fit_inside),
                "outside" to stringResource(MR.strings.data_saver_fit_outside),
                "cover" to stringResource(MR.strings.data_saver_fit_cover),
                "contain" to stringResource(MR.strings.data_saver_fit_contain),
                "fill" to stringResource(MR.strings.data_saver_fit_fill),
                "scale-down" to stringResource(MR.strings.data_saver_fit_scale_down),
            ),
            enabled = enabled,
        )
    }

    @Composable
    private fun getDataSaverNoUpscale(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SwitchPreference {
        return Preference.PreferenceItem.SwitchPreference(
            pref = exhPreferences.dataSaverNoUpscale,
            title = stringResource(MR.strings.data_saver_no_upscale),
            subtitle = stringResource(MR.strings.data_saver_no_upscale_summary),
            enabled = enabled,
        )
    }

    @Composable
    private fun getDataSaverBrightness(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SliderPreference {
        val brightness by exhPreferences.dataSaverBrightness.collectAsState()
        return Preference.PreferenceItem.SliderPreference(
            value = brightness,
            min = -100,
            max = 100,
            title = stringResource(MR.strings.data_saver_brightness),
            subtitle = stringResource(MR.strings.data_saver_brightness_summary, brightness),
            enabled = enabled,
            onValueChanged = {
                exhPreferences.dataSaverBrightness.set(it)
                true
            },
        )
    }

    @Composable
    private fun getDataSaverContrast(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SliderPreference {
        val contrast by exhPreferences.dataSaverContrast.collectAsState()
        return Preference.PreferenceItem.SliderPreference(
            value = contrast,
            min = -100,
            max = 100,
            title = stringResource(MR.strings.data_saver_contrast),
            subtitle = stringResource(MR.strings.data_saver_contrast_summary, contrast),
            enabled = enabled,
            onValueChanged = {
                exhPreferences.dataSaverContrast.set(it)
                true
            },
        )
    }

    @Composable
    private fun getDataSaverSaturation(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SliderPreference {
        val saturation by exhPreferences.dataSaverSaturation.collectAsState()
        return Preference.PreferenceItem.SliderPreference(
            value = saturation,
            min = -100,
            max = 100,
            title = stringResource(MR.strings.data_saver_saturation),
            subtitle = stringResource(MR.strings.data_saver_saturation_summary, saturation),
            enabled = enabled,
            onValueChanged = {
                exhPreferences.dataSaverSaturation.set(it)
                true
            },
        )
    }

    @Composable
    private fun getDataSaverSharpen(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SliderPreference {
        val sharpen by exhPreferences.dataSaverSharpen.collectAsState()
        return Preference.PreferenceItem.SliderPreference(
            value = sharpen,
            min = 0,
            max = 10,
            title = stringResource(MR.strings.data_saver_sharpen),
            subtitle = stringResource(MR.strings.data_saver_sharpen_summary, sharpen),
            enabled = enabled,
            onValueChanged = {
                exhPreferences.dataSaverSharpen.set(it)
                true
            },
        )
    }

    @Composable
    private fun getDataSaverBlur(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.SliderPreference {
        val blur by exhPreferences.dataSaverBlur.collectAsState()
        return Preference.PreferenceItem.SliderPreference(
            value = blur,
            min = 0,
            max = 100,
            title = stringResource(MR.strings.data_saver_blur),
            subtitle = stringResource(MR.strings.data_saver_blur_summary, blur),
            enabled = enabled,
            onValueChanged = {
                exhPreferences.dataSaverBlur.set(it)
                true
            },
        )
    }

    @Composable
    private fun getDataSaverFilter(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.ListPreference<String> {
        return Preference.PreferenceItem.ListPreference(
            pref = exhPreferences.dataSaverFilter,
            title = stringResource(MR.strings.data_saver_filter),
            entries = persistentMapOf(
                "none" to stringResource(MR.strings.data_saver_filter_none),
                "greyscale" to stringResource(MR.strings.data_saver_filter_greyscale),
                "sepia" to stringResource(MR.strings.data_saver_filter_sepia),
                "negate" to stringResource(MR.strings.data_saver_filter_negate),
            ),
            enabled = enabled,
        )
    }

    @Composable
    private fun getDataSaverServerUrl(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.EditTextPreference {
        return Preference.PreferenceItem.EditTextPreference(
            pref = exhPreferences.dataSaverServerUrl,
            title = stringResource(MR.strings.data_saver_server_url),
            enabled = enabled,
        )
    }

    @Composable
    private fun getDataSaverApiKey(
        enabled: Boolean,
        exhPreferences: ExhPreferences,
    ): Preference.PreferenceItem.EditTextPreference {
        return Preference.PreferenceItem.EditTextPreference(
            pref = exhPreferences.dataSaverApiKey,
            title = stringResource(MR.strings.data_saver_api_key),
            enabled = enabled,
        )
    }
}
