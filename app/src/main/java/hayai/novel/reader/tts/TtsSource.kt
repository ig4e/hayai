package hayai.novel.reader.tts

/**
 * The viewer-side contract the [NovelTtsController] calls back into. Implemented by
 * `NovelWebViewViewer`. The controller never touches the WebView, the chapter cache, or
 * navigation directly — every cross-process operation goes through this surface so the
 * controller stays unit-testable and there is exactly one place that knows about the DOM.
 */
interface TtsSource {

    /**
     * Returns the paragraph list for [chapterId]. If the chapter is not yet loaded, the
     * implementation should trigger the load and suspend until `Page.State.Ready` (or fail
     * with `null` on timeout / error). Paragraphs MUST be in the same order as the DOM's
     * `data-paragraph-index` attributes for that chapter so the highlight stays in sync.
     */
    suspend fun awaitChapterParagraphs(chapterId: Long): List<String>?

    /**
     * Triggers `loadNextChapter()` on the activity and suspends until the new chapter is
     * Ready. Returns the new chapter's DB id, or `null` if there is no next chapter, the
     * load failed, or it timed out.
     */
    suspend fun awaitNextChapterReady(): Long?

    /** Mirror of [awaitNextChapterReady] in the other direction. */
    suspend fun awaitPreviousChapterReady(): Long?

    /** DB id of the chapter currently visible in the viewport (best effort). */
    fun currentVisibleChapterId(): Long?

    /**
     * Paragraph index (0-based, chapter-local) of the first paragraph currently visible
     * in the viewport, or `null` if it can't be resolved synchronously.
     */
    suspend fun currentVisibleParagraphIndex(): Int?

    /**
     * Paint the highlight on paragraph [paragraphIndex] inside the chapter scoped to
     * [chapterId]. The viewer is responsible for scroll-into-view if the user enabled it.
     */
    fun paintHighlight(chapterId: Long, paragraphIndex: Int)

    fun clearHighlight()

    fun chapterTitle(chapterId: Long): String?

    fun novelTitle(): String?
}
