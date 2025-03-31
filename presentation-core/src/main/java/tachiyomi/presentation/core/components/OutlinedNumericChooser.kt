package tachiyomi.presentation.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.R
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun OutlinedNumericChooser(
    value: Int,
    onValueChanged: (Int) -> Unit,
    valueRange: IntRange = 0..Int.MAX_VALUE,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onValueChanged((value - 1).coerceIn(valueRange)) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_remove_24dp),
                    contentDescription = stringResource(MR.strings.action_decrement),
                )
            }

            Text(
                text = value.toString(),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )

            IconButton(
                onClick = { onValueChanged((value + 1).coerceIn(valueRange)) },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_24dp),
                    contentDescription = stringResource(MR.strings.action_increment),
                )
            }
        }
    }
}
