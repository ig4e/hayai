package tachiyomi.presentation.core.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("UnusedReceiverParameter")
val CustomIcons.Filter: ImageVector
    get() {
        if (_filter != null) {
            return _filter!!
        }
        _filter = Builder(
            name = "Filter",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color.Transparent),
                stroke = SolidColor(Color(0xFF000000)),
                strokeLineWidth = 1.5f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(12f, 3f)
                curveToRelative(2.755f, 0f, 5.455f, 0.232f, 8.083f, 0.678f)
                curveToRelative(0.533f, 0.09f, 0.917f, 0.556f, 0.917f, 1.096f)
                verticalLineToRelative(1.044f)
                arcTo(2.25f, 2.25f, 0f, false, false, 20.341f, 7.409f)
                lineToRelative(-5.432f, 5.432f)
                arcTo(2.25f, 2.25f, 0f, false, false, 14.25f, 14.432f)
                verticalLineToRelative(2.927f)
                arcTo(2.25f, 2.25f, 0f, false, false, 13.006f, 19.372f)
                lineTo(9.75f, 21f)
                verticalLineToRelative(-6.568f)
                arcTo(2.25f, 2.25f, 0f, false, false, 9.091f, 12.841f)
                lineTo(3.659f, 7.409f)
                arcTo(2.25f, 2.25f, 0f, false, false, 3f, 5.818f)
                verticalLineTo(4.774f)
                curveToRelative(0f, -0.54f, 0.384f, -1.006f, 0.917f, -1.096f)
                arcTo(48.32f, 48.32f, 0f, false, false, 12f, 3f)
                close()
            }
        }
            .build()
        return _filter!!
    }

@Suppress("ObjectPropertyName")
private var _filter: ImageVector? = null
