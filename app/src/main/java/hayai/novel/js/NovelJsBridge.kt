package hayai.novel.js

/**
 * Interface registered into QuickJS as "__bridge".
 *
 * Network methods (fetch/fetchText/fetchFile/fetchProto/sleep) are suspend so the
 * Kotlin implementation can switch to Dispatchers.IO for the blocking OkHttp work
 * without occupying the per-source QuickJS worker thread. They are bound to JS via
 * `asyncFunction`, so callers must `await` them. Storage methods stay synchronous —
 * they're cheap SharedPreferences reads/writes that don't justify a thread switch.
 */
interface NovelJsBridge {
    /**
     * HTTP fetch. Returns JSON string:
     * {"status":200,"statusText":"OK","body":"...","bodyBase64":"...","headers":{"key":"value"}}
     */
    suspend fun fetch(url: String, initJson: String): String

    /**
     * HTTP fetch returning body as text with specific charset encoding.
     */
    suspend fun fetchText(url: String, initJson: String, encoding: String): String

    /**
     * HTTP fetch returning response body as base64-encoded string.
     */
    suspend fun fetchFile(url: String, initJson: String): String

    /** Get a storage value by key. Returns JSON string or null. */
    fun storageGet(pluginId: String, key: String): String?

    /** Set a storage value. valueJson contains the full StoredItem JSON. */
    fun storageSet(pluginId: String, key: String, valueJson: String)

    /** Delete a storage key. */
    fun storageDelete(pluginId: String, key: String)

    /** Get all storage keys for a plugin. Returns JSON array string. */
    fun storageGetAllKeys(pluginId: String): String

    /** Clear all storage for a plugin. */
    fun storageClearAll(pluginId: String)

    /**
     * Suspend for [durationMs] without occupying the JS worker thread. JS code calls this
     * via `await __bridge.sleep(ms)`; backed by `kotlinx.coroutines.delay`.
     */
    suspend fun sleep(durationMs: Int)
}
