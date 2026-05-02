package yokai.core.migration.migrations

import hayai.novel.migrations.NovelThemeIdMigration
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import yokai.core.migration.Migration

val migrations: ImmutableList<Migration> = persistentListOf(
    // Always run
    SetupAppUpdateMigration(),
    SetupBackupCreateMigration(),
    SetupExtensionUpdateMigration(),
    SetupLibraryUpdateMigration(),

    // Yokai fork
    CutoutMigration(),
    ExtensionInstallerEnumMigration(),
    RepoJsonMigration(),
    ThePurgeMigration(),

    // Hayai novel reader
    NovelThemeIdMigration(),
)
