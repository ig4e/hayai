package hayai.novel.plugin

import android.content.Context
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.network.NetworkHelper
import hayai.novel.js.JsPluginLoader
import hayai.novel.js.NovelJsBridgeImpl
import hayai.novel.plugin.model.NovelPlugin
import hayai.novel.plugin.model.NovelPluginIndex
import hayai.novel.repo.NovelRepoRepository
import hayai.novel.source.NovelSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File

/**
 * Manages LNReader JavaScript novel plugins.
 * Handles: discovery from repos, downloading, installing, updating, creating NovelSource instances.
 */
class NovelPluginManager(
    private val context: Context,
    private val repoRepository: NovelRepoRepository,
    private val networkHelper: NetworkHelper,
) {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val bridge = NovelJsBridgeImpl(context)
    private val pluginLoader = JsPluginLoader(context)

    private val pluginsDir: File
        get() = File(context.filesDir, "novel_plugins/plugins").also { it.mkdirs() }

    private val _installedSourcesFlow = MutableStateFlow<List<NovelSource>>(emptyList())
    val installedSourcesFlow = _installedSourcesFlow.asStateFlow()

    private val _availablePluginsFlow = MutableStateFlow<List<NovelPluginIndex>>(emptyList())
    val availablePluginsFlow = _availablePluginsFlow.asStateFlow()

    private val _installedPluginsFlow = MutableStateFlow<List<NovelPlugin.Installed>>(emptyList())
    val installedPluginsFlow = _installedPluginsFlow.asStateFlow()

    private val installedSources = mutableMapOf<String, NovelSource>()

    init {
        // Load installed plugins on startup
        scope.launch {
            loadInstalledPlugins()
        }
        // Watch repos for changes
        scope.launch {
            repoRepository.subscribeAll().collectLatest {
                refreshAvailablePlugins()
            }
        }
    }

    /**
     * Load all locally installed plugins from disk.
     */
    private suspend fun loadInstalledPlugins() {
        val pluginDirs = pluginsDir.listFiles()?.filter { it.isDirectory } ?: return
        val sources = mutableListOf<NovelSource>()
        val installed = mutableListOf<NovelPlugin.Installed>()

        for (dir in pluginDirs) {
            val jsFile = File(dir, "index.js")
            if (!jsFile.exists()) continue

            try {
                val code = jsFile.readText()
                val metadata = pluginLoader.extractMetadata(code) ?: continue

                // Read saved metadata for iconUrl and lang
                val savedMeta = try {
                    val metaFile = File(dir, "meta.json")
                    if (metaFile.exists()) json.decodeFromString<NovelPluginIndex>(metaFile.readText()) else null
                } catch (_: Exception) { null }

                val iconUrl = savedMeta?.iconUrl
                val lang = savedMeta?.let { langFromLnReaderLang(it.lang) } ?: getLangCode(dir.name, metadata)

                val source = NovelSource(
                    pluginId = metadata.id,
                    pluginName = metadata.name,
                    lang = lang,
                    siteUrl = metadata.site,
                    pluginCode = code,
                    pluginFilters = metadata.filters,
                    iconUrl = iconUrl,
                    context = context,
                    bridge = bridge,
                    userAgent = getUserAgent(),
                )

                installedSources[metadata.id] = source
                sources.add(source)
                installed.add(
                    NovelPlugin.Installed(
                        id = metadata.id,
                        name = metadata.name,
                        lang = lang,
                        version = metadata.version,
                        siteUrl = metadata.site,
                        iconUrl = iconUrl,
                        source = source,
                    ),
                )

                Logger.d { "NovelPluginManager: Loaded plugin '${metadata.name}' (${metadata.id})" }
            } catch (e: Exception) {
                Logger.e(e) { "NovelPluginManager: Failed to load plugin from ${dir.name}" }
            }
        }

        _installedSourcesFlow.value = sources
        _installedPluginsFlow.value = installed
    }

    /**
     * Fetch available plugins from all configured repos.
     */
    suspend fun refreshAvailablePlugins() {
        val repos = repoRepository.getAll()
        val allPlugins = mutableListOf<NovelPluginIndex>()

        for (repo in repos) {
            try {
                val request = Request.Builder()
                    .url(repo.baseUrl)
                    .addHeader("pragma", "no-cache")
                    .addHeader("cache-control", "no-cache")
                    .build()

                val response = networkHelper.client.newCall(request).execute()
                val body = response.body?.string() ?: continue

                val plugins = json.decodeFromString<List<NovelPluginIndex>>(body)
                allPlugins.addAll(plugins)

                Logger.d { "NovelPluginManager: Fetched ${plugins.size} plugins from ${repo.baseUrl}" }
            } catch (e: Exception) {
                Logger.e(e) { "NovelPluginManager: Failed to fetch plugins from ${repo.baseUrl}" }
            }
        }

        _availablePluginsFlow.value = allPlugins
    }

    /**
     * Install a plugin by downloading its JS code.
     */
    suspend fun installPlugin(plugin: NovelPluginIndex): Boolean {
        return try {
            val request = Request.Builder()
                .url(plugin.url)
                .addHeader("pragma", "no-cache")
                .addHeader("cache-control", "no-cache")
                .build()

            val response = networkHelper.client.newCall(request).execute()
            val code = response.body?.string() ?: return false

            // Validate plugin loads correctly
            val metadata = pluginLoader.extractMetadata(code) ?: return false

            // Save to disk
            val pluginDir = File(pluginsDir, metadata.id).also { it.mkdirs() }
            File(pluginDir, "index.js").writeText(code)

            // Save metadata for lang resolution
            File(pluginDir, "meta.json").writeText(
                json.encodeToString(NovelPluginIndex.serializer(), plugin),
            )

            // Create source and register
            val source = NovelSource(
                pluginId = metadata.id,
                pluginName = metadata.name,
                lang = langFromLnReaderLang(plugin.lang),
                siteUrl = metadata.site,
                pluginCode = code,
                pluginFilters = metadata.filters,
                iconUrl = plugin.iconUrl,
                context = context,
                bridge = bridge,
                userAgent = getUserAgent(),
            )

            installedSources[metadata.id] = source

            // Update flows
            val currentSources = _installedSourcesFlow.value.toMutableList()
            currentSources.removeAll { it.pluginId == metadata.id }
            currentSources.add(source)
            _installedSourcesFlow.value = currentSources

            val currentInstalled = _installedPluginsFlow.value.toMutableList()
            currentInstalled.removeAll { it.id == metadata.id }
            currentInstalled.add(
                NovelPlugin.Installed(
                    id = metadata.id,
                    name = metadata.name,
                    lang = langFromLnReaderLang(plugin.lang),
                    version = metadata.version,
                    siteUrl = metadata.site,
                    iconUrl = plugin.iconUrl,
                    source = source,
                ),
            )
            _installedPluginsFlow.value = currentInstalled

            Logger.d { "NovelPluginManager: Installed plugin '${metadata.name}'" }
            true
        } catch (e: Exception) {
            Logger.e(e) { "NovelPluginManager: Failed to install plugin ${plugin.id}" }
            false
        }
    }

    /**
     * Uninstall a plugin.
     */
    fun uninstallPlugin(pluginId: String) {
        installedSources[pluginId]?.destroy()
        installedSources.remove(pluginId)

        // Delete from disk
        File(pluginsDir, pluginId).deleteRecursively()

        // Update flows
        _installedSourcesFlow.value = _installedSourcesFlow.value.filter { it.pluginId != pluginId }
        _installedPluginsFlow.value = _installedPluginsFlow.value.filter { it.id != pluginId }

        Logger.d { "NovelPluginManager: Uninstalled plugin '$pluginId'" }
    }

    /**
     * Check if a plugin is installed.
     */
    fun isInstalled(pluginId: String): Boolean = installedSources.containsKey(pluginId)

    /**
     * Get installed version of a plugin, or null.
     */
    fun getInstalledVersion(pluginId: String): String? {
        return _installedPluginsFlow.value.find { it.id == pluginId }?.version
    }

    private fun getUserAgent(): String {
        return try {
            android.webkit.WebSettings.getDefaultUserAgent(context)
        } catch (_: Exception) {
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
    }

    /**
     * Get language code from LNReader's plugin directory name or metadata.
     */
    private fun getLangCode(dirName: String, metadata: hayai.novel.js.JsPluginLoader.PluginMetadata): String {
        // Try to read saved metadata
        val metaFile = File(pluginsDir, "${metadata.id}/meta.json")
        if (metaFile.exists()) {
            try {
                val index = json.decodeFromString<NovelPluginIndex>(metaFile.readText())
                return langFromLnReaderLang(index.lang)
            } catch (_: Exception) {
            }
        }
        return "en" // default
    }

    companion object {
        /**
         * Convert LNReader language names to ISO 639-1 codes.
         */
        fun langFromLnReaderLang(lang: String): String = when (lang.lowercase()) {
            "english" -> "en"
            "arabic" -> "ar"
            "chinese" -> "zh"
            "french" -> "fr"
            "german" -> "de"
            "indonesian" -> "id"
            "italian" -> "it"
            "japanese" -> "ja"
            "korean" -> "ko"
            "polish" -> "pl"
            "portuguese" -> "pt"
            "russian" -> "ru"
            "spanish" -> "es"
            "thai" -> "th"
            "turkish" -> "tr"
            "ukrainian" -> "uk"
            "vietnamese" -> "vi"
            "multilingual" -> "all"
            else -> lang.take(2).lowercase()
        }
    }
}
