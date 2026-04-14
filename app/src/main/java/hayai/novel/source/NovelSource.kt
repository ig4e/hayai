package hayai.novel.source

import android.content.Context
import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import hayai.novel.js.NovelJsBridge
import hayai.novel.js.NovelJsRuntime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * A CatalogueSource implementation backed by an LNReader JavaScript plugin.
 * Delegates all data fetching to the JS plugin via NovelJsRuntime.
 * Novels are mapped to SManga, chapters to SChapter - treated identically to manga
 * throughout the app, with the only difference at the reader layer (text vs images).
 */
class NovelSource(
    val pluginId: String,
    private val pluginName: String,
    override val lang: String,
    private val siteUrl: String,
    private val pluginCode: String,
    val iconUrl: String?,
    private val context: Context,
    private val bridge: NovelJsBridge,
    private val userAgent: String = "",
) : CatalogueSource, TextSource {

    private val json = Json { ignoreUnknownKeys = true }

    override val name: String get() = pluginName

    override val id: Long by lazy {
        val key = "novel/${pluginId.lowercase()}/$lang/1"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        (0..7).map { bytes[it].toLong() and 0xff shl (8 * (7 - it)) }
            .reduce(Long::or) and Long.MAX_VALUE
    }

    override val supportsLatest: Boolean = true

    // Lazy-initialized JS runtime
    private var runtime: NovelJsRuntime? = null

    private suspend fun ensureRuntime(): NovelJsRuntime {
        val existing = runtime
        if (existing != null && existing.isInitialized) return existing

        val rt = NovelJsRuntime(context, bridge, pluginId, userAgent)
        rt.initialize(pluginCode)
        runtime = rt
        return rt
    }

    // --- CatalogueSource methods ---

    override suspend fun getPopularManga(page: Int): MangasPage {
        val rt = ensureRuntime()
        val resultJson = rt.callMethod("popularNovels", "$page, {showLatestNovels: false, filters: undefined}")
        return parseNovelItems(resultJson)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        val rt = ensureRuntime()
        val resultJson = rt.callMethod("popularNovels", "$page, {showLatestNovels: true, filters: undefined}")
        return parseNovelItems(resultJson)
    }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        val rt = ensureRuntime()
        val escapedQuery = query.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
        val resultJson = rt.callMethod("searchNovels", "\"$escapedQuery\", $page")
        return parseNovelItems(resultJson)
    }

    override suspend fun getMangaDetails(manga: SManga): SManga {
        val rt = ensureRuntime()
        val escapedUrl = manga.url.replace("\\", "\\\\").replace("\"", "\\\"")
        val resultJson = rt.callMethod("parseNovel", "\"$escapedUrl\"")
        return parseSourceNovel(resultJson, manga)
    }

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        val rt = ensureRuntime()
        val escapedUrl = manga.url.replace("\\", "\\\\").replace("\"", "\\\"")
        val resultJson = rt.callMethod("parseNovel", "\"$escapedUrl\"")
        return parseChapterList(resultJson)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        // Novel chapters don't have image pages.
        // Return a single placeholder page. The actual text content
        // is loaded by NovelPageLoader via getChapterText().
        return listOf(Page(0, chapter.url))
    }

    override fun getFilterList(): FilterList {
        // TODO: Parse plugin filters property into Tachiyomi FilterList
        // For now, return empty. Filters will be implemented in a follow-up.
        return FilterList()
    }

    // --- Novel-specific methods ---

    /**
     * Fetch chapter text content (HTML string).
     * Called by NovelPageLoader and the download system.
     */
    suspend fun getChapterText(chapterUrl: String): String {
        val rt = ensureRuntime()
        val escapedUrl = chapterUrl.replace("\\", "\\\\").replace("\"", "\\\"")
        val resultJson = rt.callMethod("parseChapter", "\"$escapedUrl\"")
        // parseChapter returns a string (HTML), JSON-stringified it becomes a quoted string
        return try {
            json.decodeFromString<String>(resultJson)
        } catch (_: Exception) {
            // If it's not a valid JSON string, return as-is (strip quotes if present)
            resultJson.removeSurrounding("\"")
        }
    }

    /**
     * Resolve a relative URL to absolute using the plugin's resolveUrl or site URL.
     */
    fun resolveUrl(path: String, isNovel: Boolean = false): String {
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("//")) {
            return path
        }
        val site = siteUrl.trimEnd('/')
        val cleanPath = path.trimStart('/')
        return "$site/$cleanPath"
    }

    fun destroy() {
        runtime?.close()
        runtime = null
    }

    // --- Parsing helpers ---

    private fun parseNovelItems(jsonStr: String): MangasPage {
        return try {
            val items = json.decodeFromString<JsonArray>(jsonStr)
            val mangas = items.mapNotNull { element ->
                try {
                    val obj = element.jsonObject
                    SManga.create().apply {
                        title = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        url = obj["path"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        thumbnail_url = obj["cover"]?.jsonPrimitive?.contentOrNull?.let { resolveUrl(it) }
                    }
                } catch (e: Exception) {
                    Logger.w(e) { "NovelSource: Failed to parse novel item" }
                    null
                }
            }
            // LNReader plugins return empty array when no more pages
            MangasPage(mangas, mangas.isNotEmpty())
        } catch (e: Exception) {
            Logger.e(e) { "NovelSource: Failed to parse novel items" }
            MangasPage(emptyList(), false)
        }
    }

    private fun parseSourceNovel(jsonStr: String, existing: SManga): SManga {
        return try {
            val obj = json.decodeFromString<JsonObject>(jsonStr)
            existing.apply {
                obj["name"]?.jsonPrimitive?.contentOrNull?.let { title = it }
                obj["author"]?.jsonPrimitive?.contentOrNull?.let { author = it }
                obj["artist"]?.jsonPrimitive?.contentOrNull?.let { artist = it }
                obj["summary"]?.jsonPrimitive?.contentOrNull?.let { description = it }
                obj["cover"]?.jsonPrimitive?.contentOrNull?.let { thumbnail_url = resolveUrl(it) }

                val genres = obj["genres"]?.jsonPrimitive?.contentOrNull
                genre = if (genres != null) {
                    "Novel, $genres"
                } else {
                    "Novel"
                }

                status = when (obj["status"]?.jsonPrimitive?.contentOrNull) {
                    "Ongoing" -> SManga.ONGOING
                    "Completed" -> SManga.COMPLETED
                    "Licensed" -> SManga.LICENSED
                    "Publishing Finished" -> SManga.PUBLISHING_FINISHED
                    "Cancelled" -> SManga.CANCELLED
                    "On Hiatus" -> SManga.ON_HIATUS
                    else -> SManga.UNKNOWN
                }

                initialized = true
            }
        } catch (e: Exception) {
            Logger.e(e) { "NovelSource: Failed to parse novel details" }
            existing
        }
    }

    private fun parseChapterList(jsonStr: String): List<SChapter> {
        return try {
            val obj = json.decodeFromString<JsonObject>(jsonStr)
            val chapters = obj["chapters"]?.jsonArray ?: return emptyList()

            chapters.mapNotNull { element ->
                try {
                    val ch = element.jsonObject
                    SChapter.create().apply {
                        name = ch["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        url = ch["path"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        chapter_number = ch["chapterNumber"]?.jsonPrimitive?.floatOrNull ?: -1f
                        date_upload = parseReleaseTime(ch["releaseTime"]?.jsonPrimitive?.contentOrNull)
                    }
                } catch (e: Exception) {
                    Logger.w(e) { "NovelSource: Failed to parse chapter" }
                    null
                }
            }
        } catch (e: Exception) {
            Logger.e(e) { "NovelSource: Failed to parse chapter list" }
            emptyList()
        }
    }

    private fun parseReleaseTime(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        return try {
            // Try ISO format first
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(dateStr)?.time ?: 0L
        } catch (_: Exception) {
            try {
                // Try simple date format
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time ?: 0L
            } catch (_: Exception) {
                try {
                    // Try as epoch millis
                    dateStr.toLong()
                } catch (_: Exception) {
                    0L
                }
            }
        }
    }
}
