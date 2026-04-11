package exh.metadata.metadata

import android.content.Context
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.model.copy
import exh.md.utils.MangaDexRelation
import exh.metadata.metadata.base.TrackerIdMetadata
import kotlinx.serialization.Serializable

@Serializable
class MangaDexSearchMetadata : RaisedSearchMetadata(), TrackerIdMetadata {
    var mdUuid: String? = null

    var cover: String? = null

    var title: String? by titleDelegate(TITLE_TYPE_MAIN)
    var altTitles: List<String>? = null

    var description: String? = null

    var authors: List<String>? = null
    var artists: List<String>? = null

    var langFlag: String? = null

    var lastChapterNumber: Int? = null
    var rating: Float? = null

    override var anilistId: String? = null
    override var kitsuId: String? = null
    override var myAnimeListId: String? = null
    override var mangaUpdatesId: String? = null
    override var animePlanetId: String? = null

    var status: Int? = null

    var followStatus: Int? = null
    var relation: MangaDexRelation? = null

    override fun createMangaInfo(manga: SManga): SManga {
        val key = mdUuid?.let { "/manga/$it" }

        val title = title

        val cover = cover

        val author = authors?.joinToString()

        val artist = artists?.joinToString()

        val status = status

        val genres = tagsToGenreString()

        val description = description

        return manga.copy(
            url = key ?: manga.url,
            title = title ?: manga.title,
            thumbnail_url = cover ?: manga.thumbnail_url,
            author = author ?: manga.author,
            artist = artist ?: manga.artist,
            status = status ?: manga.status,
            genre = genres,
            description = description ?: manga.description,
        )
    }

    override fun getExtraInfoPairs(context: Context): List<Pair<String, String>> {
        return listOfNotNull(
            getItem(mdUuid) { "ID" },
            getItem(cover) { "Thumbnail URL" },
            getItem(title) { "Title" },
            getItem(authors, { it.joinToString() }) { "Author" },
            getItem(artists, { it.joinToString() }) { "Artist" },
            getItem(langFlag) { "Language" },
            getItem(lastChapterNumber) { "Last chapter number" },
            getItem(rating) { "Average rating" },
            getItem(status) { "Status" },
            getItem(followStatus) { "Follow status" },
            getItem(anilistId) { "AniList ID" },
            getItem(kitsuId) { "Kitsu ID" },
            getItem(myAnimeListId) { "MAL ID" },
            getItem(mangaUpdatesId) { "MangaUpdates ID" },
            getItem(animePlanetId) { "Anime-Planet ID" },
        )
    }

    companion object {
        private const val TITLE_TYPE_MAIN = 0

        const val TAG_TYPE_DEFAULT = 0
    }
}
