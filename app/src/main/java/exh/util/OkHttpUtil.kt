package exh.util

import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.nodes.Document

fun Response.interceptAsHtml(block: (Document) -> Unit): Response {
    return if (body.contentType()?.type == "text" &&
        body.contentType()?.subtype == "html"
    ) {
        val bodyString = body.string()
        val rebuiltResponse = newBuilder()
            .body(bodyString.toResponseBody(body.contentType()))
            .build()
        try {
            // Search for captcha
            val parsed = asJsoup(html = bodyString)
            block(parsed)
        } catch (t: Throwable) {
            // Ignore all errors
            Logger.w("OkHttpUtil") { "Interception error! ${t.message}" }
        } finally {
            close()
        }

        rebuiltResponse
    } else {
        this
    }
}
