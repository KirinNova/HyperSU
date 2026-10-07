package com.sukisu.ultra.ui.kernelFlash

import androidx.compose.runtime.Stable
import com.sukisu.ultra.ui.screen.install.InstallMethod

enum class KpmPatchOption {
    FOLLOW_KERNEL,
    PATCH_KPM,
    UNDO_PATCH_KPM
}

@Stable
data class AnyKernel3State(
    val kpmPatchOption: KpmPatchOption,
    val showSlotSelectionDialog: Boolean,
    val showKpmPatchDialog: Boolean,
    /**
     * True while the user is being asked to confirm the archive they picked.
     *
     * Picking a file used to open the KPM dialog straight away, which asked a question the user
     * had not raised. This confirmation takes that place: it reports what is about to be
     * flashed and waits for an answer, and the KPM choice stays on its own row.
     */
    val showConfirmDialog: Boolean,
    val onHorizonKernelSelected: (InstallMethod.KernelArchive) -> Unit,
    val onSlotSelected: (String) -> Unit,
    val onDismissSlotDialog: () -> Unit,
    val onOptionSelected: (KpmPatchOption) -> Unit,
    val onDismissPatchDialog: () -> Unit,
    val onConfirmFlash: () -> Unit,
    val onDismissConfirmDialog: () -> Unit,
    val onReopenSlotDialog: (InstallMethod.KernelArchive) -> Unit,
    val onReopenKpmDialog: (InstallMethod.KernelArchive) -> Unit
)
