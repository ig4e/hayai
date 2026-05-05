package hayai.novel.reader.quote

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Saved quote from a novel chapter. Storage shape ports verbatim from Tsundoku
 * (`tsundoku/.../ui/reader/quote/Quote.kt`) so JSON files written by Tsundoku
 * could in theory be read here without migration.
 */
@Serializable
data class Quote(
    val id: String = UUID.randomUUID().toString(),
    val novelName: String,
    val chapterName: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
) {
    companion object {
        fun create(novelName: String, chapterName: String, content: String): Quote {
            return Quote(
                novelName = novelName,
                chapterName = chapterName,
                content = content.trim(),
            )
        }
    }
}

@Serializable
data class NovelQuotes(
    val novelId: Long,
    val quotes: List<Quote> = emptyList(),
)
