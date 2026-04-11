package exh.metadata.metadata

import android.content.Context
import android.net.Uri
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import kotlinx.serialization.Serializable

@Serializable
class LanraragiSearchMetadata : RaisedSearchMetadata() {
    var url get() = arcId?.let { "/reader?id=$it" }
        set(a) {
            a?.let {
                arcId = a
            }
        }

    var arcId: String? = null

    var title: String? = null

    var summary: String? = null

    var pageCount: Int? = null

    var baseUrl: String? = null

    var filename: String? = null

    var extension: String? = null

    override fun createMangaInfo(manga: SManga): SManga {
        val key = url

        val cover = if (baseUrl != null && arcId != null) {
            getThumbnailUri(baseUrl!!, arcId!!, 1)
        } else {
            null
        }

        val title = title
        // Set artist (if we can find one)
        val artist = tags.ofNamespace(LANRARAGI_NAMESPACE_ARTIST).let { tags ->
            if (tags.isNotEmpty()) tags.joinToString(transform = { it.name }) else null
        }

        // Copy tags -> genres
        val genres = tagsToGenreString()

        // We default to completed
        val status = SManga.COMPLETED

        return manga.copy(
            url = key ?: manga.url,
            thumbnail_url = cover ?: manga.thumbnail_url,
            title = title ?: manga.title,
            artist = artist ?: manga.artist,
            author = artist ?: manga.author,
            genre = genres,
            status = status,
            description = summary ?: manga.description,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return listOfNotNull(
            getItem(arcId) { "ID" },
            getItem(pageCount) { "Page count" },
            getItem(filename) { "Filename" },
            getItem(extension) { "File extension" },
            getItem(baseUrl) { "Base URL" },
        )
    }

    companion object {
        const val TAG_TYPE_DEFAULT = 0

        const val LANRARAGI_NAMESPACE_OTHER = "other"

        const val LANRARAGI_NAMESPACE_DATE_ADDED = "date_added"

        const val LANRARAGI_NAMESPACE_TIMESTAMP = "timestamp"

        const val LANRARAGI_NAMESPACE_ARTIST = "artist"

        fun getApiUriBuilder(baseUrl: String, path: String): Uri.Builder {
            return Uri.parse("$baseUrl$path").buildUpon()
        }

        fun getThumbnailUri(baseUrl: String, id: String, page: Int): String {
            val uri = getApiUriBuilder(baseUrl, "/api/archives/$id/thumbnail")

            if (page > 1) {
                uri.appendQueryParameter("page", page.toString())
                uri.appendQueryParameter("no_fallback", "true")
            }

            return uri.toString()
        }
    }
}
