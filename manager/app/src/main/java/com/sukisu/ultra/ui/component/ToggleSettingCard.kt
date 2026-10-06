package com.sukisu.ultra.ui.component

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.sukisu.ultra.ui.component.folk.folkPressScale

@Composable
fun ToggleSettingCard(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    flat: Boolean = false,
    icon: ImageVector? = null,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    ExpressiveCard(flat = flat) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .folkPressScale(interactionSource, enabled)
                .toggleable(
                    value = checked,
                    onValueChange = {
                        if (enabled) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onCheckedChange(it)
                        }
                    },
                    role = Role.Switch,
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                )
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(16.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                )
            }

            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        }
    }
}
