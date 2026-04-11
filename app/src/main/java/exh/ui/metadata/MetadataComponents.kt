package exh.ui.metadata

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

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
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = ratingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

object MetadataUIUtil {
    fun getRatingString(rating: Float?): String = when (rating?.roundToInt()) {
        0 -> "Abysmal"
        1 -> "Terrible"
        2 -> "Bad"
        3 -> "Poor"
        4 -> "Below Average"
        5 -> "Average"
        6 -> "Above Average"
        7 -> "Good"
        8 -> "Very Good"
        9 -> "Great"
        10 -> "Masterpiece"
        else -> "No rating"
    }

    fun getGenreAndColour(genre: String): Pair<Color, String>? = when (genre) {
        "doujinshi", "Doujinshi" -> Color(0xFFF44336) to "Doujinshi"
        "manga", "Japanese Manga", "Manga" -> Color(0xFFFF9800) to "Manga"
        "artistcg", "artist CG", "artist-cg", "Artist CG" -> Color(0xFFFBC02D) to "Artist CG"
        "gamecg", "game CG", "game-cg", "Game CG" -> Color(0xFF4CAF50) to "Game CG"
        "western" -> Color(0xFF8BC34A) to "Western"
        "non-h", "non-H" -> Color(0xFF2196F3) to "Non-H"
        "imageset", "image Set" -> Color(0xFF3F51B5) to "Image Set"
        "cosplay" -> Color(0xFF9C27B0) to "Cosplay"
        "asianporn", "asian Porn" -> Color(0xFF9575CD) to "Asian Porn"
        "misc" -> Color(0xFF795548) to "Misc"
        "Korean Manhwa" -> Color(0xFFFBC02D) to "Manhwa"
        "Chinese Manhua" -> Color(0xFF4CAF50) to "Manhua"
        "Comic" -> Color(0xFF8BC34A) to "Comic"
        "artbook" -> Color(0xFF3F51B5) to "Artbook"
        "webtoon" -> Color(0xFF2196F3) to "Webtoon"
        "Video" -> Color(0xFF8BC34A) to "Video"
        else -> null
    }
}
