package com.sukisu.ultra.ui.theme.glass

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported

/**
 * The page background, captured so a glass plate can refract it.
 *
 * Null everywhere the platform cannot run a runtime shader, which is below API 33 - this
 * app's minSdk is 26 while miuix-blur declares 33, so the plate falls back to the tinted
 * path rather than shipping a component that draws nothing.
 */
val LocalGlassBackdrop = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Records the caller's background into a [LayerBackdrop], or returns null when blur is out
 * of reach.
 *
 * The branch on [isRuntimeShaderSupported] is a device constant, so it takes the same side
 * on every recomposition and the remembers below are never skipped on one pass and read on
 * the next.
 *
 * The captured layer should hold the background only. A plate that samples a layer it is
 * itself drawn into would read back its own output, so consumers live outside the layer -
 * [ProvideGlassBackdrop] hands them the handle through a composition local.
 */
@Composable
fun rememberGlassBackdrop(): LayerBackdrop? {
    if (!isRuntimeShaderSupported()) return null
    val layer = rememberGraphicsLayer()
    return rememberLayerBackdrop(layer) {
        drawContent()
    }
}

/** Publishes [backdrop] to the plates drawn underneath this content. */
@Composable
fun ProvideGlassBackdrop(backdrop: LayerBackdrop?, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlassBackdrop provides backdrop, content = content)
}

/** [layerBackdrop] for a nullable handle, so callers do not branch at every call site. */
fun Modifier.layerBackdropIf(backdrop: LayerBackdrop?): Modifier =
    if (backdrop == null) this else then(layerBackdrop(backdrop))
