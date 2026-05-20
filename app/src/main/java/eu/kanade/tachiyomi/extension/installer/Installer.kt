package eu.kanade.tachiyomi.extension.installer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.CallSuper
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.extension.util.ExtensionInstaller.Companion.EXTRA_DOWNLOAD_ID
import java.util.Collections
import java.util.concurrent.atomic.AtomicReference
import yokai.util.koin.injectLazy

abstract class Installer(
    internal val context: Context,
    // TODO: Remove finishedQueue
    internal val finishedQueue: (Installer) -> Unit,
) {

    private val extensionManager: ExtensionManager by injectLazy()

    private var waitingInstall = AtomicReference<Entry>(null)
    private val queue = Collections.synchronizedList(mutableListOf<Entry>())

    private val cancelReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val downloadId = intent.getLongExtra(EXTRA_DOWNLOAD_ID, -1).takeIf { it >= 0 } ?: return
            cancelQueue(downloadId)
        }
    }

    abstract var ready: Boolean

    fun isInQueue(pkgName: String) = queue.any { it.pkgName == pkgName }

    /**
     * Add an item to install queue.
     *
     * @param downloadId Download ID as known by [ExtensionManager]
     * @param uri Uri of APK to install
     */
    fun addToQueue(downloadId: Long, pkgName: String, uri: Uri) {
        queue.add(Entry(downloadId, pkgName, uri))
        checkQueue()
    }

    @CallSuper
    open fun processEntry(entry: Entry) {
        extensionManager.setInstalling(entry.downloadId, entry.uri.hashCode())
    }

    open fun cancelEntry(entry: Entry): Boolean {
        return true
    }

    /**
     * Tells the queue to continue processing the next entry and updates the install step
     * of the completed entry ([waitingInstall]) to [ExtensionManager].
     *
     * @param resultStep new install step for the processed entry.
     * @see waitingInstall
     */
    fun continueQueue(succeeded: Boolean) {
        val completedEntry = waitingInstall.getAndSet(null)
        if (completedEntry != null) {
            extensionManager.setInstallationResult(completedEntry.downloadId, succeeded)
            checkQueue()
        }
    }

    fun checkQueue() {
        if (!ready) {
            return
        }
        if (queue.isEmpty()) {
            finishedQueue(this)
            return
        }
        val nextEntry = queue.first()
        if (waitingInstall.compareAndSet(null, nextEntry)) {
            queue.removeAt(0)
            processEntry(nextEntry)
        }
    }

    @CallSuper
    open fun onDestroy() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(cancelReceiver)
        queue.forEach { extensionManager.setInstallationResult(it.pkgName, false) }
        queue.clear()
        waitingInstall.set(null)
    }

    protected fun getActiveEntry(): Entry? = waitingInstall.get()

    /**
     * Cancels queue for the provided download ID if exists.
     *
     * @param downloadId Download ID as known by [ExtensionManager]
     */
    fun cancelQueue(downloadId: Long) {
        val waitingInstall = this.waitingInstall.get()
        val toCancel = queue.find { it.downloadId == downloadId } ?: waitingInstall ?: return
        if (cancelEntry(toCancel)) {
            queue.remove(toCancel)
            if (waitingInstall == toCancel) {
                // Currently processing removed entry, continue queue
                this.waitingInstall.set(null)
                checkQueue()
            }
            queue.forEach { extensionManager.setInstallationResult(it.downloadId, false) }
//            extensionManager.up(downloadId, InstallStep.Idle)
        }
    }

    data class Entry(
        val downloadId: Long,
        val pkgName: String,
        val uri: Uri,
    )
}
