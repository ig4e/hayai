package hayai.novel.reader

import dev.icerock.moko.resources.StringResource
import yokai.i18n.MR

/**
 * Configurable tap-action used by the novel reader's gesture detector for the
 * `novelDoubleTapAction` and `novelLongTapAction` preferences. Each enum entry has a
 * matching string resource for the in-reader settings sheet.
 *
 * The dispatch site is `NovelWebViewViewer.dispatchTapAction(action)` — it handles every
 * value in one switch so adding a new action only requires extending this enum (plus its
 * string) and the switch arm.
 */
enum class NovelTapAction(val labelRes: StringResource) {
    NONE(MR.strings.novel_tap_action_none),
    TOGGLE_MENU(MR.strings.novel_tap_action_toggle_menu),
    NEXT_CHAPTER(MR.strings.novel_tap_action_next_chapter),
    PREVIOUS_CHAPTER(MR.strings.novel_tap_action_previous_chapter),
    SCROLL_DOWN(MR.strings.novel_tap_action_scroll_down),
    SCROLL_UP(MR.strings.novel_tap_action_scroll_up),
    TOGGLE_AUTO_SCROLL(MR.strings.novel_tap_action_toggle_auto_scroll),
    START_TTS(MR.strings.novel_tap_action_start_tts),
    STOP_TTS(MR.strings.novel_tap_action_stop_tts),
    DEFINE_SELECTED(MR.strings.novel_tap_action_define_selected),
    ;

    companion object {
        fun fromOrdinal(ordinal: Int): NovelTapAction =
            entries.getOrNull(ordinal) ?: NONE
    }
}
