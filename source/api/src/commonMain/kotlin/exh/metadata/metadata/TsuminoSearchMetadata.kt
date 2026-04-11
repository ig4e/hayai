package exh.metadata.metadata

import android.content.Context
import androidx.core.net.toUri
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import exh.metadata.MetadataUtil
import exh.util.nullIfEmpty
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

@Serializable
class TsuminoSearchMetadata : RaisedSearchMetadata() {
    var tmId: Int? = null

    var title by titleDelegate(TITLE_TYPE_MAIN)

    var artist: String? = null

    var uploadDate: Long? = null

    var length: Int? = null

    var ratingString: String? = null

    var averageRating: Float? = null

    var userRatings: Long? = null

    var favorites: Long? = null

    var category: String? = null

    var collection: String? = null

    var group: String? = null

    var parody: List<String> = emptyList()

    var character: List<String> = emptyList()

    override fun createMangaInfo(manga: SManga): SManga {
        val title = title
        val cover = tmId?.let { BASE_URL.replace("www", "content") + thumbUrlFromId(it.toString()) }

        val artist = artist

        val status = SManga.UNKNOWN

        // Copy tags -> genres
        val genres = tagsToGenreString()

        val description = "meta"

        return manga.copy(
            title = title ?: manga.title,
            thumbnail_url = cover ?: manga.thumbnail_url,
            artist = artist ?: manga.artist,
            status = status,
            genre = genres,
            description = description,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return listOfNotNull(
            getItem(tmId) { "ID" },
            getItem(title) { "Title" },
            getItem(uploader) { "Uploader" },
            getItem(
                uploadDate,
                {
                    MetadataUtil.EX_DATE_FORMAT
                        .format(ZonedDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault()))
                },
            ) {
                "Date posted"
            },
            getItem(length) { "Page count" },
            getItem(ratingString) { "Rating string" },
            getItem(averageRating) { "Average rating" },
            getItem(userRatings) { "Total ratings" },
            getItem(favorites) { "Total favorites" },
            getItem(category) { "Genre" },
            getItem(collection) { "Collection" },
            getItem(group) { "Group" },
            getItem(parody.nullIfEmpty(), { it.joinToString() }) { "Parodies" },
            getItem(character.nullIfEmpty(), { it.joinToString() }) { "Characters" },
        )
    }

    companion object {
        private const val TITLE_TYPE_MAIN = 0

        const val TAG_TYPE_DEFAULT = 0

        val BASE_URL = "https://www.tsumino.com"

        val TSUMINO_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        fun tmIdFromUrl(url: String) = url.toUri().lastPathSegment

        fun thumbUrlFromId(id: String) = "/thumbs/$id/1"
    }
}
