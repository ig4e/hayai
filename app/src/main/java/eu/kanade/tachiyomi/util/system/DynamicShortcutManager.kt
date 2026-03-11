package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import kotlinx.coroutines.flow.first
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.domain.history.model.HistoryWithRelations
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import tachiyomi.core.common.Constants

class DynamicShortcutManager(
    private val context: Context,
    private val getHistory: GetHistory = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val sourcePreferences: SourcePreferences = Injekt.get(),
    private val uiPreferences: UiPreferences = Injekt.get(),
) {

    suspend fun refreshDynamicShortcuts() {
        if (!uiPreferences.dynamicShortcuts().get()) {
            clearManagedShortcuts()
            return
        }

        val seriesEnabled = uiPreferences.showSeriesInShortcuts().get()
        val sourcesEnabled = uiPreferences.showSourcesInShortcuts().get()
        if (!seriesEnabled && !sourcesEnabled) {
            clearManagedShortcuts()
            return
        }

        val shortcutLimit = context.getSystemService<android.content.pm.ShortcutManager>()
            ?.maxShortcutCountPerActivity
            ?.coerceAtLeast(1)
            ?: 5

        val candidates = mutableListOf<ShortcutCandidate>()

        if (seriesEnabled) {
            val history = getHistory.subscribe("").first()
            val latestHistory = history.firstOrNull()
            if (latestHistory != null) {
                candidates += ShortcutCandidate(
                    id = CONTINUE_READING_SHORTCUT_ID,
                    timestamp = latestHistory.readAt?.time ?: 0L,
                    builder = { buildContinueReadingShortcut(latestHistory) },
                )
            }

            history
                .distinctBy { it.mangaId }
                .forEach { item ->
                    candidates += ShortcutCandidate(
                        id = "manga-${item.mangaId}",
                        timestamp = item.readAt?.time ?: 0L,
                        builder = { buildMangaShortcut(item) },
                    )
                }
        }

        if (sourcesEnabled) {
            val sourceEntries = sourcePreferences.lastUsedSources().get()
                .mapNotNull { entry ->
                    val split = entry.split(":")
                    val sourceId = split.getOrNull(0)?.toLongOrNull()
                    val timestamp = split.getOrNull(1)?.toLongOrNull()
                    if (sourceId != null && timestamp != null) {
                        sourceId to timestamp
                    } else {
                        null
                    }
                }
            val fallbackSourceId = sourcePreferences.lastUsedSource().get()
            val recentSources = if (sourceEntries.isNotEmpty()) {
                sourceEntries
            } else if (fallbackSourceId > 0) {
                listOf(fallbackSourceId to 0L)
            } else {
                emptyList()
            }

            recentSources.forEach { (sourceId, timestamp) ->
                candidates += ShortcutCandidate(
                    id = "source-$sourceId",
                    timestamp = timestamp,
                    builder = { buildSourceShortcut(sourceManager.getOrStub(sourceId)) },
                )
            }
        }

        val shortcuts = mutableListOf<ShortcutInfoCompat>()
        candidates
            .sortedByDescending { it.timestamp }
            .forEach { candidate ->
                if (shortcuts.size >= shortcutLimit || shortcuts.any { it.id == candidate.id }) {
                    return@forEach
                }
                candidate.builder()?.let { shortcuts += it }
            }

        if (shortcuts.isNotEmpty()) {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } else {
            clearManagedShortcuts()
        }
    }

    suspend fun refreshContinueReadingShortcut() {
        refreshDynamicShortcuts()
    }

    private suspend fun buildContinueReadingShortcut(lastHistory: HistoryWithRelations): ShortcutInfoCompat? {
        val entryIntent = getEntryIntent(lastHistory)

        val shortcut = ShortcutInfoCompat.Builder(context, CONTINUE_READING_SHORTCUT_ID)
            .setShortLabel(context.stringResource(MR.strings.shortcut_continue_reading))
            .setLongLabel(lastHistory.title)
            .setIcon(IconCompat.createWithResource(context, R.drawable.sc_history_48dp))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                },
            )
            .setAlwaysBadged()
            .setIntents(
                arrayOf(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                    },
                    entryIntent,
                ),
            )
            .build()

        return shortcut
    }

    private suspend fun buildMangaShortcut(history: HistoryWithRelations): ShortcutInfoCompat? {
        val entryIntent = getEntryIntent(history)
        return ShortcutInfoCompat.Builder(context, "manga-${history.mangaId}")
            .setShortLabel(history.title)
            .setLongLabel(history.title)
            .setIcon(IconCompat.createWithResource(context, R.drawable.ic_book_24dp))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                },
            )
            .setAlwaysBadged()
            .setIntents(
                arrayOf(
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_VIEW
                    },
                    entryIntent,
                ),
            )
            .build()
    }

    private fun buildSourceShortcut(source: Source): ShortcutInfoCompat {
        return ShortcutInfoCompat.Builder(context, "source-${source.id}")
            .setShortLabel(source.name)
            .setLongLabel(source.name)
            .setIcon(IconCompat.createWithResource(context, R.drawable.sc_extensions_48dp))
            .setIntent(
                Intent(context, MainActivity::class.java).apply {
                    action = Constants.SHORTCUT_SOURCE
                    putExtra(Constants.SOURCE_EXTRA, source.id)
                },
            )
            .setAlwaysBadged()
            .build()
    }

    private suspend fun getEntryIntent(history: HistoryWithRelations): Intent {
        val shouldOpenChapter = uiPreferences.openChapterInShortcuts().get()
        if (shouldOpenChapter) {
            val chapter = getNextChapters.await(
                mangaId = history.mangaId,
                fromChapterId = history.chapterId,
                onlyUnread = false,
            ).firstOrNull()
            if (chapter != null) {
                return ReaderActivity.newIntent(context, chapter.mangaId, chapter.id)
            }
        }
        return Intent(context, MainActivity::class.java).apply {
            action = Constants.SHORTCUT_MANGA
            putExtra(Constants.MANGA_EXTRA, history.mangaId)
        }
    }

    private fun clearManagedShortcuts() {
        val managedIds = ShortcutManagerCompat.getDynamicShortcuts(context)
            .map { it.id }
            .filter {
                it == CONTINUE_READING_SHORTCUT_ID ||
                    it.startsWith("manga-") ||
                    it.startsWith("source-") ||
                    it.startsWith("Manga-") ||
                    it.startsWith("Source-")
            }
        if (managedIds.isNotEmpty()) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, managedIds)
        }
    }

    private data class ShortcutCandidate(
        val id: String,
        val timestamp: Long,
        val builder: suspend () -> ShortcutInfoCompat?,
    )

    companion object {
        private const val CONTINUE_READING_SHORTCUT_ID = "continue_reading"
    }
}
