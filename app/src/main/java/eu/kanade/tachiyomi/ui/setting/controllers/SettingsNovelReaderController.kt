package eu.kanade.tachiyomi.ui.setting.controllers

import yokai.util.koin.get
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.ui.setting.bindTo
import eu.kanade.tachiyomi.ui.setting.defaultValue
import eu.kanade.tachiyomi.ui.setting.infoPreference
import eu.kanade.tachiyomi.ui.setting.listPreference
import eu.kanade.tachiyomi.ui.setting.onChange
import eu.kanade.tachiyomi.ui.setting.preferenceCategory
import eu.kanade.tachiyomi.ui.setting.seekBarPreference
import eu.kanade.tachiyomi.ui.setting.switchPreference
import yokai.domain.ui.settings.ReaderPreferences
import yokai.i18n.MR
import yokai.util.lang.getString
import eu.kanade.tachiyomi.ui.setting.summaryMRes as summaryRes
import eu.kanade.tachiyomi.ui.setting.titleMRes as titleRes

/**
 * Global novel reader settings, mirroring Tsundoku's split: Display, Text, Formatting,
 * Navigation, Auto-Scroll, Content, TTS. Advanced/CSS/JS/Regex/bottom-bar editing live
 * in the in-reader sheet (live preview against the chapter), not here.
 *
 * The actual preference DSL is delegated to the file-level
 * [populateNovelReaderPreferences] extension so the new
 * [SettingsReaderHubController] tabbed wrapper can invoke it against its own
 * attached [activity]/[preferences]/[viewScope] without this controller having
 * to be attached itself.
 */
class SettingsNovelReaderController : SettingsLegacyController() {

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = MR.strings.novel_reader
        populateNovelReaderPreferences(this)
    }
}

fun SettingsLegacyController.populateNovelReaderPreferences(screen: PreferenceScreen) {
    val readerPreferences: ReaderPreferences = get()

    screen.apply {
        preferenceCategory {
            titleRes = MR.strings.display

            // Rendering mode toggle removed: novel reading is WebView-only after the
            // viewer consolidation. The native NovelViewer was deleted; no fallback exists.

            listPreference(activity) {
                bindTo(readerPreferences.novelTheme)
                titleRes = MR.strings.novel_reader_theme
                entries = listOf(
                    context.getString(MR.strings.novel_theme_app),
                    context.getString(MR.strings.novel_theme_light),
                    context.getString(MR.strings.novel_theme_dark),
                    context.getString(MR.strings.novel_theme_sepia),
                    context.getString(MR.strings.novel_theme_black),
                )
                entryValues = listOf("app", "light", "dark", "sepia", "black")
            }

            switchPreference {
                bindTo(readerPreferences.novelKeepScreenOn)
                titleRes = MR.strings.keep_screen_on
            }

            switchPreference {
                bindTo(readerPreferences.novelBlockMedia)
                titleRes = MR.strings.novel_block_media
                summaryRes = MR.strings.novel_block_media_summary
            }
        }

        preferenceCategory {
            titleRes = MR.strings.novel_text

            seekBarPreference {
                bindTo(readerPreferences.novelFontSize)
                titleRes = MR.strings.novel_font_size
                min = 8
                max = 32
                showSeekBarValue = true
            }

            listPreference(activity) {
                bindTo(readerPreferences.novelFontFamily)
                titleRes = MR.strings.novel_font_family
                entries = listOf(
                    "Default",
                    "Sans Serif",
                    "Serif",
                    "Monospace",
                )
                entryValues = listOf("default", "sans-serif", "serif", "monospace")
            }

            switchPreference {
                bindTo(readerPreferences.novelUseOriginalFonts)
                titleRes = MR.strings.novel_use_original_fonts
                summaryRes = MR.strings.novel_use_original_fonts_summary
            }

            listPreference(activity) {
                bindTo(readerPreferences.novelTextAlign)
                titleRes = MR.strings.novel_text_align
                entries = listOf(
                    context.getString(MR.strings.novel_text_align_left),
                    context.getString(MR.strings.novel_text_align_center),
                    context.getString(MR.strings.novel_text_align_justify),
                )
                entryValues = listOf("left", "center", "justify")
            }
        }

        preferenceCategory {
            titleRes = MR.strings.novel_formatting

            switchPreference {
                bindTo(readerPreferences.novelHideChapterTitle)
                titleRes = MR.strings.novel_hide_chapter_title
            }

            switchPreference {
                bindTo(readerPreferences.novelForceTextLowercase)
                titleRes = MR.strings.novel_force_lowercase
            }

            switchPreference {
                bindTo(readerPreferences.novelAutoSplitText)
                titleRes = MR.strings.novel_auto_split_text
                summaryRes = MR.strings.novel_auto_split_text_summary
            }
        }

        preferenceCategory {
            titleRes = MR.strings.navigation

            switchPreference {
                bindTo(readerPreferences.novelVolumeKeysScroll)
                titleRes = MR.strings.novel_volume_keys_scroll
            }

            switchPreference {
                bindTo(readerPreferences.novelTapToScroll)
                titleRes = MR.strings.novel_tap_to_scroll
            }

            switchPreference {
                bindTo(readerPreferences.novelSwipeNavigation)
                titleRes = MR.strings.novel_swipe_navigation
            }

            switchPreference {
                bindTo(readerPreferences.novelTextSelectable)
                titleRes = MR.strings.novel_text_selectable
            }

            switchPreference {
                bindTo(readerPreferences.novelShowProgressSlider)
                titleRes = MR.strings.novel_show_progress_slider
            }

            listPreference(activity) {
                bindTo(readerPreferences.novelProgressSliderPosition)
                titleRes = MR.strings.novel_progress_slider_position
                entries = listOf(
                    context.getString(MR.strings.novel_progress_slider_position_top_left),
                    context.getString(MR.strings.novel_progress_slider_position_top_center),
                    context.getString(MR.strings.novel_progress_slider_position_top_right),
                    context.getString(MR.strings.novel_progress_slider_position_center_left),
                    context.getString(MR.strings.novel_progress_slider_position_center_center),
                    context.getString(MR.strings.novel_progress_slider_position_center_right),
                    context.getString(MR.strings.novel_progress_slider_position_bottom_left),
                    context.getString(MR.strings.novel_progress_slider_position_bottom_center),
                    context.getString(MR.strings.novel_progress_slider_position_bottom_right),
                )
                entryValues = listOf(
                    "top-left", "top-center", "top-right",
                    "center-left", "center-center", "center-right",
                    "bottom-left", "bottom-center", "bottom-right",
                )
            }
        }

        preferenceCategory {
            titleRes = MR.strings.auto_scroll

            seekBarPreference {
                bindTo(readerPreferences.novelAutoScrollSpeed)
                titleRes = MR.strings.novel_auto_scroll_speed
                min = 1
                max = 10
                showSeekBarValue = true
            }
        }

        preferenceCategory {
            titleRes = MR.strings.novel_content

            switchPreference {
                bindTo(readerPreferences.enableEpubStyles)
                titleRes = MR.strings.novel_enable_epub_styles
            }

            switchPreference {
                bindTo(readerPreferences.enableEpubJs)
                titleRes = MR.strings.novel_enable_epub_js
                summaryRes = MR.strings.novel_enable_epub_js_summary
            }
        }

        preferenceCategory {
            titleRes = MR.strings.text_to_speech

            // Speed/pitch are Float-typed prefs. SeekBarPreference is Int-only and
            // would crash calling getPersistedInt on the Float key, so we run the
            // bar non-persistent in 0.1 steps (5..20 ↔ 0.5x..2.0x) and forward
            // changes to the Float pref ourselves.
            seekBarPreference {
                isPersistent = false
                titleRes = MR.strings.novel_tts_speed
                min = 5
                max = 20
                showSeekBarValue = false
                val initial = (readerPreferences.novelTtsSpeed.get() * 10).toInt().coerceIn(5, 20)
                defaultValue = initial
                summary = "%.1fx".format(initial / 10f)
                onChange { newValue ->
                    val v = (newValue as Int).coerceIn(5, 20)
                    readerPreferences.novelTtsSpeed.set(v / 10f)
                    summary = "%.1fx".format(v / 10f)
                    true
                }
            }

            seekBarPreference {
                isPersistent = false
                titleRes = MR.strings.novel_tts_pitch
                min = 5
                max = 20
                showSeekBarValue = false
                val initial = (readerPreferences.novelTtsPitch.get() * 10).toInt().coerceIn(5, 20)
                defaultValue = initial
                summary = "%.1fx".format(initial / 10f)
                onChange { newValue ->
                    val v = (newValue as Int).coerceIn(5, 20)
                    readerPreferences.novelTtsPitch.set(v / 10f)
                    summary = "%.1fx".format(v / 10f)
                    true
                }
            }

            switchPreference {
                bindTo(readerPreferences.novelTtsAutoNextChapter)
                titleRes = MR.strings.novel_tts_auto_next_chapter
            }

            switchPreference {
                bindTo(readerPreferences.novelTtsEnableHighlight)
                titleRes = MR.strings.novel_tts_enable_highlight
            }

            switchPreference {
                bindTo(readerPreferences.novelTtsKeepHighlightInView)
                titleRes = MR.strings.novel_tts_keep_highlight_in_view
            }

            switchPreference {
                bindTo(readerPreferences.novelTtsBackgroundPlayback)
                titleRes = MR.strings.novel_tts_background_playback
                summaryRes = MR.strings.novel_tts_background_playback_summary
            }

            infoPreference(MR.strings.novel_tts_more_in_reader)
        }
    }
}
