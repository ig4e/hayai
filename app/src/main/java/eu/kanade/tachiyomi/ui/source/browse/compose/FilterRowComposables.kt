package eu.kanade.tachiyomi.ui.source.browse.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.North
import androidx.compose.material.icons.outlined.South
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.source.model.Filter
import yokai.i18n.MR

/**
 * Per-type filter row composables. Each row mutates the supplied [Filter] instance IN PLACE
 * through [FilterMutations] — the legacy contract that
 * `BrowseSourceController.showFilters()` snapshots and compares. Do not work on copies here.
 *
 * Shared scaffolding (row layout, value chip, header / separator / empty state) lives in
 * [FilterSheetCommon] so this file stays focused on per-type rendering.
 */

// region CheckBox — Switch on the right, matches Hayai's SwitchPreferenceWidget.

@Composable
internal fun FilterCheckBoxRow(filter: Filter.CheckBox) {
    var checked by remember(filter) { mutableStateOf(filter.state) }
    FilterPreferenceRow(
        title = filter.name,
        onClick = {
            FilterMutations.toggleCheckbox(filter)
            checked = filter.state
        },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    )
}

// endregion

// region TriState — connected 3-segment selector (Off / + / −). All three states always visible.

@Composable
internal fun FilterTriStateRow(filter: Filter.TriState) {
    var state by remember(filter) { mutableIntStateOf(filter.state) }
    FilterPreferenceRow(
        title = filter.name,
        onClick = null,
        trailing = {
            TriStateSegments(
                state = state,
                onChange = { target ->
                    FilterMutations.setTriStateExact(filter, target)
                    state = filter.state
                },
            )
        },
    )
}

@Composable
private fun TriStateSegments(state: Int, onChange: (Int) -> Unit) {
    // Connected button-group shape pattern: outer corners fully rounded, inner edges square so
    // the three segments read as one piece. Colour alone communicates state (no outer border).
    Row(verticalAlignment = Alignment.CenterVertically) {
        TriStateSegment(
            selected = state == Filter.TriState.STATE_IGNORE,
            label = "•",
            activeContainer = MaterialTheme.colorScheme.secondaryContainer,
            activeContent = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = SegmentShapeStart,
            contentDescription = stringResource(MR.strings.ignore),
            onClick = { onChange(Filter.TriState.STATE_IGNORE) },
        )
        TriStateSegment(
            selected = state == Filter.TriState.STATE_INCLUDE,
            icon = Icons.Outlined.Add,
            activeContainer = MaterialTheme.colorScheme.primary,
            activeContent = MaterialTheme.colorScheme.onPrimary,
            shape = SegmentShapeMiddle,
            contentDescription = stringResource(MR.strings.include),
            onClick = { onChange(Filter.TriState.STATE_INCLUDE) },
        )
        TriStateSegment(
            selected = state == Filter.TriState.STATE_EXCLUDE,
            icon = Icons.Outlined.Block,
            activeContainer = MaterialTheme.colorScheme.error,
            activeContent = MaterialTheme.colorScheme.onError,
            shape = SegmentShapeEnd,
            contentDescription = stringResource(MR.strings.exclude),
            onClick = { onChange(Filter.TriState.STATE_EXCLUDE) },
        )
    }
}

@Composable
private fun TriStateSegment(
    selected: Boolean,
    activeContainer: Color,
    activeContent: Color,
    shape: Shape,
    contentDescription: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    label: String? = null,
) {
    val container by animateColorAsState(
        targetValue = if (selected) activeContainer else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "tri-seg-container",
    )
    val content by animateColorAsState(
        targetValue = if (selected) activeContent else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "tri-seg-content",
    )
    Surface(
        onClick = onClick,
        shape = shape,
        color = container,
        contentColor = content,
        modifier = Modifier.size(width = 38.dp, height = 30.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                icon != null -> Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(16.dp),
                )
                label != null -> Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private val SegmentShapeStart = RoundedCornerShape(
    topStartPercent = 50,
    bottomStartPercent = 50,
    topEndPercent = 0,
    bottomEndPercent = 0,
)
private val SegmentShapeMiddle = RoundedCornerShape(0)
private val SegmentShapeEnd = RoundedCornerShape(
    topStartPercent = 0,
    bottomStartPercent = 0,
    topEndPercent = 50,
    bottomEndPercent = 50,
)

// endregion

// region Select — value chip + chevron on the right; anchored DropdownMenu.

@Composable
internal fun FilterSelectRow(filter: Filter.Select<*>) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember(filter) { mutableIntStateOf(filter.state) }
    val displayValue = filter.values.getOrNull(selectedIndex)?.toString().orEmpty()
    FilterPreferenceRow(
        title = filter.name,
        onClick = { expanded = true },
        trailing = {
            Box {
                ValueChip(
                    value = displayValue,
                    trailing = Icons.Outlined.ExpandMore,
                    active = selectedIndex != 0,
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = MenuShape,
                ) {
                    filter.values.forEachIndexed { index, value ->
                        DropdownMenuItem(
                            text = { DropdownItemText(value.toString(), selected = index == selectedIndex) },
                            onClick = {
                                FilterMutations.setSelect(filter, index)
                                selectedIndex = filter.state
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
}

// endregion

// region Sort — same row as Select but the trailing arrow shows direction.

@Composable
internal fun FilterSortRow(filter: Filter.Sort) {
    var expanded by remember { mutableStateOf(false) }
    var state by remember(filter) { mutableStateOf(filter.state) }
    val selectedIndex = state?.index
    val ascending = state?.ascending == true
    val displayValue = selectedIndex?.let { filter.values.getOrNull(it) }.orEmpty()
    FilterPreferenceRow(
        title = filter.name,
        onClick = { expanded = true },
        trailing = {
            Box {
                ValueChip(
                    value = displayValue,
                    trailing = when {
                        selectedIndex == null -> Icons.Outlined.ExpandMore
                        ascending -> Icons.Outlined.North
                        else -> Icons.Outlined.South
                    },
                    active = selectedIndex != null,
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    shape = MenuShape,
                ) {
                    filter.values.forEachIndexed { index, name ->
                        val isSelected = selectedIndex == index
                        DropdownMenuItem(
                            text = { DropdownItemText(name, selected = isSelected) },
                            trailingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = if (ascending) Icons.Outlined.North else Icons.Outlined.South,
                                        contentDescription = stringResource(
                                            if (ascending) MR.strings.sort_ascending else MR.strings.sort_descending,
                                        ),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            } else {
                                null
                            },
                            onClick = {
                                FilterMutations.toggleSort(filter, index)
                                state = filter.state
                                expanded = false
                            },
                        )
                    }
                }
            }
        },
    )
}

// endregion

// region Shared dropdown helpers — keep Select / Sort menus visually identical.

private val MenuShape = RoundedCornerShape(16.dp)

@Composable
private fun DropdownItemText(text: String, selected: Boolean) {
    Text(
        text = text,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        fontWeight = if (selected) FontWeight.SemiBold else null,
    )
}

// endregion

// region Group — drill-down row.

@Composable
internal fun FilterGroupRow(
    filter: Filter.Group<*>,
    onDrill: (Filter.Group<*>) -> Unit,
) {
    val activeCount = groupActiveCount(filter)
    FilterPreferenceRow(
        title = filter.name,
        onClick = { onDrill(filter) },
        trailing = {
            ValueChip(
                value = if (activeCount > 0) "$activeCount" else null,
                trailing = Icons.Outlined.ChevronRight,
                active = activeCount > 0,
            )
        },
    )
}

// endregion

// region Section header + Separator + Text (full-width).

@Composable
internal fun FilterHeaderRow(filter: Filter.Header) {
    // Mirrors yokai.presentation.component.preference.widget.PreferenceGroupHeader so source-
    // provided notices (e-hentai's "WILL IGNORE OTHER PARAMETERS!") read as a subdued group
    // label introducing the next item.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 10.dp,
                bottom = 4.dp,
                start = FilterRowHorizontalPadding,
                end = FilterRowHorizontalPadding,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = filter.name,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
        )
    }
}

@Composable
internal fun FilterSeparatorRow(@Suppress("UNUSED_PARAMETER") filter: Filter.Separator) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 4.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
internal fun FilterTextRow(filter: Filter.Text) {
    var value by remember(filter) { mutableStateOf(filter.state) }
    // Every keystroke commits to the filter state — matches the legacy contract where the
    // source consumes the latest text without an explicit "submit" gesture.
    LaunchedEffect(value) {
        if (filter.state != value) FilterMutations.setText(filter, value)
    }
    OutlinedTextField(
        value = value,
        onValueChange = { value = it },
        placeholder = {
            Text(
                text = filter.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.bodyMedium,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = FilterRowHorizontalPadding, vertical = 4.dp),
    )
}

// endregion
