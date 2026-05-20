package eu.kanade.tachiyomi.data.coil

import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.data.cache.CoverCache
import eu.kanade.tachiyomi.domain.manga.models.Manga
import eu.kanade.tachiyomi.network.await
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.manga.MangaCoverMetadata
import java.io.File
import java.net.HttpURLConnection
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.CacheControl
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okio.FileSystem
import okio.IOException
import okio.Path.Companion.toOkioPath
import okio.Source
import okio.buffer
import okio.sink
import okio.source
import yokai.util.koin.injectLazy
import yokai.domain.manga.models.MangaCover

class MangaCoverFetcher(
    private val mangaId: Long?,
    private val url: String?,
    private val isInLibrary: Boolean,
    private val sourceLazy: Lazy<HttpSource?>,
    private val options: Options,
    private val callFactoryLazy: Lazy<Call.Factory>,
    private val diskCacheKeyLazy: Lazy<String>,
    private val coverFileLazy: Lazy<File?>,
    private val customCoverFileLazy: Lazy<File>,
    private val imageLoader: ImageLoader,
) : Fetcher {

    private val diskCacheKey: String
        get() = diskCacheKeyLazy.value

    private val fileScope = CoroutineScope(Job() + Dispatchers.IO)

    override suspend fun fetch(): FetchResult {
        if (options.useCustomCover) {
            val customCoverFile = customCoverFileLazy.value
            if (customCoverFile.exists()) {
                setRatioAndColorsInScope(mangaId, url, isInLibrary, UniFile.fromFile(customCoverFile))
                return fileLoader(customCoverFile)
            }
        }

        if (url == null) error("No cover specified")
        return when (getResourceType(url)) {
            Type.URL -> httpLoader()
            Type.File -> {
                val file = File(url.substringAfter("file://"))
                setRatioAndColorsInScope(mangaId, url, isInLibrary, UniFile.fromFile(file))
                fileLoader(file)
            }
            Type.URI -> {
                setRatioAndColorsInScope(mangaId, url, isInLibrary, UniFile.fromUri(options.context, url.toUri()))
                fileUriLoader(url)
            }
            null -> error("Invalid image")
        }
    }

    private fun fileLoader(file: File): FetchResult {
        return SourceFetchResult(
            source = ImageSource(
                file = file.toOkioPath(),
                fileSystem = FileSystem.SYSTEM,
                diskCacheKey = diskCacheKey,
            ),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private fun fileUriLoader(uri: String): FetchResult {
        val source = UniFile.fromUri(options.context, uri.toUri())!!
            .openInputStream()
            .source()
            .buffer()
        return SourceFetchResult(
            source = ImageSource(source = source, fileSystem = FileSystem.SYSTEM),
            mimeType = "image/*",
            dataSource = DataSource.DISK,
        )
    }

    private suspend fun httpLoader(): FetchResult {
        val coverFile = coverFileLazy.value
        if (coverFile?.exists() == true && options.diskCachePolicy.readEnabled) {
            if (!isInLibrary) {
                coverFile.setLastModified(Date().time)
            }
            setRatioAndColorsInScope(mangaId, url, isInLibrary, UniFile.fromFile(coverFile))
            return fileLoader(coverFile)
        }

        var snapshot = readFromDiskCache()
        try {
            // Fetch from disk cache
            if (snapshot != null) {
                val snapshotCoverCache = moveSnapshotToCoverCache(snapshot, coverFile)
                if (snapshotCoverCache != null) {
                    // Read from cover cache after added to library
                    setRatioAndColorsInScope(mangaId, url, isInLibrary, UniFile.fromFile(snapshotCoverCache))
                    return fileLoader(snapshotCoverCache)
                }

                // Read from snapshot
                setRatioAndColorsInScope(mangaId, url, isInLibrary)
                return SourceFetchResult(
                    source = snapshot.toImageSource(),
                    mimeType = "image/*",
                    dataSource = DataSource.DISK,
                )
            }

            // Fetch from network
            val response = executeNetworkRequest()
            val responseBody = checkNotNull(response.body) { "Null response source" }
            try {
                // Read from cover cache after library manga cover updated
                val responseCoverCache = writeResponseToCoverCache(response, coverFile)
                setRatioAndColorsInScope(mangaId, url, isInLibrary)
                if (responseCoverCache != null) {
                    return fileLoader(responseCoverCache)
                }

                // Read from disk cache
                snapshot = writeToDiskCache(response)
                if (snapshot != null) {
                    return SourceFetchResult(
                        source = snapshot.toImageSource(),
                        mimeType = "image/*",
                        dataSource = DataSource.NETWORK,
                    )
                }

                // Read from response if cache is unused or unusable
                return SourceFetchResult(
                    source = ImageSource(source = responseBody.source(), fileSystem = FileSystem.SYSTEM),
                    mimeType = "image/*",
                    dataSource = if (response.cacheResponse != null) DataSource.DISK else DataSource.NETWORK,
                )
            } catch (e: Exception) {
                responseBody.close()
                throw e
            }
        } catch (e: Exception) {
            snapshot?.close()
            throw e
        }
    }

    private suspend fun executeNetworkRequest(): Response {
        val client = sourceLazy.value?.client ?: callFactoryLazy.value
        val response = client.newCall(newRequest()).await()
        if (!response.isSuccessful && response.code != HttpURLConnection.HTTP_NOT_MODIFIED) {
            response.close()
            throw IOException(response.message)
        }
        return response
    }

    private fun newRequest(): Request {
        val request = Request.Builder().apply {
            url(url!!)

            val sourceHeaders = sourceLazy.value?.headers
            if (sourceHeaders != null)
                headers(sourceHeaders)
        }

        val diskRead = options.diskCachePolicy.readEnabled
        val networkRead = options.networkCachePolicy.readEnabled
        val onlyCache = !networkRead && diskRead
        val forceNetwork = networkRead && !diskRead
        val none = !networkRead && !diskRead
        when {
            onlyCache -> {
                request.cacheControl(CacheControl.FORCE_CACHE)
            }
            forceNetwork -> if (options.diskCachePolicy.writeEnabled) {
                request.cacheControl(CacheControl.FORCE_NETWORK)
            } else {
                request.cacheControl(CACHE_CONTROL_FORCE_NETWORK_NO_CACHE)
            }
            none -> {
                // This causes the request to fail with a 504 Unsatisfiable Request.
                request.cacheControl(CACHE_CONTROL_NO_NETWORK_NO_CACHE)
            }
        }

        return request.build()
    }

    private fun moveSnapshotToCoverCache(snapshot: DiskCache.Snapshot, cacheFile: File?): File? {
        if (cacheFile == null) return null
        return try {
            imageLoader.diskCache?.run {
                fileSystem.source(snapshot.data).use { input ->
                    writeSourceToCoverCache(input, cacheFile)
                }
                remove(diskCacheKey)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to write snapshot data to cover cache ${cacheFile.name}" }
            null
        }
    }

    private fun writeResponseToCoverCache(response: Response, cacheFile: File?): File? {
        if (cacheFile == null || !options.diskCachePolicy.writeEnabled) return null
        return try {
            response.peekBody(Long.MAX_VALUE).source().use { input ->
                writeSourceToCoverCache(input, cacheFile)
            }
            cacheFile.takeIf { it.exists() }
        } catch (e: Exception) {
            Logger.e(e) { "Failed to write response data to cover cache ${cacheFile.name}" }
            null
        }
    }

    private fun writeSourceToCoverCache(input: Source, cacheFile: File) {
        cacheFile.parentFile?.mkdirs()
        cacheFile.delete()
        try {
            cacheFile.sink().buffer().use { output ->
                output.writeAll(input)
            }
        } catch (e: Exception) {
            cacheFile.delete()
            throw e
        }
    }

    private fun readFromDiskCache(): DiskCache.Snapshot? {
        return if (options.diskCachePolicy.readEnabled) {
            imageLoader.diskCache?.openSnapshot(diskCacheKey)
        } else {
            null
        }
    }

    private fun writeToDiskCache(
        response: Response,
    ): DiskCache.Snapshot? {
        val diskCache = imageLoader.diskCache
        val editor = diskCache?.openEditor(diskCacheKey) ?: return null
        try {
            diskCache.fileSystem.write(editor.data) {
                response.body.source().readAll(this)
            }
            return editor.commitAndOpenSnapshot()
        } catch (e: Exception) {
            try {
                editor.abort()
            } catch (ignored: Exception) {
            }
            throw e
        }
    }

    private fun DiskCache.Snapshot.toImageSource(): ImageSource {
        return ImageSource(
            file = data,
            fileSystem = FileSystem.SYSTEM,
            diskCacheKey = diskCacheKey,
            closeable = this,
        )
    }

    private fun setRatioAndColorsInScope(mangaId: Long?, mangaThumbnailUrl: String?, isInLibrary: Boolean, ogFile: UniFile? = null, force: Boolean = false) {
        // Browse / search consume neither dominantCoverColors nor vibrantCoverColor; MangaDetails
        // recomputes its own vibrant color. Speculatively decoding + Palette-extracting every
        // browsed cover ships the Palette callback to the main looper and stalls cold-entry frames.
        if (!isInLibrary && !force) return
        fileScope.launch {
            MangaCoverMetadata.setRatioAndColors(mangaId, mangaThumbnailUrl, isInLibrary, ogFile, force)
        }
    }

    /** Modified from [MimeTypeMap.getFileExtensionFromUrl] to be more permissive with special characters. */
    private fun MimeTypeMap.getMimeTypeFromUrl(url: String?): String? {
        if (url.isNullOrBlank()) {
            return null
        }

        val extension = url
            .substringBeforeLast('#') // Strip the fragment.
            .substringBeforeLast('?') // Strip the query.
            .substringAfterLast('/') // Get the last path segment.
            .substringAfterLast('.', missingDelimiterValue = "") // Get the file extension.

        return getMimeTypeFromExtension(extension)
    }

    private fun getResourceType(cover: String?): Type? {
        return when {
            cover.isNullOrEmpty() -> null
            cover.startsWith("http", true) || cover.startsWith("Custom-", true) -> Type.URL
            cover.startsWith("/") || cover.startsWith("file://") -> Type.File
            cover.startsWith("content") -> Type.URI
            else -> null
        }
    }

    class MangaFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
    ) : Fetcher.Factory<Manga> {

        private val coverCache: CoverCache by injectLazy()
        private val sourceManager: SourceManager by injectLazy()

        override fun create(data: Manga, options: Options, imageLoader: ImageLoader): Fetcher {
            return MangaCoverFetcher(
                mangaId = data.id,
                url = data.thumbnail_url,
                isInLibrary = data.favorite,
                sourceLazy = lazy { sourceManager.get(data.source) as? HttpSource },
                options = options,
                callFactoryLazy = callFactoryLazy,
                diskCacheKeyLazy = lazy { imageLoader.components.key(data, options)!! },
                coverFileLazy = lazy { coverCache.getCoverFile(data.thumbnail_url, !data.favorite) },
                customCoverFileLazy = lazy { coverCache.getCustomCoverFile(data) },
                imageLoader = imageLoader,
            )
        }
    }

    class MangaCoverFactory(
        private val callFactoryLazy: Lazy<Call.Factory>,
    ) : Fetcher.Factory<MangaCover> {

        private val coverCache: CoverCache by injectLazy()
        private val sourceManager: SourceManager by injectLazy()

        override fun create(data: MangaCover, options: Options, imageLoader: ImageLoader): Fetcher {
            return MangaCoverFetcher(
                mangaId = data.mangaId,
                url = data.url,
                isInLibrary = data.inLibrary,
                sourceLazy = lazy { sourceManager.get(data.sourceId) as? HttpSource },
                options = options,
                callFactoryLazy = callFactoryLazy,
                diskCacheKeyLazy = lazy { imageLoader.components.key(data, options)!! },
                coverFileLazy = lazy { coverCache.getCoverFile(data.url, !data.inLibrary) },
                customCoverFileLazy = lazy { coverCache.getCustomCoverFile(data.mangaId) },
                imageLoader = imageLoader,
            )
        }
    }

    private enum class Type {
        File, URL, URI;
    }

    companion object {
        private val CACHE_CONTROL_FORCE_NETWORK_NO_CACHE = CacheControl.Builder().noCache().noStore().build()
        private val CACHE_CONTROL_NO_NETWORK_NO_CACHE = CacheControl.Builder().noCache().onlyIfCached().build()
    }
}
