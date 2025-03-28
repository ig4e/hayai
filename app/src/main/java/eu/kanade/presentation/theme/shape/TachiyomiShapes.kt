package eu.kanade.presentation.theme.shape

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Custom shapes for Tachiyomi that are more distinctive than standard Material3 shapes
 */
val TachiyomiShapes = Shapes(
    // More rounded corners for small components (like chips, buttons)
    small = RoundedCornerShape(12.dp),

    // Medium rounded corners for cards, dialogs
    medium = RoundedCornerShape(16.dp),

    // Large rounded corners for bottom sheets, large cards
    large = RoundedCornerShape(24.dp),

    // Extra large rounded corners for floating dialogs
    extraLarge = RoundedCornerShape(28.dp),
)
