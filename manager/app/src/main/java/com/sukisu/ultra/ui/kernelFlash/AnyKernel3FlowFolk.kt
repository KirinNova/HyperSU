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
import com.sukisu.ultra.ui.screen.install.withSlot
import com.sukisu.ultra.ui.theme.tokens.FolkType

/**
 * The AnyKernel3 install flow, in the FolkPatch design.
 *
 * A chosen archive goes through slot selection on an A/B device, then a confirmation; on a
 * single-slot device it goes straight to the confirmation. The KPM choice is not part of this
 * sequence - it stays on its own row on the install screen.
 *
 * The archive keeps its concrete type throughout, so the row the user picked keeps its
 * selection mark; the flow only needs the uri and the slot, which the KernelArchive interface
 * provides for either type.
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
    var showConfirmDialog by remember { mutableStateOf(false) }
    var tempKernelUri by remember { mutableStateOf<Uri?>(null) }

    /** The archive being decided on, kept so the slot step can rebuild the same type. */
    var pendingArchive by remember { mutableStateOf<InstallMethod.KernelArchive?>(null) }

    val onHorizonKernelSelected: (InstallMethod.KernelArchive) -> Unit = { method ->
        val uri = method.uri
        if (uri != null) {
            if (isAbDevice && method.slot == null) {
                tempKernelUri = uri
                pendingArchive = method
                showSlotSelectionDialog = true
            } else {
                installMethodState.value = method
                // The KPM question is not asked here any more. It used to open as soon as a
                // file was picked, before the user had said they wanted to flash at all; the
                // confirmation takes its place, and the KPM choice stays available from the
                // row on the install screen.
                showConfirmDialog = true
            }
        }
    }

    val onReopenSlotDialog: (InstallMethod.KernelArchive) -> Unit = { method ->
        val uri = method.uri
        if (uri != null && isAbDevice) {
            tempKernelUri = uri
            pendingArchive = method
            showSlotSelectionDialog = true
        }
    }

    val onReopenKpmDialog: (InstallMethod.KernelArchive) -> Unit = { method ->
        installMethodState.value = method
        showKpmPatchDialog = true
    }

    val onSlotSelected: (String) -> Unit = { slot ->
        // Rebuild the archive the user actually picked, with the slot added, so the install
        // list still matches it.
        val archive = pendingArchive
        if (archive != null && archive.uri != null) {
            installMethodState.value = archive.withSlot(slot)
            tempKernelUri = null
            pendingArchive = null
            showSlotSelectionDialog = false
            showConfirmDialog = true
        }
    }

    val onDismissSlotDialog = {
        showSlotSelectionDialog = false
        tempKernelUri = null
        pendingArchive = null
    }

    val onOptionSelected: (KpmPatchOption) -> Unit = { option ->
        kpmPatchOption = option
        showKpmPatchDialog = false
    }

    val onDismissPatchDialog = {
        showKpmPatchDialog = false
    }

    val onConfirmFlash = {
        showConfirmDialog = false
    }

    val onDismissConfirmDialog = {
        showConfirmDialog = false
        // Dropping the selection keeps the install screen honest: with nothing selected the
        // Next button is disabled and no row claims to be chosen.
        installMethodState.value = null
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
                        pendingArchive = method
                        showSlotSelectionDialog = true
                    } else {
                        installMethodState.value = method
                        showConfirmDialog = true
                    }
                }
        }
    }

    return AnyKernel3State(
        kpmPatchOption = kpmPatchOption,
        showSlotSelectionDialog = showSlotSelectionDialog,
        showKpmPatchDialog = showKpmPatchDialog,
        showConfirmDialog = showConfirmDialog,
        onHorizonKernelSelected = onHorizonKernelSelected,
        onSlotSelected = onSlotSelected,
        onDismissSlotDialog = onDismissSlotDialog,
        onOptionSelected = onOptionSelected,
        onDismissPatchDialog = onDismissPatchDialog,
        onConfirmFlash = onConfirmFlash,
        onDismissConfirmDialog = onDismissConfirmDialog,
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
