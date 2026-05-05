package eu.kanade.tachiyomi.ui.reader.settings

import android.content.Context
import android.util.AttributeSet
import eu.kanade.tachiyomi.databinding.ReaderNovelReadingBinding
import eu.kanade.tachiyomi.util.bindToPreference
import eu.kanade.tachiyomi.widget.BaseReaderSettingsView
import yokai.i18n.MR
import yokai.util.lang.getString

/**
 * Reading tab view for the novel reader settings sheet. Mirrors the manga reader's
 * `ReaderGeneralView` pattern (XML-inflated `NestedScrollView` extending [BaseReaderSettingsView]).
 */
class NovelReadingView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    BaseReaderSettingsView<ReaderNovelReadingBinding>(context, attrs) {

    override fun inflateBinding() = ReaderNovelReadingBinding.bind(this)

    override fun initGeneralPreferences() {
        with(binding) {
            // Rendering mode (default | webview)
            renderingMode.setEntries(
                listOf(
                    context.getString(MR.strings.novel_rendering_mode_default),
                    context.getString(MR.strings.novel_rendering_mode_webview),
                ),
            )
            renderingMode.setSelection(if (readerPreferences.novelRenderingMode.get() == "webview") 1 else 0)
            renderingMode.onItemSelectedListener = { idx ->
                readerPreferences.novelRenderingMode.set(if (idx == 1) "webview" else "default")
            }

            // Font family
            val fontEntries = listOf(
                "sans-serif" to context.getString(MR.strings.novel_font_sans_serif),
                "serif" to context.getString(MR.strings.novel_font_serif),
                "monospace" to context.getString(MR.strings.novel_font_monospace),
                "Georgia, serif" to context.getString(MR.strings.novel_font_georgia),
                "Times New Roman, serif" to context.getString(MR.strings.novel_font_times),
                "Arial, sans-serif" to context.getString(MR.strings.novel_font_arial),
            )
            fontFamily.setEntries(fontEntries.map { it.second })
            fontFamily.setSelection(
                fontEntries.indexOfFirst { it.first == readerPreferences.novelFontFamily.get() }.coerceAtLeast(0),
            )
            fontFamily.onItemSelectedListener = { idx ->
                readerPreferences.novelFontFamily.set(fontEntries[idx].first)
            }

            // Text alignment toggle group
            val alignButtons = mapOf(
                "left" to textAlignLeft,
                "center" to textAlignCenter,
                "right" to textAlignRight,
                "justify" to textAlignJustify,
            )
            fun applyAlignSelection(value: String) {
                textAlignGroup.clearChecked()
                alignButtons[value]?.let { textAlignGroup.check(it.id) }
            }
            applyAlignSelection(readerPreferences.novelTextAlign.get())
            textAlignGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
                if (!isChecked) return@addOnButtonCheckedListener
                val newValue = alignButtons.entries.firstOrNull { it.value.id == checkedId }?.key ?: return@addOnButtonCheckedListener
                readerPreferences.novelTextAlign.set(newValue)
            }

            // Slider rows — see widget_novel_slider_int / widget_novel_slider_float.
            bindIntSlider(fontSize, MR.strings.novel_font_size, 10, 40, readerPreferences.novelFontSize.get()) {
                readerPreferences.novelFontSize.set(it)
            }
            bindFloatSlider(lineHeight, MR.strings.novel_line_height, 10, 30, readerPreferences.novelLineHeight.get()) {
                readerPreferences.novelLineHeight.set(it)
            }
            bindFloatSlider(paragraphIndent, MR.strings.novel_paragraph_indent, 0, 50, readerPreferences.novelParagraphIndent.get()) {
                readerPreferences.novelParagraphIndent.set(it)
            }
            bindFloatSlider(paragraphSpacing, MR.strings.novel_paragraph_spacing, 0, 30, readerPreferences.novelParagraphSpacing.get()) {
                readerPreferences.novelParagraphSpacing.set(it)
            }
            bindIntSlider(marginLeft, MR.strings.novel_margin_left, 0, 100, readerPreferences.novelMarginLeft.get()) {
                readerPreferences.novelMarginLeft.set(it)
            }
            bindIntSlider(marginRight, MR.strings.novel_margin_right, 0, 100, readerPreferences.novelMarginRight.get()) {
                readerPreferences.novelMarginRight.set(it)
            }
            bindIntSlider(marginTop, MR.strings.novel_margin_top, 0, 200, readerPreferences.novelMarginTop.get()) {
                readerPreferences.novelMarginTop.set(it)
            }
            bindIntSlider(marginBottom, MR.strings.novel_margin_bottom, 0, 200, readerPreferences.novelMarginBottom.get()) {
                readerPreferences.novelMarginBottom.set(it)
            }
            bindIntSlider(autoSplitWordCount, MR.strings.novel_auto_split_word_count, 10, 200, readerPreferences.novelAutoSplitWordCount.get()) {
                readerPreferences.novelAutoSplitWordCount.set(it)
            }

            useOriginalFonts.bindToPreference(readerPreferences.novelUseOriginalFonts)
            autoSplitText.bindToPreference(readerPreferences.novelAutoSplitText)
        }
    }
}

/**
 * Bind one of the included `widget_novel_slider_int.xml` rows to an integer preference.
 * The included binding gives us `<sliderRow>.title`, `<sliderRow>.slider`, `<sliderRow>.value`.
 */
internal fun bindIntSlider(
    sliderRowBinding: eu.kanade.tachiyomi.databinding.WidgetNovelSliderIntBinding,
    @Suppress("DEPRECATION") titleRes: dev.icerock.moko.resources.StringResource,
    min: Int,
    max: Int,
    initial: Int,
    onChange: (Int) -> Unit,
) {
    sliderRowBinding.title.text = sliderRowBinding.root.context.getString(titleRes)
    sliderRowBinding.slider.valueFrom = min.toFloat()
    sliderRowBinding.slider.valueTo = max.toFloat()
    val safeInitial = initial.coerceIn(min, max).toFloat()
    sliderRowBinding.slider.value = safeInitial
    sliderRowBinding.value.text = safeInitial.toInt().toString()
    sliderRowBinding.slider.addOnChangeListener { _, v, _ ->
        sliderRowBinding.value.text = v.toInt().toString()
    }
    sliderRowBinding.slider.addOnSliderTouchListener(
        object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                onChange(slider.value.toInt())
            }
        },
    )
}

/**
 * Bind one of the included `widget_novel_slider_float.xml` rows to a float preference whose
 * stored value is `floatValue` and is exposed to the UI as `floatValue * 10` (integer steps).
 */
internal fun bindFloatSlider(
    sliderRowBinding: eu.kanade.tachiyomi.databinding.WidgetNovelSliderFloatBinding,
    @Suppress("DEPRECATION") titleRes: dev.icerock.moko.resources.StringResource,
    minTimes10: Int,
    maxTimes10: Int,
    initial: Float,
    onChange: (Float) -> Unit,
) {
    sliderRowBinding.title.text = sliderRowBinding.root.context.getString(titleRes)
    sliderRowBinding.slider.valueFrom = minTimes10.toFloat()
    sliderRowBinding.slider.valueTo = maxTimes10.toFloat()
    val safeInitial = (initial * 10f).toInt().coerceIn(minTimes10, maxTimes10).toFloat()
    sliderRowBinding.slider.value = safeInitial
    sliderRowBinding.value.text = "%.1f".format(safeInitial / 10f)
    sliderRowBinding.slider.addOnChangeListener { _, v, _ ->
        sliderRowBinding.value.text = "%.1f".format(v / 10f)
    }
    sliderRowBinding.slider.addOnSliderTouchListener(
        object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {}
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                onChange(slider.value / 10f)
            }
        },
    )
}
