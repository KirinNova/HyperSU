package com.sukisu.ultra.ui.screen.susfs.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
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
 * The SUS Maps tab, in the FolkPatch design: the description with its warning
 * and debug footnote, the configured map paths as rows, and the add/reset
 * actions.
 */
@Composable
fun SusMapsContent(
    susMaps: Set<String>,
    isLoading: Boolean,
    onAddSusMap: () -> Unit,
    onRemoveSusMap: (String) -> Unit,
    onEditSusMap: ((String) -> Unit)? = null,
    onReset: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FolkDescriptionGroup(
            title = stringResource(R.string.sus_maps_description_title),
            description = stringResource(R.string.sus_maps_description_text),
            warning = stringResource(R.string.sus_maps_warning),
            additionalInfo = stringResource(R.string.sus_maps_debug_info),
        )

        if (susMaps.isEmpty()) {
            FolkSusfsEmptyState(message = stringResource(R.string.susfs_no_sus_maps_configured))
        } else {
            FolkCountedSection(
                title = stringResource(R.string.sus_maps_section),
                count = susMaps.size,
            ) {
                susMaps.toList().forEach { map ->
                    item(key = "map_$map") {
                        FolkPreference(
                            title = map,
                            icon = Icons.Default.Security,
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    onEditSusMap?.let { edit ->
                                        FolkRowIconButton(
                                            icon = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit),
                                            tint = MaterialTheme.colorScheme.primary,
                                            enabled = !isLoading,
                                            onClick = { edit(map) },
                                        )
                                    }
                                    FolkRowIconButton(
                                        icon = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        enabled = !isLoading,
                                        onClick = { onRemoveSusMap(map) },
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
        primaryButtonText = stringResource(R.string.add),
        onPrimaryClick = onAddSusMap,
        isLoading = isLoading,
    )

    if (onReset != null && susMaps.isNotEmpty()) {
        ResetButton(
            title = stringResource(R.string.susfs_reset_sus_maps_title),
            onClick = onReset,
        )
    }
}
