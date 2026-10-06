package com.sukisu.ultra.ui.component

import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sukisu.ultra.ui.component.folk.folkPressScale

@Composable
fun CheckboxItem(
    icon: ImageVector?,
    title: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (summary != null) {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else null,
        leadingContent = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            }
        } else null,
        trailingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .folkPressScale(interactionSource, enabled)
            .toggleable(
                value = checked,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                },
                role = Role.Checkbox,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
            )
    )
}

@Composable
fun SwitchItem(
    icon: ImageVector?,
    title: String,
    summary: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (summary != null) {
            {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else null,
        leadingContent = if (icon != null) {
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null
                )
            }
        } else null,
        trailingContent = {
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .folkPressScale(interactionSource, enabled)
            .toggleable(
                value = checked,
                onValueChange = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(it)
                },
                role = Role.Switch,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
            )
    )
}
