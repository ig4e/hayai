package eu.kanade.tachiyomi.network

import android.content.Context
import com.dokar.quickjs.QuickJs
import eu.kanade.tachiyomi.util.system.withIOContext
import kotlinx.coroutines.Dispatchers

/**
 * Util for evaluating JavaScript in sources.
 */
class JavaScriptEngine(context: Context) {

    /**
     * Evaluate arbitrary JavaScript code and get the result as a primtive type
     * (e.g., String, Int).
     *
     * @since extensions-lib 1.4
     * @param script JavaScript to execute.
     * @return Result of JavaScript code as a primitive type.
     */
    @Suppress("UNUSED", "UNCHECKED_CAST")
    suspend fun <T> evaluate(script: String): T = withIOContext {
        val qjs = QuickJs.create(Dispatchers.IO)
        try {
            qjs.evaluate<Any?>(script) as T
        } finally {
            qjs.close()
        }
    }
}
