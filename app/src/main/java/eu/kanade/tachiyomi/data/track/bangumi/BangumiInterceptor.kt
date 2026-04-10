package eu.kanade.tachiyomi.data.track.bangumi

import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.data.track.bangumi.dto.BGMOAuth
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.Response
import uy.kohesive.injekt.injectLazy

class BangumiInterceptor(val bangumi: Bangumi) : Interceptor {

    private val json: Json by injectLazy()

    /**
     * OAuth object used for authenticated requests.
     */
    private var oauth: BGMOAuth? = bangumi.restoreToken()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        val currAuth = oauth ?: throw Exception("Not authenticated with Bangumi")

        if (currAuth.isExpired()) {
            val response = chain.proceed(BangumiApi.refreshTokenRequest(currAuth.refreshToken!!))
            if (response.isSuccessful) {
                newAuth(json.decodeFromString<BGMOAuth>(response.body.string()))
            } else {
                response.close()
            }
        }

        val authRequest = if (originalRequest.method == "GET") {
            originalRequest.newBuilder()
                .header("User-Agent", "ig4e/hayai/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})")
                .url(
                    originalRequest.url.newBuilder()
                        .addQueryParameter("access_token", currAuth.accessToken).build(),
                )
                .build()
        } else {
            originalRequest.newBuilder()
                .post(addToken(currAuth.accessToken, originalRequest.body as FormBody))
                .header("User-Agent", "ig4e/hayai/${BuildConfig.VERSION_NAME} (${BuildConfig.APPLICATION_ID})")
                .build()
        }

        return chain.proceed(authRequest)
    }

    fun newAuth(oauth: BGMOAuth?) {
        this.oauth = if (oauth == null) {
            null
        } else {
            BGMOAuth(
                oauth.accessToken,
                oauth.tokenType,
                System.currentTimeMillis() / 1000,
                oauth.expiresIn,
                oauth.refreshToken,
                this.oauth?.userId,
            )
        }

        bangumi.saveToken(oauth)
    }

    private fun addToken(token: String, oidFormBody: FormBody): FormBody {
        val newFormBody = FormBody.Builder()
        for (i in 0 until oidFormBody.size) {
            newFormBody.add(oidFormBody.name(i), oidFormBody.value(i))
        }
        newFormBody.add("access_token", token)
        return newFormBody.build()
    }
}
