package eu.kanade.tachiyomi.ui.manga.track

import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TrackerMetadataSupportTest {

    @Test
    fun `normalized metadata trims editable fields and drops blanks`() {
        val metadata = TrackMangaMetadata(
            title = "  Title  ",
            authors = "  ",
            artists = " Artist ",
            thumbnailUrl = " https://example.com/cover.jpg ",
            description = "\n Description \n",
        )

        metadata.toNormalizedMetadata() shouldBe TrackMangaMetadata(
            title = "Title",
            authors = null,
            artists = "Artist",
            thumbnailUrl = "https://example.com/cover.jpg",
            description = "Description",
        )
    }

    @Test
    fun `form state round trip preserves normalized tracker metadata`() {
        val metadata = TrackMangaMetadata(
            title = " Title ",
            authors = " Author ",
            artists = " Artist ",
            thumbnailUrl = " https://example.com/thumb.jpg ",
            description = " Description ",
        )

        metadata.toFormState().toMetadata() shouldBe TrackMangaMetadata(
            title = "Title",
            authors = "Author",
            artists = "Artist",
            thumbnailUrl = "https://example.com/thumb.jpg",
            description = "Description",
        )
    }
}
