package eu.kanade.tachiyomi.source.online.all

import android.content.Context
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Response

/**
 * Built-in E-Hentai/ExHentai source.
 * When isExHentai is false, this is the E-Hentai source.
 * When isExHentai is true, this is the ExHentai source.
 *
 * TODO: Full implementation will be provided by the EH sources agent.
 */
class EHentai(override val id: Long, private val isExHentai: Boolean, private val context: Context) : HttpSource() {

    override val name: String = if (isExHentai) "ExHentai" else "E-Hentai"
    override val baseUrl: String = if (isExHentai) "https://exhentai.org" else "https://e-hentai.org"
    override val lang: String = "all"
    override val supportsLatest: Boolean = true

    override fun popularMangaRequest(page: Int) = throw UnsupportedOperationException("Not yet implemented")
    override fun popularMangaParse(response: Response) = throw UnsupportedOperationException("Not yet implemented")
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = throw UnsupportedOperationException("Not yet implemented")
    override fun searchMangaParse(response: Response) = throw UnsupportedOperationException("Not yet implemented")
    override fun latestUpdatesRequest(page: Int) = throw UnsupportedOperationException("Not yet implemented")
    override fun latestUpdatesParse(response: Response) = throw UnsupportedOperationException("Not yet implemented")
    override fun mangaDetailsParse(response: Response) = throw UnsupportedOperationException("Not yet implemented")
    override fun chapterListParse(response: Response) = throw UnsupportedOperationException("Not yet implemented")
    override fun chapterPageParse(response: Response) = throw UnsupportedOperationException("Not yet implemented")
    override fun pageListParse(response: Response) = throw UnsupportedOperationException("Not yet implemented")
    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException("Not yet implemented")
}
