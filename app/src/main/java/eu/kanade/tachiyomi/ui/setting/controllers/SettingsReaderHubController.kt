package eu.kanade.tachiyomi.ui.setting.controllers

import android.content.ComponentName
import android.content.Intent
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferenceValues
import eu.kanade.tachiyomi.data.preference.changesIn
import eu.kanade.tachiyomi.ui.reader.settings.OrientationType
import eu.kanade.tachiyomi.ui.reader.settings.PageLayout
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBackgroundColor
import eu.kanade.tachiyomi.ui.reader.settings.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.settings.ReadingModeType
import eu.kanade.tachiyomi.ui.reader.viewer.ViewerNavigation
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.ui.setting.bindTo
import eu.kanade.tachiyomi.ui.setting.defaultValue
import eu.kanade.tachiyomi.ui.setting.infoPreference
import eu.kanade.tachiyomi.ui.setting.intListPreference
import eu.kanade.tachiyomi.ui.setting.listPreference
import eu.kanade.tachiyomi.ui.setting.multiSelectListPreferenceMat
import eu.kanade.tachiyomi.ui.setting.onClick
import eu.kanade.tachiyomi.ui.setting.preference
import eu.kanade.tachiyomi.ui.setting.preferenceCategory
import eu.kanade.tachiyomi.ui.setting.seekBarPreference
import eu.kanade.tachiyomi.ui.setting.switchPreference
import eu.kanade.tachiyomi.util.lang.addBetaTag
import eu.kanade.tachiyomi.util.system.DeviceUtil
import eu.kanade.tachiyomi.util.system.isTablet
import eu.kanade.tachiyomi.util.view.activityBinding
import uy.kohesive.injekt.injectLazy
import yokai.domain.ui.settings.ReaderPreferences
import yokai.domain.ui.settings.ReaderPreferences.CutoutBehaviour
import yokai.domain.ui.settings.ReaderPreferences.LandscapeCutoutBehaviour
import yokai.i18n.MR
import yokai.util.lang.getString
import eu.kanade.tachiyomi.data.preference.PreferenceKeys as Keys
import eu.kanade.tachiyomi.ui.setting.summaryMRes as summaryRes
import eu.kanade.tachiyomi.ui.setting.titleMRes as titleRes

/**
 * Unified Reader settings screen. Merges the manga-reader preferences (formerly
 * [SettingsReaderController]) and the novel-reader preferences (formerly
 * [SettingsNovelReaderController]) into a single screen so users find every
 * reader option in one place — see plans/partitioned-foraging-karp.md Phase F.5
 * (issue #13).
 *
 * Layout is two visual sections, "Manga reader" first and "Novel reader" second,
 * each introduced by a wide [preferenceCategory] header. Every preference key
 * is preserved (no migration). The in-reader [eu.kanade.tachiyomi.ui.reader.settings.TabbedNovelReaderSettingsSheet]
 * still drives live previews from the same [ReaderPreferences] backing store, so
 * this screen and the in-reader sheet stay in sync automatically.
 *
 * TODO(F.5/issue #13): Convert this to a true TabLayout-driven screen with
 * Manga and Novel as separate tabs (PreferenceController doesn't natively
 * support tabs — needs a custom Conductor controller hosting two child
 * PreferenceControllers behind a ViewPager2). Until then, the two stacked
 * sections give users the same "one place to find every reader pref" win.
 */
class SettingsReaderHubController : SettingsLegacyController() {

    private val readerPreferences: ReaderPreferences by injectLazy()

    override fun setupPreferenceScreen(screen: PreferenceScreen) = screen.apply {
        titleRes = MR.strings.reader

        // ----- MANGA READER ------------------------------------------------
        preferenceCategory {
            titleRes = MR.strings.manga_reader
            summaryRes = MR.strings.manga_reader_settings_summary
        }
        preferenceCategory {
            titleRes = MR.strings.general
            intListPreference(activity) {
                key = Keys.defaultReadingMode
                titleRes = MR.strings.default_reading_mode
                entriesRes = ReadingModeType.entries.drop(1)
                    .map { value -> value.stringRes }.toTypedArray()
                entryValues = ReadingModeType.entries.drop(1)
                    .map { value -> value.flagValue }
                defaultValue = 2
            }
            intListPreference(activity) {
                key = Keys.doubleTapAnimationSpeed
                titleRes = MR.strings.double_tap_anim_speed
                entries = listOf(
                    context.getString(MR.strings.no_animation),
                    context.getString(MR.strings.fast),
                    context.getString(MR.strings.normal),
                )
                entryValues = listOf(1, 250, 500)
                defaultValue = 500
            }
            switchPreference {
                key = Keys.enableTransitions
                titleRes = MR.strings.animate_page_transitions
                defaultValue = true
            }
            seekBarPreference {
                key = Keys.preloadSize
                titleRes = MR.strings.page_preload_amount
                summaryRes = MR.strings.amount_of_pages_to_preload
                min = 1
                max = 20
                showSeekBarValue = true
                setDefaultValue(6)
            }
            multiSelectListPreferenceMat(activity) {
                key = Keys.readerBottomButtons
                titleRes = MR.strings.display_buttons_bottom_reader
                val enumConstants = ReaderBottomButton.entries
                entriesRes = ReaderBottomButton.entries.map { it.stringRes }.toTypedArray()
                entryValues = enumConstants.map { it.value }
                allSelectionRes = MR.strings.display_options
                allIsAlwaysSelected = true
                showAllLast = true
                val defaults = ReaderBottomButton.BUTTONS_DEFAULTS.toMutableList()
                if (context.isTablet()) {
                    defaults.add(ReaderBottomButton.ShiftDoublePage.value)
                }
                defaultValue = defaults
            }
            infoPreference(MR.strings.certain_buttons_can_be_found)
        }

        preferenceCategory {
            titleRes = MR.strings.display

            intListPreference(activity) {
                key = Keys.defaultOrientationType
                titleRes = MR.strings.default_orientation
                val enumConstants = OrientationType.entries.drop(1)
                entriesRes = enumConstants.map { it.stringRes }.toTypedArray()
                entryValues = enumConstants.map { value -> value.flagValue }
                defaultValue = OrientationType.FREE.flagValue
            }
            intListPreference(activity) {
                // Novel reader uses the same enum; key kept verbatim.
                key = "pref_novel_default_orientation_type_key"
                titleRes = MR.strings.novel_default_orientation
                val enumConstants = OrientationType.entries.drop(1)
                entriesRes = enumConstants.map { it.stringRes }.toTypedArray()
                entryValues = enumConstants.map { value -> value.flagValue }
                defaultValue = OrientationType.FREE.flagValue
            }
            intListPreference(activity) {
                key = Keys.readerTheme
                titleRes = MR.strings.background_color
                val enumConstants = ReaderBackgroundColor.entries
                entriesRes = enumConstants.map { it.longStringRes ?: it.stringRes }.toTypedArray()
                entryValues = enumConstants.map { it.prefValue }
                defaultValue = ReaderBackgroundColor.SMART_PAGE.prefValue
            }
            switchPreference {
                key = Keys.fullscreen
                titleRes = MR.strings.fullscreen
                defaultValue = true
            }
            switchPreference {
                bindTo(readerPreferences.cutoutShort())
                title = context.getString(MR.strings.pref_cutout_short).addBetaTag(context)

                preferences.fullscreen().changesIn(viewScope) {
                    isVisible = DeviceUtil.hasCutout(activity).ordinal >= DeviceUtil.CutoutSupport.MODERN.ordinal && it
                }
            }
            if (DeviceUtil.isVivo && DeviceUtil.hasCutout(activity) == DeviceUtil.CutoutSupport.LEGACY) {
                preference {
                    title = context.getString(MR.strings.pref_legacy_cutout).addBetaTag(context)
                    summaryRes = MR.strings.pref_legacy_cutout_info

                    onClick {
                        val intent = Intent().apply {
                            setComponent(ComponentName("com.android.settings", "com.vivo.settings.display.FullScreenDisplayActivity"))
                        }
                        startActivity(intent)
                    }
                }
            }
            listPreference(activity) {
                bindTo(readerPreferences.landscapeCutoutBehavior())
                title = "${context.getString(MR.strings.cutout_area_behavior)} (${context.getString(MR.strings.landscape)})"
                val values = LandscapeCutoutBehaviour.entries
                entriesRes = values.map { it.titleResId }.toTypedArray()
                entryValues = values.map { it.name }

                preferences.fullscreen().changesIn(viewScope) {
                    isVisible = DeviceUtil.hasCutout(activity).ordinal >= DeviceUtil.CutoutSupport.MODERN.ordinal && it
                }
            }
            switchPreference {
                key = Keys.keepScreenOn
                titleRes = MR.strings.keep_screen_on
                defaultValue = true
            }
            switchPreference {
                key = Keys.showPageNumber
                titleRes = MR.strings.show_page_number
                defaultValue = true
            }
        }

        preferenceCategory {
            titleRes = MR.strings.reading

            switchPreference {
                key = Keys.skipRead
                titleRes = MR.strings.skip_read_chapters
                defaultValue = false
            }
            switchPreference {
                key = Keys.skipFiltered
                titleRes = MR.strings.skip_filtered_chapters
                defaultValue = true
            }
            switchPreference {
                bindTo(preferences.skipDupe())
                titleRes = MR.strings.skip_dupe_chapters
            }
            switchPreference {
                key = Keys.alwaysShowChapterTransition
                titleRes = MR.strings.always_show_chapter_transition
                summaryRes = MR.strings.if_disabled_transition_will_skip
                defaultValue = true
            }
        }

        preferenceCategory {
            titleRes = MR.strings.paged

            intListPreference(activity) {
                key = Keys.navigationModePager
                titleRes = MR.strings.tap_zones
                entries = context.resources.getStringArray(R.array.reader_nav).also { values ->
                    entryRange = 0..values.size
                }.toList()
                defaultValue = "0"
            }
            listPreference(activity) {
                key = Keys.pagerNavInverted
                titleRes = MR.strings.invert_tapping
                entriesRes = arrayOf(
                    MR.strings.none,
                    MR.strings.horizontally,
                    MR.strings.vertically,
                    MR.strings.both_axes,
                )
                entryValues = listOf(
                    ViewerNavigation.TappingInvertMode.NONE.name,
                    ViewerNavigation.TappingInvertMode.HORIZONTAL.name,
                    ViewerNavigation.TappingInvertMode.VERTICAL.name,
                    ViewerNavigation.TappingInvertMode.BOTH.name,
                )
                defaultValue = ViewerNavigation.TappingInvertMode.NONE.name
            }

            intListPreference(activity) {
                key = Keys.imageScaleType
                titleRes = MR.strings.scale_type
                entriesRes = arrayOf(
                    MR.strings.fit_screen,
                    MR.strings.stretch,
                    MR.strings.fit_width,
                    MR.strings.fit_height,
                    MR.strings.original_size,
                    MR.strings.smart_fit,
                )
                entryRange = 1..6
                defaultValue = 1
            }

            listPreference(activity) {
                bindTo(readerPreferences.pagerCutoutBehavior())
                titleRes = MR.strings.cutout_area_behavior
                val values = CutoutBehaviour.entries
                entriesRes = values.map { it.titleResId }.toTypedArray()
                entryValues = values.map { it.name }

                isVisible = DeviceUtil.hasCutout(activity).ordinal >= DeviceUtil.CutoutSupport.LEGACY.ordinal

                activityBinding?.root?.post {
                    isVisible = DeviceUtil.hasCutout(activity).ordinal >= DeviceUtil.CutoutSupport.LEGACY.ordinal
                }
            }

            switchPreference {
                bindTo(preferences.landscapeZoom())
                titleRes = MR.strings.zoom_double_page_spreads
                visibleIf(preferences.imageScaleType()) { it == 1 }
            }
            intListPreference(activity) {
                key = Keys.zoomStart
                titleRes = MR.strings.zoom_start_position
                entriesRes = arrayOf(
                    MR.strings.automatic,
                    MR.strings.left,
                    MR.strings.right,
                    MR.strings.center,
                )
                entryRange = 1..4
                defaultValue = 1
            }
            switchPreference {
                key = Keys.cropBorders
                titleRes = MR.strings.crop_borders
                defaultValue = false
            }
            switchPreference {
                bindTo(preferences.navigateToPan())
                titleRes = MR.strings.navigate_pan
            }
            intListPreference(activity) {
                key = Keys.pageLayout
                title = context.getString(MR.strings.page_layout).addBetaTag(context)
                dialogTitleRes = MR.strings.page_layout
                val enumConstants = PageLayout.entries
                entriesRes = enumConstants.map { it.fullStringRes }.toTypedArray()
                entryValues = enumConstants.map { it.value }
                defaultValue = PageLayout.AUTOMATIC.value
            }
            infoPreference(MR.strings.automatic_can_still_switch).apply {
                preferences.pageLayout().changesIn(viewScope) { isVisible = it == PageLayout.AUTOMATIC.value }
            }
            switchPreference {
                key = Keys.automaticSplitsPage
                titleRes = MR.strings.split_double_pages_portrait
                defaultValue = false
                preferences.pageLayout().changesIn(viewScope) { isVisible = it == PageLayout.AUTOMATIC.value }
            }
            switchPreference {
                key = Keys.invertDoublePages
                titleRes = MR.strings.invert_double_pages
                defaultValue = false
                preferences.pageLayout().changesIn(viewScope) { isVisible = it != PageLayout.SINGLE_PAGE.value }
            }
        }
        preferenceCategory {
            titleRes = MR.strings.long_strip

            intListPreference(activity) {
                key = Keys.navigationModeWebtoon
                titleRes = MR.strings.tap_zones
                entries = context.resources.getStringArray(R.array.reader_nav).also { values ->
                    entryRange = 0..values.size
                }.toList()
                defaultValue = "0"
            }
            listPreference(activity) {
                key = Keys.webtoonNavInverted
                titleRes = MR.strings.invert_tapping
                entriesRes = arrayOf(
                    MR.strings.none,
                    MR.strings.horizontally,
                    MR.strings.vertically,
                    MR.strings.both_axes,
                )
                entryValues = listOf(
                    ViewerNavigation.TappingInvertMode.NONE.name,
                    ViewerNavigation.TappingInvertMode.HORIZONTAL.name,
                    ViewerNavigation.TappingInvertMode.VERTICAL.name,
                    ViewerNavigation.TappingInvertMode.BOTH.name,
                )
                defaultValue = ViewerNavigation.TappingInvertMode.NONE.name
            }
            listPreference(activity) {
                bindTo(preferences.webtoonReaderHideThreshold())
                titleRes = MR.strings.pref_hide_threshold
                val enumValues = PreferenceValues.ReaderHideThreshold.entries
                entriesRes = enumValues.map { it.titleResId }.toTypedArray()
                entryValues = enumValues.map { it.name }
            }
            switchPreference {
                key = Keys.cropBordersWebtoon
                titleRes = MR.strings.crop_borders
                defaultValue = false
            }

            intListPreference(activity) {
                key = Keys.webtoonSidePadding
                titleRes = MR.strings.pref_long_strip_side_padding
                entriesRes = arrayOf(
                    MR.strings.long_strip_side_padding_0,
                    MR.strings.long_strip_side_padding_5,
                    MR.strings.long_strip_side_padding_10,
                    MR.strings.long_strip_side_padding_15,
                    MR.strings.long_strip_side_padding_20,
                    MR.strings.long_strip_side_padding_25,
                )
                entryValues = listOf(0, 5, 10, 15, 20, 25)
                defaultValue = "0"
            }

            intListPreference(activity) {
                key = Keys.webtoonPageLayout
                title = context.getString(MR.strings.page_layout)
                dialogTitleRes = MR.strings.page_layout
                val enumConstants = arrayOf(PageLayout.SINGLE_PAGE, PageLayout.SPLIT_PAGES)
                entriesRes = enumConstants.map { it.fullStringRes }.toTypedArray()
                entryValues = enumConstants.map { it.webtoonValue }
                defaultValue = PageLayout.SINGLE_PAGE.value
            }

            switchPreference {
                bindTo(preferences.webtoonInvertDoublePages())
                titleRes = MR.strings.invert_double_pages
            }

            switchPreference {
                bindTo(preferences.webtoonEnableZoomOut())
                titleRes = MR.strings.enable_zoom_out
            }

            switchPreference {
                bindTo(readerPreferences.webtoonDoubleTapZoomEnabled())
                titleRes = MR.strings.pref_double_tap_zoom
            }
        }
        preferenceCategory {
            titleRes = MR.strings.navigation

            switchPreference {
                key = Keys.readWithVolumeKeys
                titleRes = MR.strings.volume_keys
                defaultValue = false
            }
            switchPreference {
                key = Keys.readWithVolumeKeysInverted
                titleRes = MR.strings.invert_volume_keys
                defaultValue = false

                preferences.readWithVolumeKeys().changesIn(viewScope) { isVisible = it }
            }
        }

        preferenceCategory {
            titleRes = MR.strings.actions

            switchPreference {
                key = Keys.readWithLongTap
                titleRes = MR.strings.show_on_long_press
                defaultValue = true
            }
            switchPreference {
                bindTo(preferences.folderPerManga())
                titleRes = MR.strings.save_pages_separately
                summaryRes = MR.strings.create_folders_by_manga_title
            }
        }

        // ----- NOVEL READER ------------------------------------------------
        preferenceCategory {
            titleRes = MR.strings.novel_reader
            summaryRes = MR.strings.novel_reader_settings_summary
        }
        preferenceCategory {
            titleRes = MR.strings.display

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

            seekBarPreference {
                key = readerPreferences.novelTtsSpeed.key()
                titleRes = MR.strings.novel_tts_speed
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
