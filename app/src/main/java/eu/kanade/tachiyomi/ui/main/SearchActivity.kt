package eu.kanade.tachiyomi.ui.main

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.RouterTransaction
import com.bluelinelabs.conductor.changehandler.SimpleSwapChangeHandler
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.ui.base.SmallToolbarInterface
import eu.kanade.tachiyomi.ui.base.controller.BaseLegacyController
import eu.kanade.tachiyomi.ui.base.controller.DialogController
import eu.kanade.tachiyomi.ui.base.controller.FadeChangeHandler
import eu.kanade.tachiyomi.ui.manga.MangaDetailsController
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.ui.setting.SettingsLegacyController
import eu.kanade.tachiyomi.ui.setting.controllers.SettingsReaderHubController
import eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController
import eu.kanade.tachiyomi.ui.source.globalsearch.GlobalSearchController
import eu.kanade.tachiyomi.util.chapter.ChapterSort
import eu.kanade.tachiyomi.util.system.extensionIntentForText
import eu.kanade.tachiyomi.util.view.withFadeTransaction
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.util.system.launchUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yokai.util.koin.injectLazy
import yokai.domain.chapter.interactor.GetChapter
import yokai.domain.manga.interactor.GetManga
import yokai.presentation.core.Constants
import yokai.presentation.core.util.IntentCommon

class SearchActivity : MainActivity() {
    private val getChapter: GetChapter by injectLazy()
    private val getManga: GetManga by injectLazy()

    private var backToMain = false

    // SearchActivity pushes controllers directly onto the top router (no bottom-nav, no
    // per-tab back-stacks). Skip the RootTabsController scaffolding that MainActivity uses.
    override fun usesRootTabsController(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.toolbar.navigationIcon = backDrawable
        binding.toolbar.setNavigationOnClickListener { popToRoot() }
        binding.searchToolbar.setNavigationOnClickListener {
            val rootSearchController = router.backstack.lastOrNull()?.controller
            if ((
                rootSearchController is RootSearchInterface ||
                    (currentToolbar != binding.searchToolbar && binding.appBar.useLargeToolbar)
                ) && rootSearchController !is SmallToolbarInterface
            ) {
                binding.searchToolbar.menu.findItem(R.id.action_search)?.expandActionView()
            } else {
                popToRoot()
            }
        }
        (router.backstack.lastOrNull()?.controller as? BaseLegacyController<*>)?.setTitle()
        (router.backstack.lastOrNull()?.controller as? SettingsLegacyController)?.setTitle()

        // SearchActivity's pushed controller (GlobalSearchController, MangaDetailsController,
        // etc.) doesn't carry the LibraryController-style releaseSplash() hook, so when this
        // activity is launched cold (process_text / SEARCH / share intent) the splash would
        // otherwise hang until the 5 s SPLASH_MAX_DURATION cap. Release it here — onCreate has
        // already configured the toolbar and pushed the search controller, so the activity is
        // visually ready behind the splash.
        releaseSplash()
    }

    // Override finishAfterTransition since the animation gets weird when launching this from other apps
    override fun finishAfterTransition() {
        if (backToMain) {
            super.finishAfterTransition()
        } else {
            finish()
        }
    }

    override fun backPress() {
        if (router.backstack.size <= 1 || !router.handleBack()) {
            SecureActivityDelegate.locked = true
        }
    }

    private fun popToRoot() {
        if (intentShouldGoBack()) {
            onBackPressedDispatcher.onBackPressed()
        } else if (!router.handleBack()) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            backToMain = true
            startActivity(intent)
            finishAfterTransition()
        }
    }

    override fun setFloatingToolbar(show: Boolean, solidBG: Boolean, changeBG: Boolean, showSearchAnyway: Boolean) {
        super.setFloatingToolbar(show, solidBG, changeBG, showSearchAnyway)
        val useLargeTB = binding.appBar.useLargeToolbar
        if (!useLargeTB) {
            binding.searchToolbar.navigationIcon = backDrawable
        } else if (showSearchAnyway) {
            binding.searchToolbar.navigationIcon =
                if (!show) searchDrawable else backDrawable
        }
    }

    private fun intentShouldGoBack() =
        intent.action in listOf(Constants.SHORTCUT_MANGA, SHORTCUT_READER_SETTINGS, SHORTCUT_BROWSE)

    override fun syncActivityViewWithController(
        to: Controller?,
        from: Controller?,
        isPush: Boolean,
    ) {
        if (from is DialogController || to is DialogController) {
            return
        }
        reEnableBackPressedCallBack()
        setFloatingToolbar(canShowFloatingToolbar(to))
        nav.isVisible = false
        binding.bottomView?.isVisible = false
    }

    override fun handleIntentAction(intent: Intent): Boolean {
        val notificationId = intent.getIntExtra("notificationId", -1)
        if (notificationId > -1) {
            NotificationReceiver.dismissNotification(
                applicationContext,
                notificationId,
                intent.getIntExtra("groupId", 0),
            )
        }
        when (intent.action) {
            Intent.ACTION_SEARCH, Intent.ACTION_SEND, "com.google.android.gms.actions.SEARCH_ACTION" -> {
                // If the intent match the "standard" Android search intent
                // or the Google-specific search intent (triggered by saying or typing "search *query* on *Tachiyomi*" in Google Search/Google Assistant)

                // Get the search query provided in extras, and if not null, perform a global search with it.
                val query = intent.getStringExtra(SearchManager.QUERY) ?: intent.getStringExtra(Intent.EXTRA_TEXT)
                if (!query.isNullOrEmpty()) {
                    extensionIntentForText(query)?.let {
                        startActivity(it)
                        finish()
                        return true
                    }
                    router.replaceTopController(GlobalSearchController(query).withFadeTransaction())
                } else {
                    finish()
                }
            }
            INTENT_SEARCH -> {
                val query = intent.getStringExtra(INTENT_SEARCH_QUERY)
                val filter = intent.getStringExtra(INTENT_SEARCH_FILTER)
                if (!query.isNullOrEmpty()) {
                    if (router.backstackSize > 1) {
                        router.popToRoot()
                    }
                    router.replaceTopController(GlobalSearchController(query, filter).withFadeTransaction())
                }
            }
            Constants.SHORTCUT_MANGA, Constants.SHORTCUT_MANGA_BACK -> {
                val extras = intent.extras ?: return false
                val isBack = intent.action == Constants.SHORTCUT_MANGA_BACK
                val mangaId = extras.getLong(Constants.MANGA_EXTRA)
                if (isBack && preferences.openChapterInShortcuts().get() && mangaId != 0L) {
                    // Resolve manga + next unread chapter off main; either launch reader or fall
                    // back to the details controller. We always return true synchronously — the
                    // routing decision is made inside the coroutine.
                    lifecycleScope.launchUI {
                        val manga = withContext(Dispatchers.IO) { getManga.awaitById(mangaId) }
                        val nextUnreadChapter = manga?.let {
                            val chapters = withContext(Dispatchers.IO) { getChapter.awaitAll(it) }
                            ChapterSort(it).getNextUnreadChapter(chapters, false)
                        }
                        if (manga != null && nextUnreadChapter != null) {
                            startActivity(ReaderActivity.newIntent(this@SearchActivity, manga, nextUnreadChapter))
                            finish()
                        } else {
                            SecureActivityDelegate.promptLockIfNeeded(this@SearchActivity, true)
                            router.replaceTopController(
                                RouterTransaction.with(MangaDetailsController(extras))
                                    .pushChangeHandler(SimpleSwapChangeHandler())
                                    .popChangeHandler(FadeChangeHandler()),
                            )
                        }
                    }
                    return true
                }
                if (isBack) {
                    SecureActivityDelegate.promptLockIfNeeded(this, true)
                }
                router.replaceTopController(
                    RouterTransaction.with(MangaDetailsController(extras))
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler()),
                )
            }
            SHORTCUT_SOURCE -> {
                val extras = intent.extras ?: return false
                SecureActivityDelegate.promptLockIfNeeded(this, true)
                router.replaceTopController(
                    RouterTransaction.with(BrowseSourceController(extras))
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler()),
                )
            }
            SHORTCUT_READER_SETTINGS -> {
                router.replaceTopController(
                    RouterTransaction.with(SettingsReaderHubController())
                        .pushChangeHandler(SimpleSwapChangeHandler())
                        .popChangeHandler(FadeChangeHandler()),
                )
            }
            else -> return false
        }
        return true
    }

    companion object {
        fun openMangaIntent(context: Context, id: Long?, canReturnToMain: Boolean = false) =
            IntentCommon.openManga(context, id, canReturnToMain)

        fun openReaderSettings(context: Context) = Intent(
            context,
            SearchActivity::class
                .java,
        )
            .apply {
                action = SHORTCUT_READER_SETTINGS
            }
    }
}
