package exh.ui.browse

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.icerock.moko.resources.StringResource
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.domain.manga.models.Manga
import exh.metadata.MetadataUtil
import exh.metadata.metadata.EHentaiSearchMetadata
import exh.metadata.metadata.RaisedSearchMetadata
import exh.util.SourceTagsUtil
import exh.util.SourceTagsUtil.GenreColor
import exh.util.floor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import yokai.i18n.MR
import yokai.presentation.manga.components.MangaCover
import java.time.Instant
import java.time.ZoneId

@Composable
fun BrowseSourceEHentaiList(
    mangaList: List<Pair<Manga, RaisedSearchMetadata?>>,
    contentPadding: PaddingValues,
    onMangaClick: (Manga) -> Unit,
    onMangaLongClick: (Manga) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
    ) {
        items(mangaList) { (manga, metadata) ->
            BrowseSourceEHentaiListItem(
                manga = manga,
                metadata = metadata,
                onClick = { onMangaClick(manga) },
                onLongClick = { onMangaLongClick(manga) },
            )
        }
    }
}

@Composable
fun BrowseSourceEHentaiListItem(
    manga: Manga,
    metadata: RaisedSearchMetadata?,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = onClick,
) {
    if (metadata !is EHentaiSearchMetadata) return
    val overlayColor = MaterialTheme.colorScheme.background.copy(alpha = 0.66f)

    val languageText by produceState("", metadata) {
        value = withContext(Dispatchers.IO) {
            val locale = SourceTagsUtil.getLocaleSourceUtil(
                metadata.tags
                    .firstOrNull { it.namespace == EHentaiSearchMetadata.EH_LANGUAGE_NAMESPACE }
                    ?.name,
            )
            val pageCount = metadata.length
            if (locale != null && pageCount != null) {
                "${locale.toLanguageTag().uppercase()} - $pageCount pages"
            } else if (pageCount != null) {
                "$pageCount pages"
            } else {
                locale?.toLanguageTag()?.uppercase().orEmpty()
            }
        }
    }
    val datePosted by produceState("", metadata) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                metadata.datePosted?.let {
                    MetadataUtil.EX_DATE_FORMAT.format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
                }
            }.getOrNull().orEmpty()
        }
    }
    val genre by produceState<Pair<GenreColor, StringResource>?>(null, metadata) {
        value = withContext(Dispatchers.IO) {
            when (metadata.genre) {
                "doujinshi" -> GenreColor.DOUJINSHI_COLOR to MR.strings.doujinshi
                "manga" -> GenreColor.MANGA_COLOR to MR.strings.manga
                "artistcg" -> GenreColor.ARTIST_CG_COLOR to MR.strings.artist_cg
                "gamecg" -> GenreColor.GAME_CG_COLOR to MR.strings.game_cg
                "western" -> GenreColor.WESTERN_COLOR to MR.strings.western
                "non-h" -> GenreColor.NON_H_COLOR to MR.strings.non_h
                "imageset" -> GenreColor.IMAGE_SET_COLOR to MR.strings.image_set
                "cosplay" -> GenreColor.COSPLAY_COLOR to MR.strings.cosplay
                "asianporn" -> GenreColor.ASIAN_PORN_COLOR to MR.strings.asian_porn
                "misc" -> GenreColor.MISC_COLOR to MR.strings.misc
                else -> null
            }
        }
    }

    Row(
        modifier = Modifier
            .height(148.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            MangaCover(
                data = manga,
                modifier = Modifier
                    .fillMaxHeight()
                    .drawWithContent {
                        drawContent()
                        if (manga.favorite) {
                            drawRect(overlayColor)
                        }
                    },
            )
            if (manga.favorite) {
                Text(
                    text = stringResource(MR.strings.in_library),
                    modifier = Modifier
                        .padding(4.dp)
                        .align(Alignment.TopStart),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = manga.title,
                    maxLines = 2,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    overflow = TextOverflow.Ellipsis,
                )
                metadata.uploader?.let {
                    Text(
                        text = it,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 8.dp),
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 14.sp,
                    )
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    val color = genre?.first?.color
                    val res = genre?.second
                    Card(
                        colors = if (color != null) {
                            CardDefaults.cardColors(Color(color))
                        } else {
                            CardDefaults.cardColors()
                        },
                    ) {
                        Text(
                            text = if (res != null) {
                                stringResource(res)
                            } else {
                                metadata.genre.orEmpty()
                            },
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        languageText,
                        maxLines = 1,
                        fontSize = 14.sp,
                    )
                    Text(
                        datePosted,
                        maxLines = 1,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}
