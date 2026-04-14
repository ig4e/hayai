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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.time.Instant
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
    private val pluginFilters: JsonObject? = null,
    val iconUrl: String?,
    private val context: Context,
    private val bridge: NovelJsBridge,
    private val userAgent: String = "",
) : CatalogueSource, TextSource {

    private val json = Json { ignoreUnknownKeys = true }
    private val filterDefinitions = parseFilterDefinitions(pluginFilters)

    override val name: String get() = pluginName
    val baseUrl: String get() = siteUrl
    override val webViewUrl: String get() = baseUrl

    override fun toString() = "$name (${lang.uppercase()})"

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

    override suspend fun getPopularManga(page: Int): MangasPage {
        val rt = ensureRuntime()
        val resultJson = rt.callMethod(
            "popularNovels",
            "$page, {showLatestNovels: false, filters: ${buildFiltersJsArg()}}",
        )
        Logger.d { "NovelSource($pluginId): popularNovels result (first 500 chars): ${resultJson.take(500)}" }
        return parseNovelItems(resultJson)
    }

    override suspend fun getLatestUpdates(page: Int): MangasPage {
        val rt = ensureRuntime()
        val resultJson = rt.callMethod(
            "popularNovels",
            "$page, {showLatestNovels: true, filters: ${buildFiltersJsArg()}}",
        )
        return parseNovelItems(resultJson)
    }

    override suspend fun getSearchManga(page: Int, query: String, filters: FilterList): MangasPage {
        val rt = ensureRuntime()

        if (query.isBlank()) {
            val resultJson = rt.callMethod(
                "popularNovels",
                "$page, {showLatestNovels: false, filters: ${buildFiltersJsArg(filters)}}",
            )
            return parseNovelItems(resultJson)
        }

        val resultJson = rt.callMethod("searchNovels", "${escapeJsString(query)}, $page")
        return parseNovelItems(resultJson)
    }

    override suspend fun getMangaDetails(manga: SManga): SManga {
        val rt = ensureRuntime()
        val resultJson = rt.callMethod("parseNovel", escapeJsString(manga.url))
        return parseSourceNovel(resultJson, manga)
    }

    override suspend fun getChapterList(manga: SManga): List<SChapter> {
        val rt = ensureRuntime()
        val resultJson = rt.callMethod("parseNovel", escapeJsString(manga.url))
        return parseChapterList(resultJson)
    }

    override suspend fun getPageList(chapter: SChapter): List<Page> {
        // Novel chapters don't have image pages.
        // Return a single placeholder page. The actual text content
        // is loaded by NovelPageLoader via getChapterText().
        return listOf(Page(0, chapter.url))
    }

    override fun getFilterList(): FilterList {
        if (filterDefinitions.isEmpty()) {
            return FilterList()
        }
        return FilterList(filterDefinitions.map { it.toFilter() })
    }

    /**
     * Fetch chapter text content (HTML string).
     * Called by NovelPageLoader and the download system.
     */
    suspend fun getChapterText(chapterUrl: String): String {
        val rt = ensureRuntime()
        val resultJson = rt.callMethod("parseChapter", escapeJsString(chapterUrl))
        // parseChapter returns a string (HTML), JSON-stringified it becomes a quoted string
        return try {
            json.decodeFromString<String>(resultJson)
        } catch (_: Exception) {
            // If it's not a valid JSON string, return as-is (strip quotes if present)
            resultJson.removeSurrounding("\"")
        }
    }

    override fun getMangaUrl(manga: SManga): String {
        return resolveUrl(manga.url)
    }

    override fun getChapterUrl(chapter: SChapter): String {
        return resolveUrl(chapter.url)
    }

    override fun getChapterUrl(manga: SManga?, chapter: SChapter): String? {
        val chapterUrl = chapter.url.trim()
        return if (chapterUrl.isBlank()) {
            manga?.let { getMangaUrl(it) }
        } else {
            resolveUrl(chapterUrl)
        }
    }

    /**
     * Resolve a relative URL to absolute using the plugin's resolveUrl or site URL.
     */
    fun resolveUrl(path: String): String {
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

    private fun buildFiltersJsArg(filters: FilterList? = null): String {
        if (filterDefinitions.isEmpty()) {
            return "undefined"
        }

        val filtersByKey = mutableMapOf<String, Filter<*>>()
        filters?.forEach { filter ->
            filterKey(filter)?.let { filtersByKey[it] = filter }
        }

        val filtersJson = buildJsonObject {
            filterDefinitions.forEach { definition ->
                put(definition.key, definition.toJson(filtersByKey[definition.key]))
            }
        }
        return filtersJson.toString()
    }

    private fun filterKey(filter: Filter<*>): String? {
        return when (filter) {
            is LnTextFilter -> filter.key
            is LnSwitchFilter -> filter.key
            is LnPickerFilter -> filter.key
            is LnCheckboxGroupFilter -> filter.key
            is LnXCheckboxGroupFilter -> filter.key
            else -> null
        }
    }

    private fun parseFilterDefinitions(filters: JsonObject?): List<LnFilterDefinition> {
        if (filters == null) {
            return emptyList()
        }

        return filters.entries.mapNotNull { (key, value) ->
            val obj = value as? JsonObject ?: return@mapNotNull null
            val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val label = obj["label"]?.jsonPrimitive?.contentOrNull ?: key

            when (type) {
                FILTER_TYPE_TEXT -> LnTextFilterDefinition(
                    key = key,
                    label = label,
                    defaultValue = obj["value"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                )
                FILTER_TYPE_SWITCH -> LnSwitchFilterDefinition(
                    key = key,
                    label = label,
                    defaultValue = obj["value"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull() ?: false,
                )
                FILTER_TYPE_PICKER -> LnPickerFilterDefinition(
                    key = key,
                    label = label,
                    options = parseFilterOptions(obj),
                    defaultValue = obj["value"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                )
                FILTER_TYPE_CHECKBOX -> LnCheckboxFilterDefinition(
                    key = key,
                    label = label,
                    options = parseFilterOptions(obj),
                    defaultValues = parseStringArray(obj["value"]).toSet(),
                )
                FILTER_TYPE_XCHECKBOX -> {
                    val valueObj = obj["value"] as? JsonObject
                    LnXCheckboxFilterDefinition(
                        key = key,
                        label = label,
                        options = parseFilterOptions(obj),
                        defaultInclude = parseStringArray(valueObj?.get("include")).toSet(),
                        defaultExclude = parseStringArray(valueObj?.get("exclude")).toSet(),
                        includePresent = valueObj?.containsKey("include") == true,
                        excludePresent = valueObj?.containsKey("exclude") == true,
                    )
                }
                else -> null
            }
        }
    }

    private fun parseFilterOptions(obj: JsonObject): List<LnFilterOption> {
        val options = obj["options"] as? JsonArray ?: return emptyList()
        return options.mapNotNull { element ->
            val option = element as? JsonObject ?: return@mapNotNull null
            val label = option["label"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val value = option["value"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            LnFilterOption(label = label, value = value)
        }
    }

    private fun parseStringArray(element: JsonElement?): List<String> {
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { it.jsonPrimitive.contentOrNull }
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

                genre = obj["genres"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }

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
            }.reversed()
        } catch (e: Exception) {
            Logger.e(e) { "NovelSource: Failed to parse chapter list" }
            emptyList()
        }
    }

    private fun parseReleaseTime(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return 0L
        return try {
            Instant.parse(dateStr).toEpochMilli()
        } catch (_: Exception) {
            try {
                // Try ISO format without timezone
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(dateStr)?.time ?: 0L
            } catch (_: Exception) {
                try {
                    // Try simple date format
                    SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)?.time ?: 0L
                } catch (_: Exception) {
                    try {
                        // Try as epoch millis/seconds
                        val epoch = dateStr.toLong()
                        if (dateStr.length <= 10) epoch * 1000 else epoch
                    } catch (_: Exception) {
                        0L
                    }
                }
            }
        }
    }

    private fun escapeJsString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return "\"$escaped\""
    }

    private sealed class LnFilterDefinition(
        open val key: String,
        open val label: String,
    ) {
        abstract fun toFilter(): Filter<*>
        abstract fun toJson(filter: Filter<*>?): JsonObject
    }

    private data class LnFilterOption(
        val label: String,
        val value: String,
    )

    private data class LnTextFilterDefinition(
        override val key: String,
        override val label: String,
        val defaultValue: String,
    ) : LnFilterDefinition(key, label) {
        override fun toFilter(): Filter<*> = LnTextFilter(key, label, defaultValue)

        override fun toJson(filter: Filter<*>?): JsonObject {
            val currentValue = (filter as? LnTextFilter)?.state ?: defaultValue
            return buildJsonObject {
                put("type", FILTER_TYPE_TEXT)
                put("label", label)
                put("value", currentValue)
            }
        }
    }

    private data class LnSwitchFilterDefinition(
        override val key: String,
        override val label: String,
        val defaultValue: Boolean,
    ) : LnFilterDefinition(key, label) {
        override fun toFilter(): Filter<*> = LnSwitchFilter(key, label, defaultValue)

        override fun toJson(filter: Filter<*>?): JsonObject {
            val currentValue = (filter as? LnSwitchFilter)?.state ?: defaultValue
            return buildJsonObject {
                put("type", FILTER_TYPE_SWITCH)
                put("label", label)
                put("value", currentValue)
            }
        }
    }

    private data class LnPickerFilterDefinition(
        override val key: String,
        override val label: String,
        val options: List<LnFilterOption>,
        val defaultValue: String,
    ) : LnFilterDefinition(key, label) {
        override fun toFilter(): Filter<*> {
            val normalizedOptions = if (options.any { it.value == defaultValue }) {
                options
            } else {
                listOf(
                    LnFilterOption(
                        label = if (defaultValue.isBlank()) DEFAULT_PICKER_LABEL else defaultValue,
                        value = defaultValue,
                    ),
                ) + options
            }
            val selectedIndex = normalizedOptions.indexOfFirst { it.value == defaultValue }.coerceAtLeast(0)
            return LnPickerFilter(key, label, normalizedOptions, selectedIndex)
        }

        override fun toJson(filter: Filter<*>?): JsonObject {
            val currentValue = (filter as? LnPickerFilter)?.selectedValue() ?: defaultValue
            return buildJsonObject {
                put("type", FILTER_TYPE_PICKER)
                put("label", label)
                put("value", currentValue)
                putJsonArray("options") {
                    options.forEach { add(optionToJson(it)) }
                }
            }
        }
    }

    private data class LnCheckboxFilterDefinition(
        override val key: String,
        override val label: String,
        val options: List<LnFilterOption>,
        val defaultValues: Set<String>,
    ) : LnFilterDefinition(key, label) {
        override fun toFilter(): Filter<*> {
            return LnCheckboxGroupFilter(
                key = key,
                name = label,
                state = options.map { option ->
                    LnCheckboxOption(
                        optionValue = option.value,
                        name = option.label,
                        state = option.value in defaultValues,
                    )
                },
            )
        }

        override fun toJson(filter: Filter<*>?): JsonObject {
            val currentValues = (filter as? LnCheckboxGroupFilter)?.state
                ?.filter { it.state }
                ?.map { it.optionValue }
                ?: orderedValues(options, defaultValues)

            return buildJsonObject {
                put("type", FILTER_TYPE_CHECKBOX)
                put("label", label)
                putJsonArray("value") {
                    currentValues.forEach { add(JsonPrimitive(it)) }
                }
                putJsonArray("options") {
                    options.forEach { add(optionToJson(it)) }
                }
            }
        }
    }

    private data class LnXCheckboxFilterDefinition(
        override val key: String,
        override val label: String,
        val options: List<LnFilterOption>,
        val defaultInclude: Set<String>,
        val defaultExclude: Set<String>,
        val includePresent: Boolean,
        val excludePresent: Boolean,
    ) : LnFilterDefinition(key, label) {
        override fun toFilter(): Filter<*> {
            return LnXCheckboxGroupFilter(
                key = key,
                name = label,
                state = options.map { option ->
                    val state = when {
                        option.value in defaultInclude -> Filter.TriState.STATE_INCLUDE
                        option.value in defaultExclude -> Filter.TriState.STATE_EXCLUDE
                        else -> Filter.TriState.STATE_IGNORE
                    }
                    LnXCheckboxOption(
                        optionValue = option.value,
                        name = option.label,
                        state = state,
                    )
                },
            )
        }

        override fun toJson(filter: Filter<*>?): JsonObject {
            val group = filter as? LnXCheckboxGroupFilter
            val included = group?.state
                ?.filter { it.state == Filter.TriState.STATE_INCLUDE }
                ?.map { it.optionValue }
                ?: orderedValues(options, defaultInclude)
            val excluded = group?.state
                ?.filter { it.state == Filter.TriState.STATE_EXCLUDE }
                ?.map { it.optionValue }
                ?: orderedValues(options, defaultExclude)

            return buildJsonObject {
                put("type", FILTER_TYPE_XCHECKBOX)
                put("label", label)
                putJsonObject("value") {
                    if (includePresent || included.isNotEmpty()) {
                        putJsonArray("include") {
                            included.forEach { add(JsonPrimitive(it)) }
                        }
                    }
                    if (excludePresent || excluded.isNotEmpty()) {
                        putJsonArray("exclude") {
                            excluded.forEach { add(JsonPrimitive(it)) }
                        }
                    }
                }
                putJsonArray("options") {
                    options.forEach { add(optionToJson(it)) }
                }
            }
        }
    }

    private class LnTextFilter(
        val key: String,
        name: String,
        state: String,
    ) : Filter.Text(name, state)

    private class LnSwitchFilter(
        val key: String,
        name: String,
        state: Boolean,
    ) : Filter.CheckBox(name, state)

    private class LnPickerFilter(
        val key: String,
        name: String,
        private val options: List<LnFilterOption>,
        state: Int,
    ) : Filter.Select<String>(name, options.map { it.label }.toTypedArray(), state) {
        fun selectedValue(): String {
            return options.getOrNull(state)?.value ?: options.firstOrNull()?.value.orEmpty()
        }
    }

    private class LnCheckboxOption(
        val optionValue: String,
        name: String,
        state: Boolean,
    ) : Filter.CheckBox(name, state)

    private class LnCheckboxGroupFilter(
        val key: String,
        name: String,
        state: List<LnCheckboxOption>,
    ) : Filter.Group<LnCheckboxOption>(name, state)

    private class LnXCheckboxOption(
        val optionValue: String,
        name: String,
        state: Int,
    ) : Filter.TriState(name, state)

    private class LnXCheckboxGroupFilter(
        val key: String,
        name: String,
        state: List<LnXCheckboxOption>,
    ) : Filter.Group<LnXCheckboxOption>(name, state)

    companion object {
        private const val FILTER_TYPE_TEXT = "Text"
        private const val FILTER_TYPE_SWITCH = "Switch"
        private const val FILTER_TYPE_PICKER = "Picker"
        private const val FILTER_TYPE_CHECKBOX = "Checkbox"
        private const val FILTER_TYPE_XCHECKBOX = "XCheckbox"
        private const val DEFAULT_PICKER_LABEL = "Any"

        private fun orderedValues(options: List<LnFilterOption>, selected: Set<String>): List<String> {
            if (selected.isEmpty()) {
                return emptyList()
            }

            val ordered = options.mapNotNull { option ->
                option.value.takeIf { it in selected }
            }.toMutableList()

            selected.forEach { value ->
                if (value !in ordered) {
                    ordered.add(value)
                }
            }
            return ordered
        }

        private fun optionToJson(option: LnFilterOption): JsonObject {
            return buildJsonObject {
                put("label", option.label)
                put("value", option.value)
            }
        }
    }
}
