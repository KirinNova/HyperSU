package com.sukisu.ultra.ui.screen.module

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.sukisu.ultra.ui.component.folk.folkGroupColor
import com.sukisu.ultra.ui.theme.tokens.FolkShape
import com.sukisu.ultra.ui.theme.tokens.FolkType

/**
 * The confirmation shown before a module zip is flashed.
 *
 * Selecting a zip used to open the flash screen and start immediately, so the user never saw
 * what they had picked. This dialog reads `module.prop` first and reports the name, version,
 * author and description, and only flashes after an explicit confirmation.
 *
 * A zip without a readable `module.prop` still gets a dialog - it just says so, and names the
 * file instead - because the decision to flash is the user's either way.
 */
@Composable
fun ModuleInstallConfirmDialog(
    info: ModuleZipInfo?,
    fileName: String,
    fileCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    FolkAlertDialog(onDismissRequest = onDismiss, width = 320.dp) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.module_install_confirm_title),
                style = FolkType.Title,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                shape = FolkShape.Corner16,
                color = folkGroupColor(),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    if (info == null || !info.hasModuleProp) {
                        // Nothing to describe: say so rather than showing empty rows, which
                        // would read as a parsing failure the user cannot act on.
                        Text(
                            text = stringResource(R.string.module_install_confirm_no_prop),
                            style = FolkType.Summary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    InfoRow(
                        label = stringResource(R.string.module_install_confirm_name),
                        value = info?.displayName(fileName) ?: fileName,
                    )
                    info?.id?.takeIf { it.isNotBlank() }?.let {
                        InfoRow(
                            label = stringResource(R.string.module_install_confirm_id),
                            value = it,
                        )
                    }
                    info?.version?.takeIf { it.isNotBlank() }?.let {
                        InfoRow(
                            label = stringResource(R.string.module_install_confirm_version),
                            value = it,
                        )
                    }
                    info?.author?.takeIf { it.isNotBlank() }?.let {
                        InfoRow(
                            label = stringResource(R.string.module_install_confirm_author),
                            value = it,
                        )
                    }
                    info?.description?.takeIf { it.isNotBlank() }?.let {
                        InfoRow(
                            label = stringResource(R.string.module_install_confirm_description),
                            value = it,
                        )
                    }

                    if (fileCount > 1) {
                        InfoRow(
                            label = stringResource(R.string.module_install_confirm_files),
                            value = fileCount.toString(),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, colors = FolkButtonDefaults.textColors()) {
                    Text(stringResource(android.R.string.cancel))
                }
                Button(
                    onClick = onConfirm,
                    colors = FolkButtonDefaults.filledColors(),
                ) {
                    Text(stringResource(R.string.module_install_confirm_action))
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = FolkType.Caption,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = FolkType.Summary)
    }
}
