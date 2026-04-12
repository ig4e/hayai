package eu.kanade.tachiyomi.ui.crash

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.util.CrashLogUtil
import eu.kanade.tachiyomi.util.system.setThemeByPref
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy
import yokai.i18n.MR
import yokai.presentation.onboarding.InfoScreen
import yokai.presentation.theme.Size
import yokai.presentation.theme.YokaiTheme

class CrashActivity : AppCompatActivity() {
    internal val preferences: PreferencesHelper by injectLazy()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setThemeByPref(preferences)

        val exception = GlobalExceptionHandler.getThrowableFromIntent(intent)
        setContent {
            val scope = rememberCoroutineScope()
            val context = LocalContext.current

            YokaiTheme {
                InfoScreen(
                    icon = Icons.Outlined.BugReport,
                    headingText = stringResource(MR.strings.crash_screen_title),
                    subtitleText = stringResource(MR.strings.crash_screen_description, stringResource(MR.strings.app_name)),
                    acceptText = stringResource(MR.strings.crash_screen_share_logs),
                    onAcceptClick = {
                        scope.launch {
                            CrashLogUtil(context).shareLogs(exception)
                        }
                    },
                    canAccept = true,
                    rejectText = stringResource(MR.strings.dump_crash_logs),
                    onRejectClick = {
                        scope.launch {
                            CrashLogUtil(context).dumpLogs(exception)
                        }
                    },
                    tertiaryText = stringResource(MR.strings.crash_screen_restart_application),
                    onTertiaryClick = {
                        finishAffinity()
                        startActivity(Intent(this@CrashActivity, MainActivity::class.java))
                    },
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = Size.small)
                            .clip(MaterialTheme.shapes.small)
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Text(
                            text = exception.toString(),
                            fontSize = MaterialTheme.typography.bodySmall.fontSize,
                            modifier = Modifier.padding(all = Size.small),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
