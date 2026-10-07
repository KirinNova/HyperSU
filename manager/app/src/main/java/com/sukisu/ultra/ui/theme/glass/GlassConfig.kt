package com.sukisu.ultra.ui.theme.glass

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import com.sukisu.ultra.ksuApp

/**
 * The knobs on the liquid glass, in one persisted object.
 *
 * Same contract as [com.sukisu.ultra.ui.theme.BackgroundConfig]: read-only properties the rest
 * of the app observes, every write through a named function that persists in the same step,
 * values loaded on first access because composition touches this before anything else does.
 *
 * [revision] is not persisted - it exists so [rememberGlassSpec] can rebuild the resolved
 * colours when a knob moves, since a remembered value would otherwise keep the old alphas
 * until the theme changed underneath it.
 */
object GlassConfig {

    private const val PREFS = "settings"
    private const val KEY_ENABLED = "glass_enabled"
    private const val KEY_INTENSITY = "glass_intensity"
    private const val KEY_BLUR = "glass_blur"
    private const val KEY_BLUR_ENABLED = "glass_blur_enabled"
    private const val KEY_RIM = "glass_rim"
    private const val KEY_SPECULAR = "glass_specular"
    private const val KEY_SHEEN = "glass_sheen"

    /**
     * Where the blur slider starts: miuix's own BlurDefaults.BlurRadius, so an untouched
     * setting is exactly the effect the library ships.
     */
    const val DEFAULT_BLUR = 20f

    private val prefs: SharedPreferences
        get() = ksuApp.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private var loaded = false

    private val enabledState = mutableStateOf(true)
    private val intensityState = mutableStateOf(1f)
    private val blurState = mutableStateOf(DEFAULT_BLUR)
    private val blurEnabledState = mutableStateOf(true)
    private val rimState = mutableStateOf(true)
    private val specularState = mutableStateOf(true)
    private val sheenState = mutableStateOf(true)
    private val revisionState = mutableStateOf(0)

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        enabledState.value = prefs.getBoolean(KEY_ENABLED, true)
        intensityState.value = prefs.getFloat(KEY_INTENSITY, 1f)
        blurState.value = prefs.getFloat(KEY_BLUR, DEFAULT_BLUR)
        blurEnabledState.value = prefs.getBoolean(KEY_BLUR_ENABLED, true)
        rimState.value = prefs.getBoolean(KEY_RIM, true)
        specularState.value = prefs.getBoolean(KEY_SPECULAR, true)
        sheenState.value = prefs.getBoolean(KEY_SHEEN, true)
    }

    private fun touched() {
        revisionState.value += 1
    }

    /** The master switch. Off means every plate falls back to a plain clipped surface. */
    val enabled: Boolean
        get() {
            ensureLoaded()
            return enabledState.value
        }

    /** Multiplier on the plate alphas, 0.4f..1.6f. 1f is the shipped look. */
    val intensity: Float
        get() {
            ensureLoaded()
            return intensityState.value
        }

    /** Backdrop blur radius in dp, 0f to turn refraction off while keeping the tint. */
    val blur: Float
        get() {
            ensureLoaded()
            return blurState.value
        }

    /** Whether any plate may blur the backdrop - the app-wide "enable blur" switch. */
    val blurEnabled: Boolean
        get() {
            ensureLoaded()
            return blurEnabledState.value
        }

    /** The hairline rim around each plate. */
    val rim: Boolean
        get() {
            ensureLoaded()
            return rimState.value
        }

    /** The specular wash across the top of each plate. */
    val specular: Boolean
        get() {
            ensureLoaded()
            return specularState.value
        }

    /** The diagonal sheen that breaks the flat fill. */
    val sheen: Boolean
        get() {
            ensureLoaded()
            return sheenState.value
        }

    /** Bumped on every write so remembered specs resolve against the current knobs. */
    val revision: Int
        get() {
            ensureLoaded()
            return revisionState.value
        }

    fun setEnabled(value: Boolean) {
        ensureLoaded()
        enabledState.value = value
        prefs.edit { putBoolean(KEY_ENABLED, value) }
        touched()
    }

    fun setIntensity(value: Float) {
        ensureLoaded()
        val clamped = value.coerceIn(0.4f, 1.6f)
        intensityState.value = clamped
        prefs.edit { putFloat(KEY_INTENSITY, clamped) }
        touched()
    }

    fun setBlur(value: Float) {
        ensureLoaded()
        val clamped = value.coerceIn(0f, 60f)
        blurState.value = clamped
        prefs.edit { putFloat(KEY_BLUR, clamped) }
        touched()
    }

    fun setBlurEnabled(value: Boolean) {
        ensureLoaded()
        blurEnabledState.value = value
        prefs.edit { putBoolean(KEY_BLUR_ENABLED, value) }
        touched()
    }

    fun setRim(value: Boolean) {
        ensureLoaded()
        rimState.value = value
        prefs.edit { putBoolean(KEY_RIM, value) }
        touched()
    }

    fun setSpecular(value: Boolean) {
        ensureLoaded()
        specularState.value = value
        prefs.edit { putBoolean(KEY_SPECULAR, value) }
        touched()
    }

    fun setSheen(value: Boolean) {
        ensureLoaded()
        sheenState.value = value
        prefs.edit { putBoolean(KEY_SHEEN, value) }
        touched()
    }
}
