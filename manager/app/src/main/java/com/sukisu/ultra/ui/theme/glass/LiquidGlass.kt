package com.sukisu.ultra.ui.theme.glass

import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.blur.textureBlur

/** Resolved from the ambient scheme so glass tracks the theme the same way the palette does. */
@Composable
fun rememberGlassSpec(strength: GlassStrength = GlassStrength.Regular): GlassSpec {
    val scheme = MaterialTheme.colorScheme
    // The revision key is what makes a knob move the plates: without it a remembered spec
    // would keep serving the alphas it resolved at the first composition.
    val revision = GlassConfig.revision
    return remember(scheme, strength, revision) { GlassSpec.from(scheme, strength) }
}

/**
 * The rim miuix paints from the refracted layer, matched to the plate's weight.
 *
 * miuix ships one stroke per size in a light and a dark flavour. Choosing by [strength] keeps
 * the stroke proportional to the plate - a floating sheet gets the wide catch, a row inside a
 * shelf gets the narrow one - and flipping on [GlassSpec.dark] keeps the highlight reading as
 * light landing on the plate rather than a grey seam over black.
 */
private fun GlassSpec.highlight(): Highlight {
    val light = !dark
    return when (strength) {
        GlassStrength.Subtle -> if (light) Highlight.GlassStrokeSmallLight else Highlight.GlassStrokeSmallDark
        GlassStrength.Regular -> if (light) Highlight.GlassStrokeMiddleLight else Highlight.GlassStrokeMiddleDark
        GlassStrength.Prominent -> if (light) Highlight.GlassStrokeBigLight else Highlight.GlassStrokeBigDark
    }
}

/**
 * Turns a surface into a plate of iOS Liquid Glass.
 *
 * The stack is fixed and the order is the whole effect: body tint, diagonal sheen, specular
 * wash, then the content, and a hairline rim last so the edge always wins over whatever
 * scrolled under it. The plate is clipped to [shape], which is a continuous corner everywhere
 * this app applies it - a squircle is not decoration here, it is what makes the rim curve
 * follow the highlight instead of cutting across it.
 *
 * Where the platform can run a runtime shader, miuix-blur sits in front of all of that and
 * does the part a tint cannot fake: it samples the recorded page background and refracts it.
 * The tint then drops to [GlassSpec.tintBlurred] so the refracted content stays visible, and
 * the rim is handed to miuix's own stroke, which draws it from the blurred layer instead of
 * from a gradient that knows nothing about what is behind it. Below API 33 the same modifier
 * is a tinted plate, so the design still holds where blur does not exist.
 *
 * Everything is drawn inside the shape and costs one draw pass.
 *
 * @param shape must be the same shape the caller lays content out with, so the rim lands on
 *   the silhouette rather than a second, nearly identical one.
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    strength: GlassStrength = GlassStrength.Regular,
    rim: Boolean = true,
    specular: Boolean = true,
    sheen: Boolean = true,
    /**
     * Whether the plate may refract the captured page background.
     *
     * Chrome that draws outside the captured layer - a dialog in its own window, the
     * bottom bar that floats over the nav host, a top bar laid out above the content -
     * cannot sample it: the layer is recorded in another window's coordinates and reading
     * it back would smear the page across a surface that is not over the page. Those
     * callers pass false and get the tinted plate, which is the same glass without the
     * part that requires the backdrop.
     */
    refract: Boolean = true,
): Modifier {
    // Resolved before the early return so the remember slot order is identical whether or
    // not glass is on - a conditional remember would desync the slot table on toggle.
    val spec = rememberGlassSpec(strength)
    val backdrop = LocalGlassBackdrop.current
    if (!GlassConfig.enabled) return this.clip(shape)

    val doRim = rim && GlassConfig.rim
    val doSpecular = specular && GlassConfig.specular
    val doSheen = sheen && GlassConfig.sheen
    val blurRadius = GlassConfig.blur
    val blurred = refract && backdrop != null && blurRadius > 0f && GlassConfig.blurEnabled

    return this
        .then(
            if (blurred && backdrop != null) {
                Modifier.textureBlur(
                    backdrop = backdrop,
                    shape = shape,
                    blurRadius = blurRadius,
                    noiseCoefficient = BlurDefaults.NoiseCoefficient,
                    highlight = if (rim) spec.highlight() else null,
                )
            } else {
                Modifier
            }
        )
        .clip(shape)
        .drawWithContent {
            // Body.
            drawRect(Brush.verticalGradient(if (blurred) spec.tintBlurred else spec.tint))

            if (doSheen) {
                // A diagonal that runs across the plate, brightest near the middle, so the
                // fill never reads as an even wash.
                drawRect(
                    Brush.linearGradient(
                        colors = spec.sheen,
                        start = Offset(size.width * 0.1f, 0f),
                        end = Offset(size.width * 0.9f, size.height),
                    )
                )
            }

            if (doSpecular) {
                // Clamped to the top of the plate: a highlight that runs to the bottom edge
                // is a glow, not a reflection.
                drawRect(
                    Brush.verticalGradient(
                        colors = spec.specular,
                        startY = 0f,
                        endY = size.height * spec.specularHeight,
                    )
                )
            }

            drawContent()
        }
        .then(
            // Only the unblurred plate draws its own rim; the refracted one already carries
            // miuix's stroke, and two hairlines disagreeing about the silhouette read as a
            // doubling of the edge rather than as a lit rim.
            if (doRim && !blurred) {
                Modifier.border(
                    width = spec.rimWidth,
                    brush = Brush.verticalGradient(spec.rim),
                    shape = shape,
                )
            } else {
                Modifier
            }
        )
}
