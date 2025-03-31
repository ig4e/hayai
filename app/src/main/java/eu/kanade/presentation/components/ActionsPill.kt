package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip

@Composable
fun ActionsPill(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
    ) {
        content()
    }
}

@Composable
fun AroundPill(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
    ) {
        content()
    }
}
