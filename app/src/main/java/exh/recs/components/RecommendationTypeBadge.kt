package exh.recs.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import exh.recs.sources.RecommendationContentType
import yokai.i18n.MR

/**
 * Small "Manga" / "Novel" pill displayed on a recommendation card so the user can tell at
 * a glance which content type a result is — useful for novel detail pages where AniList /
 * MyAnimeList previously surfaced manga-typed recs and vice versa. Returns null layout
 * for [RecommendationContentType.UNKNOWN] so callers can render it unconditionally.
 */
@Composable
fun RecommendationTypeBadge(
    contentType: RecommendationContentType,
    modifier: Modifier = Modifier,
) {
    val labelRes = when (contentType) {
        RecommendationContentType.MANGA -> MR.strings.manga
        RecommendationContentType.NOVEL -> MR.strings.novel
        RecommendationContentType.UNKNOWN -> return
    }
    Text(
        text = stringResource(labelRes),
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
    )
}
