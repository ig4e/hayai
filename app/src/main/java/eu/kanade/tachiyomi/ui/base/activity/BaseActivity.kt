package eu.kanade.tachiyomi.ui.base.activity

import android.animation.ValueAnimator
import android.content.Intent
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.interpolator.view.animation.LinearOutSlowInInterpolator
import androidx.viewbinding.ViewBinding
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.SearchActivity
import eu.kanade.tachiyomi.ui.security.SecureActivityDelegate
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getThemeWithExtras
import eu.kanade.tachiyomi.util.system.setLocaleByAppCompat
import eu.kanade.tachiyomi.util.system.setThemeByPref
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import yokai.domain.SplashState
import yokai.presentation.theme.ReducedMotion
import android.R as AR

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    val preferences: PreferencesHelper by injectLazy()
    lateinit var binding: VB
    val isBindingInitialized get() = this::binding.isInitialized

    private var updatedTheme: Resources.Theme? = null
    internal val splashState: SplashState = Injekt.get()

    override fun onCreate(savedInstanceState: Bundle?) {
        setLocaleByAppCompat()
        updatedTheme = null
        setThemeByPref(preferences)
        super.onCreate(savedInstanceState)
        SecureActivityDelegate.setSecure(this)
        applyReducedMotionTransitions()
    }

    /**
     * Strips this activity's open/close transitions when the user has reduced motion enabled.
     * On Android 14+ a single `overrideActivityTransition` call covers both directions; on older
     * platforms we fall back to `overridePendingTransition` from [startActivity] / [finish].
     */
    private fun applyReducedMotionTransitions() {
        if (!ReducedMotion.isEnabled()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        }
    }

    @Suppress("DEPRECATION")
    override fun startActivity(intent: Intent) {
        super.startActivity(intent)
        if (ReducedMotion.isEnabled() && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overridePendingTransition(0, 0)
        }
    }

    @Suppress("DEPRECATION")
    override fun startActivity(intent: Intent, options: Bundle?) {
        super.startActivity(intent, options)
        if (ReducedMotion.isEnabled() && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overridePendingTransition(0, 0)
        }
    }

    @Suppress("DEPRECATION")
    override fun finish() {
        super.finish()
        if (ReducedMotion.isEnabled() && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overridePendingTransition(0, 0)
        }
    }

    fun maybeInstallSplashScreen(savedInstanceState: Bundle?): SplashScreen? {
        if (splashState.shown || savedInstanceState != null) {
            setTheme(R.style.Theme_Tachiyomi)
            splashState.ready = true
            return null
        } else {
            splashState.shown = true
        }

        return installSplashScreen()
    }

    fun SplashScreen.configure() {
        val startTime = System.currentTimeMillis()
        this.setKeepOnScreenCondition {
            val elapsed = System.currentTimeMillis() - startTime
            elapsed <= SPLASH_MIN_DURATION || (!splashState.ready && elapsed <= SPLASH_MAX_DURATION)
        }
        this.setSplashScreenExitAnimation()
    }

    private fun SplashScreen.setSplashScreenExitAnimation() {
        val root = findViewById<View>(AR.id.content)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            this.setOnExitAnimationListener { splashProvider ->
                // For some reason the SplashScreen applies (incorrect) Y translation to the iconView
                splashProvider.iconView.translationY = 0F

                val activityAnim = ValueAnimator.ofFloat(1F, 0F).apply {
                    interpolator = LinearOutSlowInInterpolator()
                    duration = SPLASH_EXIT_ANIM_DURATION
                    addUpdateListener { va ->
                        val value = va.animatedValue as Float
                        root.translationY = value * 16.dpToPx
                    }
                }

                val splashAnim = ValueAnimator.ofFloat(1F, 0F).apply {
                    interpolator = FastOutSlowInInterpolator()
                    duration = SPLASH_EXIT_ANIM_DURATION
                    addUpdateListener { va ->
                        val value = va.animatedValue as Float
                        splashProvider.view.alpha = value
                    }
                    doOnEnd {
                        splashProvider.remove()
                    }
                }

                activityAnim.start()
                splashAnim.start()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (this !is SearchActivity) {
            SecureActivityDelegate.promptLockIfNeeded(this)
        }
    }

    override fun getTheme(): Resources.Theme {
        val newTheme = getThemeWithExtras(super.getTheme(), preferences, updatedTheme)
        updatedTheme = newTheme
        return newTheme
    }

    companion object {
        // Splash screen
        private const val SPLASH_MIN_DURATION = 500 // ms
        private const val SPLASH_MAX_DURATION = 5000 // ms
        private const val SPLASH_EXIT_ANIM_DURATION = 400L // ms
    }
}
