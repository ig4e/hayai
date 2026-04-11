package exh.metadata.metadata

import exh.metadata.metadata.base.RaisedTag

/**
 * Base class for raised (parsed) search metadata.
 * TODO: This stub will be replaced by the full implementation from the metadata agent.
 */
open class RaisedSearchMetadata {
    val tags = mutableListOf<RaisedTag>()

    var genre: String? = null
    var datePosted: Long? = null
    var averageRating: Double? = null
    var uploader: String? = null
    var length: Int? = null

    companion object {
        const val TAG_TYPE_VIRTUAL = 3

        fun List<RaisedTag>.toGenreString(): String? {
            return joinToString { "${it.namespace ?: ""}:${it.name}" }.takeIf { it.isNotBlank() }
        }
    }
}
