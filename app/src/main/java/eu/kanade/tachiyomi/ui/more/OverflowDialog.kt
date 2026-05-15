package eu.kanade.tachiyomi.ui.more

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updateLayoutParams
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.R
import yokai.i18n.MR
import yokai.util.lang.getString
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.core.preference.toggle
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.databinding.TachiOverflowLayoutBinding
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.lang.addBetaTag
import eu.kanade.tachiyomi.util.lang.withSubtitle
import eu.kanade.tachiyomi.util.system.dpToPx
import eu.kanade.tachiyomi.util.system.getResourceColor
import eu.kanade.tachiyomi.util.system.openInBrowser
import uy.kohesive.injekt.injectLazy
import android.R as AR

class OverflowDialog(
    activity: MainActivity,
    showUpdateLibrary: Boolean = false,
    onUpdateLibrary: () -> Unit = {},
) : Dialog(activity, R.style.OverflowDialogTheme) {

    val binding = TachiOverflowLayoutBinding.inflate(activity.layoutInflater, null, false)
    val preferences: PreferencesHelper by injectLazy()

    init {
        setContentView(binding.root)

        binding.overflowCardView.backgroundTintList = ColorStateList.valueOf(
            ColorUtils.blendARGB(
                activity.getResourceColor(R.attr.background),
                activity.getResourceColor(R.attr.colorSecondary),
                0.075f,
            ),
        )
        binding.touchOutside.setOnClickListener {
            cancel()
        }
        if (showUpdateLibrary) {
            binding.updateLibraryItem.visibility = View.VISIBLE
            binding.updateLibraryDivider.visibility = View.VISIBLE
            binding.updateLibraryItem.setOnClickListener {
                onUpdateLibrary()
                dismiss()
            }
        }
        val incogText = context.getString(MR.strings.incognito_mode)
        with(binding.incognitoModeItem) {
            val titleText = context.getString(
                if (preferences.incognitoMode().get()) {
                    MR.strings.turn_off_
                } else {
                    MR.strings.turn_on_
                },
                incogText,
            )
            val subtitleText = context.getString(MR.strings.pauses_reading_history)
            text = titleText.withSubtitle(context, subtitleText)
            setIcon(
                if (preferences.incognitoMode().get()) {
                    R.drawable.ic_incognito_24dp
                } else {
                    R.drawable.ic_glasses_24dp
                },
            )
            setOnClickListener {
                preferences.incognitoMode().toggle()
                val incog = preferences.incognitoMode().get()
                val newTitle = context.getString(
                    if (incog) {
                        MR.strings.turn_off_
                    } else {
                        MR.strings.turn_on_
                    },
                    incogText,
                )
                text = newTitle.withSubtitle(context, subtitleText)
                val drawable = AnimatedVectorDrawableCompat.create(
                    context,
                    if (incog) {
                        R.drawable.anim_read_to_incog
                    } else {
                        R.drawable.anim_incog_to_read
                    },
                )
                setIcon(drawable)
                (getIcon() as? AnimatedVectorDrawableCompat)?.start()
            }
        }
        binding.updateReportItem.setOnClickListener {
            // Reuse the existing in-app handler that the file-open icon on
            // TabbedRecentsOptionsSheet and the post-update notification both go
            // through, so all three entry points land on the same screen without
            // duplicating router/controller wiring here.
            val intent = Intent(activity, MainActivity::class.java).apply {
                action = MainActivity.SHORTCUT_LIBRARY_UPDATE_REPORT
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            activity.startActivity(intent)
            dismiss()
        }

        binding.settingsItem.setOnClickListener {
            activity.showSettings()
            dismiss()
        }

        binding.helpItem.setOnClickListener {
            activity.openInBrowser(URL_HELP)
            dismiss()
        }

        val vName = "v${BuildConfig.VERSION_NAME}".substringBefore("-")
        val newVName = buildSpannedString {
            color(context.getResourceColor(AR.attr.textColorSecondary)) {
                append(vName)
            }
            if (BuildConfig.BETA) {
                append("".addBetaTag(context, false))
            }
        }

        binding.aboutItem.text = context.getString(MR.strings.about).withSubtitle(newVName)

        binding.aboutItem.setOnClickListener {
            activity.showAbout()
            dismiss()
        }

        binding.statsItem.setOnClickListener {
            activity.showStats()
            dismiss()
        }

        binding.overflowCardView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = activity.toolbarHeight - 2.dpToPx
        }
        window?.let { window ->
            window.navigationBarColor = Color.TRANSPARENT
            window.decorView.fitsSystemWindows = true
            val wic = WindowInsetsControllerCompat(window, window.decorView)
            wic.isAppearanceLightStatusBars = false
            wic.isAppearanceLightNavigationBars = false
        }
    }

    private companion object {
        private const val URL_HELP = "https://mihon.app/docs/guides/troubleshooting/"
    }
}
