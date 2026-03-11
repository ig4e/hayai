package eu.kanade.presentation.manga.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.Button
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MangaInfoButtons(
    showRecommendsButton: Boolean,
    showMergeWithAnotherButton: Boolean,
    onRecommendClicked: () -> Unit,
    onMergeWithAnotherClicked: () -> Unit,
) {
    if (showRecommendsButton || showMergeWithAnotherButton) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    text = stringResource(tachiyomi.i18n.MR.strings.label_extras),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                if (showMergeWithAnotherButton) {
                    Button(
                        onClick = onMergeWithAnotherClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(stringResource(SYMR.strings.merge_with_another_source))
                    }
                }
                if (showRecommendsButton) {
                    Button(
                        onClick = onRecommendClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    ) {
                        Text(stringResource(SYMR.strings.az_recommends))
                    }
                }
            }
        }
    }
}
