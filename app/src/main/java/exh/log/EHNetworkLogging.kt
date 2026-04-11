package exh.log

import co.touchlab.kermit.Logger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

fun OkHttpClient.Builder.maybeInjectEHLogger(): OkHttpClient.Builder {
    if (EHLogLevel.shouldLog(EHLogLevel.EXTREME)) {
        val logger = Logger.withTag("EH-NETWORK")
        val httpLogger = HttpLoggingInterceptor.Logger { message ->
            logger.d { message }
        }
        return addInterceptor(
            HttpLoggingInterceptor(httpLogger).apply {
                level = HttpLoggingInterceptor.Level.BODY
            },
        )
    }
    return this
}
