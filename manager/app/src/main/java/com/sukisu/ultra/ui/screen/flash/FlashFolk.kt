package com.sukisu.ultra.ui.screen.flash

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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SmallExtendedFloatingActionButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.R
import com.sukisu.ultra.ui.component.KeyEventBlocker
import com.sukisu.ultra.ui.component.folk.FolkLogCard
import com.sukisu.ultra.ui.component.folk.FolkLogText
import com.sukisu.ultra.ui.component.folk.FolkScaffold

/**
 * The flashing progress screen in the FolkPatch design: the live log of the
 * flash streamed into a monospace card, with a save action and, once the flash
 * succeeds, a reboot action.
 *
 * The volume keys stay blocked for the duration so a stray press cannot
 * interrupt a write, and the log keeps auto-scrolling to its end as lines
 * arrive. The jailbreak warning is the shared confirm dialog.
 */
@Composable
fun FlashScreenFolk(
    state: FlashUiState,
    actions: FlashScreenActions,
    snackBarHost: SnackbarHostState,
) {
    val scrollState = rememberScrollState()

    if (state.showJailbreakWarning) {
        JailbreakFlashWarningDialog(
            onConfirm = actions.onConfirmJailbreakWarning,
            onDismiss = actions.onDismissJailbreakWarning,
        )
    }

    FolkScaffold(
        title = stringResource(
            when (state.flashingStatus) {
                FlashingStatus.FLASHING -> R.string.flashing
                FlashingStatus.SUCCESS -> R.string.flash_success
                FlashingStatus.FAILED -> R.string.flash_failed
            }
        ),
        onBack = actions.onBack,
        snackbarHostState = snackBarHost,
        actions = {
            IconButton(onClick = actions.onSaveLog) {
                Icon(Icons.Filled.Save, stringResource(R.string.save_log))
            }
        },
        floatingActionButton = {
            if (state.showRebootAction) {
                SmallExtendedFloatingActionButton(
                    onClick = actions.onReboot,
                    icon = { Icon(Icons.Filled.Refresh, null) },
                    text = { Text(stringResource(state.rebootLabelRes)) },
                    modifier = Modifier.padding(
                        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                    ),
                )
            }
        },
    ) { innerPadding ->
        KeyEventBlocker {
            it.key == Key.VolumeDown || it.key == Key.VolumeUp
        }

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
                    16.dp + 54.dp +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
            )
        }
    }
}
