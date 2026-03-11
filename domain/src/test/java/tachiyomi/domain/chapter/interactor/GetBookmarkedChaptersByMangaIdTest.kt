package tachiyomi.domain.chapter.interactor

import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.chapter.repository.ChapterRepository

class GetBookmarkedChaptersByMangaIdTest {

    @Test
    fun `await returns bookmarked chapters from repository`() = runTest {
        val mangaId = 1L
        val expected = listOf(
            Chapter.create().copy(id = 11L, mangaId = mangaId, bookmark = true, name = "Chapter 11"),
            Chapter.create().copy(id = 12L, mangaId = mangaId, bookmark = true, name = "Chapter 12"),
        )
        val chapterRepository = mockk<ChapterRepository>()
        coEvery { chapterRepository.getBookmarkedChaptersByMangaId(mangaId) } returns expected

        val result = GetBookmarkedChaptersByMangaId(chapterRepository).await(mangaId)

        result shouldBe expected
        coVerify(exactly = 1) { chapterRepository.getBookmarkedChaptersByMangaId(mangaId) }
    }

    @Test
    fun `await returns empty list when repository throws`() = runTest {
        val mangaId = 2L
        val chapterRepository = mockk<ChapterRepository>()
        coEvery { chapterRepository.getBookmarkedChaptersByMangaId(mangaId) } throws IllegalStateException("boom")

        val result = GetBookmarkedChaptersByMangaId(chapterRepository).await(mangaId)

        result.shouldBeEmpty()
        coVerify(exactly = 1) { chapterRepository.getBookmarkedChaptersByMangaId(mangaId) }
    }
}
