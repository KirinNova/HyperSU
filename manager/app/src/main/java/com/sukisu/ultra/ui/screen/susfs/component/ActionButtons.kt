package com.sukisu.ultra.ui.screen.susfs.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.ui.component.folk.FolkButtonDefaults

/**
 * The bottom action pair of a SuSFS section, in the FolkPatch design: a tonal
 * secondary button beside a filled primary one, or a single full-width primary
 * button when there is no secondary action.
 */
@Composable
fun BottomActionButtons(
    modifier: Modifier = Modifier,
    primaryButtonText: String,
    onPrimaryClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 4.dp,
                bottom = 8.dp,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (secondaryButtonText != null && onSecondaryClick != null) {
            OutlinedButton(
                onClick = onSecondaryClick,
                enabled = !isLoading && enabled,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                colors = FolkButtonDefaults.tonalColors(),
            ) {
                Text(
                    text = secondaryButtonText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = onPrimaryClick,
                enabled = !isLoading && enabled,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                colors = FolkButtonDefaults.filledColors(),
            ) {
                Text(
                    text = primaryButtonText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        } else {
            Button(
                onClick = onPrimaryClick,
                enabled = !isLoading && enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
                colors = FolkButtonDefaults.filledColors(),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(text = primaryButtonText)
            }
        }
    }
}

/** The destructive reset action of a section, in the Folk colours. */
@Composable
fun ResetButton(
    modifier: Modifier = Modifier,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
    ) {
        Text(text = title)
    }
}
