package yokai.presentation.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Tab
import androidx.compose.material.icons.outlined.ViewDay
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.icerock.moko.resources.compose.stringResource
import eu.kanade.tachiyomi.core.storage.preference.collectAsState
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.library.LibraryItem
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import yokai.i18n.MR
import yokai.presentation.theme.Size

internal class LibraryDisplayStep : OnboardingStep, KoinComponent {

    private val preferences: PreferencesHelper by inject()

    override val isComplete: Boolean = true

    @Composable
    override fun Content() {
        val selected by preferences.libraryDisplayMode().collectAsState()

        Column(
            modifier = Modifier
                .padding(Size.medium)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(Size.medium),
        ) {
            Text(
                text = stringResource(MR.strings.onboarding_library_view_description),
                style = MaterialTheme.typography.bodyMedium,
            )

            DisplayModeCard(
                title = stringResource(MR.strings.continuous),
                description = stringResource(MR.strings.onboarding_library_view_continuous_description),
                icon = Icons.Outlined.ViewDay,
                selected = selected == LibraryItem.DISPLAY_MODE_CONTINUOUS,
                onClick = {
                    preferences.libraryDisplayMode().set(LibraryItem.DISPLAY_MODE_CONTINUOUS)
                },
            )

            DisplayModeCard(
                title = stringResource(MR.strings.tabbed),
                description = stringResource(MR.strings.onboarding_library_view_tabbed_description),
                icon = Icons.Outlined.Tab,
                selected = selected == LibraryItem.DISPLAY_MODE_TABBED,
                onClick = {
                    preferences.libraryDisplayMode().set(LibraryItem.DISPLAY_MODE_TABBED)
                },
            )
        }
    }

    @Composable
    private fun DisplayModeCard(
        title: String,
        description: String,
        icon: ImageVector,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        OutlinedCard(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Size.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.width(Size.medium))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(Size.tiny),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (selected) {
                    Spacer(Modifier.width(Size.small))
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
