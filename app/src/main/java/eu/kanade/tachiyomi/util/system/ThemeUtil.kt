package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.content.edit
import androidx.core.view.WindowInsetsControllerCompat
import androidx.preference.PreferenceManager
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceKeys
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBackgroundColor
import yokai.util.koin.injectLazy
import android.graphics.Color as AColor

object ThemeUtil {

    /** Migration method */
    fun convertNewThemes(context: Context) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val lightTheme = prefs.getString(PreferenceKeys.lightTheme, "DEFAULT")
        val darkTheme = prefs.getString(PreferenceKeys.darkTheme, "DEFAULT")

        prefs.edit {
            putString(
                PreferenceKeys.lightTheme,
                when (lightTheme) {
                    "SPRING" -> Themes.SPRING_AND_DUSK
                    "STRAWBERRY_DAIQUIRI" -> Themes.STRAWBERRIES
                    else -> null
                }?.name,
            )
            putString(
                PreferenceKeys.darkTheme,
                when (darkTheme) {
                    "DUSK" -> Themes.SPRING_AND_DUSK
                    "CHOCOLATE_STRAWBERRIES" -> Themes.STRAWBERRIES
                    else -> null
                }?.name,
            )
        }
    }

    fun isPitchBlack(context: Context): Boolean {
        val preferences: PreferencesHelper by injectLazy()
        return context.isInNightMode() && preferences.themeDarkAmoled().get()
    }

    fun readerBackgroundColor(theme: Int, default: Int = AColor.WHITE): Int {
        return when (ReaderBackgroundColor.fromPreference(theme)) {
            ReaderBackgroundColor.GRAY -> AColor.rgb(32, 33, 37)
            ReaderBackgroundColor.BLACK -> AColor.BLACK
            ReaderBackgroundColor.WHITE -> AColor.WHITE
            else -> default
        }
    }

    @Composable
    fun readerContentColor(theme: Int, default: Color = Color.Black): Color {
        return when (ReaderBackgroundColor.fromPreference(theme)) {
            ReaderBackgroundColor.GRAY -> Color.White
            ReaderBackgroundColor.BLACK -> Color.White
            ReaderBackgroundColor.WHITE -> Color.Black
            else -> default
        }
    }
}

fun AppCompatActivity.setThemeByPref(preferences: PreferencesHelper) {
    setTheme(getPrefTheme(preferences).styleRes)
    val wic = WindowInsetsControllerCompat(window, window.decorView)
    wic.isAppearanceLightStatusBars = !isDarkMode(preferences)
    wic.isAppearanceLightNavigationBars = !isDarkMode(preferences)
}

fun AppCompatActivity.getThemeWithExtras(theme: Resources.Theme, preferences: PreferencesHelper, oldTheme: Resources.Theme?): Resources.Theme {
    val useAmoled = isDarkMode(preferences) && preferences.themeDarkAmoled().get()
    if (oldTheme != null && useAmoled) {
        val array = oldTheme.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
        val bg = array.getColor(0, 0)
        if (bg == AColor.BLACK) {
            return oldTheme
        }
    }
    if (useAmoled) {
        theme.applyStyle(R.style.ThemeOverlay_Tachiyomi_Amoled, true)
    }
    return theme
}

fun Context.isDarkMode(preferences: PreferencesHelper) =
    applicationContext.isInNightMode() || preferences.nightMode().get() == AppCompatDelegate.MODE_NIGHT_YES

fun Context.getPrefTheme(preferences: PreferencesHelper): Themes {
    // Using a try catch in case I start to remove themes
    return try {
        (
            if (isDarkMode(preferences) && preferences.nightMode().get() != AppCompatDelegate.MODE_NIGHT_NO) {
                preferences.darkTheme()
            } else {
                preferences.lightTheme()
            }
            ).get()
    } catch (e: Exception) {
        ThemeUtil.convertNewThemes(preferences.context)
        getPrefTheme(preferences)
    }
}
