package exh.recs.sources

import org.jsoup.Jsoup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class NovelUpdatesParserTest {

    @Test
    fun `parseSearchResults extracts ajax search entries`() {
        val html = """
            <ul>
              <li class="search_li_results">
                <a class="a_search" href="https://www.novelupdates.com/series/martial-peak/">
                  <img class="search_profile_image" src="//cdn.novelupdates.com/img/series_2866.jpg">
                  <span>Martial Peak</span>
                </a>
              </li>
            </ul>0
        """.trimIndent()

        val results = NovelUpdatesParser.parseSearchResults(html)

        assertEquals(1, results.size)
        assertEquals("Martial Peak", results.single().title)
        assertEquals("https://www.novelupdates.com/series/martial-peak/", results.single().url)
        assertEquals("https://cdn.novelupdates.com/img/series_2866.jpg", results.single().thumbnailUrl)
    }

    @Test
    fun `selectBestSearchResult prefers closest title match`() {
        val results = listOf(
            NovelUpdatesSearchResult(
                title = "Martial World",
                url = "https://www.novelupdates.com/series/martial-world/",
                thumbnailUrl = null,
            ),
            NovelUpdatesSearchResult(
                title = "Martial Peak",
                url = "https://www.novelupdates.com/series/martial-peak/",
                thumbnailUrl = null,
            ),
        )

        val selected = NovelUpdatesParser.selectBestSearchResult(results, "Martial Peak")

        assertEquals("https://www.novelupdates.com/series/martial-peak/", selected?.url)
    }

    @Test
    fun `parseSeriesRecommendations reads recommendations before related series`() {
        val html = """
            <html>
              <body>
                <div class="seriestitlenu">Martial Peak</div>
                <h5 class="seriesother">Related Series</h5>
                <a class="genre" href="https://www.novelupdates.com/series/humanitys-great-sage/">Humanity's Great Sage</a>
                <h5 class="seriesother">Recommendations</h5>
                <a class="genre" href="https://www.novelupdates.com/series/martial-world/">Martial World</a>
                <br />
                <a class="genre" href="https://www.novelupdates.com/series/martial-god-asura/">Martial God Asura</a>
                <h5 class="seriesother">Recommendation Lists</h5>
              </body>
            </html>
        """.trimIndent()

        val document = Jsoup.parse(html, NovelUpdatesPagingSource.BASE_URL)
        val results = NovelUpdatesParser.parseSeriesRecommendations(
            document = document,
            excludeUrl = "https://www.novelupdates.com/series/martial-peak/",
        )

        assertEquals(
            listOf("Martial World", "Martial God Asura", "Humanity's Great Sage"),
            results.map { it.title },
        )
    }

    @Test
    fun `parseRecommendationListEntries extracts series titles from list page`() {
        val html = """
            <html>
              <body>
                <div class="search_body_nu">
                  <div class="search_title">
                    <a href="https://www.novelupdates.com/series/sovereign-of-the-three-realms/">Sovereign of the Three Realms</a>
                  </div>
                </div>
                <div class="search_body_nu">
                  <div class="search_title">
                    <a href="https://www.novelupdates.com/series/martial-peak/">Martial Peak</a>
                  </div>
                </div>
              </body>
            </html>
        """.trimIndent()

        val document = Jsoup.parse(html, NovelUpdatesPagingSource.BASE_URL)
        val results = NovelUpdatesParser.parseRecommendationListEntries(
            document = document,
            excludeUrl = "https://www.novelupdates.com/series/martial-peak/",
        )

        assertEquals(listOf("Sovereign of the Three Realms"), results.map { it.title })
    }

    @Test
    fun `isValidSeriesPage accepts matching title and rejects 404 pages`() {
        val validDocument = Jsoup.parse(
            """
                <html>
                  <body>
                    <div class="seriestitlenu">Martial Peak</div>
                  </body>
                </html>
            """.trimIndent(),
            NovelUpdatesPagingSource.BASE_URL,
        )
        val notFoundDocument = Jsoup.parse(
            """
                <html>
                  <body>
                    <div class="page-404"></div>
                  </body>
                </html>
            """.trimIndent(),
            NovelUpdatesPagingSource.BASE_URL,
        )

        assertTrue(NovelUpdatesParser.isValidSeriesPage(validDocument, "Martial Peak"))
        assertFalse(NovelUpdatesParser.isValidSeriesPage(notFoundDocument, "Martial Peak"))
    }
}
