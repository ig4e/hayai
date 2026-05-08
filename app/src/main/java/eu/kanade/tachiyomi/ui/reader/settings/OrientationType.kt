package eu.kanade.tachiyomi.ui.reader.settings

import android.content.pm.ActivityInfo
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import yokai.i18n.MR
import yokai.util.lang.getString
import dev.icerock.moko.resources.compose.stringResource

private const val SHIFT = 0x00000003

enum class OrientationType(val prefValue: Int, val flag: Int, val stringRes: StringResource, @DrawableRes val iconRes: Int) {
    DEFAULT(0, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, MR.strings.default_value, R.drawable.ic_screen_rotation_24dp),
    FREE(1, ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED, MR.strings.free, R.drawable.ic_screen_rotation_24dp),
    PORTRAIT(2, ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT, MR.strings.portrait, R.drawable.ic_stay_current_portrait_24dp),
    LANDSCAPE(3, ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE, MR.strings.landscape, R.drawable.ic_stay_current_landscape_24dp),
    LOCKED_PORTRAIT(4, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT, MR.strings.locked_portrait, R.drawable.ic_screen_lock_portrait_24dp),
    LOCKED_LANDSCAPE(5, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, MR.strings.locked_landscape, R.drawable.ic_screen_lock_landscape_24dp),
    // Locked upside-down. Sensor PORTRAIT already auto-rotates between portrait and reverse,
    // so this entry covers the "force the screen flipped" case the sensor mode doesn't.
    REVERSE_PORTRAIT(6, ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT, MR.strings.reverse_portrait, R.drawable.ic_screen_lock_portrait_24dp),
    ;

    val flagValue = prefValue shl SHIFT

    companion object {
        const val MASK = 7 shl SHIFT

        fun fromPreference(preference: Int): OrientationType =
            entries.find { it.flagValue == preference } ?: FREE

        fun fromSpinner(position: Int?) = entries.find { value -> value.prefValue == position } ?: DEFAULT
    }
}
