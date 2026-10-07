package com.sukisu.ultra.ui.component.folk

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import com.sukisu.ultra.ui.theme.tokens.FolkShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.sukisu.ultra.ui.util.APDialogBlurBehindUtils

/**
 * The frame every [BasicAlertDialog] in the app shares: a fixed-width rounded surface with the
 * wallpaper blur set up behind it. A dialog supplies only its own column, and the shape and the
 * blur live here so they cannot drift apart from dialog to dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolkAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 310.dp,
    shape: Shape = FolkShape.Dialog,
    blurBehind: Boolean = true,
    dialogProperties: DialogProperties = DialogProperties(
        decorFitsSystemWindows = true,
        usePlatformDefaultWidth = false,
    ),
    content: @Composable () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        properties = dialogProperties,
    ) {
        Surface(
            modifier = modifier
                .width(width)
                .wrapContentHeight(),
            shape = shape,
            tonalElevation = AlertDialogDefaults.TonalElevation,
            color = AlertDialogDefaults.containerColor,
        ) {
            content()

            if (blurBehind) {
                val dialogWindowProvider = LocalView.current.parent as DialogWindowProvider
                APDialogBlurBehindUtils.setupWindowBlurListener(dialogWindowProvider.window)
            }
        }
    }
}
