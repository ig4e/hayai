package eu.kanade.tachiyomi.ui.setting.controllers

import eu.kanade.tachiyomi.ui.setting.SettingsComposeController
import exh.ui.settings.SettingsEhScreen
import yokai.presentation.settings.ComposableSettings

class SettingsEhController : SettingsComposeController() {
    override fun getComposableSettings(): ComposableSettings = SettingsEhScreen
}
