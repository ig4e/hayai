package eu.kanade.tachiyomi.ui.manga.track

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.domain.manga.interactor.ApplyTrackerMetadata
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.update
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.CustomLabelTextField
import tachiyomi.presentation.core.i18n.stringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Immutable
data class TrackerMetadataEnrichmentState(
    val isLoading: Boolean = false,
    val candidates: List<TrackerMetadataCandidate> = emptyList(),
    val selectedTrackerId: Long? = null,
    val form: TrackerMetadataFormState = TrackerMetadataFormState(),
    val shouldClose: Boolean = false,
)

data class TrackerMetadataEnrichmentScreen(
    private val mangaId: Long,
    private val preferredTrackerId: Long? = null,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { Model(mangaId, preferredTrackerId) }
        val state by screenModel.state.collectAsState()

        LaunchedEffect(state.shouldClose) {
            if (state.shouldClose) {
                navigator.pop()
            }
        }

        AlertDialog(
            onDismissRequest = screenModel::skip,
            confirmButton = {
                TextButton(
                    onClick = screenModel::apply,
                    enabled = !state.isLoading,
                ) {
                    Text(stringResource(MR.strings.action_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = screenModel::skip) {
                    Text(stringResource(MR.strings.onboarding_action_skip))
                }
            },
            title = {
                Text(
                    text = stringResource(SYMR.strings.fill_from_tracker),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            text = {
                TrackerMetadataEnrichmentContent(
                    state = state,
                    onSelectedTrackerChange = screenModel::selectTracker,
                    onTitleChange = screenModel::updateTitle,
                    onAuthorChange = screenModel::updateAuthor,
                    onArtistChange = screenModel::updateArtist,
                    onThumbnailUrlChange = screenModel::updateThumbnailUrl,
                    onDescriptionChange = screenModel::updateDescription,
                )
            },
        )
    }

    private class Model(
        private val mangaId: Long,
        private val preferredTrackerId: Long?,
        private val getTracks: GetTracks = Injekt.get(),
        private val trackerManager: eu.kanade.tachiyomi.data.track.TrackerManager = Injekt.get(),
        private val getManga: GetManga = Injekt.get(),
        private val applyTrackerMetadata: ApplyTrackerMetadata = Injekt.get(),
        private val context: Application = Injekt.get(),
    ) : StateScreenModel<TrackerMetadataEnrichmentState>(TrackerMetadataEnrichmentState()) {

        private var manga: Manga? = null

        init {
            screenModelScope.launchNonCancellable {
                manga = getManga.await(mangaId)
                loadCandidates()
            }
        }

        private suspend fun loadCandidates() {
            mutableState.update { it.copy(isLoading = true) }
            val candidates = loadTrackerMetadataCandidates(
                mangaId = mangaId,
                getTracks = getTracks,
                trackerManager = trackerManager,
                onError = { tracker, e ->
                    logcat(LogPriority.ERROR, e) { "Failed loading tracker metadata for enrichment" }
                    withUIContext {
                        context.toast(
                            context.stringResource(
                                MR.strings.track_error,
                                tracker.name,
                                e.message.orEmpty(),
                            ),
                        )
                    }
                },
            )

            if (candidates.isEmpty()) {
                withUIContext {
                    context.toast(context.stringResource(SYMR.strings.no_tracker_metadata_available))
                }
                mutableState.update { it.copy(isLoading = false, shouldClose = true) }
                return
            }

            val selectedTrackerId = preferredTrackerId
                ?.takeIf { preferredId -> candidates.any { it.tracker.id == preferredId } }
                ?: candidates.first().tracker.id
            val selectedCandidate = candidates.first { it.tracker.id == selectedTrackerId }
            mutableState.update {
                it.copy(
                    isLoading = false,
                    candidates = candidates,
                    selectedTrackerId = selectedTrackerId,
                    form = selectedCandidate.metadata.toFormState(),
                )
            }
        }

        fun selectTracker(trackerId: Long) {
            val selectedCandidate = state.value.candidates.firstOrNull { it.tracker.id == trackerId } ?: return
            mutableState.update {
                it.copy(
                    selectedTrackerId = trackerId,
                    form = selectedCandidate.metadata.toFormState(),
                )
            }
        }

        fun updateTitle(value: String) {
            mutableState.update { it.copy(form = it.form.copy(title = value)) }
        }

        fun updateAuthor(value: String) {
            mutableState.update { it.copy(form = it.form.copy(author = value)) }
        }

        fun updateArtist(value: String) {
            mutableState.update { it.copy(form = it.form.copy(artist = value)) }
        }

        fun updateThumbnailUrl(value: String) {
            mutableState.update { it.copy(form = it.form.copy(thumbnailUrl = value)) }
        }

        fun updateDescription(value: String) {
            mutableState.update { it.copy(form = it.form.copy(description = value)) }
        }

        fun apply() {
            val currentManga = manga ?: return
            val metadata = state.value.form.toMetadata()
            screenModelScope.launchNonCancellable {
                applyTrackerMetadata.await(currentManga, metadata)
                mutableState.update { it.copy(shouldClose = true) }
            }
        }

        fun skip() {
            mutableState.update { it.copy(shouldClose = true) }
        }
    }
}

@Composable
private fun TrackerMetadataEnrichmentContent(
    state: TrackerMetadataEnrichmentState,
    onSelectedTrackerChange: (Long) -> Unit,
    onTitleChange: (String) -> Unit,
    onAuthorChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onThumbnailUrlChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .width(360.dp)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.systemBars),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(SYMR.strings.tracker_enrichment_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.isLoading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (state.candidates.size > 1) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    state.candidates.forEach { candidate ->
                        val selected = candidate.tracker.id == state.selectedTrackerId
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            tonalElevation = if (selected) 3.dp else 0.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                            modifier = Modifier.clickable { onSelectedTrackerChange(candidate.tracker.id) },
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                TrackLogoIcon(candidate.tracker)
                                Text(
                                    text = candidate.tracker.name,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }

            TrackerMetadataSection(stringResource(SYMR.strings.fill_from_tracker)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CustomLabelTextField(
                        value = state.form.title,
                        onValueChange = onTitleChange,
                        labelText = context.stringResource(SYMR.strings.title_hint, ""),
                        singleLine = true,
                    )
                    CustomLabelTextField(
                        value = state.form.author,
                        onValueChange = onAuthorChange,
                        labelText = context.stringResource(SYMR.strings.author_hint, ""),
                        singleLine = true,
                    )
                    CustomLabelTextField(
                        value = state.form.artist,
                        onValueChange = onArtistChange,
                        labelText = context.stringResource(SYMR.strings.artist_hint, ""),
                        singleLine = true,
                    )
                    CustomLabelTextField(
                        value = state.form.thumbnailUrl,
                        onValueChange = onThumbnailUrlChange,
                        labelText = context.stringResource(SYMR.strings.thumbnail_url_hint, ""),
                        singleLine = true,
                    )
                    CustomLabelTextField(
                        value = state.form.description,
                        onValueChange = onDescriptionChange,
                        labelText = context.stringResource(SYMR.strings.description_hint, ""),
                        minLines = 4,
                        maxLines = 6,
                    )
                }
            }
        }
    }
}

@Composable
private fun TrackerMetadataSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            content()
        }
    }
}
