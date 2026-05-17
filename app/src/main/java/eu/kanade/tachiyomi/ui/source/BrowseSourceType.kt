package eu.kanade.tachiyomi.ui.source

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.CatalogueSource
import hayai.novel.source.NovelSource
import yokai.i18n.MR

/**
 * The two top-level source families surfaced as Novel/Manga tabs in [BrowseController].
 *
 * Used to:
 *  - filter the catalogue list to only the active type
 *  - key the per-type "pinned" set ([eu.kanade.tachiyomi.data.preference.PreferencesHelper.pinnedNovelCatalogues] / [eu.kanade.tachiyomi.data.preference.PreferencesHelper.pinnedMangaCatalogues])
 *  - key the per-type "last used" source ([eu.kanade.tachiyomi.data.preference.PreferencesHelper.lastUsedNovelSource] / [eu.kanade.tachiyomi.data.preference.PreferencesHelper.lastUsedMangaSource])
 */
enum class BrowseSourceType(val stringRes: StringResource) {
    Manga(MR.strings.manga),
    Novel(MR.strings.novel),
    ;

    companion object {
        fun fromOrdinal(ordinal: Int): BrowseSourceType =
            entries.getOrNull(ordinal) ?: Manga
    }
}

/** Classifies a [CatalogueSource] into one of the [BrowseSourceType] tabs. */
val CatalogueSource.browseType: BrowseSourceType
    get() = if (this is NovelSource) BrowseSourceType.Novel else BrowseSourceType.Manga
