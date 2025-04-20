package eu.kanade.tachiyomi.ui.base.delegate

import android.app.Activity
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.tachiyomi.R
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

interface ThemingDelegate {
    fun applyAppTheme(activity: Activity)

    companion object {
        fun getThemeResIds(appTheme: AppTheme, isAmoled: Boolean): List<Int> {
            return buildList(2) {
                add(themeResources.getOrDefault(appTheme, R.style.Theme_Tachiyomi))
                if (isAmoled) add(R.style.ThemeOverlay_Tachiyomi_Amoled)
            }
        }
    }
}

class ThemingDelegateImpl : ThemingDelegate {
    override fun applyAppTheme(activity: Activity) {
        val uiPreferences = Injekt.get<UiPreferences>()
        ThemingDelegate.getThemeResIds(uiPreferences.appTheme().get(), uiPreferences.themeDarkAmoled().get())
            .forEach(activity::setTheme)
    }
}

private val themeResources: Map<AppTheme, Int> = mapOf(
    AppTheme.MONET to R.style.Theme_Tachiyomi_Monet,
    AppTheme.ZINC to R.style.Theme_Tachiyomi_Zinc,
    AppTheme.EMERALD to R.style.Theme_Tachiyomi_Emerald,
    AppTheme.ROSE to R.style.Theme_Tachiyomi_Rose,
    AppTheme.VIOLET to R.style.Theme_Tachiyomi_Violet,
    AppTheme.MONOCHROME to R.style.Theme_Tachiyomi_Monochrome,
    AppTheme.GREEN_APPLE to R.style.Theme_Tachiyomi_GreenApple,
    AppTheme.LAVENDER to R.style.Theme_Tachiyomi_Lavender,
    AppTheme.MIDNIGHT_DUSK to R.style.Theme_Tachiyomi_MidnightDusk,
    AppTheme.NORD to R.style.Theme_Tachiyomi_Nord,
    AppTheme.STRAWBERRY to R.style.Theme_Tachiyomi_Strawberry,
    AppTheme.TAKO to R.style.Theme_Tachiyomi_Tako,
    AppTheme.TEALTURQUOISE to R.style.Theme_Tachiyomi_TealTurquoise,
    AppTheme.TIDAL_WAVE to R.style.Theme_Tachiyomi_TidalWave,
    AppTheme.YINYANG to R.style.Theme_Tachiyomi_YinYang,
    AppTheme.YOTSUBA to R.style.Theme_Tachiyomi_Yotsuba,
    // Deprecated but still used in the code
    AppTheme.DARK_BLUE to R.style.Theme_Tachiyomi,
    AppTheme.HOT_PINK to R.style.Theme_Tachiyomi,
    AppTheme.BLUE to R.style.Theme_Tachiyomi,
    // SY
    AppTheme.PURE_RED to R.style.Theme_Tachiyomi,
)
