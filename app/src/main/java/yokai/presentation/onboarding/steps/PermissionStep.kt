package yokai.presentation.onboarding.steps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.util.system.isShizukuInstalled
import yokai.i18n.MR
import yokai.presentation.component.Gap
import yokai.presentation.theme.Size

internal class PermissionStep : OnboardingStep {

    private var installGranted by mutableStateOf(false)
    private var notificationGranted by mutableStateOf(false)
    private var batteryGranted by mutableStateOf(false)

    override val isComplete: Boolean
        get() = installGranted

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        // LifecycleEventEffect(ON_RESUME) didn't reliably re-fire when returning from the system
        // Settings activity that the Grant buttons launch, so the buttons stayed stuck on "Grant"
        // even after the user had granted the permission. Attaching a DefaultLifecycleObserver
        // directly mirrors Mihon's approach and re-checks each permission on every resume.
        DisposableEffect(lifecycleOwner.lifecycle) {
            val observer = object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    installGranted =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.packageManager.canRequestPackageInstalls()
                        } else {
                            @Suppress("DEPRECATION")
                            Settings.Secure.getInt(
                                context.contentResolver,
                                Settings.Secure.INSTALL_NON_MARKET_APPS
                            ) != 0
                        } || context.isShizukuInstalled
                    notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                            PackageManager.PERMISSION_GRANTED
                    } else {
                        true
                    }
                    batteryGranted = context.getSystemService<PowerManager>()!!
                        .isIgnoringBatteryOptimizations(context.packageName)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        Column(
            modifier = Modifier.padding(vertical = Size.medium),
        ) {
            SectionHeader(stringResource(MR.strings.onboarding_permission_type_required))

            PermissionItem(
                title = stringResource(MR.strings.onboarding_permission_install_apps),
                subtitle = stringResource(MR.strings.onboarding_permission_install_apps_description),
                granted = installGranted,
                onButtonClick = {
                    val intent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                        } else {
                            Intent(Settings.ACTION_SECURITY_SETTINGS)
                        }
                    context.startActivity(intent)
                },
            )

            Gap(Size.medium)

            SectionHeader(stringResource(MR.strings.onboarding_permission_type_optional))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val permissionRequester =
                    rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { /* No-op, handled on resume */ },
                    )
                PermissionItem(
                    title = stringResource(MR.strings.onboarding_permission_notifications),
                    subtitle =
                    stringResource(MR.strings.onboarding_permission_notifications_description),
                    granted = notificationGranted,
                    onButtonClick = {
                        permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }

            PermissionItem(
                title = stringResource(MR.strings.onboarding_permission_ignore_battery_opts),
                subtitle =
                stringResource(MR.strings.onboarding_permission_ignore_battery_opts_description),
                granted = batteryGranted,
                onButtonClick = {
                    @SuppressLint("BatteryLife")
                    val intent =
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                    context.startActivity(intent)
                },
            )
        }
    }

    @Composable
    private fun SectionHeader(
        text: String,
        modifier: Modifier = Modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            modifier = modifier.padding(horizontal = Size.medium),
        )
    }

    @Composable
    private fun PermissionItem(
        title: String,
        subtitle: String,
        granted: Boolean,
        modifier: Modifier = Modifier,
        onButtonClick: () -> Unit,
    ) {
        ListItem(
            modifier = modifier,
            headlineContent = { Text(text = title) },
            supportingContent = { Text(text = subtitle) },
            trailingContent = {
                OutlinedButton(
                    enabled = !granted,
                    onClick = onButtonClick,
                ) {
                    if (granted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Text(stringResource(MR.strings.onboarding_permission_action_grant))
                    }
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    }
}
