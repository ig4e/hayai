package hayai.novel.tts.engine

/**
 * Maps a half-open character range `[charStart, charEnd)` of the synthesized utterance to a
 * half-open audio range `[audioStartMs, audioEndMs)` measured from the start of the utterance's
 * playback. The playback layer schedules a highlight transition at `audioStartMs` and clears it
 * (or moves to the next word) at `audioEndMs`.
 *
 * Engines populate timings from `UtteranceProgressListener.onRangeStart` (API 26+). When the
 * engine doesn't fire that callback the playback layer falls back to estimating timings by
 * distributing the chunk's audio duration across word lengths.
 */
data class WordTiming(
    val charStart: Int,
    val charEnd: Int,
    val audioStartMs: Long,
    val audioEndMs: Long,
)
