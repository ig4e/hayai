package eu.kanade.tachiyomi.ui.manga

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.ui.manga.track.TrackerMetadataCandidate
import eu.kanade.tachiyomi.ui.manga.track.loadTrackerMetadataCandidates
import eu.kanade.tachiyomi.ui.manga.track.toFormState
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.toast
import exh.util.dropBlank
import exh.util.trimOrNull
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.track.interactor.GetTracks
import tachiyomi.domain.track.model.Track
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.CustomLabelTextField
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun EditMangaDialog(
    manga: Manga,
    onDismissRequest: () -> Unit,
    onPositiveClick: (
        title: String?,
        author: String?,
        artist: String?,
        thumbnailUrl: String?,
        description: String?,
        tags: List<String>?,
        status: Long?,
    ) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val getTracks = remember { Injekt.get<GetTracks>() }
    val trackerManager = remember { Injekt.get<TrackerManager>() }

    var showTrackerSelectionDialog by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf(emptyList<TrackerMetadataCandidate>()) }

    var titleText by remember { mutableStateOf(if (manga.title != manga.ogTitle) manga.title else "") }
    var authorText by remember { mutableStateOf(if (manga.author != manga.ogAuthor) manga.author.orEmpty() else "") }
    var artistText by remember { mutableStateOf(if (manga.artist != manga.ogArtist) manga.artist.orEmpty() else "") }
    var thumbnailUrlText by remember { mutableStateOf(if (manga.thumbnailUrl != manga.ogThumbnailUrl) manga.thumbnailUrl.orEmpty() else "") }
    var descriptionText by remember { mutableStateOf(if (manga.description != manga.ogDescription) manga.description.orEmpty() else "") }
    var genreTags by remember { mutableStateOf(manga.genre.orEmpty().dropBlank().toMutableStateList()) }
    var statusSelection by remember {
        mutableStateOf(
            when (manga.status.toInt()) {
                SManga.UNKNOWN -> 0
                SManga.ONGOING -> 1
                SManga.COMPLETED -> 2
                SManga.LICENSED -> 3
                SManga.PUBLISHING_FINISHED, 61 -> 4
                SManga.CANCELLED, 62 -> 5
                SManga.ON_HIATUS, 63 -> 6
                else -> 0
            },
        )
    }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    fun resetTags() {
        genreTags = if (manga.genre.isNullOrEmpty() || manga.isLocal()) {
            mutableListOf<String>().toMutableStateList()
        } else {
            manga.ogGenre.orEmpty().dropBlank().toMutableStateList()
        }
    }

    fun resetInfo() {
        titleText = ""
        authorText = ""
        artistText = ""
        thumbnailUrlText = ""
        descriptionText = ""
        resetTags()
    }

    fun autofillFromTracker(candidate: TrackerMetadataCandidate) {
        val trackerMangaMetadata = candidate.metadata.toFormState()
        trackerMangaMetadata.title.takeIf { it.isNotBlank() }?.let { titleText = it }
        trackerMangaMetadata.author.takeIf { it.isNotBlank() }?.let { authorText = it }
        trackerMangaMetadata.artist.takeIf { it.isNotBlank() }?.let { artistText = it }
        trackerMangaMetadata.thumbnailUrl.takeIf { it.isNotBlank() }?.let { thumbnailUrlText = it }
        trackerMangaMetadata.description.takeIf { it.isNotBlank() }?.let { descriptionText = it }
    }

    suspend fun getTrackers() {
        tracks = loadTrackerMetadataCandidates(
            mangaId = manga.id,
            getTracks = getTracks,
            trackerManager = trackerManager,
            onError = { tracker, e ->
                tracker.logcat(LogPriority.ERROR, e)
                context.toast(
                    context.stringResource(
                        MR.strings.track_error,
                        tracker.name,
                        e.message ?: "",
                    ),
                )
            },
        ).filterNot { it.tracker is EnhancedTracker }

        if (tracks.isEmpty()) {
            context.toast(context.stringResource(SYMR.strings.entry_not_tracked))
            return
        }

        if (tracks.size > 1) {
            showTrackerSelectionDialog = true
            return
        }

        autofillFromTracker(tracks.first())
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        confirmButton = {
            TextButton(
                onClick = {
                    onPositiveClick(
                        titleText.ifEmpty { null },
                        authorText.ifEmpty { null },
                        artistText.ifEmpty { null },
                        thumbnailUrlText.ifEmpty { null },
                        descriptionText.ifEmpty { null },
                        genreTags.takeIf { it.isNotEmpty() }?.toList(),
                        when (statusSelection) {
                            1 -> SManga.ONGOING
                            2 -> SManga.COMPLETED
                            3 -> SManga.LICENSED
                            4 -> SManga.PUBLISHING_FINISHED
                            5 -> SManga.CANCELLED
                            6 -> SManga.ON_HIATUS
                            else -> null
                        }?.toLong(),
                    )
                    onDismissRequest()
                },
            ) {
                Text(stringResource(MR.strings.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(stringResource(MR.strings.action_edit))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .width(360.dp)
                    .padding(horizontal = 8.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(manga)
                                .build(),
                            contentDescription = manga.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StatusDropdown(
                            selectedIndex = statusSelection,
                            onStatusSelected = { statusSelection = it },
                        )
                        CustomLabelTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            labelText = stringResource(SYMR.strings.title_hint, manga.ogTitle),
                            singleLine = true,
                        )
                    }
                }

                EditSection("Author & artist") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CustomLabelTextField(
                            value = authorText,
                            onValueChange = { authorText = it },
                            labelText = stringResource(SYMR.strings.author_hint, manga.ogAuthor ?: ""),
                            singleLine = true,
                        )
                        CustomLabelTextField(
                            value = artistText,
                            onValueChange = { artistText = it },
                            labelText = stringResource(SYMR.strings.artist_hint, manga.ogArtist ?: ""),
                            singleLine = true,
                        )
                    }
                }

                EditSection(stringResource(SYMR.strings.more_info)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CustomLabelTextField(
                            value = thumbnailUrlText,
                            onValueChange = { thumbnailUrlText = it },
                            labelText = stringResource(
                                SYMR.strings.thumbnail_url_hint,
                                manga.ogThumbnailUrl?.let {
                                    it.chop(40) + if (it.length > 46) ".${it.substringAfterLast(".").chop(6)}" else ""
                                } ?: "",
                            ),
                        )
                        CustomLabelTextField(
                            value = descriptionText,
                            onValueChange = { descriptionText = it },
                            labelText = stringResource(
                                SYMR.strings.description_hint,
                                manga.ogDescription?.takeIf { it.isNotBlank() }?.replace("\n", " ")?.chop(20) ?: "",
                            ),
                            minLines = 3,
                            maxLines = 5,
                        )
                    }
                }

                EditSection(stringResource(SYMR.strings.genre)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = ::resetTags,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(stringResource(MR.strings.action_reset))
                        }
                        TagChipGroup(
                            tags = genreTags,
                            onTagRemoved = { tag -> genreTags.remove(tag) },
                            onAddTagClick = { showAddTagDialog = true },
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { scope.launch { getTrackers() } },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(SYMR.strings.fill_from_tracker))
                    }
                    OutlinedButton(
                        onClick = ::resetInfo,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(SYMR.strings.reset_info))
                    }
                }
            }
        },
    )

    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddTagDialog = false
                newTagText = ""
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        newTagText.takeIf { it.isNotBlank() }?.let { text ->
                            genreTags.addAll(text.split(",").map { it.trimOrNull() }.filterNotNull())
                        }
                        newTagText = ""
                        showAddTagDialog = false
                    },
                ) {
                    Text(stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        newTagText = ""
                        showAddTagDialog = false
                    },
                ) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
            title = { Text(stringResource(SYMR.strings.add_tags)) },
            text = {
                CustomLabelTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    labelText = stringResource(SYMR.strings.multi_tags_comma_separated),
                    modifier = Modifier.fillMaxWidth(),
                )
            },
        )
    }

    if (showTrackerSelectionDialog) {
        TrackerSelectDialog(
            tracks = tracks,
            onDismissRequest = { showTrackerSelectionDialog = false },
            onTrackerSelect = { candidate ->
                autofillFromTracker(candidate)
                showTrackerSelectionDialog = false
            },
        )
    }
}

@Composable
private fun EditSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusDropdown(
    selectedIndex: Int,
    onStatusSelected: (Int) -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val statusOptions = remember {
        listOf(
            MR.strings.label_default,
            MR.strings.ongoing,
            MR.strings.completed,
            MR.strings.licensed,
            MR.strings.publishing_finished,
            MR.strings.cancelled,
            MR.strings.on_hiatus,
        ).map { context.stringResource(it) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(MR.strings.status),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
        ) {
            CustomLabelTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                readOnly = true,
                value = statusOptions[selectedIndex],
                onValueChange = {},
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                statusOptions.forEachIndexed { index, text ->
                    DropdownMenuItem(
                        text = { Text(text) },
                        onClick = {
                            onStatusSelected(index)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChipGroup(
    tags: List<String>,
    onTagRemoved: (String) -> Unit,
    onAddTagClick: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onTagRemoved(tag) },
                        modifier = Modifier.size(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(MR.strings.action_remove),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .clickable(onClick = onAddTagClick)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(SYMR.strings.add_tags),
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(SYMR.strings.add_tags),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun TrackerSelectDialog(
    tracks: List<TrackerMetadataCandidate>,
    onDismissRequest: () -> Unit,
    onTrackerSelect: (TrackerMetadataCandidate) -> Unit,
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(MR.strings.action_cancel))
            }
        },
        title = {
            Text(stringResource(SYMR.strings.select_tracker))
        },
        text = {
            FlowRow(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                tracks.forEach { candidate ->
                    TrackLogoIcon(
                        candidate.tracker,
                        onClick = { onTrackerSelect(candidate) },
                    )
                }
            }
        },
    )
}
