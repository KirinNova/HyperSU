package com.sukisu.ultra.ui.kernelFlash

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.folk.FolkAlertDialog
import com.sukisu.ultra.ui.component.folk.FolkButtonDefaults
import com.sukisu.ultra.ui.component.folk.FolkSelectableRow
import com.sukisu.ultra.ui.screen.install.InstallMethod
import com.sukisu.ultra.ui.theme.tokens.FolkType

/**
 * The AnyKernel3 install flow, in the FolkPatch design.
 *
 * A chosen kernel goes through slot selection on an A/B device and then the KPM
 * patch choice; on a single-slot device it goes straight to the patch choice.
 * The state machine, the reopen callbacks and the preselected-URI effect are all
 * unchanged - only the two dialogs are Folk now.
 */
@Composable
fun rememberAnyKernel3State(
    installMethodState: MutableState<InstallMethod?>,
    preselectedKernelUri: String?,
    horizonKernelSummary: String,
    isAbDevice: Boolean,
): AnyKernel3State {
    var kpmPatchOption by remember { mutableStateOf(KpmPatchOption.FOLLOW_KERNEL) }
    var showSlotSelectionDialog by remember { mutableStateOf(false) }
    var showKpmPatchDialog by remember { mutableStateOf(false) }
    var tempKernelUri by remember { mutableStateOf<Uri?>(null) }

    val onHorizonKernelSelected: (InstallMethod.HorizonKernel) -> Unit = { method ->
        val uri = method.uri
        if (uri != null) {
            if (isAbDevice && method.slot == null) {
                tempKernelUri = uri
                showSlotSelectionDialog = true
            } else {
                installMethodState.value = method
                showKpmPatchDialog = true
            }
        }
    }

    val onReopenSlotDialog: (InstallMethod.HorizonKernel) -> Unit = { method ->
        val uri = method.uri
        if (uri != null && isAbDevice) {
            tempKernelUri = uri
            showSlotSelectionDialog = true
        }
    }

    val onReopenKpmDialog: (InstallMethod.HorizonKernel) -> Unit = { method ->
        installMethodState.value = method
        showKpmPatchDialog = true
    }

    val onSlotSelected: (String) -> Unit = { slot ->
        val uri = tempKernelUri ?: (installMethodState.value as? InstallMethod.HorizonKernel)?.uri
        if (uri != null) {
            installMethodState.value = InstallMethod.HorizonKernel(
                uri = uri,
                slot = slot,
                summary = horizonKernelSummary,
            )
            tempKernelUri = null
            showSlotSelectionDialog = false
            showKpmPatchDialog = true
        }
    }

    val onDismissSlotDialog = {
        showSlotSelectionDialog = false
    }

    val onOptionSelected: (KpmPatchOption) -> Unit = { option ->
        kpmPatchOption = option
        showKpmPatchDialog = false
    }

    val onDismissPatchDialog = {
        showKpmPatchDialog = false
    }

    LaunchedEffect(preselectedKernelUri, isAbDevice, horizonKernelSummary) {
        preselectedKernelUri?.let { uriString ->
            runCatching { uriString.toUri() }
                .getOrNull()
                ?.let { preselectedUri ->
                    val method = InstallMethod.HorizonKernel(
                        uri = preselectedUri,
                        summary = horizonKernelSummary,
                    )
                    if (isAbDevice) {
                        tempKernelUri = preselectedUri
                        showSlotSelectionDialog = true
                    } else {
                        installMethodState.value = method
                        showKpmPatchDialog = true
                    }
                }
        }
    }

    return AnyKernel3State(
        kpmPatchOption = kpmPatchOption,
        showSlotSelectionDialog = showSlotSelectionDialog,
        showKpmPatchDialog = showKpmPatchDialog,
        onHorizonKernelSelected = onHorizonKernelSelected,
        onSlotSelected = onSlotSelected,
        onDismissSlotDialog = onDismissSlotDialog,
        onOptionSelected = onOptionSelected,
        onDismissPatchDialog = onDismissPatchDialog,
        onReopenSlotDialog = onReopenSlotDialog,
        onReopenKpmDialog = onReopenKpmDialog,
    )
}

@Composable
fun KpmPatchSelectionDialog(
    show: Boolean,
    currentOption: KpmPatchOption,
    onDismiss: () -> Unit,
    onOptionSelected: (KpmPatchOption) -> Unit,
) {
    if (!show) return

    var selectedOption by remember(currentOption) { mutableStateOf(currentOption) }

    FolkAlertDialog(
        onDismissRequest = onDismiss,
        width = 340.dp,
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.kpm_patch_options),
                style = FolkType.Title,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = stringResource(R.string.kpm_patch_description),
                style = FolkType.Summary,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            val options = listOf(
                KpmPatchOption.FOLLOW_KERNEL to stringResource(R.string.kpm_follow_kernel_file),
                KpmPatchOption.PATCH_KPM to stringResource(R.string.enable_kpm_patch),
                KpmPatchOption.UNDO_PATCH_KPM to stringResource(R.string.enable_kpm_undo_patch),
            )

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                options.forEach { (option, title) ->
                    FolkSelectableRow(
                        title = title,
                        selected = selectedOption == option,
                        onClick = { selectedOption = option },
                    )
                }
            }

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
                Button(
                    onClick = {
                        onOptionSelected(selectedOption)
                        onDismiss()
                    },
                    colors = FolkButtonDefaults.filledColors(),
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            }
        }
    }
}
