package hayai.novel.plugin.model

import hayai.novel.source.NovelSource
import kotlinx.serialization.Serializable

sealed class NovelPlugin {
    abstract val id: String
    abstract val name: String
    abstract val lang: String
    abstract val version: String
    abstract val siteUrl: String
    abstract val iconUrl: String?

    data class Installed(
        override val id: String,
        override val name: String,
        override val lang: String,
        override val version: String,
        override val siteUrl: String,
        override val iconUrl: String?,
        val source: NovelSource,
        val hasUpdate: Boolean = false,
    ) : NovelPlugin()

    data class Available(
        override val id: String,
        override val name: String,
        override val lang: String,
        override val version: String,
        override val siteUrl: String,
        override val iconUrl: String?,
        val jsUrl: String,
    ) : NovelPlugin()
}

/**
 * Represents a plugin entry from the plugins.min.json index.
 */
@Serializable
data class NovelPluginIndex(
    val id: String,
    val name: String,
    val site: String = "",
    val lang: String = "",
    val version: String = "",
    val url: String = "",
    val iconUrl: String? = null,
)
