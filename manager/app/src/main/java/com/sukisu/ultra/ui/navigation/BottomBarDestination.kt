package com.sukisu.ultra.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.ui.graphics.vector.ImageVector
import com.sukisu.ultra.R

/**
 * Main-tab destinations of the bottom navigation, ported from FolkPatch
 * `ui/navigation/BottomBarDestination.kt` and mapped onto HyperSU's four
 * pager tabs (Home / SuperUser / Module / Settings).
 *
 * [pageIndex] is the tab index inside the main HorizontalPager.
 */
enum class BottomBarDestination(
    @param:StringRes val label: Int,
    val iconSelected: ImageVector,
    val iconNotSelected: ImageVector,
    val pageIndex: Int,
) {
    Home(
        R.string.home,
        Icons.Rounded.Cottage,
        Icons.Outlined.Cottage,
        0,
    ),
    SuperUser(
        R.string.superuser,
        Icons.Rounded.AdminPanelSettings,
        Icons.Outlined.AdminPanelSettings,
        1,
    ),
    Module(
        R.string.module,
        Icons.Rounded.Extension,
        Icons.Outlined.Extension,
        2,
    ),
    Settings(
        R.string.settings,
        Icons.Rounded.Tune,
        Icons.Outlined.Tune,
        3,
    );

    companion object {
        /** Tab count of the main pager. */
        const val PAGE_COUNT = 4
    }
}
