package eu.kanade.tachiyomi.ui.reader.viewer

import yokai.util.koin.get
import eu.kanade.tachiyomi.core.preference.Preference
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.data.preference.changesIn
import kotlinx.coroutines.CoroutineScope
import yokai.domain.ui.settings.ReaderPreferences

/**
 * Common configuration for all viewers.
 */
abstract class ViewerConfig(
    preferences: PreferencesHelper,
    protected val scope: CoroutineScope,
    readerPreferences: ReaderPreferences = get(),
) {

    var imagePropertyChangedListener: (() -> Unit)? = null
    var reloadChapterListener: ((Boolean) -> Unit)? = null

    var navigationModeChangedListener: (() -> Unit)? = null
    var navigationModeInvertedListener: (() -> Unit)? = null

    var longTapEnabled = true
    var tappingInverted = ViewerNavigation.TappingInvertMode.NONE
    var doubleTapAnimDuration = 500
    var volumeKeysEnabled = false
    var volumeKeysInverted = false
    var alwaysShowChapterTransition = true

    var navigationOverlayForNewUser = false
    var navigationMode = 0
        protected set

    var debugMode = false

    abstract var navigator: ViewerNavigation
        protected set

    init {
        preferences.readWithLongTap()
            .register({ longTapEnabled = it })

        preferences.doubleTapAnimSpeed()
            .register({ doubleTapAnimDuration = it })

        preferences.readWithVolumeKeys()
            .register({ volumeKeysEnabled = it })

        preferences.readWithVolumeKeysInverted()
            .register({ volumeKeysInverted = it })

        preferences.alwaysShowChapterTransition()
            .register({ alwaysShowChapterTransition = it })

        readerPreferences.debugMode()
            .register({ debugMode = it })
    }

    fun <T> Preference<T>.register(
        valueAssignment: (T) -> Unit,
        onChanged: (T) -> Unit = {},
    ) {
        changesIn(scope) {
            valueAssignment(it)
            onChanged(it)
        }
    }

    protected abstract fun defaultNavigation(): ViewerNavigation

    abstract fun updateNavigation(navigationMode: Int)
}
