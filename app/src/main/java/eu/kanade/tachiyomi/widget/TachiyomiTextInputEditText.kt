package eu.kanade.tachiyomi.widget

import yokai.util.koin.get
import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import androidx.core.view.inputmethod.EditorInfoCompat
import com.google.android.material.textfield.TextInputEditText
import eu.kanade.tachiyomi.R
import yokai.i18n.MR
import yokai.util.lang.getString
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.changesIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * A custom [TextInputEditText] that sets [EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING] to imeOptions
 * if [PreferencesHelper.incognitoMode] is true. Some IMEs may not respect this flag.
 *
 * @see setIncognito
 */
class TachiyomiTextInputEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle,
) : TextInputEditText(context, attrs, defStyleAttr) {

    private var scope: CoroutineScope? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        setIncognito(scope!!)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scope?.cancel()
        scope = null
    }

    companion object {
        /**
         * Sets Flow to this [EditText] that sets [EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING] to imeOptions
         * if [PreferencesHelper.incognitoMode] is true. Some IMEs may not respect this flag.
         */
        fun EditText.setIncognito(viewScope: CoroutineScope) {
            try {
                get<PreferencesHelper>().incognitoMode().changesIn(viewScope) {
                    imeOptions = if (it) {
                        imeOptions or EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING
                    } else {
                        imeOptions and EditorInfoCompat.IME_FLAG_NO_PERSONALIZED_LEARNING.inv()
                    }
                }
            } catch (_: Exception) {
            }
        }
    }
}
