package com.sukisu.ultra.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import com.sukisu.ultra.R
import java.io.File

/**
 * How the application renders text.
 *
 * [APP_DEFAULT] uses the font bundled with the manager, [SYSTEM_DEFAULT] leaves the platform
 * font in place and [CUSTOM] uses a font the user imported.
 */
enum class FontMode {
    APP_DEFAULT,
    SYSTEM_DEFAULT,
    CUSTOM;

    /** Value written to prefs; kept stable for cross-version compatibility. */
    val serializedName: String
        get() = when (this) {
            APP_DEFAULT -> "app"
            SYSTEM_DEFAULT -> "system"
            CUSTOM -> "custom"
        }

    companion object {
        fun fromName(name: String?): FontMode? = entries.firstOrNull { it.name == name }

        fun fromSerializedName(value: String?): FontMode? = when (value) {
            "app" -> APP_DEFAULT
            "system" -> SYSTEM_DEFAULT
            "custom" -> CUSTOM
            else -> null
        }
    }
}

/**
 * The user's typeface choice, applied to [getTypography].
 *
 * State lives here so a font change recomposes the theme immediately without a restart, and the
 * file is kept in `filesDir` so it survives on every storage path the app runs on.
 */
object FontConfig {
    private const val PREFS_NAME = "font_settings"
    private const val KEY_FONT_MODE = "font_mode"
    private const val KEY_CUSTOM_FONT_ENABLED = "custom_font_enabled"
    private const val KEY_CUSTOM_FONT_PATH = "custom_font_path"
    private const val TAG = "FontConfig"

    var fontMode: FontMode by mutableStateOf(FontMode.SYSTEM_DEFAULT)
        private set

    var isCustomFontEnabled: Boolean by mutableStateOf(false)
        private set

    var customFontFilename: String? by mutableStateOf(null)
        private set

    /** Selects a font mode and persists it. Only [FontMode.CUSTOM] keeps a user file. */
    fun setFontMode(context: Context, mode: FontMode) {
        fontMode = mode
        isCustomFontEnabled = mode == FontMode.CUSTOM
        save(context)
    }

    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        customFontFilename = prefs.getString(KEY_CUSTOM_FONT_PATH, null)

        // Migrate the legacy boolean to the three-mode setting when font_mode was never written:
        // an enabled custom font becomes CUSTOM, everything else keeps the platform font.
        val storedMode = FontMode.fromName(prefs.getString(KEY_FONT_MODE, null))
        val legacyEnabled = prefs.getBoolean(KEY_CUSTOM_FONT_ENABLED, false)
        fontMode = storedMode ?: if (legacyEnabled) FontMode.CUSTOM else FontMode.SYSTEM_DEFAULT
        isCustomFontEnabled = fontMode == FontMode.CUSTOM
        var needsPersist = storedMode == null

        // A missing custom file must not leave the app in a broken state: fall back to the
        // platform font rather than rendering everything with a null typeface.
        if (isCustomFontEnabled) {
            val file = customFontFilename?.let { File(context.filesDir, it) }
            if (file == null || !file.exists()) {
                customFontFilename = null
                fontMode = FontMode.SYSTEM_DEFAULT
                isCustomFontEnabled = false
                needsPersist = true
            }
        }

        if (needsPersist) {
            save(context)
        }
    }

    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_FONT_MODE, fontMode.name)
            .putBoolean(KEY_CUSTOM_FONT_ENABLED, isCustomFontEnabled)
            .putString(KEY_CUSTOM_FONT_PATH, customFontFilename)
            .apply()
    }

    /** Copies a picked font into filesDir and switches to [FontMode.CUSTOM]. */
    fun saveFontFile(context: Context, uri: Uri): Boolean {
        val newFilename = "custom_font_${System.currentTimeMillis()}.ttf"
        val oldFilename = customFontFilename
        val destFile = File(context.filesDir, newFilename)

        val written = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            } != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save font file", e)
            false
        }

        if (!written || destFile.length() == 0L) {
            destFile.delete()
            return false
        }

        adoptFontFile(context, newFilename, oldFilename)
        return true
    }

    /**
     * 采用一个**已经落在 filesDir 里**的字体文件（主题导入解压出来的那份）。
     *
     * 与 [saveFontFile] 的区别只在来源：这里没有 content URI 可读，文件已经到位，所以只做
     * 「换名 → 删旧文件 → 切到 CUSTOM」。
     */
    fun applyCustomFont(context: Context, sourceFile: File): Boolean {
        if (!sourceFile.isFile || sourceFile.length() == 0L) return false

        val newFilename = "custom_font_${System.currentTimeMillis()}.ttf"
        val oldFilename = customFontFilename
        val destFile = File(context.filesDir, newFilename)

        val copied = try {
            sourceFile.copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adopt imported font", e)
            false
        }

        if (!copied) {
            destFile.delete()
            return false
        }

        // 归档里那份只是中转，采用后删掉，避免 filesDir 根目录留两份同名语义的字体。
        if (sourceFile.absolutePath != destFile.absolutePath) sourceFile.delete()
        adoptFontFile(context, newFilename, oldFilename)
        return true
    }

    /** 切换状态、删旧文件、清缓存并持久化。两条采用路径共用。 */
    private fun adoptFontFile(
        context: Context,
        newFilename: String,
        oldFilename: String?,
    ) {
        // Drop the previous file only once the new one is safely on disk.
        if (oldFilename != null && oldFilename != newFilename) {
            File(context.filesDir, oldFilename).delete()
        }

        fontMode = FontMode.CUSTOM
        isCustomFontEnabled = true
        customFontFilename = newFilename
        invalidateFontCache()
        save(context)
    }

    /** Removing the imported font returns to the app's bundled default. */
    fun clearFont(context: Context) {
        customFontFilename?.let { File(context.filesDir, it).delete() }
        customFontFilename = null
        fontMode = FontMode.APP_DEFAULT
        isCustomFontEnabled = false
        invalidateFontCache()
        save(context)
    }

    // Font cache so Typeface.createFromFile / resource lookups do not run on every recomposition.
    private var cachedFontFamily: FontFamily? = null
    private var cachedFilename: String? = null
    private var cachedMode: FontMode? = null

    fun getFontFamily(context: Context): FontFamily {
        if (fontMode == cachedMode && customFontFilename == cachedFilename && cachedFontFamily != null) {
            return cachedFontFamily!!
        }

        val family = when (fontMode) {
            FontMode.APP_DEFAULT -> loadBundledFont(context)
            FontMode.SYSTEM_DEFAULT -> FontFamily.Default
            FontMode.CUSTOM -> loadCustomFont(context)
        }

        cachedMode = fontMode
        cachedFilename = customFontFilename
        cachedFontFamily = family
        return family
    }

    private fun loadBundledFont(context: Context): FontFamily = try {
        ResourcesCompat.getFont(context, R.font.xiaolai)?.let { FontFamily(it) } ?: FontFamily.Default
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load bundled font", e)
        FontFamily.Default
    }

    private fun loadCustomFont(context: Context): FontFamily {
        val filename = customFontFilename ?: return FontFamily.Default
        val file = File(context.filesDir, filename)
        if (!file.exists()) return FontFamily.Default
        return try {
            FontFamily(Typeface.createFromFile(file))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom font", e)
            FontFamily.Default
        }
    }

    fun invalidateFontCache() {
        cachedFilename = null
        cachedFontFamily = null
        cachedMode = null
    }
}
