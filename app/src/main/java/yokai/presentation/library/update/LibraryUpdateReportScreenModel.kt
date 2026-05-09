package yokai.presentation.library.update

import android.content.Context
import android.net.Uri
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.tachiyomi.data.library.LibraryUpdateReport
import eu.kanade.tachiyomi.data.library.LibraryUpdateReportEntry
import eu.kanade.tachiyomi.data.library.LibraryUpdateReportStore
import eu.kanade.tachiyomi.util.storage.getUriCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LibraryUpdateReportScreenModel(
    private val context: Context,
) : StateScreenModel<LibraryUpdateReportScreenModel.State>(State.Loading) {

    private val store = LibraryUpdateReportStore(context)

    init {
        load()
    }

    fun load() {
        screenModelScope.launch(Dispatchers.IO) {
            val report = store.read()
            mutableState.update {
                State.Loaded(
                    report = report,
                    errorGroups = report?.errors.orEmpty().groupedByMessage(),
                    skippedGroups = report?.skipped.orEmpty().groupedByMessage(),
                )
            }
        }
    }

    /** FileProvider URI for the .txt log of the given tab, or null if the file is gone. */
    fun logFileUri(tab: ReportTab): Uri? {
        val file = when (tab) {
            ReportTab.ERRORS -> store.errorLogFile()
            ReportTab.SKIPPED -> store.skippedLogFile()
        } ?: return null
        return file.getUriCompat(context)
    }

    /**
     * Group entries by their message (error string or skip reason). Insertion order is
     * preserved so the screen mirrors how the .txt log groups identical reasons together.
     */
    private fun List<LibraryUpdateReportEntry>.groupedByMessage(): List<ReasonGroup> {
        if (isEmpty()) return emptyList()
        val byMessage = LinkedHashMap<String, MutableList<LibraryUpdateReportEntry>>()
        for (entry in this) {
            byMessage.getOrPut(entry.message) { mutableListOf() }.add(entry)
        }
        return byMessage.map { (message, entries) -> ReasonGroup(message, entries) }
    }

    sealed interface State {
        data object Loading : State
        data class Loaded(
            val report: LibraryUpdateReport?,
            val errorGroups: List<ReasonGroup>,
            val skippedGroups: List<ReasonGroup>,
        ) : State
    }

    data class ReasonGroup(
        val message: String,
        val entries: List<LibraryUpdateReportEntry>,
    )

    enum class ReportTab { ERRORS, SKIPPED }
}
