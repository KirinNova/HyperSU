package com.sukisu.ultra.ui.theme.glass

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * The light the glass has to refract.
 *
 * Glass over a flat fill looks like a tinted card, because there is nothing behind it to
 * bend. iOS gets its depth from the wallpaper under the UI, so the page needs the same
 * thing: a handful of very low-frequency washes in the scheme's own accents, bright enough
 * that a plate sitting over one edge visibly picks up the change, and dim enough that body
 * text contrast on the page is untouched.
 *
 * The blobs are deliberately enormous and soft - a readable gradient step on a settings row
 * would fight the labels, so nothing smaller than the screen is drawn here.
 */
@Composable
fun Modifier.glassAmbient(): Modifier {
    val scheme = MaterialTheme.colorScheme
    // A page with a wallpaper already supplies its own backdrop; lay nothing over it.
    val onCustomBackground = scheme.background.alpha < 0.99f
    if (onCustomBackground) return this

    val dark = scheme.background.luminance() < 0.5f
    val strength = if (dark) 0.20f else 0.11f
    val accent = scheme.primary
    val secondary = scheme.secondary
    val tertiary = scheme.tertiary

    return this.drawBehind {
        drawRect(scheme.background)

        // Anchor each wash off the corners so the middle of the screen stays calm; that is
        // where rows and their labels live.
        drawCircle(
            brush = radial(accent, strength),
            radius = size.width * 0.75f,
            center = Offset(size.width * 0.12f, size.height * 0.06f),
        )
        drawCircle(
            brush = radial(tertiary, strength * 0.8f),
            radius = size.width * 0.70f,
            center = Offset(size.width * 0.94f, size.height * 0.42f),
        )
        drawCircle(
            brush = radial(secondary, strength * 0.6f),
            radius = size.width * 0.80f,
            center = Offset(size.width * 0.24f, size.height * 0.96f),
        )
    }
}

private fun radial(color: Color, alpha: Float): Brush = Brush.radialGradient(
    colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
)
