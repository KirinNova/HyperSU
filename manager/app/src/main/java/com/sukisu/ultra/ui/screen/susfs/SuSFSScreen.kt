package com.sukisu.ultra.ui.screen.susfs

import androidx.compose.runtime.Composable

/**
 * The SuSFS entry point. There is one design now, so this only delegates to the
 * Folk implementation; the signature is unchanged for its callers.
 */
@Composable
fun SuSFSScreen() {
    SuSFSFolk()
}
