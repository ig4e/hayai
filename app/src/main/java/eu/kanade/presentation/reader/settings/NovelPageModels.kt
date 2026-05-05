package eu.kanade.presentation.reader.settings

import kotlinx.serialization.Serializable

// NOTE: In Tsundoku these data classes live inline at the top of `NovelPage.kt`.
// They are extracted into this file during Hayai's port so they can be referenced
// before the full `NovelPage.kt` (1655 lines) is ported. When `NovelPage.kt` is
// ported, this file may be merged inline or kept — they share the same package
// either way.

@Serializable
data class CodeSnippet(
    val title: String,
    val code: String,
    val enabled: Boolean = true,
)

@Serializable
data class RegexReplacement(
    val title: String,
    val pattern: String,
    val replacement: String,
    val enabled: Boolean = true,
    val isRegex: Boolean = true,
)
