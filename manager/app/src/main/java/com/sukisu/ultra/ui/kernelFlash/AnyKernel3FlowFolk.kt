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
import com.sukisu.ultra.ui.screen.install.archiveSlot
import com.sukisu.ultra.ui.screen.install.archiveUri
import com.sukisu.ultra.ui.screen.install.withArchiveSlot
import com.sukisu.ultra.ui.theme.tokens.FolkType

/**
 * The AnyKernel3 install flow, in the FolkPatch design.
 *
 * A chosen archive goes through slot selection on an A/B device, then a confirmation; on a
 * single-slot device it goes straight to the confirmation. The KPM choice is not part of this
 * sequence.
 *
 * The archive keeps its concrete type throughout, so the row the user picked keeps its
 * selection mark; the flow only needs the uri and the slot, which the archiveUri and
 * archiveSlot extensions provide for either row.
 */
@Composable
fun rememberAnyKernel3State(
    installMethodState: MutableState<InstallMethod?>,
    preselectedKernelUri: String?,
    horizonKernelSummary: String,
    isAbDevice: Boolean,
): AnyKernel3State {
    var showSlotSelectionDialog by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    /** The archive being decided on, kept so the slot step can rebuild the same type. */
    var pendingArchive by remember { mutableStateOf<InstallMethod?>(null) }

    val onHorizonKernelSelected: (InstallMethod) -> Unit = { method ->
        val uri = method.archiveUri
        if (uri != null) {
            if (isAbDevice && method.archiveSlot == null) {
                pendingArchive = method
                showSlotSelectionDialog = true
            } else {
                installMethodState.value = method
                showConfirmDialog = true
            }
        }
    }

    val onReopenSlotDialog: (InstallMethod) -> Unit = { method ->
        val uri = method.archiveUri
        if (uri != null && isAbDevice) {
            pendingArchive = method
            showSlotSelectionDialog = true
        }
    }

    val onSlotSelected: (String) -> Unit = { slot ->
        // Rebuild the archive the user actually picked, with the slot added, so the install
        // list still matches it.
        val archive = pendingArchive
        if (archive != null && archive.archiveUri != null) {
            installMethodState.value = archive.withArchiveSlot(slot)
            pendingArchive = null
            showSlotSelectionDialog = false
            showConfirmDialog = true
        }
    }

    val onDismissSlotDialog = {
        showSlotSelectionDialog = false
        pendingArchive = null
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
        showSlotSelectionDialog = showSlotSelectionDialog,
        showConfirmDialog = showConfirmDialog,
        onHorizonKernelSelected = onHorizonKernelSelected,
        onSlotSelected = onSlotSelected,
        onDismissSlotDialog = onDismissSlotDialog,
        onConfirmFlash = onConfirmFlash,
        onDismissConfirmDialog = onDismissConfirmDialog,
        onReopenSlotDialog = onReopenSlotDialog,
    )
}

