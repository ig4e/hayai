package tachiyomi.data.release

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import tachiyomi.domain.release.model.Release
import tachiyomi.domain.release.service.ReleaseService

class ReleaseServiceImpl(
    private val networkService: NetworkHelper,
    private val json: Json,
) : ReleaseService {

    override suspend fun latest(repository: String, releaseTagPrefix: String, prerelease: Boolean): Release {
        return with(json) {
            networkService.client
                .newCall(GET("https://api.github.com/repos/$repository/releases"))
                .awaitSuccess()
                .parseAs<List<GithubRelease>>()
                .first { release ->
                    release.version.startsWith(releaseTagPrefix) && release.prerelease == prerelease
                }
                .let(releaseMapper)
        }
    }
}
