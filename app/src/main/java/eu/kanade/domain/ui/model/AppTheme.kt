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
    MONOCHROME(MR.strings.theme_monochrome),
    GREEN_APPLE(MR.strings.theme_green_apple),
    LAVENDER(MR.strings.theme_lavender),
    MIDNIGHT_DUSK(MR.strings.theme_midnight_dusk),
    NORD(MR.strings.theme_nord),
    STRAWBERRY(MR.strings.theme_strawberry),
    TAKO(MR.strings.theme_tako),
    TEALTURQUOISE(MR.strings.theme_tealturquoise),
    TIDAL_WAVE(MR.strings.theme_tidal_wave),
    YINYANG(MR.strings.theme_yinyang),
    YOTSUBA(MR.strings.theme_yotsuba),

    // Deprecated
    DARK_BLUE(null),
    HOT_PINK(null),
    BLUE(null),

    // SY -->
    PURE_RED(null),
    // SY <--
}
