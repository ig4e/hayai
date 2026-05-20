package yokai.presentation.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import eu.kanade.tachiyomi.core.storage.preference.collectAsState
import eu.kanade.tachiyomi.ui.base.controller.BaseComposeController
import org.koin.core.component.inject
import yokai.domain.base.BasePreferences

class OnboardingController :
    BaseComposeController() {

    val basePreferences by inject<BasePreferences>()

    override fun handleBack(): Boolean {
        return if (!basePreferences.hasShownOnboarding().get()) {
            true
        } else {
            super.handleBack()
        }
    }

    @Composable
    override fun ScreenContent() {

        val hasShownOnboarding by basePreferences.hasShownOnboarding().collectAsState()

        val finishOnboarding: () -> Unit = {
            basePreferences.hasShownOnboarding().set(true)
            router.popCurrentController()
        }

        BackHandler(
            enabled = !hasShownOnboarding,
            onBack = {
                // Prevent exiting if onboarding hasn't been completed
            },
        )

        OnboardingScreen(
            onComplete = finishOnboarding
        )
    }
}
