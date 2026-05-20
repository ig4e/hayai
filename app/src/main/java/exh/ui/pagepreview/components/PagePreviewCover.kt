package exh.ui.pagepreview.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.AsyncImage
import eu.kanade.tachiyomi.R
import yokai.util.rememberResourceBitmapPainter

/**
 * Shared thumbnail used by both the inline preview strip (`PagePreviewInlineSection`)
 * and the full-screen page grid (`PagePreviewContent`). Mirrors `MangaCover`'s
 * placeholder/error treatment but routes through the source's `ImageLoader` so per-source
 * cookies/auth headers flow through for previews on gated sources (E-Hentai etc.).
 */
@Composable
internal fun PagePreviewCover(
    data: Any?,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    contentScale: ContentScale = ContentScale.FillWidth,
    contentDescription: String = "",
) {
    AsyncImage(
        model = data,
        imageLoader = imageLoader,
        placeholder = ColorPainter(Color(0x1F888888)),
        error = rememberResourceBitmapPainter(id = R.drawable.cover_error),
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier.clip(shape),
    )
}
