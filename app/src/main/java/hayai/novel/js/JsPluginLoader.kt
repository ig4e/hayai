package hayai.novel.js

import android.content.Context
import co.touchlab.kermit.Logger
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
    fun extractMetadata(pluginCode: String): PluginMetadata? {
        val bridge = NoopJsBridge()
        val runtime = NovelJsRuntime(context, bridge, "__metadata_extract__")

        return try {
            // Use a blocking init since this is for validation only
            runtime.run {
                // Initialize manually in a simpler way for metadata extraction
                val qjs = app.cash.quickjs.QuickJs.create()
                try {
                    qjs.set("__bridge", NovelJsBridge::class.java, bridge)

                    // Load minimal shims needed for plugin instantiation
                    evaluateAssetSync(qjs, "novel/shims/constants.js")
                    evaluateAssetSync(qjs, "novel/shims/storage_shim.js")
                    evaluateAssetSync(qjs, "novel/runtime/cheerio.min.js")
                    evaluateAssetSync(qjs, "novel/runtime/dayjs.min.js")
                    evaluateAssetSync(qjs, "novel/runtime/urlencode.min.js")
                    evaluateAssetSync(qjs, "novel/runtime/noble-ciphers-aes.min.js")

                    qjs.evaluate("var __userAgent = '';")
                    evaluateAssetSync(qjs, "novel/shims/fetch_bridge.js")

                    qjs.evaluate("var __pluginId = '__metadata_extract__';")
                    evaluateAssetSync(qjs, "novel/shims/require_impl.js")

                    // Load plugin
                    val wrappedCode = """
                        var __plugin = (function() {
                            var module = {};
                            var exports = module.exports = {};
                            $pluginCode;
                            return exports.default || module.exports.default || module.exports;
                        })();
                    """.trimIndent()
                    qjs.evaluate(wrappedCode, "plugin.js")

                    // Extract metadata
                    val metadataJson = qjs.evaluate("""
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
            }
        } catch (e: Exception) {
            Logger.e(e) { "JsPluginLoader: Failed to extract metadata" }
            null
        }
    }

    private fun evaluateAssetSync(qjs: app.cash.quickjs.QuickJs, assetPath: String) {
        val code = context.assets.open(assetPath).bufferedReader().use { it.readText() }
        qjs.evaluate(code, assetPath)
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
