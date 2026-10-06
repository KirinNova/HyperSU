package com.sukisu.ultra.ui.theme.tokens

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.material3.MotionScheme

/**
 * Motion scheme for the "expressive motion" experiment. Slower and springier than
 * [MotionScheme.expressive] so the switch is actually felt, but built from the same
 * spring vocabulary as the Material presets: spatial specs carry the bounce, effects
 * specs stay close to the preset so colors do not wobble.
 */
val FolkExpressiveMotionScheme: MotionScheme = object : MotionScheme {
    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = 250f)

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.55f, stiffness = 600f)

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 150f)

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 1600f)

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.6f, stiffness = 3800f)

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.8f, stiffness = 800f)
}
