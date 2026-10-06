package com.sukisu.ultra.ui.util

import android.content.Context
import android.util.Log
import com.sukisu.ultra.ksuApp
import com.sukisu.ultra.ui.theme.BackgroundConfig
import com.topjohnwu.superuser.io.SuFile
import com.topjohnwu.superuser.io.SuFileInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 解析一个模块该显示哪张横幅，移植自 FolkPatch `APMModuleItem` 的 `bannerInfo` 计算。
 *
 * 优先级：**API 模式 > 用户自己指定的横幅 > 模块自带的 `banner` 属性/文件**。
 *
 * 进程内缓存，避免每次重组都过一遍 root shell；`reload` 用于用户改了横幅或切了 API 源之后
 * 强制重取。"取不到"也要缓存下来，否则一个没有横幅的模块会在每次重组时都去读一次 root。
 */
object ModuleBanner {
    private const val TAG = "ModuleBanner"
    private const val MODULES_ROOT = "/data/adb/modules"

    /** 模块目录里可能存在的横幅文件，顺序即优先级。 */
    private val BANNER_FILES = listOf("banner.png", "banner.jpg", "banner.jpeg", "banner.webp", "banner")

    private val folkStorage by lazy { ModuleBannerStorage(ksuApp, "module_banners") }

    private val memory = mutableMapOf<String, ByteArray?>()

    /** 丢弃某个模块的缓存（用户手动换了横幅之后调用）。 */
    fun invalidate(moduleId: String) {
        memory.remove(moduleId)
    }

    fun invalidateAll() {
        memory.clear()
    }

    /** 取横幅字节；三步都没取到就返回 null，卡片据此不画横幅层。 */
    suspend fun load(
        context: Context,
        moduleId: String,
        reload: Boolean = false,
    ): ByteArray? {
        if (!reload && memory.containsKey(moduleId)) {
            return memory[moduleId]
        }
        val bytes = withContext(Dispatchers.IO) { resolve(context, moduleId) }
        memory[moduleId] = bytes
        return bytes
    }

    /**
     * 同步取一张横幅给 `produceState` 的 initialValue 用，让卡片首帧就有图，不先空一帧。
     *
     * 只碰 `filesDir` 下的本地文件（内存缓存 → API 磁盘缓存 → 指定过的横幅）——**刻意不含
     * 第 3 步「读模块目录」**，那一步要过 root shell，放在主线程会让列表滚动掉帧。取不到
     * 就返回 null，交给 [load] 在 IO 线程把三步走完。
     */
    fun loadSync(context: Context, moduleId: String): ByteArray? {
        if (!BackgroundConfig.isBannerEnabled) return null
        memory[moduleId]?.let { return it }

        if (BackgroundConfig.isBannerApiModeEnabled) {
            val source = BackgroundConfig.getEffectiveBannerApiSource()
            if (source.isNotBlank()) {
                BannerApiService.loadSync(context, moduleId, source)?.let { return it }
            }
        }

        if (BackgroundConfig.isFolkBannerEnabled) {
            folkStorage.read(moduleId)?.let { return it }
        }

        return null
    }

    private suspend fun resolve(context: Context, moduleId: String): ByteArray? {
        if (!BackgroundConfig.isBannerEnabled) return null

        // 1. API 模式：源为空或取图失败都继续往下走，而不是直接放弃整个横幅。
        if (BackgroundConfig.isBannerApiModeEnabled) {
            val source = BackgroundConfig.getEffectiveBannerApiSource()
            if (source.isNotBlank()) {
                val api = runCatching {
                    BannerApiService.getModuleBanner(context, moduleId, source)
                }.getOrNull()
                if (api != null) return api
            }
        }

        // 2. 用户自己给这个模块指定过的横幅。
        if (BackgroundConfig.isFolkBannerEnabled) {
            folkStorage.read(moduleId)?.let { return it }
        }

        // 3. 模块目录里自带的横幅。
        return readFromModuleDir(moduleId)
    }

    private fun readFromModuleDir(moduleId: String): ByteArray? {
        if (moduleId.isBlank()) return null

        return runCatching {
            val shell = getRootShell()
            fun su(path: String) = SuFile(path).apply { setShell(shell) }

            val dirPath = "$MODULES_ROOT/$moduleId"
            if (!su(dirPath).isDirectory) return@runCatching null

            // module.prop 的 `banner=` 可以是相对路径；http(s) 地址没法在这里落地成文件，
            // 直接跳过，交给后面的候选文件名。
            val propBanner = runCatching {
                SuFileInputStream.open(su("$dirPath/module.prop")).use { input ->
                    input.bufferedReader()
                        .lineSequence()
                        .firstOrNull { it.startsWith("banner=") }
                        ?.substringAfter('=')
                        ?.trim()
                }
            }.getOrNull()

            val candidates = buildList {
                propBanner
                    ?.takeIf { it.isNotBlank() && !it.startsWith("http", true) }
                    ?.let { add(it) }
                addAll(BANNER_FILES)
            }.distinct()

            candidates.firstNotNullOfOrNull { name ->
                val file = su(if (name.startsWith("/")) name else "$dirPath/$name")
                if (!file.isFile) return@firstNotNullOfOrNull null
                runCatching { SuFileInputStream.open(file).use { it.readBytes() } }
                    .getOrNull()
                    ?.takeIf { it.isNotEmpty() }
            }
        }.getOrElse {
            Log.w(TAG, "Failed to read banner for $moduleId: ${it.message}")
            null
        }
    }

    /** 换了 API 源或用户点了「清除缓存」时，把磁盘缓存和内存缓存一起丢掉。 */
    suspend fun clearApiCache(context: Context) {
        BannerApiService.clearAllCache(context)
        invalidateAll()
    }
}
