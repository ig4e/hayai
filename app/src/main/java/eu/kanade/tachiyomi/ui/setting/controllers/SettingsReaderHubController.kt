package eu.kanade.tachiyomi.ui.setting.controllers

import androidx.preference.PreferenceScreen
import com.bluelinelabs.conductor.Controller
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.ui.setting.onClick
import eu.kanade.tachiyomi.ui.setting.preference
import yokai.i18n.MR
import eu.kanade.tachiyomi.ui.setting.summaryMRes as summaryRes
import eu.kanade.tachiyomi.ui.setting.titleMRes as titleRes

/**
 * Reader settings hub. Splits Reader into "Manga reader" and "Novel reader" sub-screens —
 * mirrors Tsundoku's split so manga-only options never appear in the novel reader and vice
 * versa. Each leaf-screen owns its own preference set; this hub does no logic of its own.
 */
class SettingsReaderHubController : SettingsLegacyController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = MR.strings.reader

        preference {
            titleRes = MR.strings.manga_reader
            summaryRes = MR.strings.manga_reader_settings_summary
            onClick { navigateTo(SettingsReaderController()) }
        }

        preference {
            titleRes = MR.strings.novel_reader
            summaryRes = MR.strings.novel_reader_settings_summary
            onClick { navigateTo(SettingsNovelReaderController()) }
        }
    }

    private fun navigateTo(controller: Controller) {
        router.pushController(controller.withFadeTransaction())
    }
}
