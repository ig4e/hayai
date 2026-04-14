package exh.ui.metadata

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.metadata.metadata.base.RaisedTag
import exh.source.EH_SOURCE_ID
import exh.source.EXH_SOURCE_ID
import exh.util.SourceTagsUtil

private const val COLLAPSE_THRESHOLD = 8

@Immutable
data class DisplayTag(
    val namespace: String?,
    val text: String,
    val search: String,
    val border: Int?,
)

@Immutable
@JvmInline
value class SearchMetadataChips(
    val tags: Map<String, List<DisplayTag>>,
) {
    companion object {
        operator fun invoke(
            meta: RaisedSearchMetadata?,
            sourceId: Long,
            tags: List<String>?,
        ): SearchMetadataChips? {
            return if (meta != null) {
                SearchMetadataChips(
                    meta.tags
                        .filterNot { it.type == RaisedSearchMetadata.TAG_TYPE_VIRTUAL }
                        .map {
                            DisplayTag(
                                namespace = it.namespace,
                                text = it.name,
                                search = if (!it.namespace.isNullOrEmpty()) {
                                    SourceTagsUtil.getWrappedTag(
                                        sourceId,
                                        namespace = it.namespace,
                                        tag = it.name,
                                    )
                                } else {
                                    SourceTagsUtil.getWrappedTag(sourceId, fullTag = it.name)
                                } ?: it.name,
                                border = if (sourceId == EXH_SOURCE_ID || sourceId == EH_SOURCE_ID) {
                                    when (it.type) {
                                        EHentaiSearchMetadata.TAG_TYPE_NORMAL -> 2
                                        EHentaiSearchMetadata.TAG_TYPE_LIGHT -> 1
                                        else -> null
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                        .groupBy { it.namespace.orEmpty() },
                )
            } else if (tags != null && tags.all { it.contains(':') }) {
                SearchMetadataChips(
                    tags
                        .map { tag ->
                            val index = tag.indexOf(':')
                            val ns = tag.substring(0, index).trim()
                            val name = tag.substring(index + 1).trim()
                            DisplayTag(
                                ns,
                                name,
                                "$ns:$name",
                                null,
                            )
                        }
                        .groupBy { it.namespace.orEmpty() },
                )
            } else {
                null
            }
        }
    }
}

@Composable
fun NamespaceTags(
    tags: SearchMetadataChips,
    onClick: (item: String) -> Unit,
) {
    val totalCount = tags.tags.values.sumOf { it.size }
    var expanded by rememberSaveable { mutableStateOf(totalCount <= COLLAPSE_THRESHOLD) }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (expanded) {
            // Expanded: namespace-grouped FlowRow layout
            tags.tags.forEach { (namespace, displayTags) ->
                Row(Modifier.padding(start = 16.dp)) {
                    if (namespace.isNotEmpty()) {
                        TagsChip(
                            modifier = Modifier.padding(top = 4.dp),
                            text = namespace,
                            onClick = null,
                        )
                    }
                    FlowRow(
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        displayTags.forEach { (_, text, search, border) ->
                            val borderDp = border?.dp
                            TagsChip(
                                modifier = Modifier.padding(vertical = 4.dp),
                                text = text,
                                onClick = { onClick(search) },
                                border = borderDp?.let {
                                    SuggestionChipDefaults.suggestionChipBorder(
                                        enabled = true,
                                        borderWidth = it,
                                    )
                                } ?: SuggestionChipDefaults.suggestionChipBorder(enabled = true),
                            )
                        }
                    }
                }
            }
        } else {
            // Collapsed: single horizontal scrolling strip, all namespaces flattened
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                tags.tags.forEach { (namespace, displayTags) ->
                    if (namespace.isNotEmpty()) {
                        item(key = "ns_$namespace") {
                            TagsChip(
                                modifier = Modifier.padding(vertical = 4.dp),
                                text = namespace,
                                onClick = null,
                            )
                        }
                    }
                    items(displayTags, key = { "${namespace}_${it.text}" }) { (_, text, search, border) ->
                        val borderDp = border?.dp
                        TagsChip(
                            modifier = Modifier.padding(vertical = 4.dp),
                            text = text,
                            onClick = { onClick(search) },
                            border = borderDp?.let {
                                SuggestionChipDefaults.suggestionChipBorder(
                                    enabled = true,
                                    borderWidth = it,
                                )
                            } ?: SuggestionChipDefaults.suggestionChipBorder(enabled = true),
                        )
                    }
                }
            }
        }

        if (totalCount > COLLAPSE_THRESHOLD) {
            TextButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.padding(start = 8.dp),
            ) {
                Text(
                    text = if (expanded) "Show less" else "Show all ($totalCount)",
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

@Composable
fun TagsChip(
    text: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    border: BorderStroke? = SuggestionChipDefaults.suggestionChipBorder(enabled = true),
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        if (onClick != null) {
            SuggestionChip(
                modifier = modifier,
                onClick = onClick,
                label = {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                border = border,
            )
        } else {
            SuggestionChip(
                modifier = modifier,
                onClick = {},
                enabled = false,
                label = {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                border = border,
            )
        }
    }
}
