package exh.nh

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

/**
 * Bundled nhentai.net tag corpus used to power autocomplete in the search filter.
 *
 * The JSON files under `assets/nhtags/` are produced by `tools/nhtags/scrape_nhtags.py`
 * and follow the same shape as `assets/ehtags/` — one array per namespace, each entry
 * already prefixed with `"namespace:"`.
 */
object NHTags {
    private var cachedTags: List<String>? = null

    // Order chosen so the autocomplete dropdown surfaces small, frequently-typed
    // namespaces (categories, languages) before the long ones.
    private val namespaceFiles = listOf(
        "category", "language", "parody", "character", "tag", "group", "artist",
    )

    fun getAllTags(context: Context): List<String> {
        cachedTags?.let { return it }
        val all = namespaceFiles.flatMap { ns -> loadTagFile(context, ns) }
        cachedTags = all
        return all
    }

    fun getNamespaces(): List<String> = namespaceFiles

    private fun loadTagFile(context: Context, namespace: String): List<String> {
        return try {
            val json = context.assets.open("nhtags/$namespace.json")
                .bufferedReader()
                .use { it.readText() }
            Json.parseToJsonElement(json).jsonArray.map { it.jsonPrimitive.content }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun invalidateCache() {
        cachedTags = null
    }
}
