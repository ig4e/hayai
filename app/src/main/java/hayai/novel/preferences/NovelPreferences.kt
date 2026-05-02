package hayai.novel.preferences

import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import hayai.novel.theme.NovelTheme
import hayai.novel.theme.NovelThemeRegistry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Novel-feature preferences introduced by the polish/themes/auto-scroll/TTS work.
 *
 * Registered in [yokai.core.di.PreferenceModule] as a Koin singleton; resolve via
 * `Injekt.get<NovelPreferences>()` or `by injectLazy()`.
 *
 * Existing legacy novel preferences (`novelFontSize`, `novelLineHeight`, etc.) intentionally
 * stay on [eu.kanade.tachiyomi.data.preference.PreferencesHelper] for now to avoid touching
 * shared code; this class is the home for everything new.
 */
class NovelPreferences(private val preferenceStore: PreferenceStore) {

    // Phase 1A — micro-polish
    fun fontWeight() = preferenceStore.getInt(NovelPreferenceKeys.fontWeight, DEFAULT_FONT_WEIGHT)
    fun paragraphSpacing() = preferenceStore.getInt(NovelPreferenceKeys.paragraphSpacing, DEFAULT_PARAGRAPH_SPACING_DP)

    // Phase 1B — custom themes
    /** ID of the currently-selected theme; matches a built-in or a user-defined custom theme. */
    fun selectedThemeId() = preferenceStore.getString(NovelPreferenceKeys.selectedThemeId, NovelThemeRegistry.DEFAULT_ID)

    /** User-defined themes; persisted as JSON. The list may be empty. No size cap. */
    fun customThemes(): Preference<List<NovelTheme>> = preferenceStore.getObject(
        key = NovelPreferenceKeys.customThemes,
        defaultValue = emptyList<NovelTheme>(),
        serializer = { themes -> themesJson.encodeToString(themesSerializer, themes) },
        deserializer = { raw ->
            runCatching { themesJson.decodeFromString(themesSerializer, raw) }.getOrElse { emptyList() }
        },
    )

    // Phase 2 — auto-scroll
    fun autoScrollSpeed() = preferenceStore.getInt(NovelPreferenceKeys.autoScrollSpeed, DEFAULT_AUTO_SCROLL_SPEED_PX_PER_SEC)
    fun autoScrollPauseOnTap() = preferenceStore.getBoolean(NovelPreferenceKeys.autoScrollPauseOnTap, true)

    // Phase 3 / 4 — TTS (system engine only)
    /** System TTS voice id (`Voice.getName()`); empty falls back to the engine's default voice. */
    fun ttsVoiceId() = preferenceStore.getString(NovelPreferenceKeys.ttsVoiceId, "")

    fun ttsSpeed() = preferenceStore.getFloat(NovelPreferenceKeys.ttsSpeed, DEFAULT_TTS_SPEED)
    fun ttsPitch() = preferenceStore.getFloat(NovelPreferenceKeys.ttsPitch, DEFAULT_TTS_PITCH)
    fun ttsVolume() = preferenceStore.getFloat(NovelPreferenceKeys.ttsVolume, DEFAULT_TTS_VOLUME)
    fun ttsHighlight() = preferenceStore.getBoolean(NovelPreferenceKeys.ttsHighlight, true)
    fun ttsContinuous() = preferenceStore.getBoolean(NovelPreferenceKeys.ttsContinuous, true)
    fun ttsSentencePauseMs() = preferenceStore.getInt(NovelPreferenceKeys.ttsSentencePauseMs, DEFAULT_TTS_SENTENCE_PAUSE_MS)

    companion object {
        private val themesJson = Json { ignoreUnknownKeys = true }
        private val themesSerializer = ListSerializer(NovelTheme.serializer())
        const val DEFAULT_FONT_WEIGHT = 400
        const val MIN_FONT_WEIGHT = 100
        const val MAX_FONT_WEIGHT = 900
        const val FONT_WEIGHT_STEP = 100

        const val DEFAULT_PARAGRAPH_SPACING_DP = 12
        const val MIN_PARAGRAPH_SPACING_DP = 0
        const val MAX_PARAGRAPH_SPACING_DP = 32
        const val PARAGRAPH_SPACING_STEP = 2

        // Auto-scroll: bumped speeds substantially after testing — the previous 200 px/sec ceiling
        // was too sluggish for fluent readers, especially at small font sizes.
        const val DEFAULT_AUTO_SCROLL_SPEED_PX_PER_SEC = 120
        const val MIN_AUTO_SCROLL_SPEED_PX_PER_SEC = 20
        const val MAX_AUTO_SCROLL_SPEED_PX_PER_SEC = 1_000
        const val AUTO_SCROLL_SPEED_STEP = 10

        const val DEFAULT_TTS_SPEED = 1.0f
        const val DEFAULT_TTS_PITCH = 1.0f
        const val DEFAULT_TTS_VOLUME = 1.0f
        const val MIN_TTS_RATE = 0.5f
        const val MAX_TTS_RATE = 2.0f
        const val DEFAULT_TTS_SENTENCE_PAUSE_MS = 200
        const val MIN_TTS_SENTENCE_PAUSE_MS = 0
        const val MAX_TTS_SENTENCE_PAUSE_MS = 1000
    }
}
