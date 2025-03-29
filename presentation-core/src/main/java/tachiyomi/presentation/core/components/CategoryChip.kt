package tachiyomi.presentation.core.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CategoryChip(
    text: String,
    count: Int? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val backgroundColor by animateColorAsState(
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "backgroundColor",
    )

    val textColor by animateColorAsState(
        if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        label = "textColor",
    )

    val countColor by animateColorAsState(
        if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        },
        label = "countColor",
    )

    val countTextColor by animateColorAsState(
        if (selected) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.surface
        },
        label = "countTextColor",
    )

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = 20.dp,
                end = if (count != null) 10.dp else 20.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (count != null) {
                Box(
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (selected) MaterialTheme.colorScheme.onPrimary else countColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (count > 999) "999+" else count.toString(),
                        color = if (selected) MaterialTheme.colorScheme.primary else countTextColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
