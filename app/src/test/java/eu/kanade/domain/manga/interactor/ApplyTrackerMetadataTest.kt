package eu.kanade.domain.manga.interactor

import eu.kanade.tachiyomi.data.track.model.TrackMangaMetadata
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import tachiyomi.domain.manga.interactor.GetCustomMangaInfo
import tachiyomi.domain.manga.interactor.SetCustomMangaInfo
import tachiyomi.domain.manga.model.CustomMangaInfo
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.manga.model.MangaUpdate
import tachiyomi.domain.source.service.SourceManager

class ApplyTrackerMetadataTest {

    @Test
    fun `apply tracker metadata updates manga fields and clears overlapping overrides`() = runTest {
        val manga = Manga.create().copy(
            id = 1L,
            source = 100L,
            lastUpdate = 10L,
            ogTitle = "Original title",
            ogAuthor = "Original author",
            ogArtist = "Original artist",
            ogThumbnailUrl = "https://example.com/old.jpg",
            ogDescription = "Original description",
            coverLastModified = 99L,
        )
        val customInfo = CustomMangaInfo(
            id = manga.id,
            title = "Custom title",
            author = "Custom author",
            artist = "Custom artist",
            thumbnailUrl = "https://example.com/custom.jpg",
            description = "Custom description",
            genre = listOf("Drama"),
            status = 7L,
        )
        val metadata = TrackMangaMetadata(
            title = "  Enriched title  ",
            authors = "  Enriched author  ",
            artists = "  Enriched artist  ",
            thumbnailUrl = "  https://example.com/new.jpg  ",
            description = "  Enriched description  ",
        )

        val updateManga = mockk<UpdateManga>()
        val getCustomMangaInfo = mockk<GetCustomMangaInfo>()
        val setCustomMangaInfo = mockk<SetCustomMangaInfo>()
        val sourceManager = mockk<SourceManager>(relaxed = true)
        val mangaUpdate = slot<MangaUpdate>()
        val clearedCustomInfo = slot<CustomMangaInfo>()

        coEvery { updateManga.await(capture(mangaUpdate)) } returns true
        every { getCustomMangaInfo.get(manga.id) } returns customInfo
        every { setCustomMangaInfo.set(capture(clearedCustomInfo)) } just runs

        val result = ApplyTrackerMetadata(
            updateManga = updateManga,
            getCustomMangaInfo = getCustomMangaInfo,
            setCustomMangaInfo = setCustomMangaInfo,
            sourceManager = sourceManager,
        ).await(manga, metadata)

        mangaUpdate.captured.id shouldBe manga.id
        mangaUpdate.captured.title shouldBe "Enriched title"
        mangaUpdate.captured.author shouldBe "Enriched author"
        mangaUpdate.captured.artist shouldBe "Enriched artist"
        mangaUpdate.captured.thumbnailUrl shouldBe "https://example.com/new.jpg"
        mangaUpdate.captured.description shouldBe "Enriched description"
        mangaUpdate.captured.coverLastModified.shouldNotBeNull()

        result.ogTitle shouldBe "Enriched title"
        result.ogAuthor shouldBe "Enriched author"
        result.ogArtist shouldBe "Enriched artist"
        result.ogThumbnailUrl shouldBe "https://example.com/new.jpg"
        result.ogDescription shouldBe "Enriched description"
        result.coverLastModified shouldBe mangaUpdate.captured.coverLastModified
        result.lastUpdate shouldBe manga.lastUpdate + 1

        clearedCustomInfo.captured shouldBe customInfo.copy(
            title = null,
            author = null,
            artist = null,
            thumbnailUrl = null,
            description = null,
        )

        coVerify(exactly = 1) { updateManga.await(any()) }
    }

    @Test
    fun `apply tracker metadata keeps cover timestamp unchanged when thumbnail stays the same`() = runTest {
        val manga = Manga.create().copy(
            id = 2L,
            source = 100L,
            ogTitle = "Original title",
            ogThumbnailUrl = "https://example.com/current.jpg",
            coverLastModified = 55L,
        )
        val metadata = TrackMangaMetadata(
            title = "Updated title",
            thumbnailUrl = "https://example.com/current.jpg",
        )

        val updateManga = mockk<UpdateManga>()
        val getCustomMangaInfo = mockk<GetCustomMangaInfo>()
        val setCustomMangaInfo = mockk<SetCustomMangaInfo>(relaxed = true)
        val sourceManager = mockk<SourceManager>(relaxed = true)
        val mangaUpdate = slot<MangaUpdate>()

        coEvery { updateManga.await(capture(mangaUpdate)) } returns true
        every { getCustomMangaInfo.get(manga.id) } returns null

        val result = ApplyTrackerMetadata(
            updateManga = updateManga,
            getCustomMangaInfo = getCustomMangaInfo,
            setCustomMangaInfo = setCustomMangaInfo,
            sourceManager = sourceManager,
        ).await(manga, metadata)

        mangaUpdate.captured.coverLastModified shouldBe null
        result.coverLastModified shouldBe manga.coverLastModified
        result.ogTitle shouldBe "Updated title"
        result.ogThumbnailUrl shouldBe manga.ogThumbnailUrl
    }
}
