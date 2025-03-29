package eu.kanade.presentation.browse.components

import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import tachiyomi.i18n.MR
import tachiyomi.i18n.sy.SYMR
import tachiyomi.presentation.core.components.material.ExtendedFloatingActionButton
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.icons.CustomIcons
import tachiyomi.presentation.core.icons.Filter

@Composable
fun BrowseSourceFloatingActionButton(
    isVisible: Boolean,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        modifier = modifier,
        text = {
            Text(
                text = if (isVisible) {
                    stringResource(MR.strings.action_filter)
                } else {
                    stringResource(SYMR.strings.saved_searches)
                },
            )
        },
        icon = { Icon(CustomIcons.Filter, contentDescription = "") },
        onClick = onFabClick,
    )
}
