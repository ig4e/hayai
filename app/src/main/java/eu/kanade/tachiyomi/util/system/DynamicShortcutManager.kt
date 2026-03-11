package eu.kanade.tachiyomi.util.system

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.history.interactor.GetHistory
import tachiyomi.domain.history.interactor.GetNextChapters
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class DynamicShortcutManager(
    private val context: Context,
    private val getHistory: GetHistory = Injekt.get(),
    private val getNextChapters: GetNextChapters = Injekt.get(),
    private val uiPreferences: UiPreferences = Injekt.get(),
) {

    suspend fun refreshContinueReadingShortcut() {
        if (!uiPreferences.dynamicShortcuts().get()) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(CONTINUE_READING_SHORTCUT_ID))
            return
        }

        val lastHistory = getHistory.awaitLast()
        if (lastHistory == null) {
            ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(CONTINUE_READING_SHORTCUT_ID))
            return
        }

        val chapter = getNextChapters.await(
            mangaId = lastHistory.mangaId,
            fromChapterId = lastHistory.chapterId,
            onlyUnread = false,
        ).firstOrNull()

        val entryIntent = if (chapter != null) {
            ReaderActivity.newIntent(context, chapter.mangaId, chapter.id)
        } else {
            Intent(context, MainActivity::class.java).apply {
                action = tachiyomi.core.common.Constants.SHORTCUT_MANGA
                putExtra(tachiyomi.core.common.Constants.MANGA_EXTRA, lastHistory.mangaId)
            }
        }

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

        ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
    }

    companion object {
        private const val CONTINUE_READING_SHORTCUT_ID = "continue_reading"
    }
}
