package yokai.domain.manga.interactor

import co.touchlab.kermit.Logger
import yokai.domain.manga.MergedMangaRepository
import yokai.domain.manga.models.MergedMangaReference

class GetMergedReferencesById(
    private val mergedMangaRepository: MergedMangaRepository,
) {
    suspend fun await(id: Long): List<MergedMangaReference> {
        return try {
            mergedMangaRepository.getReferencesById(id)
        } catch (e: Exception) {
            Logger.e("GetMergedReferencesById") { "Error getting merged references: ${e.message}" }
            emptyList()
        }
    }
}
