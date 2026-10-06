package com.sukisu.ultra.ui.navigation

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Pager controller for the four main tabs. Moved out of the deleted
 * `ui/component/bottombar/BottomBar.kt` (behaviour, not design) so the new
 * FolkPatch-styled navigation shell can drive it unchanged.
 */
class MainPagerState(
    val pagerState: PagerState,
    private val coroutineScope: CoroutineScope,
    private val animatePageChanges: Boolean,
) {
    var selectedPage by mutableIntStateOf(pagerState.currentPage)
        private set

    var isNavigating by mutableStateOf(false)
        private set

    private var navJob: Job? = null

    fun animateToPage(targetIndex: Int) {
        if (targetIndex == selectedPage) return

        navJob?.cancel()

        selectedPage = targetIndex
        isNavigating = true

        navJob = coroutineScope.launch {
            val myJob = coroutineContext.job
            try {
                if (animatePageChanges) {
                    pagerState.animateScrollToPage(targetIndex)
                } else {
                    pagerState.scrollToPage(targetIndex)
                }
            } finally {
                if (navJob == myJob) {
                    isNavigating = false
                    if (pagerState.currentPage != targetIndex) {
                        selectedPage = pagerState.currentPage
                    }
                }
            }
        }
    }

    fun syncPage() {
        if (!isNavigating && selectedPage != pagerState.currentPage) {
            selectedPage = pagerState.currentPage
        }
    }
}

@Composable
fun rememberMainPagerState(
    pagerState: PagerState,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    animatePageChanges: Boolean = true,
): MainPagerState {
    return remember(pagerState, coroutineScope, animatePageChanges) {
        MainPagerState(pagerState, coroutineScope, animatePageChanges)
    }
}

/** Badge counts shown on the navigation items. */
@Immutable
data class NavigationBadgeState(
    val superuserCount: Int = 0,
    val moduleEnabledCount: Int = 0,
    val moduleUpdatableCount: Int = 0,
)

internal enum class BadgeTone { Alert, Accent }

internal data class NavBadge(val count: Int, val tone: BadgeTone)

internal fun badgeFor(index: Int, state: NavigationBadgeState): NavBadge? = when (index) {
    BottomBarDestination.SuperUser.ordinal ->
        state.superuserCount.takeIf { it > 0 }?.let { NavBadge(it, BadgeTone.Accent) }

    BottomBarDestination.Module.ordinal -> when {
        state.moduleUpdatableCount > 0 -> NavBadge(state.moduleUpdatableCount, BadgeTone.Alert)
        state.moduleEnabledCount > 0 -> NavBadge(state.moduleEnabledCount, BadgeTone.Accent)
        else -> null
    }

    else -> null
}
