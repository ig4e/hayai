package exh.md.network

import co.touchlab.kermit.Logger
import eu.kanade.tachiyomi.data.track.TrackPreferences
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
    private val preferences: TrackPreferences,
    private val mdList: Any, // TODO: Replace with MdList when available
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
                // TODO: Call mdList.logout() when MdList is available
                false
            }
        }
    }

    suspend fun logout(): Boolean {
        val oauth = MdUtil.loadOAuth(preferences, mdList)
        val sessionToken = oauth?.accessToken
        val refreshToken = oauth?.refreshToken
        if (refreshToken.isNullOrEmpty() || sessionToken.isNullOrEmpty()) {
            // TODO: Call mdList.logout() when MdList is available
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
            // TODO: Call mdList.logout() when MdList is available
        }.exceptionOrNull()

        return when (error == null) {
            true -> {
                mangaDexAuthInterceptor.setAuth(null)
                true
            }
            false -> {
                Logger.e("MangaDexLoginHelper", error) { "Error logging out" }
                false
            }
        }
    }
}
