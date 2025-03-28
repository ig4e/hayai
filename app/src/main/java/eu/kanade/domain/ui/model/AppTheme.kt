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

    // Deprecated
    DARK_BLUE(null),
    HOT_PINK(null),
    BLUE(null),

    // SY -->
    PURE_RED(null),
    // SY <--
}
