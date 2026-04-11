package exh.util

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Response

/**
 * Data saver interface for bandwidth optimization.
 * TODO: Add full BandwidthHero and WsrvNl implementations when SourcePreferences supports data saver settings.
 */
interface DataSaver {

    fun compress(imageUrl: String): String

    companion object {
        val NoOp = object : DataSaver {
            override fun compress(imageUrl: String): String {
                return imageUrl
            }
        }

        suspend fun HttpSource.getImage(page: Page, dataSaver: DataSaver): Response {
            val imageUrl = page.imageUrl ?: return getImage(page)
            page.imageUrl = dataSaver.compress(imageUrl)
            return try {
                getImage(page)
            } finally {
                page.imageUrl = imageUrl
            }
        }
    }
}
