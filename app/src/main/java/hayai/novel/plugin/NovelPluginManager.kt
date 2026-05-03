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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.Request
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import yokai.domain.base.models.Version

/**
 * Manages LNReader JavaScript novel plugins.
 * Handles: discovery from repos, downloading, installing, updating, creating NovelSource instances.
 */
class NovelPluginManager(
    private val context: Context,
    private val repoRepository: NovelRepoRepository,
    private val networkHelper: NetworkHelper,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }
    private val bridge = NovelJsBridgeImpl(context)
    private val pluginLoader = JsPluginLoader(context)
    private val cache = NovelPluginCache(context, json)

    private val pluginsDir: File
        get() = File(context.filesDir, "novel_plugins/plugins").also { it.mkdirs() }

    private val _installedSourcesFlow = MutableStateFlow<List<NovelSource>>(emptyList())
    val installedSourcesFlow = _installedSourcesFlow.asStateFlow()

    private val _availablePluginsFlow = MutableStateFlow<List<NovelPluginIndex>>(emptyList())
    val availablePluginsFlow = _availablePluginsFlow.asStateFlow()

    private val _installedPluginsFlow = MutableStateFlow<List<NovelPlugin.Installed>>(emptyList())
    val installedPluginsFlow = _installedPluginsFlow.asStateFlow()

    // ConcurrentHashMap so concurrent reads (isInstalled, getOrStub) are safe even between
    // mutex-guarded mutations — and so iteration during put/remove can't ConcurrentModificationException.
    private val installedSources = ConcurrentHashMap<String, NovelSource>()
    // Single mutex serializes the initial disk load and all install/uninstall mutations so
    // they can never race with each other on `installedSources` or the flow values.
    private val installMutex = Mutex()
    @Volatile
    private var installedPluginsLoaded = false

    // Cache the system UA once at construction. WebSettings.getDefaultUserAgent internally
    // touches a WebView, which on some Android versions throws or returns "" if invoked from
    // a thread without a Looper. Per-plugin calls from Dispatchers.IO previously produced
    // inconsistent UAs across sources.
    private val cachedUserAgent: String = computeUserAgent()

    init {
        // Load installed plugins on startup
        scope.launch {
            ensureInstalledPluginsLoaded()
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

        // Each metadata extraction creates its own QuickJS instance, so plugins can load in parallel.
        val loaded = coroutineScope {
            pluginDirs.map { dir ->
                async { loadSinglePlugin(dir) }
            }.awaitAll()
        }.filterNotNull()

        val sources = loaded.map { it.first }
        val installed = loaded.map { it.second }
        installedSources.clear()
        for (source in sources) {
            installedSources[source.pluginId] = source
        }

        _installedSourcesFlow.value = sources
        _installedPluginsFlow.value = installed
    }

    private suspend fun loadSinglePlugin(dir: File): Pair<NovelSource, NovelPlugin.Installed>? {
        val jsFile = File(dir, "index.js")
        if (!jsFile.exists()) return null

        return try {
            val savedMeta = readPluginIndex(dir)
            val cachedSource = cache.readInstalledSource(dir)

            // Only run QuickJS metadata extraction if the cache is missing required fields.
            // For installed plugins this is the cold-start hot path: each call evaluates ~5
            // minified JS bundles (cheerio, dayjs, ...) and easily takes 700ms+ per plugin.
            val needsExtraction = cachedSource?.id.isNullOrBlank() ||
                cachedSource?.name.isNullOrBlank() ||
                cachedSource?.siteUrl.isNullOrBlank()
            val metadata = if (needsExtraction) pluginLoader.extractMetadata(jsFile.readText()) else null

            val sourceId = cachedSource?.id ?: metadata?.id ?: return null
            val sourceName = cachedSource?.name ?: metadata?.name ?: return null
            val sourceSiteUrl = cachedSource?.siteUrl ?: metadata?.site ?: return null
            val sourceVersion = cachedSource?.version ?: metadata?.version ?: ""
            val sourceFilters = cachedSource?.filters ?: metadata?.filters

            val lang = savedMeta?.let { langFromLnReaderLang(it.lang) }
                ?: cachedSource?.lang?.takeIf { it.isNotBlank() }?.let(::langFromLnReaderLang)
                ?: getLangCode(sourceId)
            val iconUrl = savedMeta?.iconUrl ?: cachedSource?.iconUrl

            val source = NovelSource(
                pluginId = sourceId,
                pluginName = sourceName,
                lang = lang,
                siteUrl = sourceSiteUrl,
                // Provider closes over the file path, not the code, so we don't keep tens of
                // KB of JS source resident per installed plugin.
                pluginCodeProvider = { jsFile.readText() },
                pluginFilters = sourceFilters,
                iconUrl = iconUrl,
                context = context,
                bridge = bridge,
                userAgent = cachedUserAgent,
            )

            if (metadata != null) {
                cache.writeInstalledSource(dir, savedMeta, metadata)
            }

            Logger.d { "NovelPluginManager: Loaded plugin '$sourceName' ($sourceId)" }
            source to NovelPlugin.Installed(
                id = sourceId,
                name = sourceName,
                lang = lang,
                version = sourceVersion,
                siteUrl = sourceSiteUrl,
                iconUrl = iconUrl,
                source = source,
            )
        } catch (e: Exception) {
            Logger.e(e) { "NovelPluginManager: Failed to load plugin from ${dir.name}" }
            null
        }
    }

    suspend fun ensureInstalledPluginsLoaded() {
        if (installedPluginsLoaded) return

        installMutex.withLock {
            if (installedPluginsLoaded) return
            loadInstalledPlugins()
            installedPluginsLoaded = true
        }
    }

    /**
     * Fetch available plugins from all configured repos.
     */
    suspend fun refreshAvailablePlugins() {
        val repos = repoRepository.getAll()
        if (repos.isEmpty()) {
            _availablePluginsFlow.value = emptyList()
            return
        }

        val cachedPlugins = repos.flatMap { cache.readRepoPlugins(it.baseUrl) }
        if (cachedPlugins.isNotEmpty()) {
            _availablePluginsFlow.value = dedupePlugins(cachedPlugins)
        }

        val allPlugins = mutableListOf<NovelPluginIndex>()

        for (repo in repos) {
            try {
                val request = Request.Builder()
                    .url(repo.baseUrl)
                    .addHeader("pragma", "no-cache")
                    .addHeader("cache-control", "no-cache")
                    .build()

                val response = networkHelper.client.newCall(request).execute()
                val body = response.use { it.body.string() }
                if (body.isNullOrBlank()) {
                    allPlugins += cache.readRepoPlugins(repo.baseUrl)
                    continue
                }

                val plugins = json.decodeFromString<List<NovelPluginIndex>>(body)
                cache.writeRepoPlugins(repo.baseUrl, plugins)
                allPlugins.addAll(plugins)

                Logger.d { "NovelPluginManager: Fetched ${plugins.size} plugins from ${repo.baseUrl}" }
            } catch (e: Exception) {
                Logger.e(e) { "NovelPluginManager: Failed to fetch plugins from ${repo.baseUrl}" }
                allPlugins += cache.readRepoPlugins(repo.baseUrl)
            }
        }

        val resolvedPlugins = dedupePlugins(allPlugins)
        _availablePluginsFlow.value = resolvedPlugins
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
            val code = response.use { it.body.string() }

            // Validate plugin loads correctly
            val metadata = pluginLoader.extractMetadata(code) ?: return false

            installMutex.withLock {
                // Save to disk
                val pluginDir = File(pluginsDir, metadata.id).also { it.mkdirs() }
                val jsFile = File(pluginDir, "index.js").also { it.writeText(code) }

                // Save metadata for lang resolution
                File(pluginDir, "meta.json").writeText(
                    json.encodeToString(NovelPluginIndex.serializer(), plugin),
                )
                cache.writeInstalledSource(pluginDir, plugin, metadata)

                // Replace any prior in-memory source for this plugin id and dispose its runtime
                // before swapping it out, so the previous QuickJS context isn't leaked.
                installedSources.remove(metadata.id)?.destroy()

                val source = NovelSource(
                    pluginId = metadata.id,
                    pluginName = metadata.name,
                    lang = langFromLnReaderLang(plugin.lang),
                    siteUrl = metadata.site,
                    // Provider closes over the on-disk file, not the network response, so the
                    // plugin code is dropped from heap as soon as `code` falls out of scope.
                    pluginCodeProvider = { jsFile.readText() },
                    pluginFilters = metadata.filters,
                    iconUrl = plugin.iconUrl,
                    context = context,
                    bridge = bridge,
                    userAgent = cachedUserAgent,
                )

                installedSources[metadata.id] = source

                // Update flows
                _installedSourcesFlow.value = _installedSourcesFlow.value
                    .filter { it.pluginId != metadata.id } + source

                _installedPluginsFlow.value = _installedPluginsFlow.value
                    .filter { it.id != metadata.id } +
                    NovelPlugin.Installed(
                        id = metadata.id,
                        name = metadata.name,
                        lang = langFromLnReaderLang(plugin.lang),
                        version = metadata.version,
                        siteUrl = metadata.site,
                        iconUrl = plugin.iconUrl,
                        source = source,
                    )

                Logger.d { "NovelPluginManager: Installed plugin '${metadata.name}'" }
            }
            true
        } catch (e: Exception) {
            Logger.e(e) { "NovelPluginManager: Failed to install plugin ${plugin.id}" }
            false
        }
    }

    /**
     * Uninstall a plugin. Suspends because [NovelSource.destroy] holds a mutex that may be
     * contended with an in-flight ensureRuntime() call from another coroutine.
     */
    suspend fun uninstallPlugin(pluginId: String) {
        installMutex.withLock {
            installedSources.remove(pluginId)?.destroy()

            // Delete from disk
            File(pluginsDir, pluginId).deleteRecursively()

            // Update flows
            _installedSourcesFlow.value = _installedSourcesFlow.value.filter { it.pluginId != pluginId }
            _installedPluginsFlow.value = _installedPluginsFlow.value.filter { it.id != pluginId }

            Logger.d { "NovelPluginManager: Uninstalled plugin '$pluginId'" }
        }
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

    fun getPluginLastModified(pluginId: String): Long {
        val pluginDir = File(pluginsDir, pluginId)
        return if (pluginDir.exists()) pluginDir.lastModified() else 0L
    }

    private fun computeUserAgent(): String {
        return try {
            android.webkit.WebSettings.getDefaultUserAgent(context)
        } catch (_: Exception) {
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
        }
    }

    /**
     * Get language code from LNReader's plugin directory name or metadata.
     */
    private fun getLangCode(pluginId: String): String {
        val pluginDir = File(pluginsDir, pluginId)
        val savedMeta = readPluginIndex(pluginDir)
        return savedMeta?.let { langFromLnReaderLang(it.lang) } ?: "en"
    }

    private fun readPluginIndex(pluginDir: File): NovelPluginIndex? {
        val metaFile = File(pluginDir, "meta.json")
        if (!metaFile.exists()) return null
        return runCatching {
            json.decodeFromString<NovelPluginIndex>(metaFile.readText())
        }.getOrNull()
    }

    private fun dedupePlugins(plugins: List<NovelPluginIndex>): List<NovelPluginIndex> {
        return plugins
            .groupBy { it.id }
            .values
            .map { candidates ->
                candidates.reduce { preferred, candidate ->
                    when {
                        comparePluginVersions(candidate.version, preferred.version) > 0 -> candidate
                        comparePluginVersions(candidate.version, preferred.version) == 0 &&
                            preferred.url.isBlank() && candidate.url.isNotBlank() -> candidate
                        else -> preferred
                    }
                }
            }
            .sortedWith(compareBy({ langFromLnReaderLang(it.lang) }, { it.name }))
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

        fun isVersionNewer(candidate: String, installed: String): Boolean {
            return comparePluginVersions(candidate, installed) > 0
        }

        private fun comparePluginVersions(left: String, right: String): Int {
            return runCatching {
                Version.parse(left).compareTo(Version.parse(right))
            }.getOrElse {
                left.compareTo(right)
            }
        }
    }
}
