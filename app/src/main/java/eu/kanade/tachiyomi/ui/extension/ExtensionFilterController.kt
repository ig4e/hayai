package eu.kanade.tachiyomi.ui.extension

import android.view.View
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy
import yokai.i18n.MR

class ExtensionFilterController : SettingsLegacyController() {

    private val extensionManager: ExtensionManager by injectLazy()
    // NOVEL -->
    private val novelPluginManager: NovelPluginManager by injectLazy()
    // NOVEL <--

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleMRes = MR.strings.extensions
        renderLanguages()
    }

    override fun onAttach(view: View) {
        super.onAttach(view)

        // If no extension/novel data has been fetched yet, kick the loaders so the screen
        // doesn't stay empty when the user opens it before the bottom-sheet ever loaded
        // available extensions/plugins.
        if (extensionManager.availableExtensionsFlow.value.isEmpty()) {
            viewScope.launch { extensionManager.findAvailableExtensions() }
        }
        if (novelPluginManager.availablePluginsFlow.value.isEmpty()) {
            viewScope.launch { novelPluginManager.refreshAvailablePlugins() }
        }

        // Re-render whenever either source of languages updates so the screen reflects the
        // latest data even if it arrived after onCreatePreferences ran.
        combine(
            extensionManager.availableExtensionsFlow,
            novelPluginManager.availablePluginsFlow,
        ) { _, _ -> Unit }
            .drop(1)
            .onEach { renderLanguages() }
            .launchIn(viewScope)
    }

    private fun renderLanguages() {
        val screen = preferenceScreen ?: return
        val ctx = screen.context
        screen.removeAll()

        val activeLangs = preferences.enabledLanguages().get()

        // Combine extension languages and novel plugin languages (as ISO codes)
        val extensionLangs = extensionManager.availableExtensionsFlow.value.groupBy { it.lang }.keys
        // NOVEL -->
        val novelLangs = novelPluginManager.availablePluginsFlow.value
            .map { NovelPluginManager.langFromLnReaderLang(it.lang) }
            .toSet()
        val availableLangs = (extensionLangs + novelLangs)
            .distinct()
            .sortedWith(compareBy({ it !in activeLangs }, { LocaleHelper.getSourceDisplayName(it, ctx) }))
        // NOVEL <--

        availableLangs.forEach { lang ->
            SwitchPreferenceCompat(ctx).apply {
                screen.addPreference(this)
                title = LocaleHelper.getSourceDisplayName(lang, ctx)
                isPersistent = false
                isChecked = lang in activeLangs

                onChange { newValue ->
                    if (newValue as Boolean) {
                        preferences.enabledLanguages() += lang
                    } else {
                        preferences.enabledLanguages() -= lang
                    }
                    true
                }
            }
        }
    }
}
