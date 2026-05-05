package eu.kanade.tachiyomi.ui.reader.settings

import android.view.View
import androidx.core.view.isVisible
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.widget.TabbedBottomSheetDialog
import yokai.i18n.MR

/**
 * In-reader settings sheet for the novel reader. Shares the manga reader's container
 * ([TabbedBottomSheetDialog] + `tabbed_bottom_sheet.xml` + `bottom_sheet_rounded_background`)
 * so the visual style matches `TabbedReaderSettingsSheet` exactly. Each tab is an XML-inflated
 * [eu.kanade.tachiyomi.widget.BaseReaderSettingsView] that binds to `ReaderPreferences`.
 *
 * Five tabs: Reading | Appearance | Controls | Advanced | TTS.
 */
class TabbedNovelReaderSettingsSheet(
    val readerActivity: ReaderActivity,
) : TabbedBottomSheetDialog(readerActivity) {

    private val readingView: NovelReadingView = View.inflate(
        readerActivity, R.layout.reader_novel_reading, null,
    ) as NovelReadingView
    private val appearanceView: NovelAppearanceView = View.inflate(
        readerActivity, R.layout.reader_novel_appearance, null,
    ) as NovelAppearanceView
    private val controlsView: NovelControlsView = View.inflate(
        readerActivity, R.layout.reader_novel_controls, null,
    ) as NovelControlsView
    private val ttsView: NovelTtsView = View.inflate(
        readerActivity, R.layout.reader_novel_tts, null,
    ) as NovelTtsView

    override fun getTabViews(): List<View> = listOf(
        readingView, appearanceView, controlsView, ttsView,
    )

    // Short labels prevent text-wrap on narrower phones. EPUB toggles fold into Appearance;
    // Find & Replace is reachable from the in-reader action bar (the FindReplace icon button).
    override fun getTabTitles(): List<StringResource> = listOf(
        MR.strings.reading,
        MR.strings.appearance,
        MR.strings.controls,
        MR.strings.novel_tts_tab,
    )

    init {
        readingView.activity = readerActivity
        appearanceView.activity = readerActivity
        controlsView.activity = readerActivity
        ttsView.activity = readerActivity

        binding.menu.isVisible = false
    }
}
