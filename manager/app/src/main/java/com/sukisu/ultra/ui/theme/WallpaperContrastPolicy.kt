package com.sukisu.ultra.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

/**
 * 壁纸模式下实际用于绘制壁纸遮罩的 dim 值（0..1）。
 *
 * 由对比度保护策略计算，可能高于用户请求的 dim；用户偏好本身不被改写。非壁纸模式为 null，
 * 调用方回退到 [BackgroundConfig.getEffectiveBackgroundDim]。
 */
val LocalWallpaperDim = compositionLocalOf<Float?> { null }

/** [adaptColorScheme] 的结果：适配后的配色方案，以及建议的壁纸遮罩 dim。 */
internal data class WallpaperThemeResult(
    val colorScheme: ColorScheme,
    val renderDim: Float?,
)

// 正常文字与其背景之间的最低对比度（WCAG AA）。
private const val MIN_CONTRAST = 4.5f

/**
 * 以最亮壁纸（纯白）为最坏情况，叠加黑遮罩后，检测中性前景色在裸壁纸、半透明
 * surface、半透明 surfaceContainer 三种背景上是否都达到 [MIN_CONTRAST]。
 */
private fun passesContrast(dim: Float, scheme: ColorScheme): Boolean {
    val wall = Color.Black.copy(alpha = dim.coerceIn(0f, 1f)).compositeOver(Color.White)
    val backgrounds = listOf(
        wall,
        scheme.surface.compositeOver(wall),
        scheme.surfaceContainer.compositeOver(wall),
    ).map { it.luminance() }
    val foregrounds = listOf(
        scheme.onBackground,
        scheme.onSurface,
        scheme.onSurfaceVariant,
    ).map { it.luminance() }
    return foregrounds.all { fg ->
        backgrounds.all { bg ->
            val hi = maxOf(fg, bg)
            val lo = minOf(fg, bg)
            (hi + 0.05f) / (lo + 0.05f) >= MIN_CONTRAST
        }
    }
}

/**
 * 在浅色中性内容（深色中性方案，即浅色文字）下，按最亮壁纸估算满足对比度所需的最小 dim。
 *
 * @param requested 用户请求的 dim，结果不会低于它。
 * @return 需要的 dim；若即使 dim=1 也无法满足，返回 null（调用方应对文字使用完整 on 色，
 *         不要再叠加 alpha）。
 */
internal fun guardedDim(requested: Float, scheme: ColorScheme): Float? {
    val start = requested.coerceIn(0f, 1f)
    if (passesContrast(start, scheme)) return start
    if (!passesContrast(1f, scheme)) return null
    var low = start
    var high = 1f
    repeat(16) {
        val mid = (low + high) / 2f
        if (passesContrast(mid, scheme)) high = mid else low = mid
    }
    return high
}
