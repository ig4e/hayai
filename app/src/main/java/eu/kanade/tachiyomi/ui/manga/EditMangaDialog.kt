package eu.kanade.tachiyomi.ui.manga

import android.content.Context
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import eu.kanade.presentation.track.components.TrackLogoIcon
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.track.EnhancedTracker
import eu.kanade.tachiyomi.data.track.Tracker
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.lang.chop
import eu.kanade.tachiyomi.util.system.dpToPx
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
import androidx.compose.ui.window.DialogProperties

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

    // Dialog state
    var showTrackerSelectionDialog by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf(emptyList<Pair<Track, Tracker>>()) }

    // Form fields
    var titleText by remember { mutableStateOf(if (manga.title != manga.ogTitle) manga.title else "") }
    var authorText by remember { mutableStateOf(if (manga.author != manga.ogAuthor) manga.author.orEmpty() else "") }
    var artistText by remember { mutableStateOf(if (manga.artist != manga.ogArtist) manga.artist.orEmpty() else "") }
    var thumbnailUrlText by remember { mutableStateOf(if (manga.thumbnailUrl != manga.ogThumbnailUrl) manga.thumbnailUrl.orEmpty() else "") }
    var descriptionText by remember { mutableStateOf(if (manga.description != manga.ogDescription) manga.description.orEmpty() else "") }
    var genreTags by remember {
        mutableStateOf(manga.genre.orEmpty().dropBlank().toMutableStateList())
    }
    var statusSelection by remember { mutableStateOf(
        when (manga.status.toInt()) {
            SManga.UNKNOWN -> 0
            SManga.ONGOING -> 1
            SManga.COMPLETED -> 2
            SManga.LICENSED -> 3
            SManga.PUBLISHING_FINISHED, 61 -> 4
            SManga.CANCELLED, 62 -> 5
            SManga.ON_HIATUS, 63 -> 6
            else -> 0
        }
    ) }

    // For tag dialog
    var showAddTagDialog by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }

    fun resetInfo() {
        titleText = ""
        authorText = ""
        artistText = ""
        thumbnailUrlText = ""
        descriptionText = ""
        if (manga.genre.isNullOrEmpty() || manga.isLocal()) {
            genreTags.clear()
        } else {
            genreTags = manga.ogGenre.orEmpty().toMutableStateList()
        }
    }

    fun resetTags() {
        if (manga.genre.isNullOrEmpty() || manga.isLocal()) {
            genreTags.clear()
        } else {
            genreTags = manga.ogGenre.orEmpty().toMutableStateList()
        }
    }

    suspend fun autofillFromTracker(track: Track, tracker: Tracker) {
        try {
            val trackerMangaMetadata = tracker.getMangaMetadata(track)

            trackerMangaMetadata?.title?.takeIf { it.isNotBlank() }?.let { titleText = it }
            trackerMangaMetadata?.authors?.takeIf { it.isNotBlank() }?.let { authorText = it }
            trackerMangaMetadata?.artists?.takeIf { it.isNotBlank() }?.let { artistText = it }
            trackerMangaMetadata?.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { thumbnailUrlText = it }
            trackerMangaMetadata?.description?.takeIf { it.isNotBlank() }?.let { descriptionText = it }
        } catch (e: Throwable) {
            tracker.logcat(LogPriority.ERROR, e)
            context.toast(
                context.stringResource(
                    MR.strings.track_error,
                    tracker.name,
                    e.message ?: "",
                ),
            )
        }
    }

    suspend fun getTrackers() {
        tracks = getTracks.await(manga.id).mapNotNull { track ->
            track to (trackerManager.get(track.trackerId) ?: return@mapNotNull null)
        }.filterNot { (_, tracker) -> tracker is EnhancedTracker }

        if (tracks.isEmpty()) {
            context.toast(context.stringResource(SYMR.strings.entry_not_tracked))
            return
        }

        if (tracks.size > 1) {
            showTrackerSelectionDialog = true
            return
        }

        autofillFromTracker(tracks.first().first, tracks.first().second)
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
                        if (genreTags.isEmpty()) null else genreTags.toList(),
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
                    .padding(horizontal = 8.dp)
                    .width(360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header section with cover and status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cover image
                    Box(
                        modifier = Modifier
                            .size(width = 80.dp, height = 120.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(manga)
                                .build(),
                            contentDescription = manga.title,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    // Status and title side by side with cover
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Status dropdown
                        StatusDropdown(
                            selectedIndex = statusSelection,
                            onStatusSelected = { statusSelection = it },
                        )

                        // Title input
                        CustomLabelTextField(
                            value = titleText,
                            onValueChange = { titleText = it },
                            labelText = stringResource(SYMR.strings.title_hint, manga.ogTitle),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Author and artist in a card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(MR.strings.author_artist),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Author input
                        CustomLabelTextField(
                            value = authorText,
                            onValueChange = { authorText = it },
                            labelText = stringResource(SYMR.strings.author_hint, manga.ogAuthor ?: ""),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Artist input
                        CustomLabelTextField(
                            value = artistText,
                            onValueChange = { artistText = it },
                            labelText = stringResource(SYMR.strings.artist_hint, manga.ogArtist ?: ""),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // URL and description in a card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(MR.strings.information),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Thumbnail URL input
                        CustomLabelTextField(
                            value = thumbnailUrlText,
                            onValueChange = { thumbnailUrlText = it },
                            labelText = stringResource(
                                SYMR.strings.thumbnail_url_hint,
                                manga.ogThumbnailUrl?.let {
                                    it.chop(40) + if (it.length > 46) "." + it.substringAfterLast(".").chop(6) else ""
                                } ?: ""
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Description input
                        CustomLabelTextField(
                            value = descriptionText,
                            onValueChange = { descriptionText = it },
                            labelText = stringResource(
                                SYMR.strings.description_hint,
                                manga.ogDescription?.takeIf { it.isNotBlank() }?.replace("\n", " ")?.chop(20) ?: ""
                            ),
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                // Tags/Genre in a card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(MR.strings.genres),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )

                            TextButton(
                                onClick = { resetTags() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = stringResource(MR.strings.action_reset),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        ChipGroup(
                            tags = genreTags,
                            onTagRemoved = { tag -> genreTags.remove(tag) },
                            onAddTagClick = { showAddTagDialog = true },
                        )
                    }
                }

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Autofill button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                getTrackers()
                            }
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(SYMR.strings.fill_from_tracker))
                    }

                    // Reset info button
                    OutlinedButton(
                        onClick = { resetInfo() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(SYMR.strings.reset_info))
                    }
                }
            }
        },
    )

    // Add tag dialog
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
                            val newTags = text.split(",").map { it.trimOrNull() }.filterNotNull()
                            genreTags.addAll(newTags)
                        }
                        showAddTagDialog = false
                        newTagText = ""
                    },
                ) {
                    Text(stringResource(MR.strings.action_ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddTagDialog = false
                        newTagText = ""
                    },
                ) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
            title = {
                Text(stringResource(SYMR.strings.add_tags))
            },
            text = {
                Column {
                    CustomLabelTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        labelText = stringResource(SYMR.strings.multi_tags_comma_separated),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
        )
    }

    // Tracker selection dialog
    if (showTrackerSelectionDialog) {
        TrackerSelectDialog(
            tracks = tracks,
            onDismissRequest = { showTrackerSelectionDialog = false },
            onTrackerSelect = { tracker, track ->
                scope.launch {
                    autofillFromTracker(track, tracker)
                }
                showTrackerSelectionDialog = false
            },
        )
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

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(MR.strings.status),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp)
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
private fun ChipGroup(
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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

        // Add tag button
        Box(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(16.dp),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .clickable(onClick = onAddTagClick),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(SYMR.strings.add_tags),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
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
    tracks: List<Pair<Track, Tracker>>,
    onDismissRequest: () -> Unit,
    onTrackerSelect: (Tracker, Track) -> Unit,
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
                tracks.forEach { (track, tracker) ->
                    TrackLogoIcon(
                        tracker,
                        onClick = {
                            onTrackerSelect(tracker, track)
                        },
                    )
                }
            }
        },
    )
}
