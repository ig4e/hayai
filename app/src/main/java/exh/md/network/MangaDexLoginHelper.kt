package exh.md.network

import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.core.preference.PreferenceStore
import eu.kanade.tachiyomi.data.track.myanimelist.dto.MALOAuth
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import exh.md.utils.MdApi
import exh.md.utils.MdConstants
import exh.md.utils.MdUtil
import okhttp3.FormBody
import okhttp3.Headers
import okhttp3.OkHttpClient

class MangaDexLoginHelper(
    private val client: OkHttpClient,
    private val preferenceStore: PreferenceStore,
    private val mangaDexAuthInterceptor: MangaDexAuthInterceptor,
) {

    /**
     *  Login given the generated authorization code
     */
    suspend fun login(authorizationCode: String): Boolean {
        val loginFormBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("grant_type", MdConstants.Login.authorizationCode)
            .add("code", authorizationCode)
            .add("code_verifier", MdUtil.getPkceChallengeCode())
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()

        val error = kotlin.runCatching {
            val data = client.newCall(
                POST(MdApi.baseAuthUrl + MdApi.token, body = loginFormBody),
            ).awaitSuccess().parseAs<MALOAuth>()
            mangaDexAuthInterceptor.setAuth(data)
        }.exceptionOrNull()

        return when (error == null) {
            true -> true
            false -> {
                Logger.e("MangaDexLoginHelper", error) { "Error logging in" }
                mangaDexAuthInterceptor.setAuth(null)
                false
            }
        }
    }

    suspend fun logout(): Boolean {
        val oauth = MdUtil.loadOAuth(preferenceStore)
        val sessionToken = oauth?.accessToken
        val refreshToken = oauth?.refreshToken
        if (refreshToken.isNullOrEmpty() || sessionToken.isNullOrEmpty()) {
            mangaDexAuthInterceptor.setAuth(null)
            return true
        }

        val formBody = FormBody.Builder()
            .add("client_id", MdConstants.Login.clientId)
            .add("refresh_token", refreshToken)
            .add("redirect_uri", MdConstants.Login.redirectUri)
            .build()

        val error = kotlin.runCatching {
            client.newCall(
                POST(
                    url = MdApi.baseAuthUrl + MdApi.logout,
                    headers = Headers.Builder().add("Authorization", "Bearer $sessionToken")
                        .build(),
                    body = formBody,
                ),
            ).awaitSuccess()
        }.exceptionOrNull()

        // Always clear local auth state, even if the server-side logout fails
        mangaDexAuthInterceptor.setAuth(null)

        return when (error == null) {
            true -> true
            false -> {
                Logger.e("MangaDexLoginHelper", error) { "Error logging out" }
                false
            }
        }
    }
}
