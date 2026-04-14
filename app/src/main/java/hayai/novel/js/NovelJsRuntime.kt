package hayai.novel.js

import android.content.Context
import co.touchlab.kermit.Logger
import com.dokar.quickjs.QuickJs
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * Manages a QuickJS runtime instance for a single LNReader plugin.
 * Loads all required libraries and shims, then evaluates the plugin code.
 * Uses dokar3/quickjs-kt which properly handles async/Promise resolution.
 */
class NovelJsRuntime(
    private val context: Context,
    private val bridge: NovelJsBridge,
    private val pluginId: String,
    private val userAgent: String = "",
) {
    private var quickJs: QuickJs? = null
    private var initialized = false

    companion object {
        // QuickJS uses the native thread stack for JS execution.
        // Cheerio's recursive HTML parsing needs more than the default ~1MB.
        // Use a single-thread executor with 8MB stack for all JS work.
        internal val jsDispatcher = Executors.newSingleThreadExecutor { runnable ->
            Thread(null, runnable, "quickjs-worker", 8L * 1024 * 1024)
        }.asCoroutineDispatcher()
    }

    /**
     * Initialize the runtime with a plugin's JS code.
     * Must be called before any plugin method calls.
     */
    suspend fun initialize(pluginCode: String) {
        withContext(jsDispatcher) {
            doInitialize(pluginCode)
        }
    }

    private suspend fun doInitialize(pluginCode: String) {
        close()

        val qjs = QuickJs.create(jobDispatcher = jsDispatcher)
        quickJs = qjs

        try {
            // Step 1: Register the Kotlin bridge as "__bridge" in JS global scope
            qjs.define("__bridge") {
                function("fetch") { args ->
                    bridge.fetch(args[0] as String, args[1] as String)
                }
                function("fetchText") { args ->
                    bridge.fetchText(args[0] as String, args[1] as String, args[2] as String)
                }
                function("fetchFile") { args ->
                    bridge.fetchFile(args[0] as String, args[1] as String)
                }
                function("fetchProto") { args ->
                    bridge.fetchProto(args[0] as String, args[1] as String, args[2] as String)
                }
                function("storageGet") { args ->
                    bridge.storageGet(args[0] as String, args[1] as String)
                }
                function("storageSet") { args ->
                    bridge.storageSet(args[0] as String, args[1] as String, args[2] as String)
                    Unit
                }
                function("storageDelete") { args ->
                    bridge.storageDelete(args[0] as String, args[1] as String)
                    Unit
                }
                function("storageGetAllKeys") { args ->
                    bridge.storageGetAllKeys(args[0] as String)
                }
                function("storageClearAll") { args ->
                    bridge.storageClearAll(args[0] as String)
                    Unit
                }
            }

            // Step 2: Load constants (NovelStatus, FilterTypes, etc.)
            evaluateAsset(qjs, "novel/shims/constants.js")

            // Step 3: Load storage shim (Storage, LocalStorage, SessionStorage classes)
            evaluateAsset(qjs, "novel/shims/storage_shim.js")

            // Step 4: Pre-require stub for iconv-lite (needed by urlencode.min.js
            // before require_impl.js defines the full require function)
            qjs.evaluate<Any?>("""
                if (typeof require === 'undefined') {
                    var require = function(name) {
                        if (name === 'iconv-lite') {
                            return {
                                decode: function(buf, enc) { return typeof buf === 'string' ? buf : String(buf); },
                                encode: function(str, enc) { return str; },
                                encodingExists: function(enc) { return true; }
                            };
                        }
                        throw new Error('Module not found: ' + name);
                    };
                }
            """.trimIndent(), filename = "pre_require_stub.js")

            // Step 5: Load runtime libraries
            evaluateAsset(qjs, "novel/runtime/cheerio.min.js")
            evaluateAsset(qjs, "novel/runtime/dayjs.min.js")
            evaluateAsset(qjs, "novel/runtime/urlencode.min.js")
            evaluateAsset(qjs, "novel/runtime/noble-ciphers-aes.min.js")

            // Step 6: Load fetch bridge (fetchApi, fetchText, etc.)
            // Set User-Agent before loading
            qjs.evaluate<Any?>("var __userAgent = ${escapeJsString(userAgent)};")
            evaluateAsset(qjs, "novel/shims/fetch_bridge.js")

            // Step 7: Load require() implementation (wires all packages)
            qjs.evaluate<Any?>("var __pluginId = ${escapeJsString(pluginId)};")
            evaluateAsset(qjs, "novel/shims/require_impl.js")

            // Step 8: Load the plugin code using LNReader's exact wrapping pattern
            val wrappedCode = """
                var __plugin = (function() {
                    var module = {};
                    var exports = module.exports = {};
                    $pluginCode;
                    return exports.default || module.exports.default || module.exports;
                })();
            """.trimIndent()

            qjs.evaluate<Any?>(wrappedCode, filename = "$pluginId.js")

            // Step 9: Inject User-Agent into imageRequestInit if not present
            qjs.evaluate<Any?>("""
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
     * Uses async IIFE to properly await the Promise returned by plugin methods.
     * dokar3/quickjs-kt auto-awaits the outer Promise via its internal event loop.
     *
     * @param method The plugin method name (e.g., "popularNovels", "parseNovel")
     * @param argsJs JavaScript expression for the arguments (e.g., "1, {showLatestNovels: false}")
     * @return The raw JSON string result from the plugin
     */
    suspend fun callMethod(method: String, argsJs: String = ""): String {
        val qjs = quickJs ?: throw IllegalStateException("Runtime not initialized for plugin '$pluginId'")
        if (!initialized) throw IllegalStateException("Plugin '$pluginId' not initialized")

        return withContext(jsDispatcher) {
            try {
                val result = qjs.evaluate<Any?>("""
                    (async function() {
                        var r = await __plugin.${method}($argsJs);
                        return JSON.stringify(r);
                    })()
                """.trimIndent(), filename = "$pluginId.$method")
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
    suspend fun getProperty(property: String): String {
        val qjs = quickJs ?: throw IllegalStateException("Runtime not initialized")
        return withContext(jsDispatcher) {
            try {
                val result = qjs.evaluate<Any?>("JSON.stringify(__plugin.$property)")
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
    suspend fun evaluateRaw(script: String): String {
        val qjs = quickJs ?: throw IllegalStateException("Runtime not initialized")
        return withContext(jsDispatcher) {
            val result = qjs.evaluate<Any?>(script)
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

    private suspend fun evaluateAsset(qjs: QuickJs, assetPath: String) {
        val code = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        qjs.evaluate<Any?>(code, filename = assetPath)
    }
}
