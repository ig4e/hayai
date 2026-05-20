package eu.kanade.tachiyomi.data.backup.create.creators

import yokai.util.koin.get
import eu.kanade.tachiyomi.data.backup.models.BackupManga
import eu.kanade.tachiyomi.data.backup.models.BackupSource
import eu.kanade.tachiyomi.source.SourceManager

class SourcesBackupCreator(
    private val sourceManager: SourceManager = get(),
) {
    operator fun invoke(mangas: List<BackupManga>): List<BackupSource> {
        return mangas
            .asSequence()
            .map { it.source }
            .distinct()
            .map { sourceManager.getOrStub(it) }
            .map { BackupSource.copyFrom(it) }
            .toList()
    }
}
