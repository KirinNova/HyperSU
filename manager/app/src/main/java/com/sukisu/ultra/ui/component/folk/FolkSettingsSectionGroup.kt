package com.sukisu.ultra.ui.component.folk

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

/**
 * Convenience that composes a titled [FolkSettingsSection] with a single
 * [FolkSettingsGroup]. Use the two components directly when a section needs a
 * description, multiple groups or extra content.
 *
 * The section title is always rendered by [FolkSettingsSection], i.e. outside
 * the rounded group surface.
 */
@Composable
fun FolkSettingsSectionGroup(
    title: String,
    modifier: Modifier = Modifier,
    flat: Boolean = false,
    highlightKey: String? = null,
    shape: Shape = FolkSettingsDimens.GroupShape,
    content: FolkSettingsGroupScope.() -> Unit,
) {
    val scope = FolkSettingsGroupScope().apply(content)
    // Avoid leaving an orphan section title behind when every item is hidden.
    if (scope.items.none { it.visible }) return

    FolkSettingsSection(title = title, modifier = modifier) {
        FolkSettingsGroupItems(
            items = scope.items,
            flat = flat,
            highlightKey = highlightKey,
            shape = shape,
        )
    }
}
