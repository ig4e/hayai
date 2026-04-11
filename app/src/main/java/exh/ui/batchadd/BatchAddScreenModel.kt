package exh.ui.batchadd

import android.content.Context
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import exh.GalleryAddEvent
import exh.GalleryAdder
import exh.source.ExhPreferences
import exh.util.trimOrNull
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class BatchAddScreenModel(
    private val exhPreferences: ExhPreferences = Injekt.get(),
) : StateScreenModel<BatchAddState>(BatchAddState()) {
    private val galleryAdder by lazy { GalleryAdder() }

    fun addGalleries(context: Context) {
        val galleries = state.value.galleries
        if (galleries.isBlank()) {
            mutableState.update { it.copy(dialog = Dialog.NoGalleriesSpecified) }
            return
        }

        addGalleries(context, galleries)
    }

    private fun addGalleries(context: Context, galleries: String) {
        val splitGalleries = if (ehVisitedRegex.containsMatchIn(galleries)) {
            val url = if (exhPreferences.enableExhentai.get()) {
                "https://exhentai.org/g/"
            } else {
                "https://e-hentai.org/g/"
            }
            ehVisitedRegex.findAll(galleries).map { galleryKeys ->
                val linkParts = galleryKeys.value.split(".")
                url + linkParts[0] + "/" + linkParts[1].replace(":", "")
            }.toList()
        } else {
            galleries.split("\n").mapNotNull(String::trimOrNull)
        }

        mutableState.update { state ->
            state.copy(
                progress = 0,
                progressTotal = splitGalleries.size,
                state = State.PROGRESS,
            )
        }

        val handler = CoroutineExceptionHandler { _, throwable ->
            // Log error
        }

        screenModelScope.launch(Dispatchers.IO + handler) {
            val succeeded = mutableListOf<String>()
            val failed = mutableListOf<String>()

            splitGalleries.forEachIndexed { i, s ->
                ensureActive()
                val result = withContext(Dispatchers.IO) {
                    galleryAdder.addGallery(
                        context = context,
                        url = s,
                        fav = true,
                        retry = 2,
                    )
                }
                if (result is GalleryAddEvent.Success) {
                    succeeded.add(s)
                } else {
                    failed.add(s)
                }
                mutableState.update { state ->
                    state.copy(
                        progress = i + 1,
                        events = state.events.plus(
                            when (result) {
                                is GalleryAddEvent.Success -> "OK"
                                is GalleryAddEvent.Fail -> "Error"
                            } + " " + result.logMessage,
                        ),
                    )
                }
            }

            val summary = "Added ${succeeded.size}, Failed ${failed.size}"
            mutableState.update { state ->
                state.copy(events = state.events + summary)
            }
        }
    }

    fun finish() {
        mutableState.update { state ->
            state.copy(
                progressTotal = 0,
                progress = 0,
                galleries = "",
                state = State.INPUT,
                events = emptyList(),
            )
        }
    }

    fun updateGalleries(galleries: String) {
        mutableState.update { it.copy(galleries = galleries) }
    }

    fun dismissDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    enum class State {
        INPUT,
        PROGRESS,
    }

    sealed class Dialog {
        data object NoGalleriesSpecified : Dialog()
    }

    companion object {
        val ehVisitedRegex = """[0-9]*?\.[a-z0-9]*?:""".toRegex()
    }
}

data class BatchAddState(
    val progressTotal: Int = 0,
    val progress: Int = 0,
    val galleries: String = "",
    val state: BatchAddScreenModel.State = BatchAddScreenModel.State.INPUT,
    val events: List<String> = emptyList(),
    val dialog: BatchAddScreenModel.Dialog? = null,
)
