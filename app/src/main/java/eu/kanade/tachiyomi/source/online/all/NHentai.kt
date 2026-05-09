package eu.kanade.tachiyomi.source.online.all

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import eu.kanade.tachiyomi.source.PagePreviewInfo
import eu.kanade.tachiyomi.source.PagePreviewPage
import eu.kanade.tachiyomi.source.PagePreviewSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.source.online.MetadataSource
import eu.kanade.tachiyomi.source.online.NamespaceSource
import eu.kanade.tachiyomi.source.online.UrlImportableSource
import exh.metadata.metadata.NHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.nh.NHTags
import exh.source.DelegatedHttpSource
import exh.util.dropBlank
import exh.util.trimAll
import exh.util.trimOrNull
import exh.util.urlImportFetchSearchManga
import exh.util.urlImportFetchSearchMangaSuspend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.CacheControl
import okhttp3.Response

class NHentai(delegate: HttpSource, val context: Context) :
    DelegatedHttpSource(delegate),
    MetadataSource<NHentaiSearchMetadata, Response>,
    UrlImportableSource,
    NamespaceSource,
    PagePreviewSource {
    override val metaClass = NHentaiSearchMetadata::class
    override fun newMetaInstance() = NHentaiSearchMetadata()
    override val lang = delegate.lang

    // DelegatedHttpSource abstract members
    override val domainName: String = "nhentai"
    override fun canOpenUrl(uri: Uri): Boolean {
        return uri.pathSegments.firstOrNull()?.lowercase() == "g"
    }
    override fun chapterUrl(uri: Uri): String? = null
    override suspend fun fetchMangaFromChapterUrl(uri: Uri): Triple<SChapter, SManga, List<SChapter>>? = null

    private val sourcePreferences: SharedPreferences by lazy {
        context.getSharedPreferences("source_$id", 0x0000)
    }

    private val preferredTitle: Int
        get() = when (sourcePreferences.getString(TITLE_PREF, "full")) {
            "full" -> NHentaiSearchMetadata.TITLE_TYPE_ENGLISH
            else -> NHentaiSearchMetadata.TITLE_TYPE_SHORT
        }

    // Support direct URL importing
    @Deprecated("Use the non-RxJava API instead", replaceWith = ReplaceWith("getSearchManga"))
    override fun fetchSearchManga(page: Int, query: String, filters: FilterList) =
        urlImportFetchSearchManga(context, query) {
            val (mergedQuery, strippedFilters) = mergeAutoCompleteTags(query, filters)
            @Suppress("DEPRECATION")
            super<DelegatedHttpSource>.fetchSearchManga(page, mergedQuery, strippedFilters)
        }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        return urlImportFetchSearchMangaSuspend(context, query) {
            val (mergedQuery, strippedFilters) = mergeAutoCompleteTags(query, filters)
            super<DelegatedHttpSource>.getSearchManga(page, mergedQuery, strippedFilters)
        }
    }

    /**
     * Prepend an [AutoCompleteTags] filter to whatever the bundled extension exposes.
     * The extension never sees the autocomplete filter — [mergeAutoCompleteTags] folds
     * its chips into the query string before delegating.
     */
    override fun getFilterList(): FilterList {
        val delegateFilters = super<DelegatedHttpSource>.getFilterList().toList()
        return FilterList(listOf(AutoCompleteTags()) + delegateFilters)
    }

    /**
     * Convert chips like `tag:big breasts`, `-language:chinese`, `~artist:foo` into nhentai's
     * search syntax (`tag:"big breasts" -language:chinese artist:foo`) and append them to the
     * user-typed query. Returns the rewritten query plus the filter list with all
     * [Filter.AutoComplete] entries removed so the delegate doesn't choke on an unknown filter.
     */
    private fun mergeAutoCompleteTags(query: String, filters: FilterList): Pair<String, FilterList> {
        val autoCompleteEntries = filters
            .filterIsInstance<Filter.AutoComplete>()
            .flatMap { it.state.trimAll().dropBlank() }

        if (autoCompleteEntries.isEmpty()) return query to filters

        val rendered = autoCompleteEntries.mapNotNull(::renderTag).joinToString(" ")
        val mergedQuery = listOf(query.trim(), rendered).filter { it.isNotEmpty() }.joinToString(" ")
        val strippedFilters = FilterList(filters.filterNot { it is Filter.AutoComplete })
        return mergedQuery to strippedFilters
    }

    /** Format a single autocomplete chip into nhentai's `[-]namespace:value` token. */
    private fun renderTag(raw: String): String? {
        var tag = raw.trim()
        if (tag.isEmpty()) return null
        // `~` (OR) isn't part of nhentai's query syntax — treat it as include.
        if (tag.startsWith("~")) tag = tag.removePrefix("~").trim()
        val exclude = tag.startsWith("-")
        if (exclude) tag = tag.removePrefix("-").trim()

        val colon = tag.indexOf(':')
        val (namespace, value) = if (colon > 0 && colon < tag.length - 1) {
            tag.substring(0, colon).trim() to tag.substring(colon + 1).trim()
        } else {
            null to tag
        }
        if (value.isBlank()) return null

        // Quote multi-word values; bare slugs (`big-breasts`) and single words don't need it.
        val needsQuotes = value.any { it.isWhitespace() }
        val rendered = buildString {
            if (exclude) append('-')
            if (namespace != null) append(namespace).append(':')
            if (needsQuotes) append('"').append(value).append('"') else append(value)
        }
        return rendered
    }

    inner class AutoCompleteTags :
        Filter.AutoComplete(
            name = "Tags",
            hint = "Search tags here (e.g. tag:big breasts, -language:chinese)",
            values = NHTags.getNamespaces().map { "$it:" } + NHTags.getAllTags(context),
            skipAutoFillTags = NHTags.getNamespaces().map { "$it:" },
            validPrefixes = listOf("-", "~"),
            state = emptyList(),
        )

    override suspend fun getMangaDetails(manga: SManga): SManga {
        val response = client.newCall(mangaDetailsRequest(manga)).awaitSuccess()
        return parseToManga(manga, response)
    }

    override suspend fun parseIntoMetadata(metadata: NHentaiSearchMetadata, input: Response) {
        if (nhConfig == null) getNhConfig()
        val jsonResponse = jsonParser.decodeFromString<JsonResponse>(input.body.string())

        with(metadata) {
            nhId = jsonResponse.id

            uploadDate = jsonResponse.uploadDate

            favoritesCount = jsonResponse.numFavorites

            mediaId = jsonResponse.mediaId

            jsonResponse.title?.let { title ->
                japaneseTitle = title.japanese
                shortTitle = title.pretty
                englishTitle = title.english
            }

            preferredTitle = this@NHentai.preferredTitle

            coverImageUrl =
                jsonResponse.cover?.path?.let { "$thumbServer/$it" }
                    ?: jsonResponse.thumbnail?.path?.let { "$thumbServer/$it" }

            pageImagePreviewUrls = jsonResponse.pages.mapNotNull { it.thumbnail }

            scanlator = jsonResponse.scanlator?.trimOrNull()

            tags.clear()
            jsonResponse.tags.filter {
                it.type != null && it.name != null
            }.mapTo(tags) {
                RaisedTag(
                    it.type!!,
                    it.name!!,
                    if (it.type == NHentaiSearchMetadata.NHENTAI_CATEGORIES_NAMESPACE) {
                        RaisedSearchMetadata.TAG_TYPE_VIRTUAL
                    } else {
                        NHentaiSearchMetadata.TAG_TYPE_DEFAULT
                    },
                )
            }
        }
    }

    @Serializable
    data class JsonConfig(
        @SerialName("image_servers")
        val imageServers: List<String> = emptyList(),
        @SerialName("thumb_servers")
        val thumbServers: List<String> = emptyList(),
    )

    @Serializable
    data class JsonResponse(
        val id: Long,
        @SerialName("media_id")
        val mediaId: String? = null,
        val title: JsonTitle? = null,
        val cover: JsonPage? = null,
        val thumbnail: JsonPage? = null,
        val scanlator: String? = null,
        @SerialName("upload_date")
        val uploadDate: Long? = null,
        val tags: List<JsonTag> = emptyList(),
        @SerialName("num_pages")
        val numPages: Int? = null,
        @SerialName("num_favorites")
        val numFavorites: Long? = null,
        val pages: List<JsonPage> = emptyList(),
    )

    @Serializable
    data class JsonTitle(
        val english: String? = null,
        val japanese: String? = null,
        val pretty: String? = null,
    )

    @Serializable
    data class JsonPage(
        val path: String? = null,
        val width: Long? = null,
        val height: Long? = null,
        val thumbnail: String? = null,
    )

    @Serializable
    data class JsonTag(
        val id: Long? = null,
        val type: String? = null,
        val name: String? = null,
        val url: String? = null,
        val count: Long? = null,
    )

    override val matchingHosts = listOf(
        "nhentai.net",
    )

    override suspend fun mapUrlToMangaUrl(uri: Uri): String? {
        if (uri.pathSegments.firstOrNull()?.lowercase() != "g") {
            return null
        }

        return "$baseUrl/g/${uri.pathSegments[1]}/"
    }

    override suspend fun getPagePreviewList(manga: SManga, chapters: List<SChapter>, page: Int): PagePreviewPage {
        if (nhConfig == null) getNhConfig()
        val metadata = fetchOrLoadMetadata(manga.id()) {
            client.newCall(mangaDetailsRequest(manga)).awaitSuccess()
        }
        return PagePreviewPage(
            page,
            metadata.pageImagePreviewUrls.mapIndexed { index, path ->
                PagePreviewInfo(
                    index + 1,
                    imageUrl = "$thumbServer/$path",
                )
            },
            false,
            1,
        )
    }

    var nhConfig: JsonConfig? = null
    suspend fun getNhConfig() {
        try {
            val response =
                withContext(Dispatchers.IO) { client.newCall(GET("https://nhentai.net/api/v2/config", headers)).awaitSuccess() }
            val body = response.body.string()
            nhConfig = jsonParser.decodeFromString<JsonConfig>(body)
        } catch (_: Exception) {
            nhConfig = JsonConfig(
                (1..4).map { n -> "https://i$n.nhentai.net" },
                (1..4).map { n -> "https://t$n.nhentai.net" },
            )
        }
    }

    val thumbServer
        get() = nhConfig?.thumbServers?.random()

    override suspend fun fetchPreviewImage(page: PagePreviewInfo, cacheControl: CacheControl?): Response {
        return client.newCachelessCallWithProgress(
            if (cacheControl != null) {
                GET(page.imageUrl, cache = cacheControl)
            } else {
                GET(page.imageUrl)
            },
            page,
        ).awaitSuccess()
    }

    companion object {
        const val otherId = 7309872737163460316L

        private val jsonParser = Json {
            ignoreUnknownKeys = true
        }
        private const val TITLE_PREF = "Display manga title as:"
    }
}
