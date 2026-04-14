package hayai.novel.reader

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.databinding.ReaderNovelLayoutBinding
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView

class ReaderNovelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : BaseReaderSettingsView<ReaderNovelLayoutBinding>(context, attrs) {

    override fun inflateBinding() = ReaderNovelLayoutBinding.bind(this)

    override fun initGeneralPreferences() {
        with(binding) {
            novelFontSize.bindToIntPreference(preferences.novelFontSize(), R.array.novel_font_size_values)
            novelFontFamily.bindToPreference(preferences.novelFontFamily())
            novelLineHeight.bindToIntPreference(preferences.novelLineHeight(), R.array.novel_line_height_values)
            novelTextAlign.bindToPreference(preferences.novelTextAlign())
            novelPadding.bindToIntPreference(preferences.novelPadding(), R.array.novel_padding_values)
            novelReaderTheme.bindToPreference(preferences.novelReaderTheme())
        }
    }
}
