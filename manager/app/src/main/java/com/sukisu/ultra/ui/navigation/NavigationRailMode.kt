package com.sukisu.ultra.ui.navigation

import androidx.compose.runtime.Composable
import com.sukisu.ultra.ui.util.shouldShowSplitPane

/**
 * Whether the shell should present the destination list as a rail rather than a
 * bottom bar: only on a large enough window, and never when the floating capsule
 * is in use (the capsule always stays at the bottom).
 */
@Composable
fun useNavigationRail(enableFloatingBottomBar: Boolean): Boolean {
    return shouldShowSplitPane() && !enableFloatingBottomBar
}
