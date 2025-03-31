package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.MR

enum class AppTheme(val titleRes: StringResource?) {
    DEFAULT(MR.strings.label_default),
    MONET(MR.strings.theme_monet),
    ZINC(MR.strings.theme_zinc),
    EMERALD(MR.strings.theme_emerald),
    ROSE(MR.strings.theme_rose),
    VIOLET(MR.strings.theme_violet),

    // New themes
    GREEN_APPLE(MR.strings.theme_greenapple),
    LAVENDER(MR.strings.theme_lavender),
    MIDNIGHT_DUSK(MR.strings.theme_midnight_dusk),
    NORD(MR.strings.theme_nord),
    STRAWBERRY(MR.strings.theme_strawberry),
    TAKO(MR.strings.theme_tako),
    TEAL_TURQUOISE(MR.strings.theme_teal_turquoise),
    TIDAL_WAVE(MR.strings.theme_tidal_wave),
    YIN_YANG(MR.strings.theme_yin_yang),
    YOTSUBA(MR.strings.theme_yotsuba),
    MONOCHROME(MR.strings.theme_monochrome),
}
