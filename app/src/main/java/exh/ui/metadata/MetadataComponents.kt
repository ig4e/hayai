package exh.ui.metadata

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import kotlin.math.roundToInt
import yokai.i18n.MR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MetadataRow(
    icon: ImageVector? = null,
    label: String,
    text: String,
    onClick: (() -> Unit)? = null,
) {
    val clipboardManager = LocalClipboardManager.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(text))
                },
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun GenreChip(
    genre: String,
    color: Color? = null,
) {
    Text(
        text = genre,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color ?: MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = if (color != null) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

@Composable
fun RatingRow(
    rating: Float,
    ratingText: String,
) {
    val ratingColor = getRatingColor(rating)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Simple star display using text
        val fullStars = rating.toInt()
        val hasHalf = (rating - fullStars) >= 0.5f
        val starText = buildString {
            repeat(fullStars) { append("\u2605") }
            if (hasHalf) append("\u00BD")
            repeat(5 - fullStars - if (hasHalf) 1 else 0) { append("\u2606") }
        }
        Text(
            text = starText,
            style = MaterialTheme.typography.bodyMedium,
            color = ratingColor,
        )
        Text(
            text = ratingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Returns a color corresponding to the rating value (0-10 scale mapped to 5-star display).
 * The rating parameter is on a 0-5 star scale; multiply by 2 for the 0-10 mapping.
 */
@Composable
private fun getRatingColor(rating: Float): Color {
    val isDark = isSystemInDarkTheme()
    val ratingOut10 = (rating * 2).coerceIn(0f, 10f)
    return when {
        ratingOut10 < 2f -> if (isDark) Color(0xFFEF5350) else Color(0xFFD32F2F) // Red (bad)
        ratingOut10 < 4f -> if (isDark) Color(0xFFFF7043) else Color(0xFFE64A19) // Orange (below average)
        ratingOut10 < 6f -> if (isDark) Color(0xFFFFCA28) else Color(0xFFF9A825) // Yellow (average)
        ratingOut10 < 8f -> if (isDark) Color(0xFF9CCC65) else Color(0xFF7CB342) // Light green (good)
        else -> if (isDark) Color(0xFF66BB6A) else Color(0xFF388E3C) // Green (great)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CopyableText(
    text: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = modifier.combinedClickable(
            onClick = {},
            onLongClick = {
                clipboardManager.setText(AnnotatedString(text))
            },
        ),
    ) {
        content()
    }
}

@Composable
fun getRatingString(rating: Float?): String = when (rating?.roundToInt()) {
    0 -> stringResource(MR.strings.rating_abysmal)
    1 -> stringResource(MR.strings.rating_terrible)
    2 -> stringResource(MR.strings.rating_bad)
    3 -> stringResource(MR.strings.rating_poor)
    4 -> stringResource(MR.strings.rating_below_average)
    5 -> stringResource(MR.strings.rating_average)
    6 -> stringResource(MR.strings.rating_above_average)
    7 -> stringResource(MR.strings.rating_good)
    8 -> stringResource(MR.strings.rating_very_good)
    9 -> stringResource(MR.strings.rating_great)
    10 -> stringResource(MR.strings.rating_masterpiece)
    else -> stringResource(MR.strings.rating_none)
}

object MetadataUIUtil {

    /**
     * Returns the genre color and display name for the given genre string.
     * Use [getGenreColor] for Compose-aware dark mode support.
     */
    fun getGenreAndColour(genre: String, isDark: Boolean = false): Pair<Color, String>? = when (genre) {
        "doujinshi", "Doujinshi" -> genreColor(0xFFF44336, 0xFFEF5350, isDark) to "Doujinshi"
        "manga", "Japanese Manga", "Manga" -> genreColor(0xFFFF9800, 0xFFFFA726, isDark) to "Manga"
        "artistcg", "artist CG", "artist-cg", "Artist CG" -> genreColor(0xFFFBC02D, 0xFFFFD54F, isDark) to "Artist CG"
        "gamecg", "game CG", "game-cg", "Game CG" -> genreColor(0xFF4CAF50, 0xFF66BB6A, isDark) to "Game CG"
        "western" -> genreColor(0xFF8BC34A, 0xFF9CCC65, isDark) to "Western"
        "non-h", "non-H" -> genreColor(0xFF2196F3, 0xFF42A5F5, isDark) to "Non-H"
        "imageset", "image Set" -> genreColor(0xFF3F51B5, 0xFF5C6BC0, isDark) to "Image Set"
        "cosplay" -> genreColor(0xFF9C27B0, 0xFFAB47BC, isDark) to "Cosplay"
        "asianporn", "asian Porn" -> genreColor(0xFF9575CD, 0xFFB39DDB, isDark) to "Asian Porn"
        "misc" -> genreColor(0xFF795548, 0xFF8D6E63, isDark) to "Misc"
        "Korean Manhwa" -> genreColor(0xFFFBC02D, 0xFFFFD54F, isDark) to "Manhwa"
        "Chinese Manhua" -> genreColor(0xFF4CAF50, 0xFF66BB6A, isDark) to "Manhua"
        "Comic" -> genreColor(0xFF8BC34A, 0xFF9CCC65, isDark) to "Comic"
        "artbook" -> genreColor(0xFF3F51B5, 0xFF5C6BC0, isDark) to "Artbook"
        "webtoon" -> genreColor(0xFF2196F3, 0xFF42A5F5, isDark) to "Webtoon"
        "Video" -> genreColor(0xFF8BC34A, 0xFF9CCC65, isDark) to "Video"
        else -> null
    }

    private fun genreColor(light: Long, dark: Long, isDark: Boolean): Color =
        if (isDark) Color(dark) else Color(light)
}

/**
 * Composable function that returns the genre color with dark mode awareness.
 */
@Composable
fun getGenreColor(genre: String): Color? {
    val isDark = isSystemInDarkTheme()
    return MetadataUIUtil.getGenreAndColour(genre, isDark)?.first
}
