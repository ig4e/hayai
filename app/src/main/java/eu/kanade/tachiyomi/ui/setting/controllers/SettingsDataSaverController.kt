package eu.kanade.tachiyomi.ui.setting.controllers

import eu.kanade.tachiyomi.ui.setting.SettingsComposeController
import exh.ui.settings.SettingsDataSaverScreen
import yokai.presentation.settings.ComposableSettings

class SettingsDataSaverController : SettingsComposeController() {
    override fun getComposableSettings(): ComposableSettings = SettingsDataSaverScreen
}
