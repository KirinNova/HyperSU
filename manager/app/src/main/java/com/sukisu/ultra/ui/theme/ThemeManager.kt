package com.sukisu.ultra.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Theme import / export / reset for the manager.
 *
 * A theme file is a ZIP holding `theme_config.json` plus the wallpaper images the current
 * appearance depends on. The JSON carries the appearance keys the manager already persists in
 * the `settings` preferences (`color_mode`, `key_color`, `color_style`, `color_spec`,
 * `home_layout_style`) and the wallpaper keys in [BackgroundConfig.PREFS_NAME], so importing is
 * just writing them back - MainActivityViewModel already listens for those keys and rebuilds the
 * theme as soon as one lands.
 */
object ThemeManager {

    private const val TAG = "ThemeManager"

    private const val SETTINGS_PREFS = "settings"
    private const val CONFIG_ENTRY = "theme_config.json"
    private const val THEME_EXTENSION = "fpt"

    /** Appearance keys exported with a theme, in the order they are shown in settings. */
    val appearanceKeys = listOf(
        "color_mode",
        "key_color",
        "color_style",
        "color_spec",
        "home_layout_style",
    )

    /**
     * Wallpaper preferences that are not URIs. The URIs deliberately stay out: they travel as
     * image entries and are repointed at the extracted copy on import, so listing them here
     * would resurrect a path from the machine the theme was exported on.
     *
     * Derived from the preference file rather than hand-listed, so a wallpaper added later is
     * carried by themes without touching this file.
     */
    private fun readNonUriKeys(prefs: SharedPreferences): JSONObject =
        JSONObject().also { json ->
            prefs.all.forEach { (key, value) ->
                if (key.endsWith("_uri")) return@forEach
                when (value) {
                    null -> Unit
                    is Float -> json.put(key, value.toDouble())
                    else -> json.put(key, value)
                }
            }
        }

    /** 字体/音乐/音效的偏好里没有 URI，键原样带走即可。 */
    private fun readAllKeys(prefs: SharedPreferences): JSONObject =
        JSONObject().also { json ->
            prefs.all.forEach { (key, value) ->
                when (value) {
                    null -> Unit
                    is Float -> json.put(key, value.toDouble())
                    else -> json.put(key, value)
                }
            }
        }

    private fun prefsString(context: Context, file: String, key: String): String? =
        context.getSharedPreferences(file, Context.MODE_PRIVATE)
            .getString(key, null)
            ?.takeIf { it.isNotBlank() }

    /**
     * 应用归档里的字体段落。
     *
     * 顺序很重要：先决定模式，再处理文件。自定义字体只在**归档确实带了文件**时才采用，
     * 否则退回内置字体，而不是留一个指向不存在文件的 CUSTOM 模式（那会让整个界面掉回默认字体
     * 却仍然显示「已启用自定义字体」）。
     */
    private fun applyFontSection(context: Context, section: JSONObject) {
        val modeName = section.optString("font_mode", "")
        val customPath = section.optString("custom_font_path", "").takeIf { it.isNotBlank() }
        val legacyEnabled = section.optBoolean("custom_font_enabled", false)

        val mode = FontMode.fromName(modeName)
            ?: if (legacyEnabled && customPath != null) FontMode.CUSTOM else null

        when (mode) {
            FontMode.SYSTEM_DEFAULT -> FontConfig.setFontMode(context, FontMode.SYSTEM_DEFAULT)
            FontMode.APP_DEFAULT -> FontConfig.setFontMode(context, FontMode.APP_DEFAULT)
            FontMode.CUSTOM -> {
                val file = customPath?.let { File(context.filesDir, it) }
                if (file != null && file.isFile) {
                    FontConfig.applyCustomFont(context, file)
                } else {
                    // 归档声明了自定义字体却没带文件：退回内置字体，别留坏状态。
                    FontConfig.setFontMode(context, FontMode.APP_DEFAULT)
                }
            }
            // 归档没有字体信息：保持用户当前字体不变。
            null -> Unit
        }
    }

    /** 应用归档里的音乐段落；带文件才启用，其余字段照写。 */
    private fun applyMusicSection(context: Context, section: JSONObject) {
        val filename = section.optString("music_filename", "").takeIf { it.isNotBlank() }
        val enabled = section.optBoolean("music_enabled", false)
        val hasFile = filename != null && File(context.filesDir, "music/$filename").isFile

        MusicConfig.setMusicEnabledState(enabled && hasFile)
        MusicConfig.setVolumeValue(section.optDouble("volume", 1.0).toFloat())
        MusicConfig.setAutoPlayEnabledState(section.optBoolean("auto_play", false))
        MusicConfig.setLoopingEnabledState(section.optBoolean("looping_enabled", false))
        MusicConfig.setMusicFilenameValue(if (hasFile) filename else null)
        MusicConfig.save(context)
    }

    /** 应用归档里的音效段落；预设不需要文件，自定义文件缺失时退回预设。 */
    private fun applySoundSection(context: Context, section: JSONObject) {
        val enabled = section.optBoolean("sound_effect_enabled", false)
        val filename = section.optString("sound_effect_filename", "").takeIf { it.isNotBlank() }
        val sourceType = section.optString(
            "sound_effect_source_type",
            SoundEffectConfig.SOURCE_TYPE_PRESET,
        )
        val hasFile = filename != null && File(context.filesDir, "sound_effects/$filename").isFile

        SoundEffectConfig.setEnabledState(enabled)
        SoundEffectConfig.setScopeValue(
            section.optString("sound_effect_scope", SoundEffectConfig.SCOPE_GLOBAL),
        )
        SoundEffectConfig.setSourceTypeValue(
            if (sourceType == SoundEffectConfig.SOURCE_TYPE_LOCAL && !hasFile) {
                SoundEffectConfig.SOURCE_TYPE_PRESET
            } else {
                sourceType
            },
        )
        SoundEffectConfig.setPresetNameValue(
            section.optString("sound_effect_preset_name", SoundEffectConfig.PRESETS[0]),
        )
        SoundEffectConfig.setFilenameValue(if (hasFile) filename else null)

        val startupFilename = section.optString("startup_sound_filename", "").takeIf { it.isNotBlank() }
        val startupHasFile = startupFilename != null &&
            File(context.filesDir, "sound_effects/$startupFilename").isFile
        SoundEffectConfig.setStartupEnabledState(section.optBoolean("startup_sound_enabled", false))
        SoundEffectConfig.setStartupSourceTypeValue(
            section.optString("startup_sound_source_type", SoundEffectConfig.SOURCE_TYPE_PRESET),
        )
        SoundEffectConfig.setStartupPresetNameValue(
            section.optString("startup_sound_preset_name", SoundEffectConfig.STARTUP_PRESETS[0]),
        )
        SoundEffectConfig.setStartupFilenameValue(if (startupHasFile) startupFilename else null)

        SoundEffectConfig.save(context)
    }

    /**
     * 主题要一并带走的二进制文件，返回「归档内条目名 → 本地文件」。
     *
     * 条目名带一级目录（`music/…`、`sound_effects/…`），解压时才落回原位置；字体直接在
     * `filesDir` 根，所以条目名就是文件名。`.distinctBy` 防止点击音效和开机音效指向同一个文件
     * 时往 zip 里写两次同名条目。
     */
    private fun mediaFiles(context: Context): List<Pair<String, File>> = buildList {
        prefsString(context, FONT_PREFS, "custom_font_path")?.let {
            add(it to File(context.filesDir, it))
        }
        prefsString(context, MUSIC_PREFS, "music_filename")?.let {
            add("music/$it" to File(context.filesDir, "music/$it"))
        }
        prefsString(context, SOUND_PREFS, "sound_effect_filename")?.let {
            add("sound_effects/$it" to File(context.filesDir, "sound_effects/$it"))
        }
        prefsString(context, SOUND_PREFS, "startup_sound_filename")?.let {
            add("sound_effects/$it" to File(context.filesDir, "sound_effects/$it"))
        }
    }.distinctBy { it.first }

    private fun putFile(zip: ZipOutputStream, entryName: String, file: File) {
        if (!file.isFile) return
        runCatching {
            zip.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { it.copyTo(zip) }
            zip.closeEntry()
        }.onFailure { Log.w(TAG, "failed to pack $entryName", it) }
    }

    /** Wallpaper file base name -> preference key holding its URI. */
    private val imageKeyByName = mapOf(
        "background" to "custom_background_uri",
        "background_home" to "home_background_uri",
        "background_kernel" to "kernel_background_uri",
        "background_superuser" to "superuser_background_uri",
        "background_module" to "module_background_uri",
        "background_settings" to "settings_background_uri",
        "background_video" to "video_background_uri",
        "focus_card_bg" to "focus_card_bg_uri",
        "dashboard_tile_bg_working" to "dashboard_tile_bg_uri_working",
        "dashboard_tile_bg_selinux" to "dashboard_tile_bg_uri_selinux",
        "dashboard_tile_bg_zygisk" to "dashboard_tile_bg_uri_zygisk",
        "dashboard_tile_bg_seccomp" to "dashboard_tile_bg_uri_seccomp",
    )

    private val imageExtensions =
        listOf(".jpg", ".png", ".gif", ".webp", ".mp4", ".webm", ".mkv", ".mov", ".avi", ".3gp")

    /**
     * 字体 / 音乐 / 音效各自独立的偏好文件。
     *
     * 它们不在 `background_settings` 里，所以壁纸那套 `readNonUriKeys` 覆盖不到；主题要真正
     * 「完整」就得连这三份一起带走，否则从商店下载的主题只能改配色和壁纸。
     */
    private const val FONT_PREFS = "font_settings"
    private const val MUSIC_PREFS = "music_settings"
    private const val SOUND_PREFS = "sound_effect_settings"

    /**
     * 归档里允许出现的一级子目录：音乐和音效各占一个目录，字体直接落在 `filesDir` 根。
     * 解压只放行这些目录，其余任何含 `/` 的条目一律丢弃（配合 `..` 检查防路径穿越）。
     */
    private val ARCHIVE_DIRS = setOf("music", "sound_effects")

    /** 归档里字体的合法后缀（字体没有目录前缀，靠后缀识别）。 */
    private val FONT_EXTENSIONS = listOf(".ttf", ".otf", ".ttc")

    data class ThemeMetadata(
        val name: String,
        val type: String, // "phone" or "tablet"
        val version: String,
        val author: String,
        val description: String,
    )

    /** Export the current appearance and wallpapers to [uri] as a theme archive. */
    suspend fun exportTheme(context: Context, uri: Uri, metadata: ThemeMetadata): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val settings = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
                val background = context.getSharedPreferences(
                    BackgroundConfig.PREFS_NAME,
                    Context.MODE_PRIVATE,
                )

                val json = JSONObject().apply {
                    put("metadata", JSONObject().apply {
                        put("name", metadata.name)
                        put("type", metadata.type)
                        put("version", metadata.version)
                        put("author", metadata.author)
                        put("description", metadata.description)
                    })
                    put("settings", readKeys(settings, appearanceKeys))
                    put("background", readNonUriKeys(background))
                    put("font", readAllKeys(context.getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE)))
                    put("music", readAllKeys(context.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE)))
                    put("sound", readAllKeys(context.getSharedPreferences(SOUND_PREFS, Context.MODE_PRIVATE)))
                }

                val written = context.contentResolver.openOutputStream(uri)?.use { output ->
                    ZipOutputStream(output).use { zip ->
                        zip.putNextEntry(ZipEntry(CONFIG_ENTRY))
                        zip.write(json.toString(2).toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                        imageKeyByName.keys.forEach { base -> putImage(zip, context, base) }
                        mediaFiles(context).forEach { (entryName, file) -> putFile(zip, entryName, file) }
                    }
                } != null

                if (written) Log.d(TAG, "theme exported to $uri")
                written
            } catch (e: Exception) {
                Log.e(TAG, "failed to export theme", e)
                false
            }
        }
    }

    /**
     * Read only the metadata of a theme archive, so the user can confirm before it overwrites
     * anything. Returns null when the file is not a readable theme.
     */
    suspend fun readThemeMetadata(context: Context, uri: Uri): ThemeMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                val metadata = openConfig(context, uri)?.optJSONObject("metadata")
                    ?: return@withContext null
                ThemeMetadata(
                    name = metadata.optString("name", ""),
                    type = metadata.optString("type", "phone"),
                    version = metadata.optString("version", "1.0"),
                    author = metadata.optString("author", ""),
                    description = metadata.optString("description", ""),
                )
            } catch (e: Exception) {
                Log.e(TAG, "failed to read theme metadata", e)
                null
            }
        }
    }

    /** Apply a theme archive: appearance keys, wallpaper keys and the wallpaper images. */
    suspend fun importTheme(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "theme_import")
            try {
                val json = openConfig(context, uri) ?: return@withContext false

                // Images first: the preference URIs written below must already resolve.
                val localUris = unzipImages(context, uri, tempDir)

                json.optJSONObject("settings")?.let { section ->
                    writeKeys(context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE), section)
                }
                json.optJSONObject("background")?.let { section ->
                    val background = context.getSharedPreferences(
                        BackgroundConfig.PREFS_NAME,
                        Context.MODE_PRIVATE,
                    )
                    writeKeys(background, section)
                    // A theme can come from another device, so its stored paths are meaningless
                    // here. Clear every slot first - a theme that ships no wallpaper must not
                    // leave the previous one on screen - then repoint the ones it does ship.
                    background.edit {
                        imageKeyByName.values.forEach { key -> putString(key, null) }
                        localUris.forEach { (base, localUri) ->
                            imageKeyByName[base]?.let { key -> putString(key, localUri) }
                        }
                    }
                }

                // 字体 / 音乐 / 音效：**归档里没有的段落一律不动**。
                //
                // 这一点和 FolkPatch 相反：它无条件 clear 再按归档内容重设，所以导入一个不含
                // 音乐的主题会把你现有的音乐删掉。主题商店里大部分主题只有配色和壁纸，那种
                // 行为等于「导入主题顺手清空个人设置」，这里改成缺省即保留。
                json.optJSONObject("font")?.let { section ->
                    applyFontSection(context, section)
                }
                json.optJSONObject("music")?.let { section ->
                    applyMusicSection(context, section)
                }
                json.optJSONObject("sound")?.let { section ->
                    applySoundSection(context, section)
                }

                BackgroundConfig.load(context)
                Log.d(TAG, "theme imported from $uri")
                true
            } catch (e: Exception) {
                Log.e(TAG, "failed to import theme", e)
                false
            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    /**
     * Restore the built-in appearance and drop every wallpaper. Other settings are untouched.
     */
    suspend fun resetTheme(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val settings = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
                val background = context.getSharedPreferences(
                    BackgroundConfig.PREFS_NAME,
                    Context.MODE_PRIVATE,
                )

                settings.edit {
                    putInt("color_mode", 0)
                    putInt("key_color", 0)
                    putString("color_style", PaletteStyle.TonalSpot.name)
                    putString("color_spec", ColorSpec.SpecVersion.SPEC_2025.name)
                    remove("home_layout_style")
                }
                background.edit { clear() }
                // 字体/音乐/音效的 clear* 会同时删掉文件、清偏好并把内存态复位，
                // 比手工清三份偏好再调 load 更不容易漏。
                FontConfig.clearFont(context)
                MusicConfig.clearMusic(context)
                SoundEffectConfig.clearSoundEffect(context)
                SoundEffectConfig.clearStartupSound(context)

                imageKeyByName.keys.forEach { base ->
                    imageExtensions.forEach { ext ->
                        File(context.filesDir, "$base$ext").delete()
                    }
                }

                BackgroundConfig.reset()
                BackgroundConfig.load(context)
                Log.d(TAG, "theme reset")
                true
            } catch (e: Exception) {
                Log.e(TAG, "failed to reset theme", e)
                false
            }
        }
    }

    /** The file extension a theme is saved under. */
    val themeFileExtension: String get() = THEME_EXTENSION

    private fun readKeys(prefs: SharedPreferences, keys: List<String>): JSONObject =
        JSONObject().also { json ->
            keys.forEach { key ->
                when (val value = prefs.all[key]) {
                    null -> Unit
                    is Float -> json.put(key, value.toDouble())
                    else -> json.put(key, value)
                }
            }
        }

    private fun writeKeys(prefs: SharedPreferences, json: JSONObject) {
        val editor = prefs.edit()
        json.keys().forEach { key ->
            when (val value = json.opt(key)) {
                null, JSONObject.NULL -> editor.remove(key)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                // JSON has no float type: every number round-trips through a double.
                is Double -> editor.putFloat(key, value.toFloat())
                is String -> editor.putString(key, value)
                else -> editor.putString(key, value.toString())
            }
        }
        editor.apply()
    }

    private fun openConfig(context: Context, uri: Uri): JSONObject? {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        input.use { stream ->
            ZipInputStream(stream).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == CONFIG_ENTRY || entry.name.endsWith("/$CONFIG_ENTRY")) {
                        return JSONObject(zip.readBytes().toString(Charsets.UTF_8))
                    }
                }
            }
        }
        return null
    }

    private fun putImage(zip: ZipOutputStream, context: Context, baseName: String) {
        imageExtensions.forEach { ext ->
            val file = File(context.filesDir, "$baseName$ext")
            if (!file.exists()) return@forEach
            runCatching {
                zip.putNextEntry(ZipEntry(file.name))
                FileInputStream(file).use { it.copyTo(zip) }
                zip.closeEntry()
            }.onFailure { Log.w(TAG, "failed to pack ${file.name}", it) }
        }
    }

    /** Extract the archive's wallpapers into filesDir and return their local file URIs. */
    private fun unzipImages(context: Context, uri: Uri, tempDir: File): Map<String, String> {
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        val localUris = mutableMapOf<String, String>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    // Reject traversal and unexpected nesting before anything touches the disk.
                    if (name.contains("..") || name.contains("\\") || name.startsWith("/")) continue
                    if (name.contains("//")) continue

                    val topLevel = name.substringBefore('/', "")
                    // `background` is a prefix of `background_home`, so take the longest match.
                    val base = imageKeyByName.keys
                        .filter { name.startsWith(it) }
                        .maxByOrNull { it.length }
                    val isWallpaper = base != null && imageExtensions.any { name.endsWith(it) }
                    // 字体直接落在 filesDir 根；音乐/音效各占一个已知目录。其余一律不认。
                    val isMedia = !isWallpaper && when {
                        name.contains("/") -> topLevel in ARCHIVE_DIRS
                        else -> FONT_EXTENSIONS.any { name.endsWith(it) }
                    }
                    if (!isWallpaper && !isMedia) continue

                    val target = File(context.filesDir, name)
                    // `music/…` 与 `sound_effects/…` 在全新安装上可能还不存在。
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { zip.copyTo(it) }
                    if (isWallpaper && base != null) {
                        localUris[base] = Uri.fromFile(target).toString()
                    }
                }
            }
        }
        return localUris
    }
}
