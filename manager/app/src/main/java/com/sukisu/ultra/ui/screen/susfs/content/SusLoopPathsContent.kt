package com.sukisu.ultra.ui.screen.susfs.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.folk.FolkPreference
import com.sukisu.ultra.ui.screen.susfs.component.BottomActionButtons
import com.sukisu.ultra.ui.screen.susfs.component.FolkCountedSection
import com.sukisu.ultra.ui.screen.susfs.component.FolkDescriptionGroup
import com.sukisu.ultra.ui.screen.susfs.component.FolkRowIconButton
import com.sukisu.ultra.ui.screen.susfs.component.FolkSusfsEmptyState
import com.sukisu.ultra.ui.screen.susfs.component.ResetButton

/**
 * The SUS Loop Paths tab, in the FolkPatch design: the description with its
 * restriction warning, the configured loop paths as rows, and the add/reset
 * actions.
 */
@Composable
fun SusLoopPathsContent(
    susLoopPaths: Set<String>,
    isLoading: Boolean,
    onAddLoopPath: () -> Unit,
    onRemoveLoopPath: (String) -> Unit,
    onEditLoopPath: ((String) -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FolkDescriptionGroup(
            title = stringResource(R.string.sus_loop_paths_description_title),
            description = stringResource(R.string.sus_loop_paths_description_text),
            warning = stringResource(R.string.susfs_loop_path_restriction_warning),
        )

        if (susLoopPaths.isEmpty()) {
            FolkSusfsEmptyState(message = stringResource(R.string.susfs_no_loop_paths_configured))
        } else {
            FolkCountedSection(
                title = stringResource(R.string.loop_paths_section),
                count = susLoopPaths.size,
            ) {
                susLoopPaths.toList().forEach { path ->
                    item(key = "loop_$path") {
                        FolkPreference(
                            title = path,
                            icon = Icons.Default.Loop,
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    onEditLoopPath?.let { edit ->
                                        FolkRowIconButton(
                                            icon = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit),
                                            tint = MaterialTheme.colorScheme.primary,
                                            enabled = !isLoading,
                                            onClick = { edit(path) },
                                        )
                                    }
                                    FolkRowIconButton(
                                        icon = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        enabled = !isLoading,
                                        onClick = { onRemoveLoopPath(path) },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    BottomActionButtons(
        primaryButtonText = stringResource(R.string.add_loop_path),
        onPrimaryClick = onAddLoopPath,
        isLoading = isLoading,
    )

    if (onReset != null && susLoopPaths.isNotEmpty()) {
        ResetButton(
            title = stringResource(R.string.susfs_reset_loop_paths_title),
            onClick = onReset,
        )
    }
}
