package com.sukisu.ultra.ui.theme.glass

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * What the platform can actually do, decided once.
 *
 * iOS Liquid Glass is a backdrop blur: the plate refracts whatever is behind it. Android
 * exposes that through RenderEffect, which only exists from Android 12 (API 31), and the
 * alternative the library provides - miuix-blur - documents a minSdk of 33 while this app
 * starts at 26. Rather than a broken component on older devices, the glass is built so the
 * plate reads correctly without blur and gains real refraction only where the platform
 * offers it.
 */
object GlassCapability {
    /** Backdrop blur through RenderEffect. False on API 26-30, where glass is a tinted plate. */
    val supportsRenderEffectBlur: Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}

/**
 * How far a surface is pushed into glass. The three steps are the whole vocabulary: a page
 * holds [Regular] shelves, chrome that sits over content drops to [Subtle] so it stays
 * readable, and a floating sheet or dialog goes [Prominent] to separate from the page.
 */
enum class GlassStrength { Subtle, Regular, Prominent }

/**
 * Resolved colours for one plate.
 *
 * Everything is a vertical gradient because that is the one lighting model iOS glass
 * actually uses: the plate catches light at its top edge, falls off through the body, and
 * picks up a brighter bounce at the bottom. A flat fill with a stroke does not read as
 * glass no matter what the stroke does.
 */
@Immutable
data class GlassSpec(
    /** Body of the plate, top to bottom. Drawn under the content. */
    val tint: List<Color>,
    /** Bright wash over the upper part of the plate. Never reaches the content's text rows. */
    val specular: List<Color>,
    /** The height the specular wash is allowed to occupy, as a fraction of the plate. */
    val specularHeight: Float,
    /** Diagonal light that breaks the flat fill. */
    val sheen: List<Color>,
    /** Hairline rim, top to bottom: the single detail that separates glass from a tint. */
    val rim: List<Color>,
    /** Rim thickness. One device-independent pixel: a rim that thickens stops being a rim. */
    val rimWidth: Dp,
    val strength: GlassStrength,
) {
    companion object {

        fun from(scheme: ColorScheme, strength: GlassStrength): GlassSpec {
            val dark = scheme.background.luminance() < 0.5f
            val k = when (strength) {
                GlassStrength.Subtle -> 0.55f
                GlassStrength.Regular -> 1f
                GlassStrength.Prominent -> 1.4f
            }

            // The tint is white in both modes - glass has no colour of its own, it only
            // lifts or drops the page underneath. In light mode that lift is large and
            // opaque enough to hold text; in dark mode it is a small lift over black,
            // which is what keeps an AMOLED page from turning into a grey slab.
            val bodyTop: Color
            val bodyBottom: Color
            val specTop: Color
            val specMid: Color
            val rimTop: Color
            val rimMid: Color
            val rimBottom: Color
            if (dark) {
                bodyTop = white(0.13f * k)
                bodyBottom = white(0.075f * k)
                specTop = white(0.20f * k)
                specMid = white(0.07f * k)
                rimTop = white(0.48f * k)
                rimMid = white(0.15f * k)
                rimBottom = white(0.26f * k)
            } else {
                bodyTop = white(0.62f * k)
                bodyBottom = white(0.36f * k)
                specTop = white(0.50f * k)
                specMid = white(0.18f * k)
                rimTop = white(0.95f * k)
                rimMid = white(0.42f * k)
                rimBottom = white(0.62f * k)
            }

            return GlassSpec(
                tint = listOf(bodyTop, bodyBottom),
                specular = listOf(specTop, specMid, Color.Transparent),
                specularHeight = 0.45f,
                sheen = listOf(Color.Transparent, white(0.10f * k), Color.Transparent),
                rim = listOf(rimTop, rimMid, rimBottom),
                rimWidth = 1.dp,
                strength = strength,
            )
        }

        private fun white(alpha: Float): Color =
            Color.White.copy(alpha = alpha.coerceIn(0f, 1f))
    }
}
