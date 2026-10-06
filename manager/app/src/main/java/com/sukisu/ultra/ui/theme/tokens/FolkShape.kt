package com.sukisu.ultra.ui.theme.tokens

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * The corner radii the app is allowed to use, so a screen picks a step instead of inventing one.
 * A step is named after its radius rather than a role, which keeps "same shape as before"
 * checkable against a screenshot.
 */
object FolkShape {
    /** Icon tiles and fills inside a container. */
    val Corner12: CornerBasedShape = ContinuousCornerShape(12.dp)

    /** Rows and inner cards. */
    val Corner16: CornerBasedShape = ContinuousCornerShape(16.dp)

    /** Group surfaces - the 20dp the settings pages are calibrated against. */
    val Corner20: CornerBasedShape = ContinuousCornerShape(20.dp)

    /** Dialogs and larger inner surfaces. */
    val Corner24: CornerBasedShape = ContinuousCornerShape(24.dp)

    /** Sheets and the outer corner of a grouped list. */
    val Corner28: CornerBasedShape = ContinuousCornerShape(28.dp)

    /**
     * The frame every alert dialog shares. A little softer than [Corner28] so a dialog, which is
     * small on screen, still reads clearly as a floating panel rather than a page.
     */
    val Dialog: CornerBasedShape = ContinuousCornerShape(30.dp)

    /** Capsule. */
    val CornerFull: RoundedCornerShape = RoundedCornerShape(50)

    /**
     * The ladder as Material's five slots, so a component we do not style ourselves sits on the
     * same corner family. The steps go in order, which grows the small end the most - stock 4dp and
     * 8dp read as "cut out" next to a panel - while the top slot keeps 28dp for dialogs and sheets.
     */
    val materialShapes: Shapes = Shapes(
        extraSmall = Corner12,
        small = Corner16,
        medium = Corner20,
        large = Corner24,
        extraLarge = Corner28,
    )
}
