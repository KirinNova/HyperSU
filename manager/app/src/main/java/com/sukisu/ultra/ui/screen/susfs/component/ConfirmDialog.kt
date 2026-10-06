package com.sukisu.ultra.ui.screen.susfs.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.folk.FolkAlertDialog
import com.sukisu.ultra.ui.component.folk.FolkButtonDefaults
import com.sukisu.ultra.ui.theme.tokens.FolkType

/**
 * The shared "are you sure?" dialog of the SuSFS screen, in the FolkPatch
 * design: a title, the message and a Cancel/Confirm pair with the destructive
 * action filled.
 */
@Composable
fun ConfirmDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    titleRes: Int,
    messageRes: Int,
    isLoading: Boolean = false
) {
    if (!showDialog) return

    FolkAlertDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(titleRes),
                style = FolkType.Title,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.padding(top = 10.dp))
            Text(
                text = stringResource(messageRes),
                style = FolkType.Summary,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.padding(top = 10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = FolkButtonDefaults.textColors(),
                ) {
                    Text(stringResource(R.string.cancel))
                }
                Spacer(Modifier.width(8.dp))
                TextButton(
                    onClick = {
                        if (!isLoading) onConfirm()
                    },
                    enabled = !isLoading,
                    colors = FolkButtonDefaults.textColors(),
                ) {
                    Text(stringResource(R.string.confirm))
                }
            }
        }
    }
}
