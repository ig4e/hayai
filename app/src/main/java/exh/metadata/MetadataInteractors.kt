package exh.metadata

import eu.kanade.tachiyomi.source.online.MetadataSource
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.FlatMetadata
import yokai.domain.manga.MangaRepository
import yokai.domain.manga.metadata.MangaMetadataRepository

class GetMangaIdInteractor(
    private val mangaRepository: MangaRepository,
) : MetadataSource.GetMangaId {
    override suspend fun awaitId(url: String, sourceId: Long): Long? {
        return mangaRepository.getMangaByUrlAndSource(url, sourceId)?.id
    }
}

class InsertFlatMetadataInteractor(
    private val mangaMetadataRepository: MangaMetadataRepository,
) : MetadataSource.InsertFlatMetadata {
    override suspend fun await(metadata: RaisedSearchMetadata) {
        mangaMetadataRepository.insertMetadata(metadata)
    }
}

class GetFlatMetadataByIdInteractor(
    private val mangaMetadataRepository: MangaMetadataRepository,
) : MetadataSource.GetFlatMetadataById {
    override suspend fun await(id: Long): FlatMetadata? {
        val meta = mangaMetadataRepository.getMetadataById(id) ?: return null
        val tags = mangaMetadataRepository.getTagsById(id)
        val titles = mangaMetadataRepository.getTitlesById(id)
        return FlatMetadata(meta, tags, titles)
    }
}
