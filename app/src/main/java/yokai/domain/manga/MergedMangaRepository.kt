package yokai.domain.manga

import yokai.domain.manga.models.MergedMangaReference

interface MergedMangaRepository {
    suspend fun getReferencesById(mergeId: Long): List<MergedMangaReference>
    suspend fun insert(reference: MergedMangaReference): Long?
    suspend fun deleteByMergeId(mergeId: Long)
    suspend fun deleteById(id: Long)
}
