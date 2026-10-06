package com.sukisu.ultra.ui.component.folk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animate
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.focusable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Whether the current composable is rendered inside a [FolkSettingsGroup].
 * Preferences use this to know that the surrounding surface already provides
 * the card background, so they must not draw their own.
 */
val LocalInsideFolkGroup = compositionLocalOf { false }

data class FolkGroupItem(
    val key: Any?,
    val visible: Boolean,
    val content: @Composable () -> Unit,
)

class FolkSettingsGroupScope {
    internal val items = mutableListOf<FolkGroupItem>()

    fun item(key: Any? = null, visible: Boolean = true, content: @Composable () -> Unit) {
        items.add(FolkGroupItem(key ?: items.size, visible, content))
    }
}

/**
 * A single large rounded surface that hosts a run of related preferences.
 *
 * Every preference in a settings category lives inside one of these groups so
 * the screen reads as a small number of soft "shelves" instead of a long list
 * of individual cards. The group deliberately has no outline and no shadow -
 * it separates itself from the page background with a slightly lighter
 * surface tone.
 *
 * The group carries no vertical margin of its own: inside a settings screen the
 * spacing between groups comes from [FolkSettingsSection]. When stacking groups
 * directly (e.g. one card per app in a list) the caller must add the gap, or the
 * cards will touch.
 */
@Composable
fun FolkSettingsGroup(
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    highlightKey: String? = null,
    shape: Shape = FolkSettingsDimens.GroupShape,
    content: FolkSettingsGroupScope.() -> Unit,
) {
    val scope = FolkSettingsGroupScope().apply(content)
    FolkSettingsGroupItems(
        items = scope.items,
        modifier = modifier,
        flat = flat,
        highlightKey = highlightKey,
        shape = shape,
    )
}

/**
 * Renders an already-built list of group items. Kept separate so callers such
 * as [FolkSettingsSectionGroup] can inspect item visibility before deciding
 * whether the surrounding section should exist at all.
 */
@Composable
internal fun FolkSettingsGroupItems(
    items: List<FolkGroupItem>,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    highlightKey: String? = null,
    shape: Shape = FolkSettingsDimens.GroupShape,
) {
    if (items.none { it.visible }) return

    val containerColor = folkGroupColor()
    val highlightColor = MaterialTheme.colorScheme.primary

    CompositionLocalProvider(LocalInsideFolkGroup provides true) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = FolkSettingsDimens.ScreenPadding),
            shape = shape,
            color = containerColor,
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = FolkSettingsDimens.GroupVerticalPadding)) {
                items.forEach { item ->
                    key(item.key) {
                        AnimatedVisibility(
                            visible = item.visible,
                            enter = expandVertically(
                                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
                                expandFrom = Alignment.Top,
                            ) + fadeIn(animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec()),
                            exit = shrinkVertically(
                                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                shrinkTowards = Alignment.Top,
                            ) + fadeOut(animationSpec = MaterialTheme.motionScheme.fastEffectsSpec()),
                        ) {
                            val highlightInSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
                            val highlightOutSpec = MaterialTheme.motionScheme.slowEffectsSpec<Float>()
                            val isHighlighted =
                                highlightKey != null && item.key?.toString() == highlightKey
                            val focusRequester = remember { FocusRequester() }
                            var highlightAlpha by remember { mutableStateOf(0f) }

                            if (isHighlighted) {
                                LaunchedEffect(Unit) {
                                    runCatching { focusRequester.requestFocus() }
                                    delay(600)
                                    animate(
                                        initialValue = 0f,
                                        targetValue = 0.16f,
                                        animationSpec = highlightInSpec,
                                    ) { value, _ -> highlightAlpha = value }
                                    delay(2000)
                                    animate(
                                        initialValue = 0.16f,
                                        targetValue = 0f,
                                        animationSpec = highlightOutSpec,
                                    ) { value, _ -> highlightAlpha = value }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .then(
                                        if (isHighlighted) {
                                            Modifier.focusRequester(focusRequester).focusable()
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .drawBehind {
                                        if (highlightAlpha > 0.01f) {
                                            drawRect(color = highlightColor.copy(alpha = highlightAlpha))
                                        }
                                    }
                            ) {
                                item.content()
                            }
                        }
                    }
                }
            }
        }
    }
}
