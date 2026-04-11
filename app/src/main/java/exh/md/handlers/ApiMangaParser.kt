package exh.md.handlers

import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import exh.md.dto.ChapterDataDto
import exh.md.dto.ChapterDto
import exh.md.dto.MangaDto
import exh.md.dto.StatisticsMangaDto
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import exh.md.utils.asMdMap
import exh.metadata.metadata.MangaDexSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.util.capitalize
import exh.util.floor
import exh.util.nullIfEmpty
import yokai.domain.manga.interactor.GetManga
import uy.kohesive.injekt.injectLazy
import java.util.Locale

class ApiMangaParser(
    private val lang: String,
) {
    private val getManga: GetManga by injectLazy()
    // TODO: Wire up InsertFlatMetadata and GetFlatMetadataById when metadata DB is available

    val metaClass = MangaDexSearchMetadata::class

    /**
     * Use reflection to create a new instance of metadata
     */
    private fun newMetaInstance() = MangaDexSearchMetadata()

    suspend fun parseToManga(
        manga: SManga,
        sourceId: Long,
        input: MangaDto,
        simpleChapters: List<String>,
        statistics: StatisticsMangaDto?,
        coverFileName: String?,
        coverQuality: String,
        altTitlesInDesc: Boolean,
        finalChapterInDesc: Boolean,
        preferExtensionLangTitle: Boolean,
    ): SManga {
        val mangaId = getManga.awaitByUrlAndSource(manga.url, sourceId)?.id
        val metadata = newMetaInstance()

        parseIntoMetadata(
            metadata,
            input,
            simpleChapters,
            statistics,
            coverFileName,
            coverQuality,
            altTitlesInDesc,
            finalChapterInDesc,
            preferExtensionLangTitle,
        )
        if (mangaId != null) {
            metadata.mangaId = mangaId
            // TODO: Insert flat metadata when metadata DB is available
        }

        return metadata.createMangaInfo(manga)
    }

    fun parseIntoMetadata(
        metadata: MangaDexSearchMetadata,
        mangaDto: MangaDto,
        simpleChapters: List<String>,
        statistics: StatisticsMangaDto?,
        coverFileName: String?,
        coverQuality: String,
        altTitlesInDesc: Boolean,
        finalChapterInDesc: Boolean,
        preferExtensionLangTitle: Boolean,
    ) {
        with(metadata) {
            try {
                val mangaAttributesDto = mangaDto.data.attributes
                mdUuid = mangaDto.data.id
                title = MdUtil.getTitleFromManga(mangaAttributesDto, lang, preferExtensionLangTitle)
                altTitles = mangaAttributesDto.altTitles
                    .filter { it.containsKey(lang) || it.containsKey("${mangaAttributesDto.originalLanguage}-ro") }
                    .mapNotNull { it.values.singleOrNull() }.nullIfEmpty()

                val mangaRelationshipsDto = mangaDto.data.relationships
                cover = if (!coverFileName.isNullOrEmpty()) {
                    MdUtil.cdnCoverUrl(mangaDto.data.id, "$coverFileName$coverQuality")
                } else {
                    mangaRelationshipsDto
                        .firstOrNull { relationshipDto -> relationshipDto.type == MdConstants.Types.coverArt }
                        ?.attributes
                        ?.fileName
                        ?.let { coverFileName ->
                            MdUtil.cdnCoverUrl(mangaDto.data.id, "$coverFileName$coverQuality")
                        }
                }
                val rawDesc = MdUtil.getFromLangMap(
                    langMap = mangaAttributesDto.description.asMdMap(),
                    currentLang = lang,
                    originalLanguage = mangaAttributesDto.originalLanguage,
                ).orEmpty()

                description = MdUtil.cleanDescription(rawDesc)
                    .let { if (altTitlesInDesc) MdUtil.addAltTitleToDesc(it, altTitles) else it }
                    .let {
                        if (finalChapterInDesc) {
                            MdUtil.addFinalChapterToDesc(
                                it,
                                mangaAttributesDto.lastVolume,
                                mangaAttributesDto.lastChapter,
                            )
                        } else {
                            it
                        }
                    }

                authors = mangaRelationshipsDto.filter { relationshipDto ->
                    relationshipDto.type.equals(MdConstants.Types.author, true)
                }.mapNotNull { it.attributes?.name }.distinct()

                artists = mangaRelationshipsDto.filter { relationshipDto ->
                    relationshipDto.type.equals(MdConstants.Types.artist, true)
                }.mapNotNull { it.attributes?.name }.distinct()

                langFlag = mangaAttributesDto.originalLanguage
                val lastChapter = mangaAttributesDto.lastChapter?.toFloatOrNull()
                lastChapterNumber = lastChapter?.floor()

                statistics?.rating?.let {
                    rating = it.bayesian?.toFloat()
                }

                mangaAttributesDto.links?.asMdMap<String>()?.let { links ->
                    links["al"]?.let { anilistId = it }
                    links["kt"]?.let { kitsuId = it }
                    links["mal"]?.let { myAnimeListId = it }
                    links["mu"]?.let { mangaUpdatesId = it }
                    links["ap"]?.let { animePlanetId = it }
                }

                val tempStatus = parseStatus(mangaAttributesDto.status)
                val publishedOrCancelled = tempStatus == SManga.PUBLISHING_FINISHED || tempStatus == SManga.CANCELLED
                status = if (
                    mangaAttributesDto.lastChapter != null &&
                    publishedOrCancelled &&
                    mangaAttributesDto.lastChapter in simpleChapters
                ) {
                    SManga.COMPLETED
                } else {
                    tempStatus
                }

                val nonGenres = listOfNotNull(
                    mangaAttributesDto.publicationDemographic
                        ?.let {
                            RaisedTag("Demographic", it.capitalize(Locale.US), MangaDexSearchMetadata.TAG_TYPE_DEFAULT)
                        },
                    mangaAttributesDto.contentRating
                        ?.takeUnless { it == "safe" }
                        ?.let {
                            RaisedTag(
                                "Content Rating",
                                it.capitalize(Locale.US),
                                MangaDexSearchMetadata.TAG_TYPE_DEFAULT,
                            )
                        },
                )

                val genres = nonGenres + mangaAttributesDto.tags
                    .mapNotNull {
                        it.attributes.name[lang] ?: it.attributes.name["en"]
                    }
                    .map {
                        RaisedTag("Tags", it, MangaDexSearchMetadata.TAG_TYPE_DEFAULT)
                    }

                if (tags.isNotEmpty()) tags.clear()
                tags += genres
            } catch (e: Exception) {
                Logger.e("ApiMangaParser") { "Parse into metadata error: ${e.message}" }
                throw e
            }
        }
    }

    private fun parseStatus(status: String?) = when (status) {
        "ongoing" -> SManga.ONGOING
        "completed" -> SManga.PUBLISHING_FINISHED
        "cancelled" -> SManga.CANCELLED
        "hiatus" -> SManga.ON_HIATUS
        else -> SManga.UNKNOWN
    }

    fun chapterListParse(chapterListResponse: List<ChapterDataDto>, groupMap: Map<String, String>): List<SChapter> {
        val now = System.currentTimeMillis()
        return chapterListResponse
            .filterNot { MdUtil.parseDate(it.attributes.publishAt) > now && it.attributes.externalUrl == null }
            .map {
                mapChapter(it, groupMap)
            }
    }

    fun chapterParseForMangaId(chapterDto: ChapterDto): String? {
        return chapterDto.data.relationships.find { it.type.equals("manga", true) }?.id
    }

    fun StringBuilder.appends(string: String): StringBuilder = append("$string ")

    private fun mapChapter(
        networkChapter: ChapterDataDto,
        groups: Map<String, String>,
    ): SChapter {
        val attributes = networkChapter.attributes
        val key = MdUtil.chapterSuffix + networkChapter.id
        val chapterName = StringBuilder()

        if (attributes.volume != null) {
            val vol = "Vol." + attributes.volume
            chapterName.appends(vol)
        }

        if (attributes.chapter.isNullOrBlank().not()) {
            val chp = "Ch.${attributes.chapter}"
            chapterName.appends(chp)
        }

        if (!attributes.title.isNullOrBlank()) {
            if (chapterName.isNotEmpty()) {
                chapterName.appends("-")
            }
            chapterName.append(attributes.title)
        }

        if (chapterName.isEmpty()) {
            chapterName.append("Oneshot")
        }

        val name = chapterName.toString()
        val dateUpload = MdUtil.parseDate(attributes.readableAt)

        val scanlatorName = networkChapter.relationships
            .filter {
                it.type == MdConstants.Types.scanlator
            }
            .mapNotNull { groups[it.id] }
            .map {
                if (it == "no group") {
                    "No Group"
                } else {
                    it
                }
            }
            .toSet()
            .ifEmpty { setOf("No Group") }

        val scanlator = MdUtil.getScanlatorString(scanlatorName)

        return SChapter.create().apply {
            url = key
            this.name = name
            this.scanlator = scanlator
            date_upload = dateUpload
        }
    }
}
