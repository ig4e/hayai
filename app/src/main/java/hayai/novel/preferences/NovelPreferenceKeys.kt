package hayai.novel.preferences

/**
 * Preference keys owned by the novel reader feature drop. Kept under [hayai.novel] so we don't
 * pollute [eu.kanade.tachiyomi.data.preference.PreferenceKeys] with novel-specific entries.
 *
 * Existing legacy novel keys (`novel_font_size`, `novel_font_family`, `novel_line_height`,
 * `novel_text_align`, `novel_padding`, `novel_reader_theme`) still live on the shared
 * [eu.kanade.tachiyomi.data.preference.PreferencesHelper] and continue to use their original
 * key strings; everything new for this work goes here.
 */
object NovelPreferenceKeys {
    // Phase 1A — micro-polish
    const val fontWeight = "novel_font_weight"
    const val paragraphSpacing = "novel_paragraph_spacing"

    // Phase 1B — custom themes
    const val selectedThemeId = "novel_selected_theme_id"
    const val customThemes = "novel_custom_themes"

    // Phase 2 — auto-scroll
    const val autoScrollSpeed = "novel_auto_scroll_speed"
    const val autoScrollPauseOnTap = "novel_auto_scroll_pause_on_tap"

    // Text-to-speech
    const val ttsVoiceId = "novel_tts_voice_id"
    const val ttsSpeed = "novel_tts_speed"
    const val ttsPitch = "novel_tts_pitch"
    const val ttsVolume = "novel_tts_volume"
    const val ttsHighlight = "novel_tts_highlight"
    const val ttsContinuous = "novel_tts_continuous"
    const val ttsSentencePauseMs = "novel_tts_sentence_pause_ms"
}
