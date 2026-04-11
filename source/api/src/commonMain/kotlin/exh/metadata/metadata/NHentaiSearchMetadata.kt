package exh.metadata.metadata

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import exh.metadata.MetadataUtil
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Serializable
class NHentaiSearchMetadata : RaisedSearchMetadata() {
    var url get() = nhId?.let { BASE_URL + nhIdToPath(it) }
        set(a) {
            a?.let {
                nhId = nhUrlToId(a)
            }
        }

    var nhId: Long? = null

    var uploadDate: Long? = null

    var favoritesCount: Long? = null

    var mediaId: String? = null

    var japaneseTitle by titleDelegate(TITLE_TYPE_JAPANESE)
    var englishTitle by titleDelegate(TITLE_TYPE_ENGLISH)
    var shortTitle by titleDelegate(TITLE_TYPE_SHORT)

    var coverImageUrl: String? = null
    var pageImagePreviewUrls: List<String> = emptyList()

    var scanlator: String? = null

    var preferredTitle: Int? = null

    override fun createMangaInfo(manga: SManga): SManga {
        val key = nhId?.let { nhIdToPath(it) }

        val title = when (preferredTitle) {
            TITLE_TYPE_SHORT -> shortTitle ?: englishTitle ?: japaneseTitle ?: manga.title
            0, TITLE_TYPE_ENGLISH -> englishTitle ?: japaneseTitle ?: shortTitle ?: manga.title
            else -> englishTitle ?: japaneseTitle ?: shortTitle ?: manga.title
        }

        // Set artist (if we can find one)
        val artist = tags.ofNamespace(NHENTAI_ARTIST_NAMESPACE).let { tags ->
            if (tags.isNotEmpty()) tags.joinToString(transform = { it.name }) else null
        }

        // Set group (if we can find one)
        val group = tags.ofNamespace(NHENTAI_GROUP_NAMESPACE).let { tags ->
            if (tags.isNotEmpty()) tags.joinToString(transform = { it.name }) else null
        }

        // Copy tags -> genres
        val genres = tagsToGenreString()

        // Try to automatically identify if it is ongoing, we try not to be too lenient here to avoid making mistakes
        // We default to completed
        var status = SManga.COMPLETED
        englishTitle?.let { t ->
            MetadataUtil.ONGOING_SUFFIX.find {
                t.endsWith(it, ignoreCase = true)
            }?.let {
                status = SManga.ONGOING
            }
        }

        return manga.copy(
            url = key ?: manga.url,
            thumbnail_url = coverImageUrl ?: manga.thumbnail_url,
            title = title,
            artist = group ?: manga.artist,
            author = artist ?: manga.artist,
            genre = genres,
            status = status,
            description = null,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return listOfNotNull(
            getItem(nhId) { "ID" },
            getItem(
                uploadDate,
                {
                    MetadataUtil.EX_DATE_FORMAT
                        .format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(it), ZoneId.systemDefault()))
                },
            ) {
                "Date posted"
            },
            getItem(favoritesCount) { "Total favorites" },
            getItem(mediaId) { "Media ID" },
            getItem(japaneseTitle) { "Japanese title" },
            getItem(englishTitle) { "English title" },
            getItem(shortTitle) { "Short title" },
            getItem(coverImageUrl) { "Thumbnail URL" },
            getItem(pageImagePreviewUrls.size) { "Page count" },
            getItem(scanlator) { "Scanlator" },
        )
    }

    companion object {
        private const val TITLE_TYPE_JAPANESE = 0
        const val TITLE_TYPE_ENGLISH = 1
        const val TITLE_TYPE_SHORT = 2

        const val TAG_TYPE_DEFAULT = 0

        const val BASE_URL = "https://nhentai.net"

        private const val NHENTAI_ARTIST_NAMESPACE = "artist"
        private const val NHENTAI_GROUP_NAMESPACE = "group"
        const val NHENTAI_CATEGORIES_NAMESPACE = "category"

        fun nhUrlToId(url: String) =
            url.split("/").last { it.isNotBlank() }.toLong()

        fun nhIdToPath(id: Long) = "/g/$id/"
    }
}
