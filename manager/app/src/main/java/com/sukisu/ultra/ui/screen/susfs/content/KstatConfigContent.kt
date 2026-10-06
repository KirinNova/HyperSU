package com.sukisu.ultra.ui.screen.susfs.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Update
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

/**
 * The Kstat Configuration tab, in the FolkPatch design: the description, the
 * static configurations, the managed paths with their update actions, and the
 * two add buttons.
 */
@Composable
fun KstatConfigContent(
    kstatConfigs: Set<String>,
    addKstatPaths: Set<String>,
    isLoading: Boolean,
    onAddKstatStatically: () -> Unit,
    onAddKstat: () -> Unit,
    onRemoveKstatConfig: (String) -> Unit,
    onEditKstatConfig: ((String) -> Unit)? = null,
    onRemoveAddKstat: (String) -> Unit,
    onEditAddKstat: ((String) -> Unit)? = null,
    onUpdateKstat: (String) -> Unit,
    onUpdateKstatFullClone: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        FolkDescriptionGroup(
            title = stringResource(R.string.kstat_config_description_title),
            description = stringResource(R.string.kstat_config_description_add_statically) + "\n" +
                stringResource(R.string.kstat_config_description_add) + "\n" +
                stringResource(R.string.kstat_config_description_update) + "\n" +
                stringResource(R.string.kstat_config_description_update_full_clone),
        )

        if (kstatConfigs.isNotEmpty()) {
            FolkCountedSection(
                title = stringResource(R.string.static_kstat_config),
                count = kstatConfigs.size,
            ) {
                kstatConfigs.toList().forEach { config ->
                    val parts = config.split("|")
                    item(key = "kstat_$config") {
                        FolkPreference(
                            title = parts.firstOrNull() ?: config,
                            icon = Icons.Default.Settings,
                            summary = if (parts.size > 1) {
                                parts.drop(1).joinToString(" ")
                            } else {
                                null
                            },
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    onEditKstatConfig?.let { edit ->
                                        FolkRowIconButton(
                                            icon = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit),
                                            tint = MaterialTheme.colorScheme.primary,
                                            enabled = !isLoading,
                                            onClick = { edit(config) },
                                        )
                                    }
                                    FolkRowIconButton(
                                        icon = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        enabled = !isLoading,
                                        onClick = { onRemoveKstatConfig(config) },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        if (addKstatPaths.isNotEmpty()) {
            FolkCountedSection(
                title = stringResource(R.string.kstat_path_management),
                count = addKstatPaths.size,
            ) {
                addKstatPaths.toList().forEach { path ->
                    item(key = "kstat_path_$path") {
                        FolkPreference(
                            title = path,
                            icon = Icons.Default.Folder,
                            trailing = {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    onEditAddKstat?.let { edit ->
                                        FolkRowIconButton(
                                            icon = Icons.Default.Edit,
                                            contentDescription = stringResource(R.string.edit),
                                            tint = MaterialTheme.colorScheme.primary,
                                            enabled = !isLoading,
                                            onClick = { edit(path) },
                                        )
                                    }
                                    FolkRowIconButton(
                                        icon = Icons.Default.Update,
                                        contentDescription = stringResource(R.string.update),
                                        tint = MaterialTheme.colorScheme.secondary,
                                        enabled = !isLoading,
                                        onClick = { onUpdateKstat(path) },
                                    )
                                    FolkRowIconButton(
                                        icon = Icons.Default.PlayArrow,
                                        contentDescription = stringResource(R.string.susfs_update_full_clone),
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        enabled = !isLoading,
                                        onClick = { onUpdateKstatFullClone(path) },
                                    )
                                    FolkRowIconButton(
                                        icon = Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete),
                                        tint = MaterialTheme.colorScheme.error,
                                        enabled = !isLoading,
                                        onClick = { onRemoveAddKstat(path) },
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        if (kstatConfigs.isEmpty() && addKstatPaths.isEmpty()) {
            FolkSusfsEmptyState(message = stringResource(R.string.no_kstat_config_message))
        }
    }

    BottomActionButtons(
        primaryButtonText = stringResource(R.string.add_kstat_path_title),
        onPrimaryClick = onAddKstat,
        secondaryButtonText = stringResource(R.string.add_kstat_statically_title),
        onSecondaryClick = onAddKstatStatically,
        isLoading = isLoading,
    )
}
