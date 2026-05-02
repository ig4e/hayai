package hayai.novel.tts.engine

import android.content.Context
import hayai.novel.tts.engine.system.SystemTtsEngine

/**
 * Factory for [TtsEngine] instances. The interface is kept for future extensibility; today it
 * always returns the system TTS engine.
 */
class TtsEngineFactory(private val context: Context) {

    fun create(): TtsEngine = SystemTtsEngine(context)
}
