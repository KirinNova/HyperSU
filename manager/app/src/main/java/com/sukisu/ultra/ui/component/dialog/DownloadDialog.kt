package com.sukisu.ultra.ui.component.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.folk.FolkAlertDialog
import com.sukisu.ultra.ui.component.folk.FolkButtonDefaults
import com.sukisu.ultra.ui.theme.tokens.FolkType

/**
 * The "install from URL" dialog, in the FolkPatch design: a title, a URL field
 * and a cancel/confirm pair where confirm stays disabled until the text is a
 * usable https link.
 *
 * Replaces both the Material and the Miuix variants.
 */
@Composable
fun DownloadDialog(
    show: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!show) return

    var url by remember { mutableStateOf("") }

    FolkAlertDialog(onDismissRequest = onDismiss, width = 340.dp) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.download_dialog_title),
                style = FolkType.Title,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                placeholder = { Text(stringResource(R.string.download_dialog_msg)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = FolkButtonDefaults.textColors(),
                ) {
                    Text(stringResource(android.R.string.cancel))
                }

                TextButton(
                    enabled = isValidUrl(url.trim()),
                    onClick = { onConfirm(url.trim()) },
                    colors = FolkButtonDefaults.textColors(),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}

private fun isValidUrl(url: String): Boolean {
    if (url.isEmpty()) return false
    val uri = url.toUri()
    return uri.scheme.equals("https", ignoreCase = true) &&
        !uri.host.isNullOrEmpty() &&
        !uri.path.isNullOrEmpty()
}
