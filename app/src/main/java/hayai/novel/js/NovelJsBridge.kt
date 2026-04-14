package hayai.novel.js

/**
 * Interface registered into QuickJS as "__bridge".
 * All methods are synchronous from JS perspective (QuickJS is single-threaded).
 * Kotlin implementation runs them on the calling coroutine's IO dispatcher.
 */
interface NovelJsBridge {
    /**
     * HTTP fetch. Returns JSON string:
     * {"status":200,"statusText":"OK","body":"...","bodyBase64":"...","headers":{"key":"value"}}
     */
    fun fetch(url: String, initJson: String): String

    /**
     * HTTP fetch returning body as text with specific charset encoding.
     */
    fun fetchText(url: String, initJson: String, encoding: String): String

    /**
     * HTTP fetch returning response body as base64-encoded string.
     */
    fun fetchFile(url: String, initJson: String): String

    /**
     * Protobuf fetch - encoding/decoding delegated to Kotlin.
     * Returns JSON string of decoded protobuf response.
     */
    fun fetchProto(protoInitJson: String, url: String, initJson: String): String

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
}
