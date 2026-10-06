package com.sukisu.ultra.ui.component.folk

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * The base settings row used across FolkPatch.
 *
 * Layout is intentionally hand-built instead of wrapping [androidx.compose.material3.ListItem]
 * so the icon size, text hierarchy, vertical rhythm and trailing alignment are
 * identical on every screen - and so the same row can host a switch, a value, a
 * chevron or any custom trailing content.
 */
@Composable
fun FolkPreference(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    iconTint: Color = folkIconColor(enabled),
    summary: String? = null,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    val clickModifier = if (onClick != null) {
        Modifier
            .folkPressScale(interactionSource, enabled)
            .combinedClickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                },
                onLongClick = onLongClick,
            )
    } else {
        Modifier
    }

    FolkPreferenceRow(
        title = title,
        modifier = modifier.then(clickModifier),
        icon = icon,
        enabled = enabled,
        iconTint = iconTint,
        summary = summary,
        selected = selected,
        trailing = trailing,
    )
}

@Composable
internal fun FolkPreferenceRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    iconTint: Color = folkIconColor(enabled),
    summary: String? = null,
    selected: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val titleColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val summaryColor = if (enabled) {
        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        else MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
    }

    // Rows grow with their content: a summary adds breathing room, a plain
    // single-line row stays compact. That variation is what stops the list from
    // looking like a grid of identical Material list items.
    val verticalPadding = if (summary != null) {
        FolkSettingsDimens.ItemVerticalPaddingWithSummary
    } else {
        FolkSettingsDimens.ItemVerticalPadding
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            // Every row keeps at least a 56dp touch target, but rows still
            // grow with their content instead of being force-filled.
            .defaultMinSize(minHeight = 56.dp)
            .padding(
                start = FolkSettingsDimens.ItemHorizontalPadding,
                end = FolkSettingsDimens.ItemEndPadding,
                top = verticalPadding,
                bottom = verticalPadding,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(FolkSettingsDimens.IconSize),
            )
            Spacer(Modifier.width(FolkSettingsDimens.IconSpacing))
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = folkPreferenceTitleStyle(),
                color = titleColor,
            )
            if (summary != null) {
                Spacer(Modifier.height(FolkSettingsDimens.TitleSummarySpacing))
                Text(
                    text = summary,
                    style = folkPreferenceSummaryStyle(),
                    color = summaryColor,
                )
            }
        }

        if (trailing != null) {
            Spacer(Modifier.width(FolkSettingsDimens.TrailingSpacing))
            Box(contentAlignment = Alignment.Center) { trailing() }
        }
    }
}

/** A preference that toggles a boolean value with a Material switch. */
@Composable
fun FolkSwitchPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    FolkPreferenceRow(
        title = title,
        modifier = modifier
            .folkPressScale(interactionSource, enabled)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                },
            ),
        icon = icon,
        iconTint = folkIconColor(enabled),
        summary = summary,
        enabled = enabled,
        trailing = {
            com.sukisu.ultra.ui.component.ExpressiveSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
    )
}

/** A preference that opens another screen. */
@Composable
fun FolkNavigationPreference(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    FolkPreference(
        title = title,
        modifier = modifier,
        icon = icon,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        trailing = trailing ?: { FolkChevron(enabled) },
    )
}

/** A preference whose trailing side shows the current value. */
@Composable
fun FolkValuePreference(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    value: String? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
) {
    FolkPreference(
        title = title,
        modifier = modifier,
        icon = icon,
        summary = summary,
        enabled = enabled,
        onClick = onClick,
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value,
                        style = folkPreferenceValueStyle(),
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                    )
                    if (showChevron) Spacer(Modifier.width(6.dp))
                }
                if (showChevron) FolkChevron(enabled)
            }
        },
    )
}

/**
 * A preference that shows the selected value and opens a menu to change it.
 *
 * A tap opens the menu rather than stepping to the next entry: the option lists
 * here are long (the supported locales, the su compat modes) and cycling
 * through them one tap at a time makes a target several taps away - and makes
 * the row's value jump around while the user looks for it.
 */
@Composable
fun FolkChoicePreference(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        FolkPreference(
            title = title,
            modifier = modifier,
            icon = icon,
            summary = summary,
            enabled = enabled,
            onClick = { expanded = true },
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    options.getOrNull(selectedIndex)?.let { value ->
                        Text(
                            text = value,
                            style = folkPreferenceValueStyle(),
                            color = if (enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                            },
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    FolkChevron(enabled)
                }
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                    trailingIcon = {
                        if (index == selectedIndex) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null)
                        }
                    },
                )
            }
        }
    }
}

/** A preference that toggles a boolean value with a checkbox. */
@Composable
fun FolkCheckboxPreference(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    summary: String? = null,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    FolkPreferenceRow(
        title = title,
        modifier = modifier
            .folkPressScale(interactionSource, enabled)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                interactionSource = interactionSource,
                indication = null,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                },
            ),
        icon = icon,
        iconTint = folkIconColor(enabled),
        summary = summary,
        enabled = enabled,
        trailing = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
    )
}

@Composable
fun FolkChevron(enabled: Boolean = true) {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = if (enabled) 0.7f else 0.3f,
        ),
        modifier = Modifier.size(FolkSettingsDimens.ChevronSize),
    )
}
