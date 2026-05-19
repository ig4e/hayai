package eu.kanade.tachiyomi.debug

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import leakcanary.LeakCanary
import shark.AndroidReferenceMatchers
import shark.LibraryLeakReferenceMatcher
import shark.ReferencePattern.InstanceFieldPattern

/**
 * Registers extra LeakCanary reference matchers for known Android-framework retentions
 * that aren't bugs in app code. Auto-instantiated by the manifest before Application.onCreate.
 */
class LeakCanaryConfigProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        LeakCanary.config = LeakCanary.config.copy(
            referenceMatchers = AndroidReferenceMatchers.appDefaults + listOf(
                LibraryLeakReferenceMatcher(
                    pattern = InstanceFieldPattern(
                        className = "android.app.ExitTransitionCoordinator",
                        fieldName = "mExitCallbacks",
                    ),
                    // Platform holds ActivityExitTransitionCallbacks via a binder ResultReceiver
                    // that the system releases on its own schedule.
                    description = "ExitTransitionCoordinator retained by ResultReceiver native global ref.",
                ),
            ),
        )
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
