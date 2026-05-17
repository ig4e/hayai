package yokai.domain.recents.models

data class RecentsHidden(
    val tab: Int,
    val chapterId: Long,
    val mangaId: Long,
    val hiddenAt: Long,
) {
    companion object {
        const val TAB_HISTORY = 1
        const val TAB_UPDATES = 2
    }
}
