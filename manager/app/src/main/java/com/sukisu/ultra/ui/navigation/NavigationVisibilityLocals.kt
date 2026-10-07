package com.sukisu.ultra.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

val LocalBottomBarVisible = compositionLocalOf { mutableStateOf(true) }
val LocalIsFloatingNavMode = compositionLocalOf { false }

/**
 * True for the four tab pages that live directly in [Route.Main].
 *
 * It decides whether a page paints its own opaque background. A tab page must not: the nav host
 * draws it straight over the wallpaper, and an opaque container there hides the image. A pushed
 * page must, because the nav host keeps the entry underneath composed for its card transition
 * and swipe-back gesture, so a transparent page would show the previous screen's text through it.
 *
 * Defaults to false so a page that forgets to declare itself is treated as pushed - erring
 * towards covering the screen beneath rather than leaking it.
 */
val LocalIsRootPage = compositionLocalOf { false }
