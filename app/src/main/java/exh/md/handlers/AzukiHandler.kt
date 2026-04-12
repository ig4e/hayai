package exh.md.handlers

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

class AzukiHandler(currentClient: OkHttpClient, userAgent: String) {
    val baseUrl = "https://www.azuki.co"
    private val apiUrl = "https://production.api.azuki.co"
    val headers = Headers.Builder()
        .add("User-Agent", userAgent)
        .build()

    val client: OkHttpClient = currentClient

    suspend fun fetchPageList(externalUrl: String): List<Page> {
        val chapterId = externalUrl.substringAfterLast("/").substringBefore("?")
        val request = pageListRequest(chapterId)
        return pageListParse(client.newCall(request).awaitSuccess())
    }

    private fun pageListRequest(chapterId: String): Request {
        return GET("$apiUrl/chapter/$chapterId/pages/v0", headers)
    }

    fun pageListParse(response: Response): List<Page> {
        val pages = Json.parseToJsonElement(response.body.string())
            .jsonObject["pages"]
            ?.jsonArray ?: throw Exception("Azuki: missing 'pages' in response")
        return pages.mapIndexedNotNull { index, element ->
            val url = element.jsonObject["image_wm"]
                ?.jsonObject?.get("webp")
                ?.jsonArray?.getOrNull(1)
                ?.jsonObject?.get("url")
                ?.jsonPrimitive?.content ?: return@mapIndexedNotNull null
            Page(index, url, url)
        }
    }
}
