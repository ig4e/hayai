package eu.kanade.tachiyomi.ui.source.browse.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.source.model.Filter
import yokai.i18n.MR

/**
 * Per-type composables for the redesigned source filter sheet, modelled on the rest of the app's
 * `yokai.presentation.component.preference.widget.*` row pattern: title on the left, control on
 * the right, the whole row tappable, no card background. Section grouping is handled by
 * [SourceFilterSheetContent] with [FilterHeaderRow] acting as the same kind of subdued, secondary-
 * coloured group label `PreferenceGroupHeader` uses.
 *
 * Every composable still mutates the supplied [Filter] instance IN PLACE through [FilterMutations]
 * — the legacy contract that
 * [eu.kanade.tachiyomi.ui.source.browse.BrowseSourceController.showFilters] snapshots and compares.
 * Do not work on copies here.
 */

// region Style tokens — mirror PreferenceCommon so the sheet reads as part of the same family.

internal val FilterRowHorizontalPadding = 16.dp
internal val FilterRowMinHeight = 40.dp

// endregion

// region Row scaffold — title on the left, widget on the right.

@Composable
private fun FilterPreferenceRow(
    title: String,
    onClick: (() -> Unit)?,
    trailing: @Composable (() -> Unit),
) {
    val baseMod = Modifier
        .fillMaxWidth()
        .heightIn(min = FilterRowMinHeight)
    val rowMod = if (onClick != null) baseMod.clickable(onClick = onClick) else baseMod
    Row(
        modifier = rowMod.padding(horizontal = FilterRowHorizontalPadding, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box(modifier = Modifier.padding(start = 12.dp)) { trailing() }
    }
}

// endregion

// region CheckBox — Switch on the right, matches SwitchPreferenceWidget.

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
    // Connected button-group shape pattern: outer corners fully rounded, inner edges square
    // so the three segments read as one piece. No outer border — the segment fill (or the
    // selected primary/error tint) communicates state.
    Row(verticalAlignment = Alignment.CenterVertically) {
        TriStateSegment(
            selected = state == Filter.TriState.STATE_IGNORE,
            label = "•",
            activeContainer = MaterialTheme.colorScheme.secondaryContainer,
            activeContent = MaterialTheme.colorScheme.onSecondaryContainer,
            shape = RoundedCornerShape(
                topStartPercent = 50,
                bottomStartPercent = 50,
                topEndPercent = 0,
                bottomEndPercent = 0,
            ),
            contentDescription = stringResource(MR.strings.ignore),
            onClick = { onChange(Filter.TriState.STATE_IGNORE) },
        )
        TriStateSegment(
            selected = state == Filter.TriState.STATE_INCLUDE,
            icon = Icons.Outlined.Add,
            activeContainer = MaterialTheme.colorScheme.primary,
            activeContent = MaterialTheme.colorScheme.onPrimary,
            shape = RoundedCornerShape(0),
            contentDescription = stringResource(MR.strings.include),
            onClick = { onChange(Filter.TriState.STATE_INCLUDE) },
        )
        TriStateSegment(
            selected = state == Filter.TriState.STATE_EXCLUDE,
            icon = Icons.Outlined.Block,
            activeContainer = MaterialTheme.colorScheme.error,
            activeContent = MaterialTheme.colorScheme.onError,
            shape = RoundedCornerShape(
                topStartPercent = 0,
                bottomStartPercent = 0,
                topEndPercent = 50,
                bottomEndPercent = 50,
            ),
            contentDescription = stringResource(MR.strings.exclude),
            onClick = { onChange(Filter.TriState.STATE_EXCLUDE) },
        )
    }
}

@Composable
private fun TriStateSegment(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String? = null,
    activeContainer: Color,
    activeContent: Color,
    shape: androidx.compose.ui.graphics.Shape,
    contentDescription: String,
    onClick: () -> Unit,
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
                    shape = RoundedCornerShape(16.dp),
                ) {
                    filter.values.forEachIndexed { index, value ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = value.toString(),
                                    color = if (index == selectedIndex) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (index == selectedIndex) FontWeight.SemiBold else null,
                                )
                            },
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

// region Sort — same row as Select but trailing arrow shows direction; menu items toggle direction.

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
                    shape = RoundedCornerShape(16.dp),
                ) {
                    filter.values.forEachIndexed { index, name ->
                        val isSelected = selectedIndex == index
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = name,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (isSelected) FontWeight.SemiBold else null,
                                )
                            },
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

// region Shared value chip — small surface used by Select/Sort/Group on the right.

@Composable
private fun ValueChip(
    value: String?,
    trailing: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
) {
    // Borderless chip — colour alone communicates active vs idle. Active uses the secondary
    // accent which is what the legacy filter sheet's text buttons used (?attr/colorSecondary).
    val contentColor = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.heightIn(min = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (!value.isNullOrEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
        }
        Icon(
            imageVector = trailing,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

// endregion

// region Section header + Text input + Separator (full-width).

@Composable
internal fun FilterHeaderRow(filter: Filter.Header) {
    // Mirrors yokai.presentation.component.preference.widget.PreferenceGroupHeader exactly so
    // the section labels (and any source-supplied warning notes like e-hentai's "WILL IGNORE
    // OTHER PARAMETERS!") read as the same kind of subdued, secondary-coloured group label
    // users already see in Settings — telling them what follows belongs to one section.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp, start = FilterRowHorizontalPadding, end = FilterRowHorizontalPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = filter.name,
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
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

internal fun groupActiveCount(group: Filter.Group<*>): Int = group.state.count { child ->
    when (child) {
        is Filter.CheckBox -> child.state
        is Filter.TriState -> child.state != Filter.TriState.STATE_IGNORE
        is Filter.Text -> child.state.isNotEmpty()
        is Filter.Select<*> -> child.state != 0
        else -> false
    }
}
