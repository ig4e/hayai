package eu.kanade.tachiyomi.ui.extension

import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.core.preference.minusAssign
import eu.kanade.tachiyomi.core.preference.plusAssign
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.ui.setting.onChange
import eu.kanade.tachiyomi.ui.setting.titleMRes
import eu.kanade.tachiyomi.util.system.LocaleHelper
// NOVEL -->
import hayai.novel.plugin.NovelPluginManager
// NOVEL <--
import uy.kohesive.injekt.injectLazy
import yokai.i18n.MR

class ExtensionFilterController : SettingsLegacyController() {

    private val extensionManager: ExtensionManager by injectLazy()
    // NOVEL -->
    private val novelPluginManager: NovelPluginManager by injectLazy()
    // NOVEL <--

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleMRes = MR.strings.extensions

        val activeLangs = preferences.enabledLanguages().get()

        // Combine extension languages and novel plugin languages (as ISO codes)
        val extensionLangs = extensionManager.availableExtensionsFlow.value.groupBy { it.lang }.keys
        // NOVEL -->
        val novelLangs = novelPluginManager.availablePluginsFlow.value
            .map { NovelPluginManager.langFromLnReaderLang(it.lang) }
            .toSet()
        val availableLangs = (extensionLangs + novelLangs)
            .distinct()
            .sortedWith(compareBy({ it !in activeLangs }, { LocaleHelper.getSourceDisplayName(it, context) }))
        // NOVEL <--

        availableLangs.forEach {
            SwitchPreferenceCompat(context).apply {
                preferenceScreen.addPreference(this)
                title = LocaleHelper.getSourceDisplayName(it, context)
                isPersistent = false
                isChecked = it in activeLangs

                onChange { newValue ->
                    if (newValue as Boolean) {
                        preferences.enabledLanguages() += it
                    } else {
                        preferences.enabledLanguages() -= it
                    }
                    true
                }
            }
        }
    }
}
