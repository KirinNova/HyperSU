package com.sukisu.ultra.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.sukisu.ultra.data.repository.HOME_LAYOUT_OPTIONS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Theme import / export / reset for the manager.
 *
 * A theme file (`.fpt`) is FolkPatch's container: a ZIP wrapped in AES/CBC, holding `theme.json`
 * plus the wallpaper images, font, music and sound effects the appearance depends on. See
 * [ThemeArchive] for the container itself.
 *
 * The JSON carries two vocabularies. The flat keys (`nightModeEnabled`, `isBackgroundEnabled`,
 * `musicFilename`, ...) are FolkPatch's, so a theme exported here imports there and vice versa;
 * the `hs*` sections are this app's own preferences verbatim, so importing a theme exported here
 * restores exactly what was exported rather than round-tripping through the lossier mapping.
 */
object ThemeManager {

    private const val TAG = "ThemeManager"

    private const val SETTINGS_PREFS = "settings"
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
            .stringPref(key, null)
            ?.takeIf { it.isNotBlank() }

    /**
     * Maps a FolkPatch theme's flat appearance keys onto HyperSU's preferences.
     *
     * The two apps describe the same settings with different vocabularies: FolkPatch stores a
     * boolean night mode plus a "follow system" flag, HyperSU a single `color_mode` enum. The
     * mapping below is the inverse of what [buildConfigJson] writes, so a theme exported by
     * either app lands in the same state here.
     */
    private fun applyFolkPatchAppearance(context: Context, json: JSONObject) {
        val settings = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)

        val followSys = json.optBoolean("nightModeFollowSys", true)
        val nightEnabled = json.optBoolean("nightModeEnabled", true)
        val monet = json.optBoolean("useSystemDynamicColor", false)

        // FolkPatch has no AMOLED flag, so AMOLED is not reachable from its themes; it comes
        // back through HyperSU's own section on a round trip.
        val mode = when {
            monet && followSys -> ColorMode.MONET_SYSTEM
            monet && nightEnabled -> ColorMode.MONET_DARK
            monet -> ColorMode.MONET_LIGHT
            followSys -> ColorMode.SYSTEM
            nightEnabled -> ColorMode.DARK
            else -> ColorMode.LIGHT
        }

        settings.edit {
            putInt("color_mode", mode.value)
            // FolkPatch's customColor is a theme key name; HyperSU stores a packed ARGB int.
            // Only overwrite when it parses as a number, so a catalog name does not clear the
            // user's key colour.
            json.optString("customColor", "").toIntOrNull()?.let { putInt("key_color", it) }
            json.optString("colorStyle", "").takeIf { it.isNotBlank() }?.let {
                putString("color_style", it)
            }
            // FolkPatch spells the standard MD3_2021 / MD3_2025; HyperSU uses the enum name.
            when (json.optString("colorStandard", "")) {
                "MD3_2021" -> putString("color_spec", ColorSpec.SpecVersion.SPEC_2021.name)
                "MD3_2025" -> putString("color_spec", ColorSpec.SpecVersion.SPEC_2025.name)
            }
            // FolkPatch's layout vocabulary is its own (grid_working, stats, ...). Only accept a
            // name HyperSU actually knows, otherwise the home screen would be handed a layout it
            // cannot render and fall back unpredictably.
            json.optString("homeLayoutStyle", "").takeIf { it in HOME_LAYOUT_OPTIONS }?.let {
                putString("home_layout_style", it)
            }
        }
    }

    /** Maps FolkPatch's flat background keys onto HyperSU's wallpaper preferences. */
    private fun applyFolkPatchBackground(context: Context, json: JSONObject) {
        val background = context.getSharedPreferences(BackgroundConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = json.optBoolean("isBackgroundEnabled", false)

        background.edit {
            putBoolean("custom_background_enabled", enabled)
            putFloat("custom_background_opacity", json.optDouble("backgroundOpacity", 0.5).toFloat())
            putFloat("custom_background_blur", json.optDouble("backgroundBlur", 0.0).toFloat())
            putFloat("custom_background_dim", json.optDouble("backgroundDim", 0.2).toFloat())
            putBoolean(
                "custom_background_dual_dim_enabled",
                json.optBoolean("isDualBackgroundDimEnabled", false),
            )
            putFloat("custom_background_day_dim", json.optDouble("backgroundDayDim", 0.2).toFloat())
            putFloat("custom_background_night_dim", json.optDouble("backgroundNightDim", 0.2).toFloat())
        }
    }

    /** FolkPatch's flat font keys. */
    private fun applyFolkPatchFont(context: Context, json: JSONObject) {
        val modeName = json.optString("fontMode", "")
        val mode = FontMode.fromSerializedName(modeName)
            ?: if (json.optBoolean("isFontEnabled", false)) FontMode.CUSTOM else FontMode.SYSTEM_DEFAULT

        when (mode) {
            FontMode.CUSTOM -> {
                // FolkPatch always packs the face as `font.ttf`; adopt it when present.
                val imported = File(context.filesDir, "font.ttf")
                if (imported.isFile) {
                    FontConfig.applyCustomFont(context, imported)
                } else {
                    FontConfig.setFontMode(context, FontMode.APP_DEFAULT)
                }
            }
            else -> FontConfig.setFontMode(context, mode)
        }
    }

    /** FolkPatch's flat music keys. */
    private fun applyFolkPatchMusic(context: Context, json: JSONObject) {
        val filename = json.optString("musicFilename", "").takeIf { it.isNotBlank() && it != "null" }
        val enabled = json.optBoolean("isMusicEnabled", false)
        val hasFile = filename != null && File(context.filesDir, "music/$filename").isFile

        MusicConfig.setMusicEnabledState(enabled && hasFile)
        MusicConfig.setVolumeValue(json.optDouble("musicVolume", 1.0).toFloat())
        MusicConfig.setAutoPlayEnabledState(json.optBoolean("isAutoPlayEnabled", false))
        MusicConfig.setLoopingEnabledState(json.optBoolean("isLoopingEnabled", false))
        MusicConfig.setMusicFilenameValue(if (hasFile) filename else null)
        MusicConfig.save(context)
    }

    /** FolkPatch's flat sound-effect keys. */
    private fun applyFolkPatchSound(context: Context, json: JSONObject) {
        val filename = json.optString("soundEffectFilename", "").takeIf { it.isNotBlank() && it != "null" }
        val hasFile = filename != null && File(context.filesDir, "sound_effects/$filename").isFile

        SoundEffectConfig.setEnabledState(json.optBoolean("isSoundEffectEnabled", false))
        SoundEffectConfig.setScopeValue(
            json.optString("soundEffectScope", SoundEffectConfig.SCOPE_GLOBAL),
        )
        SoundEffectConfig.setSourceTypeValue(
            if (hasFile) SoundEffectConfig.SOURCE_TYPE_LOCAL else SoundEffectConfig.SOURCE_TYPE_PRESET,
        )
        SoundEffectConfig.setFilenameValue(if (hasFile) filename else null)
        SoundEffectConfig.save(context)
    }

    /** 应用归档里的字体段落。
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
        // The face is packed twice under different names: `font.ttf` is what FolkPatch looks for
        // on import, and the `custom_font_*.ttf` name is what HyperSU's own importer resolves
        // through `custom_font_path`. The file is a few hundred KB, so the duplicate is cheap
        // next to being unreadable by one of the two managers.
        prefsString(context, FONT_PREFS, "custom_font_path")?.let { name ->
            val file = File(context.filesDir, name)
            add(name to file)
            add(FOLK_FONT_ENTRY to file)
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

    /** FolkPatch's fixed name for the packed custom face. */
    private const val FOLK_FONT_ENTRY = "font.ttf"

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
                val json = buildConfigJson(context, metadata)

                val written = context.contentResolver.openOutputStream(uri)?.use { output ->
                    // Same encrypted container FolkPatch writes, so the file is readable by
                    // either manager.
                    ThemeArchive.write(output) { zip ->
                        zip.putNextEntry(ZipEntry(ThemeArchive.CONFIG_ENTRY))
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
     * Builds the theme JSON.
     *
     * Two sets of keys go in on purpose. The `meta_*` and flat appearance keys are FolkPatch's,
     * so a theme exported here imports there; the `hs*` sections are HyperSU's own preferences
     * verbatim, so importing back restores exactly what was exported instead of round-tripping
     * through FolkPatch's lossier vocabulary.
     */
    private fun buildConfigJson(context: Context, metadata: ThemeMetadata): JSONObject {
        val settings = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE)
        val background = context.getSharedPreferences(BackgroundConfig.PREFS_NAME, Context.MODE_PRIVATE)
        val font = context.getSharedPreferences(FONT_PREFS, Context.MODE_PRIVATE)
        val music = context.getSharedPreferences(MUSIC_PREFS, Context.MODE_PRIVATE)
        val sound = context.getSharedPreferences(SOUND_PREFS, Context.MODE_PRIVATE)

        val colorMode = ColorMode.fromValue(settings.intPref("color_mode", 0))
        val colorSpec = settings.stringPref("color_spec", ColorSpec.SpecVersion.SPEC_2025.name)
            ?: ColorSpec.SpecVersion.SPEC_2025.name

        return JSONObject().apply {
            // ---- FolkPatch metadata keys ----
            put("meta_name", metadata.name)
            put("meta_type", metadata.type)
            put("meta_version", metadata.version)
            put("meta_author", metadata.author)
            put("meta_description", metadata.description)

            // ---- FolkPatch appearance keys ----
            put("isBackgroundEnabled", background.booleanPref("custom_background_enabled", false))
            put("backgroundOpacity", background.floatPref("custom_background_opacity", 0.5f).toDouble())
            put("backgroundBlur", background.floatPref("custom_background_blur", 0.2f).toDouble())
            put("backgroundDim", background.floatPref("custom_background_dim", 0.0f).toDouble())
            put("isDualBackgroundDimEnabled", background.booleanPref("custom_background_dual_dim_enabled", true))
            put("backgroundDayDim", background.floatPref("custom_background_day_dim", 0.0f).toDouble())
            put("backgroundNightDim", background.floatPref("custom_background_night_dim", 0.5f).toDouble())

            put("nightModeEnabled", colorMode.isDark)
            put("nightModeFollowSys", colorMode.isSystem)
            put("useSystemDynamicColor", colorMode.isMonet)
            // FolkPatch has no AMOLED flag; a theme cannot express it, so it is not written.
            put("customColor", settings.intPref("key_color", 0).toString())
            put("colorStyle", settings.stringPref("color_style", PaletteStyle.TonalSpot.name))
            put("colorStandard", if (colorSpec == ColorSpec.SpecVersion.SPEC_2021.name) "MD3_2021" else "MD3_2025")
            put("colorGenerationMode", if (colorMode.isMonet) "custom" else "classic")
            put("homeLayoutStyle", settings.stringPref("home_layout_style", "circle"))

            put("isFontEnabled", font.booleanPref("custom_font_enabled", false))
            put("fontMode", FontMode.fromName(font.stringPref("font_mode", null))?.serializedName ?: "system")

            put("isMusicEnabled", music.booleanPref("music_enabled", false))
            put("musicVolume", music.floatPref("volume", 1.0f).toDouble())
            put("isAutoPlayEnabled", music.booleanPref("auto_play", false))
            put("isLoopingEnabled", music.booleanPref("looping_enabled", false))
            put("musicFilename", music.stringPref("music_filename", "") ?: "")

            put("isSoundEffectEnabled", sound.booleanPref("sound_effect_enabled", false))
            put("soundEffectFilename", sound.stringPref("sound_effect_filename", "") ?: "")
            put("soundEffectScope", sound.stringPref("sound_effect_scope", SoundEffectConfig.SCOPE_GLOBAL))

            // ---- HyperSU-native sections (exact restore) ----
            put("hsSettings", readKeys(settings, appearanceKeys))
            put("hsBackground", readNonUriKeys(background))
            put("hsFont", readAllKeys(font))
            put("hsMusic", readAllKeys(music))
            put("hsSound", readAllKeys(sound))
        }
    }

    /**
     * Read only the metadata of a theme archive, so the user can confirm before it overwrites
     * anything. Returns null when the file is not a readable theme.
     *
     * Reads both the FolkPatch key names and HyperSU's own, and both containers, so a theme
     * from either source is recognised.
     */
    suspend fun readThemeMetadata(context: Context, uri: Uri): ThemeMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                val json = openConfig(context, uri) ?: return@withContext null
                // FolkPatch nests nothing and prefixes with meta_; the first HyperSU release
                // used a metadata object. Accept either.
                val nested = json.optJSONObject("metadata")
                ThemeMetadata(
                    name = nested?.optString("name")?.takeIf { it.isNotBlank() }
                        ?: json.optString("meta_name", ""),
                    type = nested?.optString("type")?.takeIf { it.isNotBlank() }
                        ?: json.optString("meta_type", "phone"),
                    version = nested?.optString("version")?.takeIf { it.isNotBlank() }
                        ?: json.optString("meta_version", "1.0"),
                    author = nested?.optString("author")?.takeIf { it.isNotBlank() }
                        ?: json.optString("meta_author", ""),
                    description = nested?.optString("description")?.takeIf { it.isNotBlank() }
                        ?: json.optString("meta_description", ""),
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

                // HyperSU-native sections win when present: they are this app's own keys, so a
                // round trip restores exactly what was exported. FolkPatch themes have only the
                // flat keys, which are mapped below.
                val hasNativeSections = json.has("hsSettings")

                json.optJSONObject("hsSettings")?.let { section ->
                    writeKeys(context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE), section)
                }
                if (!hasNativeSections) {
                    applyFolkPatchAppearance(context, json)
                }

                val backgroundPrefs = context.getSharedPreferences(
                    BackgroundConfig.PREFS_NAME,
                    Context.MODE_PRIVATE,
                )
                json.optJSONObject("hsBackground")?.let { section ->
                    writeKeys(backgroundPrefs, section)
                }
                if (!hasNativeSections) {
                    applyFolkPatchBackground(context, json)
                }
                // A theme can come from another device, so its stored paths are meaningless
                // here. Clear every slot first - a theme that ships no wallpaper must not leave
                // the previous one on screen - then repoint the ones it does ship.
                backgroundPrefs.edit {
                    imageKeyByName.values.forEach { key -> putString(key, null) }
                    localUris.forEach { (base, localUri) ->
                        imageKeyByName[base]?.let { key -> putString(key, localUri) }
                    }
                }

                // 字体 / 音乐 / 音效：**归档里没有的段落一律不动**。
                //
                // 这一点和 FolkPatch 相反：它无条件 clear 再按归档内容重设，所以导入一个不含
                // 音乐的主题会把你现有的音乐删掉。主题商店里大部分主题只有配色和壁纸，那种
                // 行为等于「导入主题顺手清空个人设置」，这里改成缺省即保留。
                val fontSection = json.optJSONObject("hsFont")
                if (fontSection != null) {
                    applyFontSection(context, fontSection)
                } else if (json.has("fontMode") || json.has("isFontEnabled")) {
                    applyFolkPatchFont(context, json)
                }

                val musicSection = json.optJSONObject("hsMusic")
                if (musicSection != null) {
                    applyMusicSection(context, musicSection)
                } else if (json.has("isMusicEnabled")) {
                    applyFolkPatchMusic(context, json)
                }

                val soundSection = json.optJSONObject("hsSound")
                if (soundSection != null) {
                    applySoundSection(context, soundSection)
                } else if (json.has("isSoundEffectEnabled")) {
                    applyFolkPatchSound(context, json)
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

    /**
     * Writes a theme section back into preferences.
     *
     * Numbers are stored by the type the *key* uses, never by the type JSON happened to parse
     * into. org.json has a single number type and re-serialises a whole-valued Double as a bare
     * integer, so `0.5` stays a Double but `1.0` comes back as an Integer - and storing that with
     * putInt made every later `getFloat` throw ClassCastException. Since these preferences are
     * loaded from Application.onCreate, that turned one import into an app that could not start.
     */
    private fun writeKeys(prefs: SharedPreferences, json: JSONObject) {
        val editor = prefs.edit()
        json.keys().forEach { key ->
            when (val value = json.opt(key)) {
                null, JSONObject.NULL -> editor.remove(key)
                is Boolean -> editor.putBoolean(key, value)
                is String -> editor.putString(key, value)
                is Number -> if (key in INT_KEYS) {
                    editor.putInt(key, value.toInt())
                } else {
                    editor.putFloat(key, value.toFloat())
                }
                else -> editor.putString(key, value.toString())
            }
        }
        editor.apply()
    }

    /**
     * The appearance keys that really are integers.
     *
     * Everything else that is numeric is a Float. Keeping this list explicit is the point: it is
     * what stops a theme import from silently changing a key's storage type.
     */
    private val INT_KEYS = setOf("color_mode", "key_color")

    /**
     * Reads the theme JSON from either container.
     *
     * Encrypted archives are decrypted first; a plain ZIP is read directly. `theme.json` is
     * FolkPatch's name and `theme_config.json` is what the first HyperSU release wrote, so both
     * are accepted.
     */
    private fun openConfig(context: Context, uri: Uri): JSONObject? {
        val format = ThemeArchive.detect(context, uri)
        if (format == ThemeArchive.Format.Unknown) return null

        val stream = ThemeArchive.openZipStream(context, uri, format) ?: return null
        stream.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val name = entry.name
                    if (name == ThemeArchive.CONFIG_ENTRY ||
                        name == ThemeArchive.LEGACY_CONFIG_ENTRY ||
                        name.endsWith("/${ThemeArchive.CONFIG_ENTRY}") ||
                        name.endsWith("/${ThemeArchive.LEGACY_CONFIG_ENTRY}")
                    ) {
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

    /**
     * Extract the archive into filesDir and return the wallpaper URIs that were restored.
     *
     * Both containers are handled by [ThemeArchive]. FolkPatch's entry names differ from
     * HyperSU's for a few slots - it writes `video_background.*` where HyperSU uses
     * `background_video.*`, and it packs the custom face as `font.ttf` - so the incoming name is
     * normalised to HyperSU's before it is written.
     */
    private fun unzipImages(context: Context, uri: Uri, tempDir: File): Map<String, String> {
        tempDir.deleteRecursively()
        tempDir.mkdirs()

        val localUris = mutableMapOf<String, String>()
        val entries = ThemeArchive.extractTo(context, uri, tempDir)

        entries.forEach { name ->
            val normalised = normaliseEntryName(name)
            val source = File(tempDir, name)
            if (!source.isFile) return@forEach

            val topLevel = normalised.substringBefore('/', "")
            // `background` is a prefix of `background_home`, so take the longest match.
            val base = imageKeyByName.keys
                .filter { normalised.startsWith(it) }
                .maxByOrNull { it.length }
            val isWallpaper = base != null && imageExtensions.any { normalised.endsWith(it) }
            // 字体直接落在 filesDir 根；音乐/音效各占一个已知目录。其余一律不认。
            val isMedia = !isWallpaper && when {
                normalised.contains("/") -> topLevel in ARCHIVE_DIRS
                else -> FONT_EXTENSIONS.any { normalised.endsWith(it) }
            }
            if (!isWallpaper && !isMedia) return@forEach

            val target = File(context.filesDir, normalised)
            // `music/…` 与 `sound_effects/…` 在全新安装上可能还不存在。
            target.parentFile?.mkdirs()
            runCatching { source.copyTo(target, overwrite = true) }

            if (isWallpaper && base != null) {
                localUris[base] = Uri.fromFile(target).toString()
            }
        }
        return localUris
    }

    /**
     * Rewrites the entry names that differ between the two managers onto HyperSU's own.
     *
     * Everything else is already shared: `background*`, `background_*`, `focus_card_bg*` and
     * `dashboard_tile_bg_*` are written under the same names by both apps.
     */
    private fun normaliseEntryName(name: String): String {
        // FolkPatch: video_background.mp4 -> HyperSU: background_video.mp4
        if (name.startsWith("video_background")) {
            return name.replaceFirst("video_background", "background_video")
        }
        return name
    }
}
