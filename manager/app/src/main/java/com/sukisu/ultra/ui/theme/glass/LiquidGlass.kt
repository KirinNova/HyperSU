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

/** Resolved from the ambient scheme so glass tracks the theme the same way the palette does. */
@Composable
fun rememberGlassSpec(strength: GlassStrength = GlassStrength.Regular): GlassSpec {
    val scheme = MaterialTheme.colorScheme
    return remember(scheme, strength) { GlassSpec.from(scheme, strength) }
}

/**
 * Turns a surface into a plate of iOS Liquid Glass.
 *
 * The stack is fixed and the order is the whole effect: body tint, diagonal sheen, specular
 * wash, then the content, and a hairline rim last so the edge always wins over whatever
 * scrolled under it. The plate is clipped to [shape], which is a continuous corner
 * everywhere this app applies it - a squircle is not decoration here, it is what makes the
 * rim curve follow the highlight instead of cutting across it.
 *
 * Everything is drawn inside the shape and costs one draw pass; the ambient layer it sits on
 * is what gives it something to refract.
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
): Modifier {
    val spec = rememberGlassSpec(strength)
    return this
        .clip(shape)
        .drawWithContent {
            // Body.
            drawRect(Brush.verticalGradient(spec.tint))

            if (sheen) {
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

            if (specular) {
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
            if (rim) {
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
