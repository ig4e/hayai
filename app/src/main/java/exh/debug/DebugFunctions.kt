package exh.debug

import android.app.Application
import eu.kanade.tachiyomi.source.SourceManager
import eu.kanade.tachiyomi.source.online.all.NHentai
import exh.eh.EHentaiUpdateWorker
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.source.nHentaiSourceIds
import exh.util.jobScheduler
import eu.kanade.tachiyomi.util.system.workManager
import kotlinx.coroutines.runBlocking
import yokai.data.DatabaseHandler
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.util.UUID

@Suppress("unused")
object DebugFunctions {
    private val app: Application by injectLazy()
    private val handler: DatabaseHandler by injectLazy()
    private val sourceManager: SourceManager by injectLazy()

    fun addAllMangaInDatabaseToLibrary() {
        runBlocking { handler.await { ehQueries.addAllMangaInDatabaseToLibrary() } }
    }

    fun convertAllEhentaiGalleriesToExhentai() = convertSources(EH_SOURCE_ID, EXH_SOURCE_ID)

    fun convertAllExhentaiGalleriesToEhentai() = convertSources(EXH_SOURCE_ID, EH_SOURCE_ID)

    fun testLaunchEhentaiBackgroundUpdater() {
        EHentaiUpdateWorker.launchBackgroundTest(app)
    }

    fun rescheduleEhentaiBackgroundUpdater() {
        EHentaiUpdateWorker.scheduleBackground(app)
    }

    fun listScheduledJobs() = app.jobScheduler.allPendingJobs.joinToString(",\n") { j ->
        val info = j.extras.getString("EXTRA_WORK_SPEC_ID")?.let {
            app.workManager.getWorkInfoById(UUID.fromString(it)).get()
        }

        if (info != null) {
            """
            {
                id: ${info.id},
                isPeriodic: ${j.extras.getBoolean("EXTRA_IS_PERIODIC")},
                state: ${info.state.name},
                tags: [
                    ${info.tags.joinToString(separator = ",\n                    ")}
                ],
            }
            """.trimIndent()
        } else {
            """
            {
                info: ${j.id},
                isPeriodic: ${j.isPeriodic},
                isPersisted: ${j.isPersisted},
                intervalMillis: ${j.intervalMillis},
            }
            """.trimIndent()
        }
    }

    fun cancelAllScheduledJobs() = app.jobScheduler.cancelAll()

    fun listAllSources() = sourceManager.getCatalogueSources().joinToString("\n") {
        "${it.id}: ${it.name} (${it.lang.uppercase()})"
    }

    fun listAllSourcesClassName() = sourceManager.getCatalogueSources().joinToString("\n") {
        "${it::class.qualifiedName}: ${it.name} (${it.lang.uppercase()})"
    }

    fun listAllHttpSources() = sourceManager.getOnlineSources().joinToString("\n") {
        "${it.id}: ${it.name} (${it.lang.uppercase()})"
    }

    fun fixReaderViewerBackupBug() {
        runBlocking { handler.await { ehQueries.fixReaderViewerBackupBug() } }
    }

    fun resetReaderViewerForAllManga() {
        runBlocking { handler.await { ehQueries.resetReaderViewerForAllManga() } }
    }

    fun migrateLangNhentaiToMultiLangSource() {
        val sources = nHentaiSourceIds - NHentai.otherId

        runBlocking { handler.await { ehQueries.migrateAllNhentaiToOtherLang(NHentai.otherId, sources) } }
    }

    private fun convertSources(from: Long, to: Long) {
        runBlocking {
            handler.await { ehQueries.migrateSource(to, from) }
        }
    }
}
