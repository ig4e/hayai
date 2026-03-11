package eu.kanade.presentation.reader.appbars

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.reader.setting.ReaderBottomButton
import eu.kanade.tachiyomi.ui.reader.setting.ReaderPreferences
import eu.kanade.tachiyomi.ui.reader.setting.ReaderOrientation
import eu.kanade.tachiyomi.ui.reader.setting.ReadingMode
import kotlinx.collections.immutable.ImmutableSet
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.material3.Text
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun ReaderBottomBar(
    // SY -->
    enabledButtons: ImmutableSet<String>,
    // SY <--
    readingMode: ReadingMode,
    onClickReadingMode: () -> Unit,
    orientation: ReaderOrientation,
    onClickOrientation: () -> Unit,
    cropEnabled: Boolean,
    onClickCropBorder: () -> Unit,
    onClickSettings: () -> Unit,
    // SY -->
    currentReadingMode: ReadingMode,
    dualPageSplitEnabled: Boolean,
    doublePages: Boolean,
    onClickChapterList: () -> Unit,
    onClickWebView: (() -> Unit)?,
    onClickBrowser: (() -> Unit)?,
    onClickShare: (() -> Unit)?,
    onClickPageLayout: () -> Unit,
    onClickShiftPage: () -> Unit,
    // SY <--
    modifier: Modifier = Modifier,
) {
    val expandedControls = Injekt.get<ReaderPreferences>().expandedReaderControls().get()
    Row(
        modifier = modifier.pointerInput(Unit) {},
        horizontalArrangement = Arrangement.spacedBy(
            space = if (expandedControls) 10.dp else 4.dp,
            alignment = Alignment.CenterHorizontally,
        ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // SY -->
        if (ReaderBottomButton.ViewChapters.isIn(enabledButtons)) {
            ReaderBottomBarButton(
                expandedControls = expandedControls,
                onClick = onClickChapterList,
                label = stringResource(ReaderBottomButton.ViewChapters.stringRes),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FormatListNumbered,
                    contentDescription = stringResource(ReaderBottomButton.ViewChapters.stringRes),
                )
            }
        }

        if (ReaderBottomButton.WebView.isIn(enabledButtons) && onClickWebView != null) {
            ReaderBottomBarButton(
                expandedControls = expandedControls,
                onClick = onClickWebView,
                label = stringResource(ReaderBottomButton.WebView.stringRes),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = stringResource(ReaderBottomButton.WebView.stringRes),
                )
            }
        }

        if (ReaderBottomButton.Browser.isIn(enabledButtons) && onClickBrowser != null) {
            ReaderBottomBarButton(
                expandedControls = expandedControls,
                onClick = onClickBrowser,
                label = stringResource(ReaderBottomButton.Browser.stringRes),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Public,
                    contentDescription = stringResource(ReaderBottomButton.Browser.stringRes),
                )
            }
        }

        if (ReaderBottomButton.Share.isIn(enabledButtons) && onClickShare != null) {
            ReaderBottomBarButton(
                expandedControls = expandedControls,
                onClick = onClickShare,
                label = stringResource(ReaderBottomButton.Share.stringRes),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = stringResource(ReaderBottomButton.Share.stringRes),
                )
            }
        }

        if (ReaderBottomButton.ReadingMode.isIn(enabledButtons)) {
            ReaderBottomBarButton(
                expandedControls = expandedControls,
                onClick = onClickReadingMode,
                label = stringResource(ReaderBottomButton.ReadingMode.stringRes),
            ) {
                Icon(
                    painter = painterResource(readingMode.iconRes),
                    contentDescription = stringResource(ReaderBottomButton.ReadingMode.stringRes),
                )
            }
        }

        if (ReaderBottomButton.Rotation.isIn(enabledButtons)) {
            ReaderBottomBarButton(
                expandedControls = expandedControls,
                onClick = onClickOrientation,
                label = stringResource(ReaderBottomButton.Rotation.stringRes),
            ) {
                Icon(
                    imageVector = orientation.icon,
                    contentDescription = stringResource(ReaderBottomButton.Rotation.stringRes),
                )
            }
        }

        val cropBorders = when (currentReadingMode) {
            ReadingMode.WEBTOON -> ReaderBottomButton.CropBordersWebtoon
            ReadingMode.CONTINUOUS_VERTICAL -> ReaderBottomButton.CropBordersContinuesVertical
            else -> ReaderBottomButton.CropBordersPager
        }
        if (cropBorders.isIn(enabledButtons)) {
            ReaderBottomBarButton(
                expandedControls = expandedControls,
                onClick = onClickCropBorder,
                label = stringResource(cropBorders.stringRes),
            ) {
                Icon(
                    painter = painterResource(
                        if (cropEnabled) R.drawable.ic_crop_24dp else R.drawable.ic_crop_off_24dp,
                    ),
                    contentDescription = stringResource(cropBorders.stringRes),
                )
            }
        }

        if (
            !dualPageSplitEnabled &&
            ReaderBottomButton.PageLayout.isIn(enabledButtons) &&
            ReadingMode.isPagerType(currentReadingMode.flagValue)
        ) {
            ReaderBottomBarButton(
                expandedControls = expandedControls,
                onClick = onClickPageLayout,
                label = stringResource(ReaderBottomButton.PageLayout.stringRes),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_book_open_variant_24dp),
                    contentDescription = stringResource(ReaderBottomButton.PageLayout.stringRes),
                )
            }
        }

        if (doublePages) {
            ReaderBottomBarButton(
                expandedControls = expandedControls,
                onClick = onClickShiftPage,
                label = stringResource(SYMR.strings.shift_double_pages),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_page_next_outline_24dp),
                    contentDescription = stringResource(SYMR.strings.shift_double_pages),
                )
            }
        }

        ReaderBottomBarButton(
            expandedControls = expandedControls,
            onClick = onClickSettings,
            label = stringResource(MR.strings.action_settings),
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(MR.strings.action_settings),
            )
        }
        // SY <--
    }
}

@Composable
private fun ReaderBottomBarButton(
    expandedControls: Boolean,
    onClick: () -> Unit,
    label: String,
    content: @Composable () -> Unit,
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = if (expandedControls) 1.dp else 0.dp,
    ) {
        IconButton(
            onClick = onClick,
            modifier = if (expandedControls) {
                Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            } else {
                Modifier
            },
        ) {
            if (expandedControls) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    content()
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                content()
            }
        }
    }
}
