package com.sukisu.ultra.ui.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The language switch that is in flight, if any.
 *
 * Applying a language recreates the activity, and that recreation is the black
 * frame the user sees. Recording the target here lets the activity that is about
 * to be torn down - and the one that replaces it - draw the switch page instead
 * of the app until the new locale is in place.
 */
object LanguageSwitchState {
    /** The tag being switched to, or null when no switch is in flight. */
    var targetTag: String? by mutableStateOf(null)
        private set

    fun begin(tag: String) {
        targetTag = tag
    }

    fun finish() {
        targetTag = null
    }
}
