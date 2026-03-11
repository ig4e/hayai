package tachiyomi.domain.release.service

import tachiyomi.domain.release.model.Release

interface ReleaseService {

    suspend fun latest(repository: String, releaseTagPrefix: String, prerelease: Boolean): Release
}
