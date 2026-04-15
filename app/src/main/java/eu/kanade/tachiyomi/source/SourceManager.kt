package eu.kanade.tachiyomi.source

import android.content.Context
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.DelegatedHttpSource
import eu.kanade.tachiyomi.source.online.HttpSource
// EXH -->
import eu.kanade.tachiyomi.source.online.all.EHentai
import eu.kanade.tachiyomi.source.online.all.MergedSource
import eu.kanade.tachiyomi.source.online.all.NHentai
import exh.source.BlacklistedSources
import exh.source.DelegatedHttpSource as ExhDelegatedHttpSource
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.EnhancedHttpSource
import exh.source.ExhPreferences
import exh.source.MERGED_SOURCE_ID
// EXH <--
// NOVEL -->
import hayai.novel.plugin.NovelPluginManager
import hayai.novel.source.TextSource
// NOVEL <--
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import uy.kohesive.injekt.injectLazy
import yokai.i18n.MR
import yokai.util.lang.getString

class SourceManager(
    private val context: Context,
    private val extensionManager: ExtensionManager,
) {

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val sourcesMapFlow = MutableStateFlow(ConcurrentHashMap<Long, Source>())

    private val stubSourcesMap = ConcurrentHashMap<Long, StubSource>()

    val catalogueSources: Flow<List<CatalogueSource>> = sourcesMapFlow.map { it.values.filterIsInstance<CatalogueSource>() }
    val onlineSources: Flow<List<HttpSource>> = catalogueSources.map { it.filterIsInstance<HttpSource>() }

    // EXH -->
    private val exhPreferences: ExhPreferences by injectLazy()
    // EXH <--

    // NOVEL -->
    private val novelPluginManager: NovelPluginManager by injectLazy()
    // NOVEL <--

    // FIXME: Delegated source, unused at the moment, J2K only delegate deep links
    private val delegatedSources = emptyList<DelegatedSource>().associateBy { it.sourceId }

    init {
        scope.launch {
            extensionManager.installedExtensionsFlow
                // EXH -->
                .combine(exhPreferences.enableExhentai.changes()) { extensions, enableExhentai ->
                    extensions to enableExhentai
                }
                .collectLatest { (extensions, enableExhentai) ->
                // EXH <--
                    val mutableMap = ConcurrentHashMap<Long, Source>(mapOf(LocalSource.ID to LocalSource(context)))

                    // EXH -->
                    mutableMap[EH_SOURCE_ID] = EHentai(EH_SOURCE_ID, false, context)
                    if (enableExhentai) {
                        mutableMap[EXH_SOURCE_ID] = EHentai(EXH_SOURCE_ID, true, context)
                    }
                    mutableMap[MERGED_SOURCE_ID] = MergedSource()
                    // EXH <--

                    extensions.forEach { extension ->
                        extension.sources.forEach {
                            // EXH -->
                            try {
                                val internalSource = it.toInternalSource()
                                if (internalSource != null) {
                                    mutableMap[it.id] = internalSource
                                }
                            } catch (e: Throwable) {
                                Logger.e(e) { "Failed to initialize source ${it.name} (${it.id}) from ${extension.name}" }
                            }
                            // EXH <--
                        }
                    }
                    novelPluginManager.ensureInstalledPluginsLoaded()
                    novelPluginManager.installedSourcesFlow.value.forEach {
                        mutableMap[it.id] = it
                    }
                    sourcesMapFlow.value = mutableMap
                }
        }

        // NOVEL -->
        scope.launch {
            novelPluginManager.installedSourcesFlow.collectLatest { novelSources ->
                val currentMap = ConcurrentHashMap(sourcesMapFlow.value)
                // Remove old novel sources
                currentMap.entries.removeIf { it.value is TextSource }
                // Add current novel sources
                novelSources.forEach { currentMap[it.id] = it }
                sourcesMapFlow.value = currentMap
            }
        }
        // NOVEL <--
    }

    fun get(sourceKey: Long): Source? {
        return sourcesMapFlow.value[sourceKey]
    }

    suspend fun awaitCatalogueSource(sourceKey: Long): CatalogueSource? {
        (get(sourceKey) as? CatalogueSource)?.let { return it }

        novelPluginManager.ensureInstalledPluginsLoaded()

        return (sourcesMapFlow.value[sourceKey] as? CatalogueSource)
            ?: novelPluginManager.installedSourcesFlow.value.firstOrNull { it.id == sourceKey }
    }

    fun getOrStub(sourceKey: Long): Source {
        sourcesMapFlow.value[sourceKey]?.let { return it }

        val novelSource = runBlocking {
            novelPluginManager.ensureInstalledPluginsLoaded()
            novelPluginManager.installedSourcesFlow.value.firstOrNull { it.id == sourceKey }
        }
        if (novelSource != null) {
            val currentMap = ConcurrentHashMap(sourcesMapFlow.value)
            currentMap[sourceKey] = novelSource
            sourcesMapFlow.value = currentMap
            stubSourcesMap.remove(sourceKey)
            return novelSource
        }

        return stubSourcesMap.getOrPut(sourceKey) {
            runBlocking { StubSource(sourceKey) }
        }
    }

    fun isDelegatedSource(source: Source): Boolean {
        return delegatedSources.values.count { it.sourceId == source.id } > 0
    }

    fun getDelegatedSource(urlName: String): DelegatedHttpSource? {
        return delegatedSources.values.find { it.urlName == urlName }?.delegatedHttpSource
    }

    fun getOnlineSources() = sourcesMapFlow.value.values.filterIsInstance<HttpSource>()

    fun getCatalogueSources() = sourcesMapFlow.value.values.filterIsInstance<CatalogueSource>()

    @Suppress("OverridingDeprecatedMember")
    inner class StubSource(override val id: Long) : Source {

        override val name: String
            get() = extensionManager.getStubSource(id)?.name ?: id.toString()

        override suspend fun getMangaDetails(manga: SManga): SManga =
            throw getSourceNotInstalledException()

        override suspend fun getChapterList(manga: SManga): List<SChapter> =
            throw getSourceNotInstalledException()

        override suspend fun getPageList(chapter: SChapter): List<Page> =
            throw getSourceNotInstalledException()

        override fun toString(): String {
            return name
        }

        private fun getSourceNotInstalledException(): Exception {
            return SourceNotFoundException(
                context.getString(
                    MR.strings.source_not_installed_,
                    extensionManager.getStubSource(id)?.name ?: id.toString(),
                ),
                id,
            )
        }

        override fun hashCode(): Int {
            return id.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as StubSource
            return id == other.id
        }
    }

    private data class DelegatedSource(
        val urlName: String,
        val sourceId: Long,
        val delegatedHttpSource: DelegatedHttpSource,
    )

    // EXH -->
    /**
     * Wraps extension sources with EXH delegated sources when they match
     * the DELEGATED_SOURCES table. Returns null if source is blacklisted.
     */
    private fun Source.toInternalSource(): Source? {
        val sourceQName = this::class.qualifiedName
        val factories = DELEGATED_SOURCES.entries
            .filter { it.value.factory }
            .map { it.value.originalSourceQualifiedClassName }
        val delegate = if (sourceQName != null) {
            val matched = factories.find { sourceQName.startsWith(it) }
            if (matched != null) {
                DELEGATED_SOURCES[matched]
            } else {
                DELEGATED_SOURCES[sourceQName]
            }
        } else {
            null
        }
        val newSource = if (this is HttpSource && delegate != null) {
            try {
                val constructor = delegate.newSourceClass.constructors.find { it.parameters.size == 2 }
                if (constructor == null) {
                    Logger.e { "No 2-param constructor found for delegate ${delegate.newSourceClass.simpleName}, skipping delegation for ${this.name}" }
                    this
                } else {
                    val enhancedSource = EnhancedHttpSource(
                        this,
                        constructor.call(this, context),
                    )

                    currentDelegatedSources[enhancedSource.originalSource.id] = DelegatedSourceEntry(
                        enhancedSource.originalSource.name,
                        enhancedSource.originalSource.id,
                        enhancedSource.originalSource::class.qualifiedName ?: delegate.originalSourceQualifiedClassName,
                        (enhancedSource.enhancedSource as ExhDelegatedHttpSource)::class,
                        delegate.factory,
                    )
                    enhancedSource
                }
            } catch (e: Throwable) {
                Logger.e(e) { "Failed to wrap ${this.name} with delegated source, using original" }
                this
            }
        } else {
            this
        }

        return if (id in BlacklistedSources.BLACKLISTED_EXT_SOURCES) {
            null
        } else {
            newSource
        }
    }

    companion object {
        private const val fillInSourceId = Long.MAX_VALUE

        // Delegation table: maps extension qualified class names to EXH enhanced sources.
        // When an extension source matches, it gets wrapped with EnhancedHttpSource
        // for metadata handling. Factory sources match by prefix (e.g., multi-lang).
        // Additional delegated sources (Pururin, Tsumino, HBrowse, 8Muses, MangaDex,
        // LANraragi) will be added here as their DelegatedHttpSource implementations
        // are ported from SY.
        val DELEGATED_SOURCES = listOf(
            DelegatedSourceEntry(
                "NHentai",
                fillInSourceId,
                "eu.kanade.tachiyomi.extension.all.nhentai.NHentai",
                NHentai::class,
                true,
            ),
        ).associateBy { it.originalSourceQualifiedClassName }

        val currentDelegatedSources: MutableMap<Long, DelegatedSourceEntry> = mutableMapOf()

        data class DelegatedSourceEntry(
            val sourceName: String,
            val sourceId: Long,
            val originalSourceQualifiedClassName: String,
            val newSourceClass: KClass<out ExhDelegatedHttpSource>,
            val factory: Boolean = false,
        )
    }
    // EXH <--
}

class SourceNotFoundException(message: String, val id: Long) : Exception(message)
