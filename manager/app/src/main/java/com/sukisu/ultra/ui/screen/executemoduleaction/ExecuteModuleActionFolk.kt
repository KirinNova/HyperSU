package com.sukisu.ultra.ui.screen.executemoduleaction

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.folk.FolkLogCard
import com.sukisu.ultra.ui.component.folk.FolkLogText
import com.sukisu.ultra.ui.component.folk.FolkScaffold

/**
 * The module action-script output, in the FolkPatch design.
 *
 * The streamed output goes into the shared monospace log card and auto-scrolls
 * as it grows. The behaviour is unchanged: saving the log is still offered, and
 * when the screen was opened from a shortcut it closes itself rather than
 * leaving a task behind.
 */
@Composable
fun ExecuteModuleActionScreenFolk(
    state: ExecuteModuleActionUiState,
    actions: ExecuteModuleActionScreenActions,
    snackBarHost: SnackbarHostState,
) {
    val scrollState = rememberScrollState()

    FolkScaffold(
        title = stringResource(R.string.action),
        onBack = actions.onBack,
        snackbarHostState = snackBarHost,
        actions = {
            IconButton(onClick = actions.onSaveLog) {
                Icon(Icons.Filled.Save, stringResource(R.string.save_log))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
        ) {
            LaunchedEffect(state.text) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            FolkLogCard(modifier = Modifier.padding(top = 8.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FolkLogText(text = state.text)
                }
            }

            Spacer(
                Modifier.height(
                    16.dp +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }
}
