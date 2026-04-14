package exh.recs.sources

import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.data.database.models.seriesType
import eu.kanade.tachiyomi.data.track.TrackManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.all.MangaDex
import exh.log.xLogE
import exh.md.similar.MangaDexSimilarPagingSource
import exh.pref.DelegateSourcePreferences
import exh.source.getMainSource
import exh.source.isMdBasedSource
import kotlinx.serialization.json.Json
import eu.kanade.tachiyomi.domain.manga.models.Manga
import yokai.domain.track.interactor.GetTrack
import yokai.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy

/**
 * General class for recommendation sources.
 */
abstract class RecommendationPagingSource(
    protected val manga: Manga?,
) {
    // Display name
    abstract val name: String

    // Localized category name
    open val category: StringResource = MR.strings.similar_titles

    /**
     * Recommendation sources that display results from a source extension
     * can override this property to associate results with a specific source.
     * This is used to redirect the user directly to the corresponding manga screen.
     * If null, the user will be prompted to choose a source via SmartSearch.
     */
    open val associatedSourceId: Long? = null

    /**
     * Fetch the next page of recommendations.
     */
    abstract suspend fun requestNextPage(currentPage: Int): MangasPage

    companion object {
        fun createSources(manga: Manga, source: CatalogueSource): List<RecommendationPagingSource> {
            if (manga.seriesType() == Manga.TYPE_NOVEL) {
                return listOf(
                    MangaUpdatesCommunityPagingSource(manga),
                    MangaUpdatesSimilarPagingSource(manga),
                ).sortedWith(compareBy({ it.name }, { it.category.resourceId }))
            }

            return buildList {
                add(AniListPagingSource(manga))
                add(MangaUpdatesCommunityPagingSource(manga))
                add(MangaUpdatesSimilarPagingSource(manga))
                add(MyAnimeListPagingSource(manga))

                // Only include MangaDex if the delegate sources are enabled and the source is MD-based
                if (source.isMdBasedSource() && Injekt.get<DelegateSourcePreferences>().delegateSources.get()) {
                    val mainSource = source.getMainSource<MangaDex>()
                    if (mainSource != null) {
                        add(MangaDexSimilarPagingSource(manga, mainSource))
                    }
                }

                // Only include Comick if the source manga is from there
                if (source.isComickSource()) {
                    add(ComickPagingSource(manga, source))
                }
            }.sortedWith(compareBy({ it.name }, { it.category.resourceId }))
        }
    }
}

/**
 * General class for recommendation sources backed by trackers.
 */
abstract class TrackerRecommendationPagingSource(
    protected val endpoint: String,
    manga: Manga?,
) : RecommendationPagingSource(manga) {
    private val getTrack: GetTrack by injectLazy()

    protected val trackManager: TrackManager by injectLazy()
    protected val client by lazy { Injekt.get<NetworkHelper>().client }
    protected val json by injectLazy<Json>()

    /**
     * Tracker id associated with the recommendation source.
     *
     * If not null and the tracker is attached to the source manga,
     * the remote id will be used to directly identify the manga on the tracker.
     * Otherwise, a search will be performed using the manga title.
     */
    abstract val associatedTrackerId: Long?

    abstract suspend fun getRecsBySearch(search: String): List<SManga>
    abstract suspend fun getRecsById(id: String): List<SManga>

    override suspend fun requestNextPage(currentPage: Int): MangasPage {
        val mangaId = manga?.id ?: throw NoResultsException()
        val tracks = getTrack.awaitAllByMangaId(mangaId)

        val recs = try {
            val id = tracks.find { it.sync_id == associatedTrackerId }?.media_id
            val results = if (id != null && id != 0L) {
                getRecsById(id.toString())
            } else {
                getRecsBySearch(manga.title)
            }

            results.ifEmpty { throw NoResultsException() }
        } catch (e: Exception) {
            if (e !is NoResultsException) {
                xLogE("Error fetching recommendations from $name", e)
            }
            throw e
        }

        return MangasPage(recs, false)
    }
}

class NoResultsException : Exception("No results")
