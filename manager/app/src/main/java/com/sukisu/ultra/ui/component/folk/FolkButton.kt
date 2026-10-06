package com.sukisu.ultra.ui.component.folk

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.sukisu.ultra.ui.theme.tokens.FolkMotion
import com.sukisu.ultra.ui.theme.tokens.FolkTheme

/**
 * The three button roles. The disabled state keeps its label readable - an inset
 * fill with the secondary text colour rather than a washed-out grey - because a
 * disabled button here usually still says what it is waiting for.
 */
object FolkButtonDefaults {
    /** Primary action. */
    @Composable
    fun filledColors(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        disabledContainerColor = FolkTheme.palette.groupedInset,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    /** Secondary action: an inset fill with the accent for the label. */
    @Composable
    fun tonalColors(): ButtonColors = ButtonDefaults.buttonColors(
        containerColor = FolkTheme.palette.groupedInset,
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContainerColor = FolkTheme.palette.groupedInset,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    /** Emphasised action: a light wash of the accent. */
    @Composable
    fun tintedColors(): ButtonColors {
        val accent = MaterialTheme.colorScheme.primary
        return ButtonDefaults.buttonColors(
            containerColor = accent.copy(alpha = 0.14f),
            contentColor = accent,
            disabledContainerColor = FolkTheme.palette.groupedInset,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    /** Text action with a disabled label that still says what is unavailable. */
    @Composable
    fun textColors(): ButtonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.primary,
        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    val LargeContentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
}

/**
 * Scale on press instead of a ripple. Pair it with `indication = null` on the
 * clickable and the same [MutableInteractionSource]; a press must still feel
 * like something, so callers should also fire a haptic.
 */
@Composable
fun Modifier.folkPressScale(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) FolkMotion.PressedScale else 1f,
        // Squeeze in fast so even a quick tap reads; release uses the spring.
        animationSpec = if (pressed && enabled) FolkMotion.PressDown else FolkMotion.PressScale,
        label = "folkPressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
