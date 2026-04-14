package hayai.novel.js

import android.content.Context
import co.touchlab.kermit.Logger
import com.dokar.quickjs.QuickJs
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Loads and validates LNReader JS plugins.
 * Extracts plugin metadata (id, name, version, etc.) by running the plugin in a
 * temporary QuickJS instance, matching how LNReader's build-plugin-manifest.js works.
 */
class JsPluginLoader(
    private val context: Context,
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    data class PluginMetadata(
        val id: String,
        val name: String,
        val version: String,
        val site: String,
        val icon: String? = null,
        val hasFilters: Boolean = false,
        val hasResolveUrl: Boolean = false,
        val supportsPagePlugin: Boolean = false,
    )

    /**
     * Extract metadata from a plugin JS file without keeping the runtime alive.
     * Uses a temporary QuickJS instance that is immediately closed.
     *
     * @param pluginCode Raw JavaScript source of the plugin
     * @return PluginMetadata if valid, null if plugin fails to load
     */
    suspend fun extractMetadata(pluginCode: String): PluginMetadata? {
        val bridge = NoopJsBridge()

        return try {
            val qjs = QuickJs.create(Dispatchers.IO)
            try {
                // Register no-op bridge
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

                // Load minimal shims needed for plugin instantiation
                evaluateAsset(qjs, "novel/shims/constants.js")
                evaluateAsset(qjs, "novel/shims/storage_shim.js")

                // Pre-require stub: urlencode.min.js needs require("iconv-lite")
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

                evaluateAsset(qjs, "novel/runtime/cheerio.min.js")
                evaluateAsset(qjs, "novel/runtime/dayjs.min.js")
                evaluateAsset(qjs, "novel/runtime/urlencode.min.js")
                evaluateAsset(qjs, "novel/runtime/noble-ciphers-aes.min.js")

                qjs.evaluate<Any?>("var __userAgent = '';")
                evaluateAsset(qjs, "novel/shims/fetch_bridge.js")

                qjs.evaluate<Any?>("var __pluginId = '__metadata_extract__';")
                evaluateAsset(qjs, "novel/shims/require_impl.js")

                // Load plugin
                val wrappedCode = """
                    var __plugin = (function() {
                        var module = {};
                        var exports = module.exports = {};
                        $pluginCode;
                        return exports.default || module.exports.default || module.exports;
                    })();
                """.trimIndent()
                qjs.evaluate<Any?>(wrappedCode, filename = "plugin.js")

                // Extract metadata
                val metadataJson = qjs.evaluate<Any?>("""
                    JSON.stringify({
                        id: __plugin.id || '',
                        name: __plugin.name || '',
                        version: __plugin.version || '',
                        site: __plugin.site || '',
                        icon: __plugin.icon || null,
                        hasFilters: !!__plugin.filters,
                        hasResolveUrl: typeof __plugin.resolveUrl === 'function',
                        supportsPagePlugin: typeof __plugin.parsePage === 'function'
                    })
                """.trimIndent())

                val result = json.decodeFromString<PluginMetadata>(metadataJson.toString())
                if (result.id.isBlank() || result.name.isBlank()) {
                    Logger.w { "JsPluginLoader: Plugin has empty id or name" }
                    null
                } else {
                    result
                }
            } finally {
                qjs.close()
            }
        } catch (e: Exception) {
            Logger.e(e) { "JsPluginLoader: Failed to extract metadata" }
            null
        }
    }

    private suspend fun evaluateAsset(qjs: QuickJs, assetPath: String) {
        val code = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        qjs.evaluate<Any?>(code, filename = assetPath)
    }

    /**
     * No-op bridge for metadata extraction. No actual network/storage calls needed.
     */
    private class NoopJsBridge : NovelJsBridge {
        override fun fetch(url: String, initJson: String): String =
            """{"status":0,"statusText":"noop","body":"","bodyBase64":"","headers":{}}"""
        override fun fetchText(url: String, initJson: String, encoding: String): String = ""
        override fun fetchFile(url: String, initJson: String): String = ""
        override fun fetchProto(protoInitJson: String, url: String, initJson: String): String = "{}"
        override fun storageGet(pluginId: String, key: String): String? = null
        override fun storageSet(pluginId: String, key: String, valueJson: String) {}
        override fun storageDelete(pluginId: String, key: String) {}
        override fun storageGetAllKeys(pluginId: String): String = "[]"
        override fun storageClearAll(pluginId: String) {}
    }
}
