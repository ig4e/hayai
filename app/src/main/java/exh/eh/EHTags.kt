package exh.eh

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

object EHTags {
    private var cachedTags: List<String>? = null
    private var cachedNamespaces: List<String>? = null

    private val namespaceFiles = listOf(
        "reclass", "language", "parody", "character", "group",
        "artist", "cosplayer", "male", "female", "mixed", "other", "location",
    )

    fun getAllTags(context: Context): List<String> {
        cachedTags?.let { return it }
        val all = namespaceFiles.flatMap { ns ->
            loadTagFile(context, ns)
        }
        cachedTags = all
        return all
    }

    fun getNamespaces(): List<String> = namespaceFiles

    private fun loadTagFile(context: Context, namespace: String): List<String> {
        return try {
            val json = context.assets.open("ehtags/$namespace.json")
                .bufferedReader()
                .use { it.readText() }
            Json.parseToJsonElement(json).jsonArray.map { it.jsonPrimitive.content }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun invalidateCache() {
        cachedTags = null
        cachedNamespaces = null
    }
}
