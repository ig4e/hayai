package eu.kanade.tachiyomi.ui.setting.controllers.debug

import yokai.util.koin.get
import android.app.Application
import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import eu.kanade.tachiyomi.ui.base.presenter.BaseCoroutinePresenter
import eu.kanade.tachiyomi.util.lang.toDateTimestampString
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class WorkerInfoPresenter : BaseCoroutinePresenter<WorkerInfoController>() {
    private val workManager by lazy { WorkManager.getInstance(get<Application>()) }

    val finished by lazy {
        workManager
            .getWorkInfosLiveData(
                WorkQuery.fromStates(
                    WorkInfo.State.SUCCEEDED,
                    WorkInfo.State.FAILED,
                    WorkInfo.State.CANCELLED,
                ),
            )
            .asFlow()
            .map(::constructString)
            .stateIn(presenterScope, SharingStarted.WhileSubscribed(), "")
    }

    val running by lazy {
        workManager
            .getWorkInfosLiveData(WorkQuery.fromStates(WorkInfo.State.RUNNING))
            .asFlow()
            .map(::constructString)
            .stateIn(presenterScope, SharingStarted.WhileSubscribed(), "")
    }

    val enqueued by lazy {
        workManager
            .getWorkInfosLiveData(WorkQuery.fromStates(WorkInfo.State.ENQUEUED))
            .asFlow()
            .map(::constructString)
            .stateIn(presenterScope, SharingStarted.WhileSubscribed(), "")
    }

    private fun constructString(list: List<WorkInfo>) = buildString {
        if (list.isEmpty()) {
            appendLine("-")
        } else {
            val newList = list.toList()
            newList.forEach { workInfo ->
                appendLine("Id: ${workInfo.id}")
                appendLine("Tags:")
                workInfo.tags.forEach {
                    appendLine(" - $it")
                }
                appendLine("State: ${workInfo.state}")
                if (workInfo.state == WorkInfo.State.ENQUEUED) {
                    val timestamp = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(workInfo.nextScheduleTimeMillis),
                        ZoneId.systemDefault(),
                    )
                        .toDateTimestampString(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
                    appendLine("Next scheduled run: $timestamp")
                    appendLine("Attempt #${workInfo.runAttemptCount + 1}")
                }
                appendLine()
            }
        }
    }
}
