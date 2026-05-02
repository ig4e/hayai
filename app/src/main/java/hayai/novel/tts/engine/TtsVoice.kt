package hayai.novel.tts.engine

import kotlinx.serialization.Serializable

/**
 * A selectable voice within an engine. Engines own their voice catalog; the registry presents
 * them by [id], and the UI shows [displayName].
 *
 * The system engine stores Android's `Voice.getName()` directly in [id]. [extras] carries any
 * engine-private metadata callers may want surfaced (quality, latency, network requirement).
 */
@Serializable
data class TtsVoice(
    val id: String,
    val displayName: String,
    val language: String,
    val gender: Gender = Gender.Unknown,
    val extras: Map<String, String> = emptyMap(),
) {
    @Serializable
    enum class Gender { Female, Male, Unknown }
}
