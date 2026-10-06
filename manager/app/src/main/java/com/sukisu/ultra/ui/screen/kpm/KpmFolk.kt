package com.sukisu.ultra.ui.screen.kpm

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.folk.FolkAlertDialog
import com.sukisu.ultra.ui.component.folk.FolkButtonDefaults
import com.sukisu.ultra.ui.component.folk.FolkScaffold
import com.sukisu.ultra.ui.component.folk.FolkStateView
import com.sukisu.ultra.ui.theme.tokens.FolkShape
import com.sukisu.ultra.ui.theme.tokens.FolkType
import com.sukisu.ultra.ui.viewmodel.KpmViewModel
import kotlinx.coroutines.delay

/**
 * The kernel module (KPM) list, in the FolkPatch design.
 *
 * Each module is a Folk surface with its name, version, author and args, an
 * expanding-by-ellipsis description, and the control/uninstall actions. The
 * dismissible notice, the install-mode choice and the control-args dialog are
 * all Folk now. The five-second refresh loop is the caller's; this screen only
 * reports through [KpmActions].
 */
@Composable
fun KpmFolk(
    viewModel: KpmViewModel,
    actions: KpmActions,
    bottomInnerPadding: Dp = 0.dp,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val showEmptyState by remember {
        derivedStateOf {
            state.moduleList.isEmpty() &&
                state.searchStatus.searchText.isEmpty() &&
                !state.isRefreshing
        }
    }

    val sharedPreferences = remember(context) {
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
    }
    var isNoticeClosed by remember {
        mutableStateOf(sharedPreferences.getBoolean("is_notice_closed", false))
    }

    var isRefreshing by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            delay(350)
            actions.onRefresh()
            isRefreshing = false
        }
    }

    if (state.showInstallModeDialog) {
        FolkAlertDialog(
            onDismissRequest = actions.onDismissInstallDialog,
            width = 340.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.kpm_install_mode),
                    style = FolkType.Title,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp),
                )

                state.tempModuleName?.let {
                    Text(
                        text = stringResource(R.string.kpm_install_mode_description, it),
                        style = FolkType.Summary,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { actions.onConfirmInstall("", false) },
                        colors = FolkButtonDefaults.filledColors(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.kpm_install_mode_load))
                    }

                    Button(
                        onClick = { actions.onConfirmInstall("", true) },
                        colors = FolkButtonDefaults.tonalColors(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.kpm_install_mode_embed))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = actions.onDismissInstallDialog,
                        colors = FolkButtonDefaults.textColors(),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }

    FolkScaffold(
        title = stringResource(R.string.kpm_title),
        actions = {
            IconButton(onClick = actions.onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.refresh),
                )
            }
        },
        floatingActionButton = {
            SmallFloatingActionButton(
                onClick = actions.onRequestInstall,
                modifier = Modifier.padding(bottom = bottomInnerPadding),
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = stringResource(R.string.kpm_install),
                )
            }
        },
    ) { innerPadding ->
        if (showEmptyState) {
            FolkStateView(
                title = stringResource(R.string.kpm_empty),
                hint = stringResource(R.string.kpm_notice),
                icon = Icons.Outlined.Memory,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding()),
            )
            return@FolkScaffold
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { if (!isRefreshing) isRefreshing = true },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxHeight(),
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 16.dp + innerPadding.calculateBottomPadding() + bottomInnerPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!isNoticeClosed) {
                    item {
                        androidx.compose.material3.Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = FolkShape.Corner16,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .size(24.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                )

                                Text(
                                    text = stringResource(R.string.kernel_module_notice),
                                    modifier = Modifier.weight(1f),
                                    style = FolkType.Summary,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )

                                IconButton(
                                    onClick = {
                                        isNoticeClosed = true
                                        sharedPreferences.edit().putBoolean("is_notice_closed", true).apply()
                                    },
                                    modifier = Modifier.size(24.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = stringResource(R.string.close_notice),
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }

                items(state.moduleList, key = { it.id }) { module ->
                    KpmModuleRow(
                        module = module,
                        state = state,
                        actions = actions,
                        onUninstall = { actions.onRequestUninstall(module.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun KpmModuleRow(
    module: KpmViewModel.ModuleInfo,
    state: KpmUiState,
    actions: KpmActions,
    onUninstall: () -> Unit,
) {
    val showInputDialog = state.inputDialogState.visible &&
        state.inputDialogState.moduleId == module.id

    if (showInputDialog) {
        FolkAlertDialog(
            onDismissRequest = actions.onHideInputDialog,
            width = 340.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(R.string.kpm_control),
                    style = FolkType.Title,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )

                OutlinedTextField(
                    value = state.inputDialogState.args,
                    onValueChange = actions.onInputArgsChange,
                    label = { Text(stringResource(R.string.kpm_args)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                if (state.inputDialogState.args.isEmpty() && module.args.isNotEmpty()) {
                    Text(
                        text = module.args,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = FolkType.Caption,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = actions.onHideInputDialog,
                        colors = FolkButtonDefaults.textColors(),
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    Button(
                        onClick = actions.onExecuteControl,
                        colors = FolkButtonDefaults.filledColors(),
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }

    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FolkShape.Corner20,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = module.name,
                style = FolkType.Title,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${stringResource(R.string.kpm_version)}: ${module.version}",
                style = FolkType.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${stringResource(R.string.kpm_author)}: ${module.author}",
                style = FolkType.Caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (module.args.isNotEmpty()) {
                Text(
                    text = "${stringResource(R.string.kpm_args)}: ${module.args}",
                    style = FolkType.Caption,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (module.description.isNotBlank()) {
                Text(
                    text = module.description,
                    style = FolkType.Summary,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 4,
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                thickness = 0.5.dp,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(
                    visible = module.hasAction,
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    IconButton(onClick = { actions.onShowInputDialog(module.id) }) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.kpm_control),
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = onUninstall,
                    colors = FolkButtonDefaults.tonalColors(),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.kpm_uninstall))
                }
            }
        }
    }
}
