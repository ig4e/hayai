package eu.kanade.tachiyomi.source.online

import eu.kanade.tachiyomi.source.Source

/**
 * Stub interface for MetadataSource.
 * TODO: Full implementation will be added by the metadata agent.
 */
interface MetadataSource<M, I> : Source {
    val metaClass: kotlin.reflect.KClass<M>

    fun newMetaInstance(): M

    suspend fun parseIntoMetadata(metadata: M, input: I)
}
