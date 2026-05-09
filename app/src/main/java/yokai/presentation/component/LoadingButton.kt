package yokai.presentation.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import yokai.presentation.theme.isReducedMotion
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// REF: https://gist.github.com/mmolosay/584ce5c47567cb66228b76ef98c3c4e4

private const val SpringStiffness = Spring.StiffnessMediumLow

@Composable
fun LoadingButton(
    text: () -> String,
    loading: () -> Boolean,
    onClick: () -> Unit,
) {
    val reducedMotion = isReducedMotion
    val transition = updateTransition(
        targetState = loading(),
        label = "master transition",
    )
    val horizontalContentPadding by transition.animateDp(
        transitionSpec = {
            if (reducedMotion) snap() else spring(stiffness = SpringStiffness)
        },
        targetValueByState = { toLoading -> if (toLoading) 12.dp else 24.dp },
        label = "button's content padding",
    )
    Button(
        onClick = onClick,
        modifier = Modifier.defaultMinSize(minWidth = 1.dp),
        contentPadding = PaddingValues(
            horizontal = horizontalContentPadding,
            vertical = 8.dp,
        ),
    ) {
        Box(contentAlignment = Alignment.Center) {
            LoadingContent(
                loadingStateTransition = transition,
                reducedMotion = reducedMotion,
            )
            PrimaryContent(
                text = text(),
                loadingStateTransition = transition,
                reducedMotion = reducedMotion,
            )
        }
    }
}

@Composable
private fun LoadingContent(
    loadingStateTransition: Transition<Boolean>,
    reducedMotion: Boolean,
) {
    loadingStateTransition.AnimatedVisibility(
        visible = { loading -> loading },
        enter = if (reducedMotion) EnterTransition.None else fadeIn(),
        exit = if (reducedMotion) ExitTransition.None else fadeOut(
            animationSpec = spring(
                stiffness = SpringStiffness,
                visibilityThreshold = 0.10f,
            ),
        ),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = LocalContentColor.current,
            strokeWidth = 1.5f.dp,
            strokeCap = StrokeCap.Round,
        )
    }
}

@Composable
private fun PrimaryContent(
    text: String,
    loadingStateTransition: Transition<Boolean>,
    reducedMotion: Boolean,
) {
    loadingStateTransition.AnimatedVisibility(
        visible = { loading -> !loading },
        enter = if (reducedMotion) EnterTransition.None else fadeIn() + expandHorizontally(
            animationSpec = spring(
                stiffness = SpringStiffness,
                dampingRatio = Spring.DampingRatioMediumBouncy,
                visibilityThreshold = IntSize.VisibilityThreshold,
            ),
            expandFrom = Alignment.CenterHorizontally,
        ),
        exit = if (reducedMotion) ExitTransition.None else fadeOut(
            animationSpec = spring(
                stiffness = SpringStiffness,
                visibilityThreshold = 0.10f,
            ),
        ) + shrinkHorizontally(
            animationSpec = spring(
                stiffness = SpringStiffness,
                // dampingRatio is not applicable here, size cannot become negative
                visibilityThreshold = IntSize.VisibilityThreshold,
            ),
            shrinkTowards = Alignment.CenterHorizontally,
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier
                // so that bouncing button's width doesn't cut first and last letters
                .padding(horizontal = 4.dp),
            fontSize = 16.sp,
        )
    }
}

@Preview
@Composable
private fun LoadingButtonPreview() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        LoadingButton(
            text = { "Test" },
            loading = { false },
            onClick = {},
        )
    }
}
