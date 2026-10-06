package com.sukisu.ultra.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import com.sukisu.ultra.ksuApp

/**
 * The user's own page background: one image picked from storage, drawn behind every screen.
 *
 * Persisted in the same settings preferences as the rest of the appearance options, but exposed
 * as Compose state so a change made on the settings page is visible to the wallpaper on the
 * screen the user is standing on without an app restart.
 *
 * The properties are read-only on purpose - they are the state the rest of the app observes, and
 * every write goes through a named function that persists it in the same step. Leaving them as
 * writable properties would put a second, non-persisting way to change them within reach.
 *
 * The values are read on first access rather than at object construction: this object is first
 * touched from composition, long after the application exists, but no later than it.
 */
object BackgroundConfig {

    private const val PREFS = "settings"
    private const val KEY_ENABLED = "background_enabled"
    private const val KEY_URI = "background_uri"
    private const val KEY_BLUR = "background_blur"
    private const val KEY_DIM = "background_dim"

    /**
     * Dimming that ships as the default.
     *
     * The wallpaper is user art, so anything stronger than a light veil would be deciding the
     * user's picture for them; anything weaker leaves dark text on dark corners.
     */
    const val DEFAULT_DIM = 0.35f

    private val prefs: SharedPreferences
        get() = ksuApp.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var loaded = false

    private val enabledState = mutableStateOf(false)
    private val uriState = mutableStateOf("")
    private val blurState = mutableStateOf(0f)
    private val dimState = mutableStateOf(DEFAULT_DIM)

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        enabledState.value = prefs.getBoolean(KEY_ENABLED, false)
        uriState.value = prefs.getString(KEY_URI, null).orEmpty()
        blurState.value = prefs.getFloat(KEY_BLUR, 0f)
        dimState.value = prefs.getFloat(KEY_DIM, DEFAULT_DIM)
    }

    /** Whether the background is switched on at all. */
    val enabled: Boolean
        get() {
            ensureLoaded()
            return enabledState.value
        }

    /** Persisted URI string of the picked image; empty when nothing is chosen yet. */
    val uri: String
        get() {
            ensureLoaded()
            return uriState.value
        }

    /** Blur radius in dp, 0f for a sharp picture. */
    val blur: Float
        get() {
            ensureLoaded()
            return blurState.value
        }

    /** Black veil laid over the image, 0f..1f. */
    val dim: Float
        get() {
            ensureLoaded()
            return dimState.value
        }

    /**
     * True when there is a picture to draw.
     *
     * This is the single gate every caller branches on - including the scheme, which has to know
     * before it picks a background colour whether something else is about to show through it.
     */
    val isActive: Boolean
        get() = enabled && uri.isNotEmpty()

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        enabledState.value = value
        prefs.edit { putBoolean(KEY_ENABLED, value) }
    }

    fun setUri(value: String) {
        ensureLoaded()
        uriState.value = value
        prefs.edit { putString(KEY_URI, value) }
    }

    fun setBlur(value: Float) {
        ensureLoaded()
        val clamped = value.coerceIn(0f, 40f)
        blurState.value = clamped
        prefs.edit { putFloat(KEY_BLUR, clamped) }
    }

    fun setDim(value: Float) {
        ensureLoaded()
        val clamped = value.coerceIn(0f, 1f)
        dimState.value = clamped
        prefs.edit { putFloat(KEY_DIM, clamped) }
    }

    /** Drop the picture but keep the switches, so the user can come back to the same setup. */
    fun clearUri() = setUri("")
}
