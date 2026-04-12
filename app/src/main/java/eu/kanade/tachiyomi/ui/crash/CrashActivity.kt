package eu.kanade.tachiyomi.ui.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
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
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Crash log", exception.toString()))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.padding(end = Size.tiny),
                        )
                        Text("Copy Error")
                    }
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
