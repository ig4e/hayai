package hayai.novel.plugin

import android.content.Context
import hayai.novel.js.JsPluginLoader
import hayai.novel.plugin.model.NovelPluginIndex
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.security.MessageDigest

internal class NovelPluginCache(
    context: Context,
    private val json: Json,
) {
    private val rootDir = File(context.filesDir, "novel_plugins/cache").also { it.mkdirs() }
    private val repoDir = File(rootDir, "repos").also { it.mkdirs() }

    fun readRepoPlugins(repoUrl: String): List<NovelPluginIndex> {
        val file = repoCacheFile(repoUrl)
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<CachedNovelRepoIndex>(file.readText()).plugins
        }.getOrDefault(emptyList())
    }

    fun writeRepoPlugins(repoUrl: String, plugins: List<NovelPluginIndex>) {
        val file = repoCacheFile(repoUrl)
        val payload = CachedNovelRepoIndex(
            baseUrl = repoUrl,
            plugins = plugins,
            updatedAt = System.currentTimeMillis(),
        )
        file.writeText(json.encodeToString(CachedNovelRepoIndex.serializer(), payload))
    }

    fun readInstalledSource(pluginDir: File): InstalledNovelSourceCache? {
        val file = File(pluginDir, INSTALLED_SOURCE_FILE)
        if (!file.exists()) return null
        return runCatching {
            json.decodeFromString<InstalledNovelSourceCache>(file.readText())
        }.getOrNull()
    }

    fun writeInstalledSource(
        pluginDir: File,
        pluginIndex: NovelPluginIndex?,
        metadata: JsPluginLoader.PluginMetadata,
    ) {
        val payload = InstalledNovelSourceCache(
            id = metadata.id,
            name = metadata.name,
            lang = pluginIndex?.lang?.let(NovelPluginManager::langFromLnReaderLang).orEmpty(),
            version = metadata.version,
            siteUrl = metadata.site,
            iconUrl = pluginIndex?.iconUrl,
            filters = metadata.filters,
        )
        File(pluginDir, INSTALLED_SOURCE_FILE)
            .writeText(json.encodeToString(InstalledNovelSourceCache.serializer(), payload))
    }

    private fun repoCacheFile(repoUrl: String): File {
        return File(repoDir, "${repoUrl.sha256()}.json")
    }

    private fun String.sha256(): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val INSTALLED_SOURCE_FILE = "source.json"
    }
}

@Serializable
internal data class CachedNovelRepoIndex(
    val baseUrl: String,
    val plugins: List<NovelPluginIndex>,
    val updatedAt: Long,
)

@Serializable
internal data class InstalledNovelSourceCache(
    val id: String,
    val name: String,
    val lang: String,
    val version: String,
    val siteUrl: String,
    val iconUrl: String? = null,
    val filters: JsonObject? = null,
)
