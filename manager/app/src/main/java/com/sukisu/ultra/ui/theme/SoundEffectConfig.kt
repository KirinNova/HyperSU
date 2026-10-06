package com.sukisu.ultra.ui.theme

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * Click / startup sound preferences, ported from FolkPatch `ui/theme/SoundEffectConfig.kt`.
 *
 * Presets are read straight out of `assets/`, custom files are copied into
 * `filesDir/sound_effects` - a content URI would not outlive the granting app.
 */
object SoundEffectConfig {
    private const val PREFS_NAME = "sound_effect_settings"
    private const val KEY_ENABLED = "sound_effect_enabled"
    private const val KEY_FILENAME = "sound_effect_filename"
    private const val KEY_SCOPE = "sound_effect_scope" // "global" or "bottom_bar"
    private const val KEY_SOURCE_TYPE = "sound_effect_source_type"
    private const val KEY_PRESET_NAME = "sound_effect_preset_name"
    private const val KEY_STARTUP_ENABLED = "startup_sound_enabled"
    private const val KEY_STARTUP_FILENAME = "startup_sound_filename"
    private const val KEY_STARTUP_SOURCE_TYPE = "startup_sound_source_type"
    private const val KEY_STARTUP_PRESET_NAME = "startup_sound_preset_name"
    private const val TAG = "SoundEffectConfig"

    const val SCOPE_GLOBAL = "global"
    const val SCOPE_BOTTOM_BAR = "bottom_bar"

    const val SOURCE_TYPE_LOCAL = "local"
    const val SOURCE_TYPE_PRESET = "preset"

    /** Bundled click sounds; the files live in `assets/sound/<name>.wav`. */
    val PRESETS = listOf("Zako", "Zako2", "Imoi", "Ehe", "Baka", "Ciallo")

    /** Bundled startup sounds; the files live in `assets/start/<name>.wav`. */
    val STARTUP_PRESETS = listOf("Zako", "Hentai")

    var isSoundEffectEnabled: Boolean by mutableStateOf(false)
        private set

    var soundEffectFilename: String? by mutableStateOf(null)
        private set

    var scope: String by mutableStateOf(SCOPE_GLOBAL)
        private set

    var sourceType: String by mutableStateOf(SOURCE_TYPE_LOCAL)
        private set

    var presetName: String by mutableStateOf(PRESETS[0])
        private set

    var isStartupSoundEnabled: Boolean by mutableStateOf(false)
        private set

    var startupSoundFilename: String? by mutableStateOf(null)
        private set

    var startupSourceType: String by mutableStateOf(SOURCE_TYPE_LOCAL)
        private set

    var startupPresetName: String by mutableStateOf(STARTUP_PRESETS[0])
        private set

    fun setEnabledState(enabled: Boolean) {
        isSoundEffectEnabled = enabled
    }

    fun setFilenameValue(filename: String?) {
        soundEffectFilename = filename
    }

    fun setScopeValue(value: String) {
        scope = value
    }

    fun setSourceTypeValue(value: String) {
        sourceType = value
    }

    fun setPresetNameValue(value: String) {
        presetName = value
    }

    fun setStartupEnabledState(enabled: Boolean) {
        isStartupSoundEnabled = enabled
    }

    fun setStartupFilenameValue(filename: String?) {
        startupSoundFilename = filename
    }

    fun setStartupSourceTypeValue(value: String) {
        startupSourceType = value
    }

    fun setStartupPresetNameValue(value: String) {
        startupPresetName = value
    }

    fun getSoundEffectDir(context: Context): File {
        val dir = File(context.filesDir, "sound_effects")
        // A plain file with the same name makes mkdirs fail silently, and the next write then
        // hits ENOTDIR; clear the placeholder and rebuild.
        if (!dir.isDirectory) {
            if (dir.exists()) dir.delete()
            dir.mkdirs()
        }
        return dir
    }

    fun getSoundEffectFile(context: Context): File? {
        val filename = soundEffectFilename ?: return null
        return File(getSoundEffectDir(context), filename)
    }

    fun getStartupSoundFile(context: Context): File? {
        val filename = startupSoundFilename ?: return null
        return File(getSoundEffectDir(context), filename)
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isSoundEffectEnabled = prefs.getBoolean(KEY_ENABLED, false)
        soundEffectFilename = prefs.getString(KEY_FILENAME, null)
        scope = prefs.getString(KEY_SCOPE, SCOPE_GLOBAL) ?: SCOPE_GLOBAL
        sourceType = prefs.getString(KEY_SOURCE_TYPE, SOURCE_TYPE_LOCAL) ?: SOURCE_TYPE_LOCAL
        presetName = prefs.getString(KEY_PRESET_NAME, PRESETS[0]) ?: PRESETS[0]

        isStartupSoundEnabled = prefs.getBoolean(KEY_STARTUP_ENABLED, false)
        startupSoundFilename = prefs.getString(KEY_STARTUP_FILENAME, null)
        startupSourceType = prefs.getString(KEY_STARTUP_SOURCE_TYPE, SOURCE_TYPE_LOCAL)
            ?: SOURCE_TYPE_LOCAL
        startupPresetName = prefs.getString(KEY_STARTUP_PRESET_NAME, STARTUP_PRESETS[0])
            ?: STARTUP_PRESETS[0]

        // A config pointing at a file that is no longer there must not leave the toggle on.
        if (isSoundEffectEnabled && sourceType == SOURCE_TYPE_LOCAL && soundEffectFilename != null) {
            if (!File(getSoundEffectDir(context), soundEffectFilename!!).exists()) {
                isSoundEffectEnabled = false
                soundEffectFilename = null
                save(context)
            }
        }

        if (isStartupSoundEnabled && startupSourceType == SOURCE_TYPE_LOCAL &&
            startupSoundFilename != null
        ) {
            if (!File(getSoundEffectDir(context), startupSoundFilename!!).exists()) {
                isStartupSoundEnabled = false
                startupSoundFilename = null
                save(context)
            }
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, isSoundEffectEnabled)
            .putString(KEY_FILENAME, soundEffectFilename)
            .putString(KEY_SCOPE, scope)
            .putString(KEY_SOURCE_TYPE, sourceType)
            .putString(KEY_PRESET_NAME, presetName)
            .putBoolean(KEY_STARTUP_ENABLED, isStartupSoundEnabled)
            .putString(KEY_STARTUP_FILENAME, startupSoundFilename)
            .putString(KEY_STARTUP_SOURCE_TYPE, startupSourceType)
            .putString(KEY_STARTUP_PRESET_NAME, startupPresetName)
            .apply()
    }

    /** Copies a picked sound into filesDir and switches the click effect to it. */
    fun saveSoundEffectFile(context: Context, uri: Uri): Boolean {
        val extension = android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "mp3"
        return copyIntoDir(
            context = context,
            uri = uri,
            filename = "sound_effect_${System.currentTimeMillis()}.$extension",
            oldFilename = soundEffectFilename,
            onSaved = { filename ->
                isSoundEffectEnabled = true
                soundEffectFilename = filename
                sourceType = SOURCE_TYPE_LOCAL
            },
        )
    }

    /** Copies a picked sound into filesDir and switches the startup effect to it. */
    fun saveStartupSoundFile(context: Context, uri: Uri): Boolean {
        val extension = android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "mp3"
        return copyIntoDir(
            context = context,
            uri = uri,
            filename = "startup_sound_${System.currentTimeMillis()}.$extension",
            oldFilename = startupSoundFilename,
            onSaved = { filename ->
                isStartupSoundEnabled = true
                startupSoundFilename = filename
                startupSourceType = SOURCE_TYPE_LOCAL
            },
        )
    }

    private fun copyIntoDir(
        context: Context,
        uri: Uri,
        filename: String,
        oldFilename: String?,
        onSaved: (String) -> Unit,
    ): Boolean {
        val destFile = File(getSoundEffectDir(context), filename)

        val written = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save sound effect file", e)
            false
        }

        if (!written || destFile.length() == 0L) {
            destFile.delete()
            return false
        }

        // Drop the previous file only once the new one is safely on disk.
        if (oldFilename != null && oldFilename != filename) {
            File(getSoundEffectDir(context), oldFilename).delete()
        }

        onSaved(filename)
        save(context)
        return true
    }

    fun clearSoundEffect(context: Context) {
        soundEffectFilename?.let { File(getSoundEffectDir(context), it).delete() }
        isSoundEffectEnabled = false
        soundEffectFilename = null
        save(context)
    }

    fun clearStartupSound(context: Context) {
        startupSoundFilename?.let { File(getSoundEffectDir(context), it).delete() }
        isStartupSoundEnabled = false
        startupSoundFilename = null
        save(context)
    }
}
