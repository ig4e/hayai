package hayai.novel.js

import android.content.Context
import app.cash.quickjs.QuickJs
import co.touchlab.kermit.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Manages a QuickJS runtime instance for a single LNReader plugin.
 * Loads all required libraries and shims, then evaluates the plugin code.
 * All JS calls are serialized via mutex (QuickJS is single-threaded).
 */
class NovelJsRuntime(
    private val context: Context,
    private val bridge: NovelJsBridge,
    private val pluginId: String,
    private val userAgent: String = "",
) {
    private var quickJs: QuickJs? = null
    private val mutex = Mutex()
    private var initialized = false

    /**
     * Initialize the runtime with a plugin's JS code.
     * Must be called before any plugin method calls.
     */
    suspend fun initialize(pluginCode: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            doInitialize(pluginCode)
        }
    }

    private fun doInitialize(pluginCode: String) {
        close()

        val qjs = QuickJs.create()
        quickJs = qjs

        try {
            // Step 1: Register the Kotlin bridge as "__bridge" in JS global scope
            qjs.set("__bridge", NovelJsBridge::class.java, bridge)

            // Step 2: Load constants (NovelStatus, FilterTypes, etc.)
            evaluateAsset(qjs, "novel/shims/constants.js")

            // Step 3: Load storage shim (Storage, LocalStorage, SessionStorage classes)
            evaluateAsset(qjs, "novel/shims/storage_shim.js")

            // Step 4: Load runtime libraries
            evaluateAsset(qjs, "novel/runtime/cheerio.min.js")
            evaluateAsset(qjs, "novel/runtime/dayjs.min.js")
            evaluateAsset(qjs, "novel/runtime/urlencode.min.js")
            evaluateAsset(qjs, "novel/runtime/noble-ciphers-aes.min.js")

            // Step 5: Load fetch bridge (fetchApi, fetchText, etc.)
            // Set User-Agent before loading
            qjs.evaluate("var __userAgent = ${escapeJsString(userAgent)};")
            evaluateAsset(qjs, "novel/shims/fetch_bridge.js")

            // Step 6: Load require() implementation (wires all packages)
            qjs.evaluate("var __pluginId = ${escapeJsString(pluginId)};")
            evaluateAsset(qjs, "novel/shims/require_impl.js")

            // Step 7: Load the plugin code using LNReader's exact wrapping pattern
            val wrappedCode = """
                var __plugin = (function() {
                    var module = {};
                    var exports = module.exports = {};
                    $pluginCode;
                    return exports.default || module.exports.default || module.exports;
                })();
            """.trimIndent()

            qjs.evaluate(wrappedCode, "$pluginId.js")

            // Step 8: Inject User-Agent into imageRequestInit if not present
            qjs.evaluate("""
                if (__plugin) {
                    if (!__plugin.imageRequestInit) {
                        __plugin.imageRequestInit = { headers: { 'User-Agent': __userAgent } };
                    } else {
                        if (!__plugin.imageRequestInit.headers) {
                            __plugin.imageRequestInit.headers = {};
                        }
                        var hasUA = Object.keys(__plugin.imageRequestInit.headers).some(
                            function(h) { return h.toLowerCase() === 'user-agent'; }
                        );
                        if (!hasUA) {
                            __plugin.imageRequestInit.headers['User-Agent'] = __userAgent;
                        }
                    }
                }
            """.trimIndent())

            initialized = true
            Logger.d { "NovelJsRuntime: Plugin '$pluginId' initialized successfully" }
        } catch (e: Exception) {
            Logger.e(e) { "NovelJsRuntime: Failed to initialize plugin '$pluginId'" }
            close()
            throw e
        }
    }

    /**
     * Call a plugin method that returns a JSON-serializable result.
     * The JS method is called, its Promise resolved, and the result JSON-stringified.
     *
     * @param method The plugin method name (e.g., "popularNovels", "parseNovel")
     * @param argsJs JavaScript expression for the arguments (e.g., "1, {showLatestNovels: false}")
     * @return The raw JSON string result from the plugin
     */
    suspend fun callMethod(method: String, argsJs: String = ""): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            val qjs = quickJs ?: throw IllegalStateException("Runtime not initialized for plugin '$pluginId'")
            if (!initialized) throw IllegalStateException("Plugin '$pluginId' not initialized")

            try {
                // QuickJS evaluate() handles Promises by resolving them synchronously
                // when the bridge methods (fetch etc.) are also synchronous.
                // Wrap in JSON.stringify to get serializable output.
                val script = """
                    (function() {
                        var __result = __plugin.${method}($argsJs);
                        if (__result && typeof __result.then === 'function') {
                            // It's a Promise - but since our bridge is synchronous,
                            // the promise should already be resolved after evaluate.
                            // QuickJS handles microtask queue during evaluate.
                            return JSON.stringify(__result);
                        }
                        return JSON.stringify(__result);
                    })()
                """.trimIndent()

                val result = qjs.evaluate(script)
                result?.toString() ?: "null"
            } catch (e: Exception) {
                Logger.e(e) { "NovelJsRuntime: Error calling $pluginId.$method" }
                throw e
            }
        }
    }

    /**
     * Get a plugin property as JSON string.
     */
    suspend fun getProperty(property: String): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            val qjs = quickJs ?: throw IllegalStateException("Runtime not initialized")
            try {
                val result = qjs.evaluate("JSON.stringify(__plugin.$property)")
                result?.toString() ?: "null"
            } catch (e: Exception) {
                Logger.e(e) { "NovelJsRuntime: Error getting property $pluginId.$property" }
                "null"
            }
        }
    }

    /**
     * Evaluate raw JS and return result as string.
     */
    suspend fun evaluateRaw(script: String): String = mutex.withLock {
        withContext(Dispatchers.IO) {
            val qjs = quickJs ?: throw IllegalStateException("Runtime not initialized")
            val result = qjs.evaluate(script)
            result?.toString() ?: "null"
        }
    }

    fun close() {
        try {
            quickJs?.close()
        } catch (_: Exception) {
        }
        quickJs = null
        initialized = false
    }

    val isInitialized: Boolean get() = initialized

    private fun escapeJsString(s: String): String {
        val escaped = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r")
        return "\"$escaped\""
    }

    private fun evaluateAsset(qjs: QuickJs, assetPath: String) {
        val code = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        qjs.evaluate(code, assetPath)
    }
}
