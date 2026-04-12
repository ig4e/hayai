package exh.source

import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.core.preference.PreferenceStore

class ExhPreferences(
    private val preferenceStore: PreferenceStore,
) {

    val isHentaiEnabled: Preference<Boolean> = preferenceStore.getBoolean("eh_is_hentai_enabled", true)

    val enableExhentai: Preference<Boolean> = preferenceStore.getBoolean(Preference.privateKey("enable_exhentai"), false)

    val imageQuality: Preference<String> = preferenceStore.getString("ehentai_quality", "auto")

    val useHentaiAtHome: Preference<Int> = preferenceStore.getInt("eh_enable_hah", 0)

    val exhUseOriginalImages: Preference<Boolean> = preferenceStore.getBoolean("eh_useOrigImages", false)

    val ehTagFilterValue: Preference<Int> = preferenceStore.getInt("eh_tag_filtering_value", 0)

    val ehTagWatchingValue: Preference<Int> = preferenceStore.getInt("eh_tag_watching_value", 0)

    // EH Cookies
    val memberIdVal: Preference<String> = preferenceStore.getString(Preference.privateKey("eh_ipb_member_id"), "")

    val passHashVal: Preference<String> = preferenceStore.getString(Preference.privateKey("eh_ipb_pass_hash"), "")
    val igneousVal: Preference<String> = preferenceStore.getString(Preference.privateKey("eh_igneous"), "")
    val ehSettingsProfile: Preference<Int> = preferenceStore.getInt(Preference.privateKey("eh_ehSettingsProfile"), -1)
    val exhSettingsProfile: Preference<Int> = preferenceStore.getInt(Preference.privateKey("eh_exhSettingsProfile"), -1)
    val exhSettingsKey: Preference<String> = preferenceStore.getString(Preference.privateKey("eh_settingsKey"), "")
    val exhSessionCookie: Preference<String> = preferenceStore.getString(Preference.privateKey("eh_sessionCookie"), "")
    val exhHathPerksCookies: Preference<String> = preferenceStore.getString(Preference.privateKey("eh_hathPerksCookie"), "")

    val exhShowSyncIntro: Preference<Boolean> = preferenceStore.getBoolean("eh_show_sync_intro", true)

    val exhReadOnlySync: Preference<Boolean> = preferenceStore.getBoolean("eh_sync_read_only", false)

    val exhLenientSync: Preference<Boolean> = preferenceStore.getBoolean("eh_lenient_sync", false)

    val exhShowSettingsUploadWarning: Preference<Boolean> = preferenceStore.getBoolean("eh_showSettingsUploadWarning2", true)

    val logLevel: Preference<Int> = preferenceStore.getInt("eh_log_level", 0)

    val exhAutoUpdateFrequency: Preference<Int> = preferenceStore.getInt("eh_auto_update_frequency", 1)

    val exhAutoUpdateRequirements: Preference<Set<String>> = preferenceStore.getStringSet("eh_auto_update_restrictions", emptySet())

    val exhAutoUpdateStats: Preference<String> = preferenceStore.getString("eh_auto_update_stats", "")

    val exhWatchedListDefaultState: Preference<Boolean> = preferenceStore.getBoolean("eh_watched_list_default_state", false)

    val exhSettingsLanguages: Preference<String> = preferenceStore.getString(
        "eh_settings_languages",
        "false*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\n" +
            "false*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\n" +
            "false*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\nfalse*false*false\n" +
            "false*false*false\nfalse*false*false",
    )

    val exhEnabledCategories: Preference<String> = preferenceStore.getString(
        "eh_enabled_categories",
        "false,false,false,false,false,false,false,false,false,false",
    )

    val enhancedEHentaiView: Preference<Boolean> = preferenceStore.getBoolean("enhanced_e_hentai_view", true)

    val recommendationSearchFlags: Preference<Int> = preferenceStore.getInt("rec_search_flags", Int.MAX_VALUE)

    // === Data Saver: Basic ===
    // 0 = Off, 1 = wsrv.nl, 2 = Custom server, 3 = Proxy via wsrv.nl
    val dataSaverMode: Preference<Int> = preferenceStore.getInt("data_saver_mode", 0)
    val dataSaverQuality: Preference<Int> = preferenceStore.getInt("data_saver_quality", 80)
    val dataSaverFormat: Preference<String> = preferenceStore.getString("data_saver_format", "webp")

    // === Data Saver: Resize ===
    val dataSaverMaxWidth: Preference<String> = preferenceStore.getString("data_saver_max_width", "0")
    val dataSaverMaxHeight: Preference<String> = preferenceStore.getString("data_saver_max_height", "0")
    val dataSaverFitMode: Preference<String> = preferenceStore.getString("data_saver_fit", "inside")
    val dataSaverNoUpscale: Preference<Boolean> = preferenceStore.getBoolean("data_saver_no_upscale", true)

    // === Data Saver: Adjustments ===
    val dataSaverBrightness: Preference<Int> = preferenceStore.getInt("data_saver_brightness", 0)
    val dataSaverContrast: Preference<Int> = preferenceStore.getInt("data_saver_contrast", 0)
    val dataSaverSaturation: Preference<Int> = preferenceStore.getInt("data_saver_saturation", 0)
    val dataSaverSharpen: Preference<Int> = preferenceStore.getInt("data_saver_sharpen", 0)

    // === Data Saver: Effects ===
    val dataSaverBlur: Preference<Int> = preferenceStore.getInt("data_saver_blur", 0)
    val dataSaverFilter: Preference<String> = preferenceStore.getString("data_saver_filter", "none")

    // === Data Saver: Server (Custom mode) ===
    val dataSaverServerUrl: Preference<String> = preferenceStore.getString("data_saver_server_url", "")
    val dataSaverApiKey: Preference<String> = preferenceStore.getString(Preference.privateKey("data_saver_api_key"), "")
}
