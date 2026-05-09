package yokai.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import eu.kanade.tachiyomi.R
import yokai.i18n.MR
import yokai.util.lang.getString
import dev.icerock.moko.resources.compose.stringResource
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import yokai.presentation.onboarding.steps.PermissionStep
import yokai.presentation.onboarding.steps.StorageStep
import yokai.presentation.onboarding.steps.ThemeStep
import yokai.presentation.theme.Size
import yokai.presentation.theme.isReducedMotion

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit = {}
) {
    val slideDistance = rememberSlideDistance()

    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember {
        listOf(
            ThemeStep(),
            StorageStep(),
            PermissionStep(),
        )
    }
    val isLastStep = currentStep == steps.lastIndex

    BackHandler(enabled = currentStep != 0, onBack = { currentStep-- })

    InfoScreen(
        icon = Icons.Outlined.RocketLaunch,
        headingText = stringResource(MR.strings.onboarding_heading),
        subtitleText = stringResource(MR.strings.onboarding_description),
        tint = MaterialTheme.colorScheme.primary,
        acceptText = stringResource(
            if (isLastStep)
                MR.strings.onboarding_finish
            else {
                MR.strings.next
            }
        ),
        canAccept = steps[currentStep].isComplete,
        onAcceptClick = {
            if (isLastStep) {
                onComplete()
            } else {
                currentStep++
            }
        },
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = Size.small)
                .clip(MaterialTheme.shapes.small)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            val reducedMotion = isReducedMotion
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (reducedMotion) EnterTransition.None togetherWith ExitTransition.None
                    else materialSharedAxisX(
                        forward = targetState > initialState,
                        slideDistance = slideDistance,
                    )
                },
                label = "stepContent",
            ) { step ->
                steps[step].Content()
            }
        }
    }
}
