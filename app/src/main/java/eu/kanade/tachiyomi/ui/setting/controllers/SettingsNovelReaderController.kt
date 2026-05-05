package eu.kanade.tachiyomi.ui.setting.controllers

import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.ui.setting.bindTo
import eu.kanade.tachiyomi.ui.setting.infoPreference
import eu.kanade.tachiyomi.ui.setting.listPreference
import eu.kanade.tachiyomi.ui.setting.preferenceCategory
import eu.kanade.tachiyomi.ui.setting.seekBarPreference
import eu.kanade.tachiyomi.ui.setting.switchPreference
import uy.kohesive.injekt.injectLazy
import yokai.domain.ui.settings.ReaderPreferences
import yokai.i18n.MR
import yokai.util.lang.getString
import eu.kanade.tachiyomi.ui.setting.summaryMRes as summaryRes
import eu.kanade.tachiyomi.ui.setting.titleMRes as titleRes

/**
 * Global novel reader settings, mirroring Tsundoku's split: Display, Text, Formatting,
 * Navigation, Auto-Scroll, Content, TTS. Advanced/CSS/JS/Regex/bottom-bar editing live
 * in the in-reader sheet (live preview against the chapter), not here.
 */
class SettingsNovelReaderController : SettingsLegacyController() {

    private val readerPreferences: ReaderPreferences by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = MR.strings.novel_reader

        preferenceCategory {
            titleRes = MR.strings.display

            listPreference(activity) {
                bindTo(readerPreferences.novelRenderingMode)
                titleRes = MR.strings.novel_rendering_mode
                entries = listOf(
                    context.getString(MR.strings.novel_rendering_mode_default),
                    context.getString(MR.strings.novel_rendering_mode_webview),
                )
                entryValues = listOf("default", "webview")
            }

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
                bindTo(readerPreferences.novelCustomBrightness)
                titleRes = MR.strings.use_custom_brightness
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

            switchPreference {
                bindTo(readerPreferences.novelInfiniteScroll)
                titleRes = MR.strings.novel_infinite_scroll
                summaryRes = MR.strings.novel_infinite_scroll_summary
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

            seekBarPreference {
                bindTo(readerPreferences.novelAutoLoadNextChapterAt)
                titleRes = MR.strings.novel_auto_load_next_at
                summaryRes = MR.strings.novel_auto_load_next_at_summary
                min = 50
                max = 100
                showSeekBarValue = true
            }

            seekBarPreference {
                bindTo(readerPreferences.novelMarkAsReadThreshold)
                titleRes = MR.strings.novel_mark_read_threshold
                min = 50
                max = 100
                showSeekBarValue = true
            }

            switchPreference {
                bindTo(readerPreferences.novelMarkShortChapterAsRead)
                titleRes = MR.strings.novel_mark_short_chapter_read
            }

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

            seekBarPreference {
                key = readerPreferences.novelTtsSpeed.key()
                titleRes = MR.strings.novel_tts_speed
                // Float pref isn't directly supported by seekBar; expose 5..20 (0.5x..2.0x)
                min = 5
                max = 20
                showSeekBarValue = true
            }

            seekBarPreference {
                key = readerPreferences.novelTtsPitch.key()
                titleRes = MR.strings.novel_tts_pitch
                min = 5
                max = 20
                showSeekBarValue = true
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
