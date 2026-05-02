package hayai.novel.reader

import android.content.Context
import android.util.AttributeSet
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderNovelLayoutBinding
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView
import hayai.novel.preferences.NovelPreferences
import hayai.novel.theme.NovelThemeRegistry
import hayai.novel.theme.ui.NovelThemeManagerSheet
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

class ReaderNovelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : BaseReaderSettingsView<ReaderNovelLayoutBinding>(context, attrs) {

    private val novelPreferences: NovelPreferences by injectLazy()
    private var themeNameJob: Job? = null

    override fun inflateBinding() = ReaderNovelLayoutBinding.bind(this)

    override fun initGeneralPreferences() {
        with(binding) {
            novelFontSize.bindToIntPreference(preferences.novelFontSize(), R.array.novel_font_size_values)
            novelFontFamily.bindToPreference(preferences.novelFontFamily())
            novelFontWeight.bindToIntPreference(
                pref = novelPreferences.fontWeight(),
                valueFrom = NovelPreferences.MIN_FONT_WEIGHT,
                valueTo = NovelPreferences.MAX_FONT_WEIGHT,
                stepSize = NovelPreferences.FONT_WEIGHT_STEP,
            )
            novelLineHeight.bindToIntPreference(preferences.novelLineHeight(), R.array.novel_line_height_values)
            novelParagraphSpacing.bindToIntPreference(
                pref = novelPreferences.paragraphSpacing(),
                valueFrom = NovelPreferences.MIN_PARAGRAPH_SPACING_DP,
                valueTo = NovelPreferences.MAX_PARAGRAPH_SPACING_DP,
                stepSize = NovelPreferences.PARAGRAPH_SPACING_STEP,
            )
            novelTextAlign.bindToPreference(preferences.novelTextAlign())
            novelPadding.bindToIntPreference(preferences.novelPadding(), R.array.novel_padding_values)

            novelReaderTheme.setTitle(context.getString(R.string.novel_reader_theme))
            novelReaderTheme.setValue(NovelThemeRegistry.resolve(
                selectedId = novelPreferences.selectedThemeId().get(),
                customs = novelPreferences.customThemes().get(),
            ).name)
            novelReaderTheme.setOnClickListener { NovelThemeManagerSheet.show(context) }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val owner = findViewTreeLifecycleOwner() ?: return

        themeNameJob = owner.lifecycleScope.launch {
            combine(
                novelPreferences.selectedThemeId().changes(),
                novelPreferences.customThemes().changes(),
            ) { selectedId, customs ->
                NovelThemeRegistry.resolve(selectedId, customs).name
            }.collect { name ->
                binding.novelReaderTheme.setValue(name)
            }
        }

        (activity.viewer as? NovelViewer)?.let { viewer ->
            binding.novelAutoScroll.bind(
                scroller = viewer.autoScroller,
                speedPref = novelPreferences.autoScrollSpeed(),
                valueFrom = NovelPreferences.MIN_AUTO_SCROLL_SPEED_PX_PER_SEC,
                valueTo = NovelPreferences.MAX_AUTO_SCROLL_SPEED_PX_PER_SEC,
                stepSize = NovelPreferences.AUTO_SCROLL_SPEED_STEP,
                scope = owner.lifecycleScope,
            )
        }
    }

    override fun onDetachedFromWindow() {
        themeNameJob?.cancel()
        themeNameJob = null
        super.onDetachedFromWindow()
    }
}
