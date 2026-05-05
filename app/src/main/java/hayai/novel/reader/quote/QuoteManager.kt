package hayai.novel.reader.quote

import android.content.Context
import co.touchlab.kermit.Logger
import com.hippo.unifile.UniFile
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import yokai.domain.storage.StorageManager
import java.io.IOException

/**
 * File-backed manager for quote storage and retrieval.
 *
 * Ports `tsundoku/.../ui/reader/quote/QuoteManager.kt` 1:1, swapping `logcat` for kermit
 * (matching Hayai's logger convention used elsewhere in `hayai/novel/reader/`).
 *
 * Quotes are persisted as JSON files at `<storage>/quotes/novel_<id>.json`.
 */
class QuoteManager(@Suppress("unused") private val context: Context) {

    private val jsonFormat = Json { prettyPrint = true }

    private val storageManager: StorageManager by lazy {
        Injekt.get<StorageManager>()
    }

    private val quotesDir: UniFile?
        get() = storageManager.getQuotesDirectory()

    private fun getQuotesFile(novelId: Long): UniFile? {
        return quotesDir?.findFile("novel_$novelId.json")
    }

    fun saveQuotes(novelId: Long, quotes: List<Quote>) {
        try {
            val novelQuotes = NovelQuotes(novelId, quotes)
            val json = jsonFormat.encodeToString(novelQuotes)

            val existingFile = getQuotesFile(novelId)
            if (existingFile?.exists() == true) {
                existingFile.delete()
            }

            val file = quotesDir?.createFile("novel_$novelId.json") ?: return
            file.openOutputStream().use { outputStream ->
                outputStream.write(json.toByteArray())
            }
            Logger.d { "Quotes saved for novel $novelId: ${quotes.size} quotes" }
        } catch (e: IOException) {
            Logger.e(e) { "Failed to save quotes for novel $novelId" }
        } catch (e: SerializationException) {
            Logger.e(e) { "Failed to serialize quotes for novel $novelId" }
        }
    }

    fun loadQuotes(novelId: Long): List<Quote> {
        return try {
            val file = getQuotesFile(novelId)
            if (file == null || !file.exists()) {
                emptyList()
            } else {
                val json = file.openInputStream().use { inputStream ->
                    String(inputStream.readBytes())
                }
                val novelQuotes = jsonFormat.decodeFromString<NovelQuotes>(json)
                novelQuotes.quotes
            }
        } catch (e: IOException) {
            Logger.e(e) { "Failed to load quotes for novel $novelId" }
            emptyList()
        } catch (e: SerializationException) {
            Logger.e(e) { "Failed to deserialize quotes for novel $novelId" }
            emptyList()
        }
    }

    fun addQuote(novelId: Long, quote: Quote) {
        val existingQuotes = loadQuotes(novelId).toMutableList()
        existingQuotes.add(quote)
        saveQuotes(novelId, existingQuotes)
    }

    fun removeQuote(novelId: Long, quoteId: String) {
        val existingQuotes = loadQuotes(novelId).toMutableList()
        existingQuotes.removeAll { it.id == quoteId }
        saveQuotes(novelId, existingQuotes)
    }

    fun updateQuote(novelId: Long, updatedQuote: Quote) {
        val existingQuotes = loadQuotes(novelId).toMutableList()
        val index = existingQuotes.indexOfFirst { it.id == updatedQuote.id }
        if (index >= 0) {
            existingQuotes[index] = updatedQuote
            saveQuotes(novelId, existingQuotes)
        }
    }

    fun getQuotes(novelId: Long): List<Quote> {
        return loadQuotes(novelId)
    }

    fun clearQuotes(novelId: Long) {
        val file = getQuotesFile(novelId)
        if (file?.exists() == true) {
            file.delete()
        }
    }

    fun getQuoteCount(novelId: Long): Int {
        return loadQuotes(novelId).size
    }
}

val Context.quoteManager: QuoteManager
    get() = QuoteManager(this)
