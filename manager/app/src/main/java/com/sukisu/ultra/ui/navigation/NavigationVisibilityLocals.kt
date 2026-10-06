package com.sukisu.ultra.ui.navigation

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf

val LocalBottomBarVisible = compositionLocalOf { mutableStateOf(true) }
val LocalIsFloatingNavMode = compositionLocalOf { false }
