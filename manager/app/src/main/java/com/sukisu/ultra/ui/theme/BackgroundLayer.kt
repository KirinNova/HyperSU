package com.sukisu.ultra.ui.theme

import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Draws the chosen picture behind a screen.
 *
 * Three things have to be right for this to read as a wallpaper rather than a sticker:
 *
 *  - it crops to fill, never letterboxes, so the picture reaches every edge;
 *  - it is sampled down before it reaches the GPU, because a full-resolution camera roll
 *    decoded at native size costs tens of megabytes every time the configuration changes;
 *  - a blurred copy is drawn overscanned, because blur samples its neighbours and the pixels
 *    just outside the viewport do not exist. Without the overscan the edges turn into a
 *    translucent halo that reads like a rendering bug.
 *
 * Blur needs RenderEffect, which is API 31. Below that the picture is drawn sharp and the
 * setting is accepted and ignored rather than hidden, so one theme file describes every device.
 */
@Composable
fun Modifier.backgroundWallpaper(): Modifier {
    // Declared before the early returns: this is a composable hook, so it has to run on every
    // recomposition even while no background is configured.
    val bitmap by rememberWallpaperBitmap(
        if (BackgroundConfig.isActive) BackgroundConfig.uri else "",
    )

    val image = bitmap ?: return this

    val blurDp = BackgroundConfig.blur
    val dim = BackgroundConfig.dim
    val blurSupported = blurDp > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    return this
        .graphicsLayer {
            if (blurSupported) {
                val radius = blurDp.dp.toPx()
                renderEffect = RenderEffect
                    .createBlurEffect(radius, radius, Shader.TileMode.CLAMP)
                    .asComposeRenderEffect()
            }
        }
        .drawBehind {
            // The blur reaches radius times three beyond the fragment it is drawn into, so the
            // picture is laid out that much larger on every side, or the first pixels of the
            // blur come back transparent.
            val bleed = if (blurSupported) blurDp.dp.toPx() * 3f else 0f
            val target = Size(size.width + bleed * 2f, size.height + bleed * 2f)

            val ratio = max(
                target.width / image.width,
                target.height / image.height,
            )
            val drawnWidth = image.width * ratio
            val drawnHeight = image.height * ratio
            val left = (target.width - drawnWidth) / 2f - bleed
            val top = (target.height - drawnHeight) / 2f - bleed

            drawImage(
                image = image,
                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                dstSize = IntSize(drawnWidth.roundToInt(), drawnHeight.roundToInt()),
            )

            if (dim > 0f) {
                drawRect(color = Color.Black.copy(alpha = dim))
            }
        }
}

/**
 * The picture behind [uri], sampled to roughly twice the screen so a full-resolution photo
 * never lands in memory at full size. Null on a bad URI or a revoked permission - the screen
 * then falls back to the flat background it had before the picture was picked.
 */
@Composable
fun rememberWallpaperBitmap(uri: String): State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, key1 = uri) {
        value = if (uri.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }

                    val screen = context.resources.displayMetrics
                    val target = max(screen.widthPixels, screen.heightPixels) * 2
                    var sample = 1
                    while (bounds.outWidth > 0 && bounds.outWidth / (sample * 2) >= target) {
                        sample *= 2
                    }

                    val options = BitmapFactory.Options().apply { inSampleSize = sample }
                    context.contentResolver.openInputStream(Uri.parse(uri))?.use {
                        BitmapFactory.decodeStream(it, null, options)
                    }
                }.getOrNull()?.asImageBitmap()
            }
        }
    }
}
