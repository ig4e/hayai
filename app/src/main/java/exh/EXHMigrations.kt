package exh

import exh.source.BlacklistedSources
import exh.source.EH_SOURCE_ID
import exh.source.HBROWSE_SOURCE_ID
import exh.source.TSUMINO_SOURCE_ID
import yokai.domain.manga.models.Manga
import java.net.URI
import java.net.URISyntaxException

object EXHMigrations {

    fun migrateBackupEntry(manga: Manga): Manga {
        var newManga = manga

        // Migrate Tsumino source IDs
        if (newManga.source == 6909L) {
            newManga = newManga.copy(
                source = TSUMINO_SOURCE_ID,
            )
        }

        if (newManga.source == 6912L) {
            newManga = newManga.copy(
                source = HBROWSE_SOURCE_ID,
                url = newManga.url + "/c00001/",
            )
        }

        // Allow importing of EHentai extension backups
        if (newManga.source in BlacklistedSources.EHENTAI_EXT_SOURCES) {
            newManga = newManga.copy(
                source = EH_SOURCE_ID,
            )
        }

        return newManga
    }

    private fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig)
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (e: URISyntaxException) {
            orig
        }
    }
}
