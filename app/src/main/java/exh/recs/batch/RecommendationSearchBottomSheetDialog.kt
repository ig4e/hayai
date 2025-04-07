package exh.recs.batch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import eu.kanade.presentation.components.UnifiedBottomSheet
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.domain.UnsortedPreferences
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.LabeledCheckbox
import tachiyomi.presentation.core.components.material.padding
import uy.kohesive.injekt.injectLazy

@Composable
fun RecommendationSearchBottomSheetDialog(
    onDismissRequest: () -> Unit,
    onSearchRequest: () -> Unit,
) {
    val context = LocalContext.current
    val preferences: UnsortedPreferences by injectLazy()

    var includeSources by remember { mutableStateOf(SearchFlags.hasIncludeSources(preferences.recommendationSearchFlags().get())) }
    var includeTrackers by remember { mutableStateOf(SearchFlags.hasIncludeTrackers(preferences.recommendationSearchFlags().get())) }
    var hideLibraryEntries by remember { mutableStateOf(SearchFlags.hasHideLibraryResults(preferences.recommendationSearchFlags().get())) }

    val canSearch = includeSources || includeTrackers

    fun updateFlags() {
        var flags = 0
        if (includeSources) flags = flags or SearchFlags.INCLUDE_SOURCES
        if (includeTrackers) flags = flags or SearchFlags.INCLUDE_TRACKERS
        if (hideLibraryEntries) flags = flags or SearchFlags.HIDE_LIBRARY_RESULTS
        preferences.recommendationSearchFlags().set(flags)
    }

    UnifiedBottomSheet(
        onDismissRequest = onDismissRequest,
        // No explicit title needed, actions provide context
        content = @Composable {
            Column {
                LabeledCheckbox(
                    label = context.stringResource(SYMR.strings.include_sources),
                    checked = includeSources,
                    onCheckedChange = {
                        includeSources = it
                        updateFlags()
                    },
                )
                LabeledCheckbox(
                    label = context.stringResource(SYMR.strings.include_tracking_services),
                    checked = includeTrackers,
                    onCheckedChange = {
                        includeTrackers = it
                        updateFlags()
                    },
                )
                Divider()
                LabeledCheckbox(
                    label = context.stringResource(SYMR.strings.pref_rec_hide_in_library),
                    checked = hideLibraryEntries,
                    onCheckedChange = {
                        hideLibraryEntries = it
                        updateFlags()
                    },
                )
            }
        },
        actions = @Composable {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(context.stringResource(MR.strings.action_cancel))
                }
                Button(
                    onClick = {
                        onDismissRequest()
                        onSearchRequest()
                    },
                    enabled = canSearch,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(context.stringResource(MR.strings.action_search))
                }
            }
        },
    )
}
