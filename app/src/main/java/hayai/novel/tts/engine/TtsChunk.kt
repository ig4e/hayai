package hayai.novel.tts.engine

/**
 * A streamed slice of synthesized audio. Engines emit `TtsChunk`s through the synthesis flow as
 * the underlying decoder produces samples; the playback layer writes [pcm] straight into an
 * [android.media.AudioTrack] and schedules [wordTimings] dispatches against playback elapsed time.
 *
 * - [pcm] holds 16-bit signed PCM samples at [sampleRate] Hz, mono.
 * - [wordTimings] is empty when the engine doesn't expose alignment for this chunk; the playback
 *   layer falls back to time-estimation in that case.
 * - [endOfUtterance] flags the final chunk for an utterance so the playback layer knows it can
 *   move to the next sentence in the queue without waiting for more audio.
 */
data class TtsChunk(
    val pcm: ShortArray,
    val sampleRate: Int,
    val wordTimings: List<WordTiming> = emptyList(),
    val endOfUtterance: Boolean = false,
) {
    // Custom equals/hashCode — the default ones on `data class` would compare ShortArray by
    // identity, which makes tests fragile and serves no real purpose here.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TtsChunk) return false
        return sampleRate == other.sampleRate &&
            endOfUtterance == other.endOfUtterance &&
            wordTimings == other.wordTimings &&
            pcm.contentEquals(other.pcm)
    }

    override fun hashCode(): Int {
        var result = pcm.contentHashCode()
        result = 31 * result + sampleRate
        result = 31 * result + wordTimings.hashCode()
        result = 31 * result + endOfUtterance.hashCode()
        return result
    }
}
