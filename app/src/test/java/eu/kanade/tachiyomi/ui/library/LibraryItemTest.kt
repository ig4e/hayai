package eu.kanade.tachiyomi.ui.library

import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.source.local.LocalSource

class LibraryItemTest {

    @Test
    fun `matches src local alias for local source manga`() {
        val item = LibraryItem(
            libraryManga = libraryManga(sourceId = LocalSource.ID),
            sourceManager = mockk(relaxed = true),
        )

        item.matches("src:local") shouldBe true
        item.matches("src:LOCAL") shouldBe true
        item.matches("src:123") shouldBe false
    }

    @Test
    fun `matches numeric src query for non local manga`() {
        val item = LibraryItem(
            libraryManga = libraryManga(sourceId = 123L),
            sourceManager = mockk(relaxed = true),
        )

        item.matches("src:123") shouldBe true
        item.matches("src:124") shouldBe false
        item.matches("src:local") shouldBe false
    }

    private fun libraryManga(sourceId: Long): LibraryManga {
        return LibraryManga(
            manga = Manga.create().copy(
                id = 1L,
                ogTitle = "Test Manga",
                source = sourceId,
            ),
            categories = emptyList(),
            totalChapters = 0L,
            readCount = 0L,
            bookmarkCount = 0L,
            latestUpload = 0L,
            chapterFetchedAt = 0L,
            lastRead = 0L,
        )
    }
}
