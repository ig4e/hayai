package exh.metadata.metadata

/**
 * E-Hentai/ExHentai specific search metadata.
 * TODO: This stub will be replaced by the full implementation from the metadata agent.
 */
class EHentaiSearchMetadata : RaisedSearchMetadata() {
    var gId: String? = null
    var gToken: String? = null
    var exh: Boolean? = null
    var title: String? = null
    var altTitle: String? = null
    var thumbnailUrl: String? = null
    var parent: String? = null
    var visible: String? = null
    var language: String? = null
    var translated: Boolean? = null
    var size: Long? = null
    var favorites: Int? = null
    var ratingCount: Int? = null
    var lastUpdateCheck: Long = 0
    var aged: Boolean = false

    companion object {
        const val TAG_TYPE_LIGHT = 0
        const val TAG_TYPE_NORMAL = 1
        const val TAG_TYPE_WEAK = 2

        const val EH_GENRE_NAMESPACE = "genre"
        const val EH_META_NAMESPACE = "meta"
        const val EH_UPLOADER_NAMESPACE = "uploader"
        const val EH_VISIBILITY_NAMESPACE = "visibility"

        fun normalizeUrl(url: String): String {
            // Extract gallery path: /g/{id}/{token}/
            val regex = Regex("/g/(\\d+)/([^/]+)/?")
            val match = regex.find(url)
            return if (match != null) {
                "/g/${match.groupValues[1]}/${match.groupValues[2]}/"
            } else {
                url
            }
        }

        fun galleryId(url: String): String {
            val regex = Regex("/g/(\\d+)/")
            val match = regex.find(url)
            return match?.groupValues?.get(1) ?: ""
        }

        fun galleryToken(url: String): String {
            val regex = Regex("/g/\\d+/([^/]+)/?")
            val match = regex.find(url)
            return match?.groupValues?.get(1) ?: ""
        }

        fun idAndTokenToUrl(id: String, token: String): String {
            return "/g/$id/$token/"
        }
    }
}
